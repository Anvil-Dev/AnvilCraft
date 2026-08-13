package dev.dubhe.anvilcraft.client.gui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.lib.v2.util.ListUtil;
import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.anvilcraft.lib.v2.util.Scrollable;
import dev.dubhe.anvilcraft.block.entity.AutoEnchantingTableBlockEntity;
import dev.dubhe.anvilcraft.client.gui.component.FluidDisplayWidget;
import dev.dubhe.anvilcraft.constant.Constant;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.inventory.AutoEnchantingTableMenu;
import dev.dubhe.anvilcraft.network.AutoEnchantingTableFluidPacket;
import dev.dubhe.anvilcraft.network.AutoEnchantingTableSyncPacket;
import dev.dubhe.anvilcraft.util.EnchantmentData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BucketItem;
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
    private static final int WARNING_DURATION_TICKS = 80;
    private static final int WARNING_TEXT_COLOR = 0xBFFF5555;
    private @Nullable Component warningMessage;
    private int warningTicks = 0;
    private int warningIndex = -1;

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
        this.searchBox.setCanLoseFocus(true);
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
        if (this.warningTicks > 0) {
            this.warningTicks--;
            if (this.warningTicks == 0) {
                this.warningMessage = null;
                this.warningIndex = -1;
            }
        }
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
        // 未放置允许的引物时，不显示自选附魔
        ItemStack primer = this.menu.getBlockEntity().getItemHandler()
            .getStackInSlot(AutoEnchantingTableBlockEntity.SLOT_PRIMER);
        if (primer.isEmpty() || !this.menu.getBlockEntity().isAllowedPrimer(primer)) {
            return;
        }
        if (this.filteredIndexes.isEmpty()) return;
        int visible = Math.min(this.filteredIndexes.size() - this.head, 10);
        PoseStack pose = guiGraphics.pose();
        for (int j = 0; j < visible; j++) {
            int index = this.head + j;
            int original = this.filteredIndexes.get(index);
            int x = this.leftPos + 47 + 18 * (index % 5);
            int y = this.topPos + 32 + 18 * ((index / 5) % 2);

            EnchantmentData data = ListUtil.safelyGet(this.menu.getEnchantments(), original).orElse(null);
            if (data == null) continue;

            ItemStack willRender = EnchantedBookItem.createForEnchantment(data.toEnchantmentInst());

            int offsetV = 0;

            boolean selected = this.menu.getSelectedIndexes().contains(original);
            if (selected) offsetV = 18;

            if (MathUtil.isInRange(mouseX, mouseY, x, y, x + 18, y + 18)) {
                offsetV = 36;
                this.renderingTooltipEnchantedBook = willRender;
            }

            guiGraphics.blit(SharedTextures.SWITCH_TABLE_BUTTON, x, y, 0, offsetV, 18, 18, 18, 54);
            pose.pushPose();
            pose.translate(0, 0, -150);
            pose.scale(1, 1, -15.9F);
            guiGraphics.renderItem(willRender, x + 1, y + (selected ? 1 : 0), (int) (partialTick * 100));
            pose.popPose();

            // 条件不满足被拒的按钮：红色呼吸灯效果
            if (this.warningTicks > 0 && index == this.warningIndex) {
                long gameTime = this.minecraft.level != null ? this.minecraft.level.getGameTime() : 0;
                float breathe = (Mth.sin(gameTime * 0.2f) + 1.0f) / 2.0f;
                int alpha = 0x80 + (int) (0x40 * breathe);
                guiGraphics.fill(x, y, x + 18, y + 18, (alpha << 24) | 0xFF5555);
            }
        }
        // 条件不满足时的警告文字（自选附魔区中央，75% 不透明红色，4 秒）
        if (this.warningTicks > 0 && this.warningMessage != null) {
            int centerX = this.leftPos + 92;
            int centerY = this.topPos + 50;
            guiGraphics.drawString(
                this.font,
                this.warningMessage,
                centerX - this.font.width(this.warningMessage) / 2,
                centerY - this.font.lineHeight / 2,
                WARNING_TEXT_COLOR,
                false
            );
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
            if (this.searchBox != null && !this.insideSearchBox(mouseX, mouseY)) {
                this.searchBox.setFocused(false);
            }
            if (this.insideScrollbar(mouseX, mouseY)) {
                this.scrollable.scrolling();
                return true;
            }
            // 点击流体槽且手持桶（空桶/经验流体桶）→ 服务端桶交互
            if (
                this.insideFluidDisplay(mouseX, mouseY)
                && this.menu.getInventory().getSelected().getItem() instanceof BucketItem
            ) {
                PacketDistributor.sendToServer(new AutoEnchantingTableFluidPacket(this.menu.getBlockEntity().getBlockPos()));
                return true;
            }
            int visible = Math.min(this.filteredIndexes.size() - this.head, 10);
            for (int j = 0; j < visible; j++) {
                int index = this.head + j;
                int x = this.leftPos + 47 + 18 * (index % 5);
                int y = this.topPos + 32 + 18 * ((index / 5) % 2);
                if (!MathUtil.isInRange(mouseX, mouseY, x, y, x + 18, y + 18)) continue;
                int original = this.filteredIndexes.get(index);
                if (this.menu.getSelectedIndexes().contains(original)) {
                    this.menu.unselect(original);
                    PacketDistributor.sendToServer(new AutoEnchantingTableSyncPacket(original, false));
                } else {
                    EnchantmentData data = ListUtil.safelyGet(this.menu.getEnchantments(), original).orElse(null);
                    if (data != null && !this.canSelect(index, data)) {
                        // 条件不满足：按钮不被按下，显示警告
                        return true;
                    }
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

    private boolean canSelect(int index, EnchantmentData data) {
        int newTotal = this.getSelectedTotalLevel() + data.level();
        // 附魔总等级不能超过书架数量
        if (newTotal > this.menu.getBlockEntity().getShelfLevel()) {
            this.showWarning(index,
                Component.translatable("screen.anvilcraft.auto_enchanting_table.warning.bookshelf"));
            return false;
        }
        // 消耗经验流体不能超过罐内最大储量
        if (newTotal * AutoEnchantingTableBlockEntity.EXP_COST_PER_SHELF
            > AutoEnchantingTableBlockEntity.FLUID_CAPACITY) {
            this.showWarning(index,
                Component.translatable("screen.anvilcraft.auto_enchanting_table.warning.fluid_capacity"));
            return false;
        }
        return true;
    }

    private int getSelectedTotalLevel() {
        int total = 0;
        for (int i : this.menu.getSelectedIndexes()) {
            EnchantmentData data = ListUtil.safelyGet(this.menu.getEnchantments(), i).orElse(null);
            if (data != null) total += data.level();
        }
        return total;
    }

    private void showWarning(int index, Component message) {
        this.warningIndex = index;
        this.warningMessage = message;
        this.warningTicks = WARNING_DURATION_TICKS;
    }

    protected boolean insideSearchBox(double mouseX, double mouseY) {
        int left = this.leftPos + 46;
        int top = this.topPos + 17;
        int right = left + 99;
        int down = top + 12;
        return MathUtil.isInRange(mouseX, mouseY, left, top, right, down);
    }

    protected boolean insideFluidDisplay(double mouseX, double mouseY) {
        int left = this.leftPos + 152;
        int top = this.topPos + 17;
        int right = left + 16;
        int down = top + 54;
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
