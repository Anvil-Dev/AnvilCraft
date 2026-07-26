package dev.dubhe.anvilcraft.client.gui.screen;

import dev.anvilcraft.lib.v2.util.ListUtil;
import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.anvilcraft.lib.v2.util.Scrollable;
import dev.dubhe.anvilcraft.constant.Constant;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.inventory.TranscendenceGrindstoneMenu;
import dev.dubhe.anvilcraft.network.TranscendenceGrindstoneSyncPacket;
import dev.dubhe.anvilcraft.util.EnchantmentData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class TranscendenceGrindstoneScreen extends AbstractContainerScreen<TranscendenceGrindstoneMenu> {
    private static final ResourceLocation BACKGROUND = SharedTextures.bg("crafting", "transcendence_grindstone");

    private final Player player;
    private final Scrollable scrollable = new Scrollable() {
        @Override
        public int row() {
            return 2;
        }

        @Override
        public int column() {
            return 3;
        }

        @Override
        public int size() {
            return TranscendenceGrindstoneScreen.this.menu.getEnchantments().size();
        }

        @Override
        public void setHead(int head) {
            TranscendenceGrindstoneScreen.this.head = head;
        }
    };
    private int head;
    private ItemStack renderingTooltipEnchantedBook;

    public TranscendenceGrindstoneScreen(
        TranscendenceGrindstoneMenu menu,
        Inventory playerInventory,
        @SuppressWarnings("unused") Component title
    ) {
        super(menu, playerInventory, Component.translatable("screen.anvilcraft.transcendence_grindstone.title"));
        this.player = playerInventory.player;
    }

    @Override
    protected void containerTick() {
        this.renderingTooltipEnchantedBook = null;
        if (this.head >= this.menu.getEnchantments().size()) this.scrollable.calculateScroll(0);
        super.containerTick();
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        this.titleLabelY = Constant.SCREEN_TITLE_Y;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (!this.menu.isGoldMode()) {
            this.renderEnchantmentSelectingArea(guiGraphics, mouseX, mouseY, partialTick);
        }
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        if (this.menu.isGoldMode() && this.menu.getSlot(2).hasItem()) {
            this.renderGoldLabels(guiGraphics);
            return;
        }

        int cost = this.menu.getCost();
        Slot result = this.menu.getSlot(2);
        if (cost <= 0 || !result.hasItem()) return;

        Component component = Component.translatable("screen.anvilcraft.ember_grindstone.cost", cost);
        int textColor = result.mayPickup(this.player) ? 0x80ff20 : 0xff6060;
        int x = this.imageWidth - 1 - this.font.width(component) - 2;
        guiGraphics.fill(x - 2, 65, this.imageWidth - 1, 76, 0x4f000000);
        guiGraphics.drawString(this.font, component, x, 66, textColor);
    }

    private void renderGoldLabels(GuiGraphics guiGraphics) {
        this.drawGoldLabel(
            guiGraphics,
            Component.translatable("screen.anvilcraft.royal_grindstone.will_remove"),
            11
        );
        this.drawGoldLabel(
            guiGraphics,
            Component.translatable(
                "screen.anvilcraft.royal_grindstone.repair_cost",
                this.menu.getRemovedRepairCost(),
                this.menu.getTotalRepairCost()
            ),
            22
        );
        this.drawGoldLabel(
            guiGraphics,
            Component.translatable(
                "screen.anvilcraft.royal_grindstone.curse_count",
                this.menu.getRemovedCurseCount(),
                this.menu.getTotalCurseCount()
            ),
            33
        );
        this.drawGoldLabel(
            guiGraphics,
            Component.translatable("screen.anvilcraft.royal_grindstone.gold_cost", this.menu.getUsedGold()),
            44
        );
    }

    private void drawGoldLabel(GuiGraphics guiGraphics, Component component, int y) {
        float scale = 0.75f;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(scale, scale, 1.0f);
        guiGraphics.drawString(this.font, component, (int) (65 / scale), (int) (y / scale), 8453920, false);
        guiGraphics.pose().popPose();
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        if (this.menu.getCarried().isEmpty() && this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            ItemStack stack = this.hoveredSlot.getItem();
            guiGraphics.renderTooltip(
                this.font,
                this.getTooltipFromContainerItem(stack),
                stack.getTooltipImage(),
                stack,
                x,
                y
            );
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

    private void renderEnchantmentSelectingArea(
        GuiGraphics guiGraphics,
        int mouseX,
        int mouseY,
        float partialTick
    ) {
        this.renderingTooltipEnchantedBook = null;
        int visibleEnchantments = Math.min(this.menu.getEnchantments().size() - this.head, 6);
        for (int index = this.head; index < this.head + visibleEnchantments; index++) {
            int x = this.leftPos + 65 + 18 * (index % 3);
            int y = this.topPos + 23 + 18 * ((index - this.head) / 3);
            EnchantmentData data = ListUtil.safelyGet(this.menu.getEnchantments(), index).orElse(null);
            if (data == null) continue;

            ItemStack renderedBook = EnchantedBookItem.createForEnchantment(data.toEnchantmentInst());
            boolean selected = this.menu.getSelectedIndexes().contains(index);
            int textureOffset = selected ? 18 : 0;
            if (MathUtil.isInRange(mouseX, mouseY, x, y, x + 18, y + 18)) {
                textureOffset = 36;
                this.renderingTooltipEnchantedBook = renderedBook;
            }

            guiGraphics.blit(SharedTextures.SWITCH_TABLE_BUTTON, x, y, 0, textureOffset, 18, 18, 18, 54);
            guiGraphics.renderItem(renderedBook, x + 1, y + (selected ? 1 : 0), (int) (partialTick * 100));
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        if (!this.menu.isGoldMode() && this.scrollable.canScroll()) {
            int left = this.leftPos + 122;
            int top = this.topPos + 23;
            int bottom = top + 36;
            guiGraphics.blit(
                SharedTextures.SWITCH_TABLE_SLIDER,
                left,
                top + (int) ((bottom - top - 12) * this.scrollable.getScrollOffs()),
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
        this.scrollable.calculateScroll(this.head / 3);
        this.init(minecraft, width, height);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && !this.menu.isGoldMode()) {
            if (this.insideScrollbar(mouseX, mouseY)) {
                this.scrollable.scrolling();
                return true;
            }
            int visibleEnchantments = Math.min(this.menu.getEnchantments().size() - this.head, 6);
            for (int index = this.head; index < this.head + visibleEnchantments; index++) {
                int x = this.leftPos + 65 + 18 * (index % 3);
                int y = this.topPos + 23 + 18 * ((index - this.head) / 3);
                if (!MathUtil.isInRange(mouseX, mouseY, x, y, x + 18, y + 18)) continue;

                Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f)
                );
                boolean select = !this.menu.getSelectedIndexes().contains(index);
                if (select) {
                    this.menu.select(index);
                } else {
                    this.menu.unselect(index);
                }
                PacketDistributor.sendToServer(new TranscendenceGrindstoneSyncPacket(index, select));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
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
            int top = this.topPos + 23;
            this.scrollable.scrollOnDrag(12, mouseY, top, top + 36);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.menu.isGoldMode() || !this.scrollable.canScroll()) return false;
        this.scrollable.scrollOnScroll(scrollY / 1.2);
        return true;
    }

    private boolean insideScrollbar(double mouseX, double mouseY) {
        int left = this.leftPos + 122;
        int top = this.topPos + 23;
        return this.scrollable.canScroll() && MathUtil.isInRange(mouseX, mouseY, left, top, left + 4, top + 36);
    }
}
