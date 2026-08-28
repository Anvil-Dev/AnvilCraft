package dev.dubhe.anvilcraft.client.gui.screen;

import dev.anvilcraft.lib.v2.util.ListUtil;
import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.anvilcraft.lib.v2.util.Scrollable;
import dev.dubhe.anvilcraft.constant.Constant;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.inventory.TranscendenceGrindstoneMenu;
import dev.dubhe.anvilcraft.network.TranscendenceGrindstoneSyncPacket;
import dev.dubhe.anvilcraft.util.EnchantmentData;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

public class TranscendenceGrindstoneScreen extends AbstractContainerScreen<TranscendenceGrindstoneMenu> {
    private static final Identifier BACKGROUND = SharedTextures.bg("crafting", "transcendence_grindstone");
    private static final int MODIFIER_SLOT_INDEX = 1;
    private static final Component MODIFIER_SLOT_TOOLTIP = Component.translatable(
        "screen.anvilcraft.transcendence_grindstone.modifier_slot.tooltip"
    );

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
    private @Nullable ItemStack renderingTooltipEnchantedBook;

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
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractContents(graphics, mouseX, mouseY, partialTick);
        if (!this.menu.isGoldMode()) {
            this.extractEnchantmentSelectingArea(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF404040, false);
        if (this.menu.isGoldMode() && this.menu.getSlot(2).hasItem()) {
            this.extractGoldLabels(graphics);
            return;
        }

        int cost = this.menu.getCost();
        Slot result = this.menu.getSlot(2);
        if (cost <= 0 || !result.hasItem()) return;

        Component component = Component.translatable("screen.anvilcraft.ember_grindstone.cost", cost);
        int textColor = result.mayPickup(this.player) ? 0x80ff20 : 0xff6060;
        int x = this.getImageWidth() - 1 - this.font.width(component) - 2;
        graphics.fill(x - 2, 65, this.getImageWidth() - 1, 76, 0x4f000000);
        graphics.text(this.font, component, x, 66, textColor);
    }

    private void extractGoldLabels(GuiGraphicsExtractor graphics) {
        this.drawGoldLabel(
            graphics,
            Component.translatable("screen.anvilcraft.royal_grindstone.will_remove"),
            27
        );
        this.drawGoldLabel(
            graphics,
            Component.translatable(
                "screen.anvilcraft.transcendence_grindstone.penalty",
                this.menu.getRemovedRepairCost(),
                this.menu.getTotalRepairCost()
            ),
            38
        );
        this.drawGoldLabel(
            graphics,
            Component.translatable(
                "screen.anvilcraft.royal_grindstone.curse_count",
                this.menu.getRemovedCurseCount(),
                this.menu.getTotalCurseCount()
            ),
            49
        );
    }

    private void drawGoldLabel(GuiGraphicsExtractor graphics, Component component, int y) {
        float scale = 0.75f;
        graphics.pose().pushMatrix();
        graphics.pose().scale(scale, scale);
        graphics.text(this.font, component, (int) (66 / scale), (int) (y / scale), 0xFF80FF20, false);
        graphics.pose().popMatrix();
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int x, int y) {
        if (this.menu.getCarried().isEmpty() && this.hoveredSlot != null) {
            if (this.hoveredSlot.hasItem()) {
                ItemStack stack = this.hoveredSlot.getItem();
                graphics.setTooltipForNextFrame(
                    this.font,
                    this.getTooltipFromContainerItem(stack),
                    stack.getTooltipImage(),
                    stack,
                    x,
                    y
                );
                return;
            }
            if (this.hoveredSlot.index == TranscendenceGrindstoneScreen.MODIFIER_SLOT_INDEX) {
                graphics.setTooltipForNextFrame(this.font, this.font.split(TranscendenceGrindstoneScreen.MODIFIER_SLOT_TOOLTIP, 150), x, y);
                return;
            }
        }
        if (this.renderingTooltipEnchantedBook != null) {
            graphics.setTooltipForNextFrame(
                this.font,
                this.getTooltipFromContainerItem(this.renderingTooltipEnchantedBook),
                this.renderingTooltipEnchantedBook.getTooltipImage(),
                this.renderingTooltipEnchantedBook,
                x,
                y
            );
        }
    }

    private void extractEnchantmentSelectingArea(
        GuiGraphicsExtractor graphics,
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

            ItemStack renderedBook = EnchantmentHelper.createBook(data.toEnchantmentInst());
            boolean selected = this.menu.getSelectedIndexes().contains(index);
            int textureOffset = selected ? 18 : 0;
            if (MathUtil.isInRange(mouseX, mouseY, x, y, x + 18, y + 18)) {
                textureOffset = 36;
                this.renderingTooltipEnchantedBook = renderedBook;
            }

            graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                SharedTextures.SWITCH_TABLE_BUTTON,
                x,
                y,
                0,
                textureOffset,
                18,
                18,
                18,
                54
            );
            graphics.item(renderedBook, x + 1, y + (selected ? 1 : 0), (int) (partialTick * 100));
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            TranscendenceGrindstoneScreen.BACKGROUND,
            this.leftPos,
            this.topPos,
            0,
            0,
            this.getImageWidth(),
            this.getImageHeight(),
            256,
            256
        );
        if (!this.menu.isGoldMode() && this.scrollable.canScroll()) {
            int left = this.leftPos + 122;
            int top = this.topPos + 23;
            int bottom = top + 36;
            graphics.blit(
                RenderPipelines.GUI_TEXTURED,
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
    public void resize(int width, int height) {
        this.scrollable.calculateScroll(this.head / 3);
        this.init(width, height);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        if (event.button() == 0 && !this.menu.isGoldMode()) {
            if (this.insideScrollbar(event.x(), event.y())) {
                this.scrollable.scrolling();
                return true;
            }
            int visibleEnchantments = Math.min(this.menu.getEnchantments().size() - this.head, 6);
            for (int index = this.head; index < this.head + visibleEnchantments; index++) {
                int x = this.leftPos + 65 + 18 * (index % 3);
                int y = this.topPos + 23 + 18 * ((index - this.head) / 3);
                if (!MathUtil.isInRange(event.x(), event.y(), x, y, x + 18, y + 18)) continue;

                if (this.minecraft != null) {
                    this.minecraft.getSoundManager().play(
                        SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f)
                    );
                }
                boolean select = !this.menu.getSelectedIndexes().contains(index);
                if (select) {
                    this.menu.select(index);
                } else {
                    this.menu.unselect(index);
                }
                ClientPacketDistributor.sendToServer(new TranscendenceGrindstoneSyncPacket(index, select));
                return true;
            }
        }
        return super.mouseClicked(event, handled);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && this.scrollable.isScrolling()) {
            this.scrollable.notScrolling();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (this.scrollable.isScrolling()) {
            int top = this.topPos + 23;
            this.scrollable.scrollOnDrag(12, event.y(), top, top + 36);
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
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
