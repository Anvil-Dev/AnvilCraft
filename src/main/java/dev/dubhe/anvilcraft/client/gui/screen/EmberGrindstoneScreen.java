package dev.dubhe.anvilcraft.client.gui.screen;

import dev.anvilcraft.lib.v2.util.ListUtil;
import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.anvilcraft.lib.v2.util.Scrollable;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.inventory.EmberGrindstoneMenu;
import dev.dubhe.anvilcraft.network.EmberGrindstoneSyncPacket;
import dev.dubhe.anvilcraft.util.EnchantmentData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

public class EmberGrindstoneScreen extends AbstractContainerScreen<EmberGrindstoneMenu> {
    private static final ResourceLocation BACKGROUND = SharedTextures.bg("crafting", "ember_grindstone");

    private final EmberGrindstoneMenu menu;
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
            return EmberGrindstoneScreen.this.menu.getEnchantments().size();
        }

        @Override
        public void setHead(int head) {
            EmberGrindstoneScreen.this.head = head;
        }
    };
    private int head = 0;
    private ItemStack renderingTooltipEnchantedBook;

    public EmberGrindstoneScreen(EmberGrindstoneMenu menu, Inventory playerInventory, @SuppressWarnings("unused") Component title) {
        super(menu, playerInventory, Component.translatable("screen.anvilcraft.ember_grindstone.title"));
        this.menu = menu;
        this.player = playerInventory.player;
    }

    @Override
    protected void containerTick() {
        this.renderingTooltipEnchantedBook = null;

        super.containerTick();
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        this.titleLabelY = 3;
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
            ItemStack itemstack = this.hoveredSlot.getItem();
            guiGraphics.renderTooltip(this.font, this.getTooltipFromContainerItem(itemstack), itemstack.getTooltipImage(), itemstack, x, y);
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
        if (this.menu.getEnchantments().isEmpty()) return;
        for (int i = this.head; i < this.head + Math.min(this.menu.getEnchantments().size() - this.head, 6); i++) {
            int x = this.leftPos + 65 + 18 * (i % 3);
            int y = this.topPos + 23 + 18 * ((i - this.head) / 3);

            EnchantmentData data = ListUtil.safelyGet(this.menu.getEnchantments(), i).orElse(null);
            if (data == null) continue;

            ItemStack willRender = EnchantedBookItem.createForEnchantment(data.toEnchantmentInst());

            int offsetV = 0;
            if (MathUtil.isInRange(mouseX, mouseY, x, y, x + 18, y + 18)) {
                offsetV = 36;
                this.renderingTooltipEnchantedBook = willRender;
            }

            boolean selected = false;
            if (this.menu.getSelectedIndex() == i) {
                offsetV = 18;
                selected = true;
            }

            guiGraphics.blit(SharedTextures.SWITCH_TABLE_BUTTON, x, y, 0, offsetV, 18, 18, 18, 54);
            guiGraphics.renderItem(willRender, x + 1, y + (selected ? 1 : 0), (int) (partialTick * 100));
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);

        int cost = this.menu.getCost();
        if (cost <= 0) return;

        Slot result = this.menu.getSlot(2);
        if (!result.hasItem()) return;

        Component component = Component.translatable("screen.anvilcraft.ember_grindstone.cost", cost);
        int textColor = 0x80ff20;
        if (!result.mayPickup(this.player)) {
            textColor = 0xff6060;
        }

        int k = this.imageWidth - 1 - this.font.width(component) - 2;
        guiGraphics.fill(k - 2, 65, this.imageWidth - 1, 76, 0x4f000000);
        guiGraphics.drawString(this.font, component, k, 66, textColor);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        RenderSupport.renderItemWithTransparency(
            Items.BOOK.getDefaultInstance(),
            guiGraphics.pose(),
            this.leftPos + 25,
            this.topPos + 42,
            0.5F
        );

        if (this.menu.canScroll()) {
            int left = this.leftPos + 122;
            int top = this.topPos + 23;
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
        this.scrollable.calculateScroll(this.head / 3);

        this.init(minecraft, width, height);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (this.insideScrollbar(mouseX, mouseY)) {
                this.scrollable.scrolling();
                return true;
            }
            for (int i = this.head; i < this.head + Math.min(this.menu.getEnchantments().size() - this.head, 6); i++) {
                int x = this.leftPos + 65 + 18 * (i % 3);
                int y = this.topPos + 23 + 18 * ((i - this.head) / 3);

                if (!MathUtil.isInRange(mouseX, mouseY, x, y, x + 18, y + 18)) continue;
                if (this.menu.getSelectedIndex() == i) {
                    this.menu.setSelectedEnchantment(-1);
                    PacketDistributor.sendToServer(new EmberGrindstoneSyncPacket(-1));
                } else {
                    this.menu.setSelectedEnchantment(i);
                    PacketDistributor.sendToServer(new EmberGrindstoneSyncPacket(i));
                }
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
