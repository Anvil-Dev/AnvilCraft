package dev.dubhe.anvilcraft.client.gui.screen;

import dev.anvilcraft.lib.v2.util.ListUtil;
import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.anvilcraft.lib.v2.util.Scrollable;
import dev.dubhe.anvilcraft.constant.Constant;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.inventory.FrostGrindstoneMenu;
import dev.dubhe.anvilcraft.network.FrostGrindstoneSyncPacket;
import dev.dubhe.anvilcraft.util.EnchantmentData;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

public class FrostGrindstoneScreen extends AbstractContainerScreen<FrostGrindstoneMenu> {
    private static final Identifier BACKGROUND = SharedTextures.bg("crafting", "frost_grindstone");

    private final FrostGrindstoneMenu menu;
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
            return FrostGrindstoneScreen.this.menu.getEnchantments().size();
        }

        @Override
        public void setHead(int head) {
            FrostGrindstoneScreen.this.head = head;
        }
    };
    private int head = 0;
    private @Nullable ItemStack renderingTooltipEnchantedBook;

    public FrostGrindstoneScreen(FrostGrindstoneMenu menu, Inventory playerInventory, @SuppressWarnings("unused") Component title) {
        super(menu, playerInventory, Component.translatable("screen.anvilcraft.frost_grindstone.title"));
        this.menu = menu;
    }

    @Override
    protected void containerTick() {
        this.renderingTooltipEnchantedBook = null;

        super.containerTick();
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF404040, false);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.getImageWidth() - this.font.width(this.title)) / 2;
        this.titleLabelY = Constant.SCREEN_TITLE_Y;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractContents(graphics, mouseX, mouseY, a);
        this.extractEnchantmentSelectingArea(graphics, mouseX, mouseY, a);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int x, int y) {
        if (this.menu.getCarried().isEmpty() && this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            ItemStack stack = this.hoveredSlot.getItem();
            graphics.setTooltipForNextFrame(
                this.font,
                this.getTooltipFromContainerItem(stack),
                stack.getTooltipImage(),
                stack,
                x,
                y
            );
        } else if (this.renderingTooltipEnchantedBook != null) {
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

    protected void extractEnchantmentSelectingArea(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.renderingTooltipEnchantedBook = null;
        if (this.menu.getEnchantments().isEmpty()) return;
        for (int i = this.head; i < this.head + Math.min(this.menu.getEnchantments().size() - this.head, 6); i++) {
            int x = this.leftPos + 65 + 18 * (i % 3);
            int y = this.topPos + 23 + 18 * ((i - this.head) / 3);

            EnchantmentData data = ListUtil.safelyGet(this.menu.getEnchantments(), i).orElse(null);
            if (data == null) continue;

            ItemStack willRender = EnchantmentHelper.createBook(data.toEnchantmentInst());

            int offsetV = 0;
            if (MathUtil.isInRange(mouseX, mouseY, x, y, x + 18, y + 18)) {
                offsetV = 36;
                this.renderingTooltipEnchantedBook = willRender;
            }

            boolean selected = false;
            if (this.menu.getSelectedIndexes().contains(i)) {
                offsetV = 18;
                selected = true;
            }

            graphics.blit(RenderPipelines.GUI_TEXTURED, SharedTextures.SWITCH_TABLE_BUTTON, x, y, 0, offsetV, 18, 18, 18, 54);
            graphics.item(willRender, x + 1, y + (selected ? 1 : 0), (int) (partialTick * 100));
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            BACKGROUND,
            this.leftPos,
            this.topPos,
            0,
            0,
            this.getImageWidth(),
            this.getImageHeight(),
            256,
            256
        );

        if (this.scrollable.canScroll()) {
            int left = this.leftPos + 122;
            int top = this.topPos + 23;
            int down = top + 36;
            graphics.blit(
                RenderPipelines.GUI_TEXTURED,
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
    public void resize(int width, int height) {
        this.scrollable.calculateScroll(this.head / 3);
        this.init(width, height);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        if (event.button() == 0) {
            if (this.insideScrollbar(event.x(), event.y())) {
                this.scrollable.scrolling();
                return true;
            }
            for (int i = this.head; i < this.head + Math.min(this.menu.getEnchantments().size() - this.head, 6); i++) {
                int x = this.leftPos + 65 + 18 * (i % 3);
                int y = this.topPos + 23 + 18 * ((i - this.head) / 3);

                if (!MathUtil.isInRange(event.x(), event.y(), x, y, x + 18, y + 18)) continue;
                if (this.menu.getSelectedIndexes().contains(i)) {
                    this.menu.unselect(i);
                    ClientPacketDistributor.sendToServer(new FrostGrindstoneSyncPacket(i, false));
                } else {
                    this.menu.select(i);
                    ClientPacketDistributor.sendToServer(new FrostGrindstoneSyncPacket(i, true));
                }
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
        if (!this.scrollable.canScroll()) {
            return false;
        } else {
            this.scrollable.scrollOnScroll(scrollY / 1.2);
            return true;
        }
    }

    protected boolean insideScrollbar(double mouseX, double mouseY) {
        int left = this.leftPos + 122;
        int top = this.topPos + 23;
        int right = left + 4;
        int down = top + 36;
        return MathUtil.isInRange(mouseX, mouseY, left, top, right, down);
    }
}
