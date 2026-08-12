package dev.dubhe.anvilcraft.client.gui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import dev.anvilcraft.lib.v2.util.ListUtil;
import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.anvilcraft.lib.v2.util.Scrollable;
import dev.dubhe.anvilcraft.client.gui.component.FluidDisplayWidget;
import dev.dubhe.anvilcraft.constant.Constant;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.inventory.AutoEnchantingTableMenu;
import dev.dubhe.anvilcraft.network.AutoEnchantingTableSyncPacket;
import dev.dubhe.anvilcraft.util.EnchantmentData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

public class AutoEnchantingTableScreen extends AbstractContainerScreen<AutoEnchantingTableMenu> {
    private static final ResourceLocation BACKGROUND = SharedTextures.bg("machine", "auto_enchanting_table");

    private final List<Integer> filteredIndexes = new ArrayList<>();
    private final Scrollable scrollable = new Scrollable() {
        @Override
        public int row() {
            return 2;
        }

        @Override
        public int column() {
            return 5;
        }

        @Override
        public int size() {
            return AutoEnchantingTableScreen.this.filteredIndexes.size();
        }

        @Override
        public void setHead(int head) {
            AutoEnchantingTableScreen.this.head = head;
        }
    };
    private int head = 0;
    private String filterText = "";
    private @Nullable EditBox searchBox;
    private @Nullable ItemStack renderingTooltipEnchantedBook;

    public AutoEnchantingTableScreen(
        AutoEnchantingTableMenu menu,
        Inventory playerInventory,
        Component title
    ) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        this.titleLabelY = Constant.SCREEN_TITLE_Y;

        this.searchBox = new EditBox(
            this.font,
            this.leftPos + 59,
            this.topPos + 19,
            83,
            9,
            Component.translatable("screen.anvilcraft.auto_enchanting_table.search")
        );
        this.searchBox.setBordered(false);
        this.searchBox.setMaxLength(64);
        this.searchBox.setValue(this.filterText);
        this.searchBox.setResponder(this::onSearchTextChange);
        this.addRenderableWidget(this.searchBox);

        this.addRenderableWidget(new FluidDisplayWidget(
            this.leftPos + 152,
            this.topPos + 17,
            16,
            54,
            this.menu.getBlockEntity().getFluidHandler(),
            handler -> Component.literal(""),
            (mouseX, mouseY, button) -> {
            }
        ));
    }

    private void onSearchTextChange(String text) {
        this.filterText = text;
        this.refreshFilter();
        this.scrollable.reset();
    }

    private void refreshFilter() {
        this.filteredIndexes.clear();
        String query = this.filterText.toLowerCase();
        List<EnchantmentData> enchantments = this.menu.getEnchantments();
        for (int i = 0; i < enchantments.size(); i++) {
            EnchantmentData data = enchantments.get(i);
            String name = data.enchantment().value().description().getString().toLowerCase();
            String id = data.enchantment().getRegisteredName().toLowerCase();
            if (query.isEmpty() || name.contains(query) || id.contains(query)) {
                this.filteredIndexes.add(i);
            }
        }
    }

    @Override
    protected void containerTick() {
        this.renderingTooltipEnchantedBook = null;
        super.containerTick();
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderEnchantmentSelectingArea(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        if (this.menu.getCarried().isEmpty() && this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            ItemStack stack = this.hoveredSlot.getItem();
            guiGraphics.renderTooltip(this.font, this.getTooltipFromContainerItem(stack), stack.getTooltipImage(), stack, x, y);
        } else if (this.renderingTooltipEnchantedBook != null) {
            guiGraphics.renderTooltip(
                this.font,
                this.getTooltipFromContainerItem(this.renderingTooltipEnchantedBook),
                this.renderingTooltipEnchantedBook.getTooltipImage(),
                this.renderingTooltipEnchantedBook,
                x,
                y
            );
        }
    }

    protected void renderEnchantmentSelectingArea(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderingTooltipEnchantedBook = null;
        if (this.filteredIndexes.isEmpty()) return;
        int visible = Math.min(this.filteredIndexes.size() - this.head, 10);
        for (int j = 0; j < visible; j++) {
            int index = this.head + j;
            int original = this.filteredIndexes.get(index);
            int x = this.leftPos + 47 + 18 * (index % 5);
            int y = this.topPos + 32 + 18 * (index / 5);

            EnchantmentData data = ListUtil.safelyGet(this.menu.getEnchantments(), original).orElse(null);
            if (data == null) continue;

            ItemStack willRender = EnchantedBookItem.createForEnchantment(data.toEnchantmentInst());

            int offsetV = 0;
            if (MathUtil.isInRange(mouseX, mouseY, x, y, x + 18, y + 18)) {
                offsetV = 36;
                this.renderingTooltipEnchantedBook = willRender;
            }

            boolean selected = this.menu.getSelectedIndexes().contains(original);
            if (selected) offsetV = 18;

            guiGraphics.blit(SharedTextures.SWITCH_TABLE_BUTTON, x, y, 0, offsetV, 18, 18, 18, 54);
            guiGraphics.renderItem(willRender, x + 1, y + (selected ? 1 : 0), (int) (partialTick * 100));
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        if (this.scrollable.canScroll()) {
            int left = this.leftPos + 140;
            int top = this.topPos + 32;
            int down = top + 36;
            guiGraphics.blit(
                SharedTextures.SWITCH_TABLE_SLIDER,
                left,
                top + (int) ((down - top - 12) * this.scrollable.getScrollOffs()),
                0,
                0,
                4,
                12,
                8,
                12
            );
        }
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        this.scrollable.calculateScroll(this.head / 5);
        this.init(minecraft, width, height);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (this.searchBox != null) {
                boolean focusing = this.insideSearchBox(mouseX, mouseY);
                boolean changed = this.searchBox.isFocused() == focusing;
                this.searchBox.setFocused(focusing);
                if (changed) {
                    return true;
                }
            }
            if (this.insideScrollbar(mouseX, mouseY)) {
                this.scrollable.scrolling();
                return true;
            }
            int visible = Math.min(this.filteredIndexes.size() - this.head, 10);
            for (int j = 0; j < visible; j++) {
                int index = this.head + j;
                int x = this.leftPos + 47 + 18 * (index % 5);
                int y = this.topPos + 32 + 18 * (index / 5);
                if (!MathUtil.isInRange(mouseX, mouseY, x, y, x + 18, y + 18)) continue;
                int original = this.filteredIndexes.get(index);
                if (this.menu.getSelectedIndexes().contains(original)) {
                    this.menu.unselect(original);
                    PacketDistributor.sendToServer(new AutoEnchantingTableSyncPacket(original, false));
                } else {
                    this.menu.select(original);
                    PacketDistributor.sendToServer(new AutoEnchantingTableSyncPacket(original, true));
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchBox != null && this.searchBox.isFocused()) {
            // 搜索框聚焦时按一次 Esc：取消聚焦；再按一次才退出
            if (keyCode == InputConstants.KEY_ESCAPE) {
                this.searchBox.setFocused(false);
                return true;
            }
            // 搜索框聚焦时完全接替键盘输入
            this.searchBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.searchBox == null) return super.charTyped(codePoint, modifiers);
        if (this.searchBox.isFocused()) {
            return this.searchBox.charTyped(codePoint, modifiers);
        }
        // 输入时搜索框完全接替：自动聚焦并输入
        this.searchBox.setFocused(true);
        return this.searchBox.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.scrollable.isScrolling()) {
            this.scrollable.notScrolling();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.scrollable.isScrolling()) {
            int top = this.topPos + 32;
            this.scrollable.scrollOnDrag(12, mouseY, top, top + 36);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!this.scrollable.canScroll()) return false;
        this.scrollable.scrollOnScroll(scrollY / 1.2);
        return true;
    }

    protected boolean insideSearchBox(double mouseX, double mouseY) {
        int left = this.leftPos + 46;
        int top = this.topPos + 17;
        int right = left + 99;
        int down = top + 12;
        return MathUtil.isInRange(mouseX, mouseY, left, top, right, down);
    }

    protected boolean insideScrollbar(double mouseX, double mouseY) {
        int left = this.leftPos + 140;
        int top = this.topPos + 32;
        int right = left + 4;
        int down = top + 36;
        return MathUtil.isInRange(mouseX, mouseY, left, top, right, down);
    }
}
