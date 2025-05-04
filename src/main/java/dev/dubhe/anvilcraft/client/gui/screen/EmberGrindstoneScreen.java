package dev.dubhe.anvilcraft.client.gui.screen;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.EmberGrindstoneMenu;

import dev.dubhe.anvilcraft.network.SyncEmberGrindstonePacket;
import dev.dubhe.anvilcraft.util.ListUtil;
import dev.dubhe.anvilcraft.util.MathUtil;
import dev.dubhe.anvilcraft.util.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

public class EmberGrindstoneScreen extends AbstractContainerScreen<EmberGrindstoneMenu> {
    private static final ResourceLocation BACKGROUND =
        AnvilCraft.of("textures/gui/container/smithing/background/ember_grindstone.png");

    private static final ResourceLocation BUTTON =
        AnvilCraft.of("textures/gui/container/smithing/ember_grindstone_button.png");
    private static final ResourceLocation SLIDER =
        AnvilCraft.of("textures/gui/container/smithing/ember_grindstone_slider.png");

    private final EmberGrindstoneMenu menu;
    private final Player player;
    private float scrollOffs = 0.0F;
    private int lastRowIndex = 0;
    private boolean scrolling = false;
    private ItemStack renderingTooltipEnchantedBook;

    public EmberGrindstoneScreen(EmberGrindstoneMenu menu, Inventory playerInventory, @SuppressWarnings("unused") Component title) {
        super(menu, playerInventory, Component.translatable("screen.anvilcraft.ember_grindstone.title"));
        this.menu = menu;
        this.player = playerInventory.player;
    }

    @Override
    protected void containerTick() {
        this.renderingTooltipEnchantedBook = null;

        int rowIndex = this.menu.getRowIndexForScroll(this.scrollOffs);
        if (rowIndex != this.lastRowIndex) {
            this.scrollOffs = this.menu.getScrollForRowIndex(this.lastRowIndex);
        }

        super.containerTick();
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderEnchantmentSelectingArea(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    protected void renderTooltip(@NotNull GuiGraphics guiGraphics, int x, int y) {
        if (this.menu.getCarried().isEmpty() && this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            ItemStack itemstack = this.hoveredSlot.getItem();
            guiGraphics.renderTooltip(this.font, this.getTooltipFromContainerItem(itemstack), itemstack.getTooltipImage(), itemstack, x, y);
        } else if (this.renderingTooltipEnchantedBook != null) {
            guiGraphics.renderTooltip(
                this.font, this.getTooltipFromContainerItem(this.renderingTooltipEnchantedBook),
                this.renderingTooltipEnchantedBook.getTooltipImage(), this.renderingTooltipEnchantedBook, x, y);
        }
    }

    protected void renderEnchantmentSelectingArea(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderingTooltipEnchantedBook = null;
        int scrollOver = this.lastRowIndex * 3;
        if (!this.menu.getEnchantments().isEmpty()) for (int i = 0; i < Math.min(this.menu.getEnchantments().size() + scrollOver, 6); i++) {
            int x = this.leftPos + 65 + 18 * (i % 3);
            int y = this.topPos + 23 + 18 * (i / 3);

            EnchantmentInstance enchantment = ListUtil.safelyGet(this.menu.getEnchantments(), i + scrollOver);
            if (enchantment == null) continue;
            ItemStack willRender = EnchantedBookItem.createForEnchantment(enchantment);
            boolean selected = false;
            int vOffset = 0;
            if (MathUtil.isInRange(mouseX, mouseY, x, y, x + 18, y + 18)) {
                vOffset = 36;
                this.renderingTooltipEnchantedBook = willRender;
            }
            if (this.menu.getSelectedIndex() - scrollOver == i) {
                vOffset = 18;
                selected = true;
            }
            guiGraphics.blit(BUTTON, x, y, 0, vOffset, 18, 18, 18, 54);

            guiGraphics.renderItem(willRender, x + 1, y + (selected ? 1 : 0), (int) (partialTick * 100));
        }
    }

    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        int i = this.menu.getCost();
        if (i > 0) {
            int j = 0x80ff20;
            Component component;
            if (!this.menu.getSlot(2).hasItem()) {
                component = null;
            } else {
                component = Component.translatable("screen.anvilcraft.ember_grindstone.cost", i);
                if (!this.menu.getSlot(2).mayPickup(this.player)) {
                    j = 0xff6060;
                }
            }

            if (component != null) {
                int k = this.imageWidth - 1 - this.font.width(component) - 2;
                guiGraphics.fill(k - 2, 65, this.imageWidth - 1, 76, 0x4f000000);
                guiGraphics.drawString(this.font, component, k, 66, j);
            }
        }
    }

    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        RenderHelper.renderItemWithTransparency(Items.BOOK.getDefaultInstance(), guiGraphics.pose(), this.leftPos + 25, this.topPos + 42, 0.5F);

        if (this.menu.canScroll()) {
            int left = this.leftPos + 122;
            int top = this.topPos + 23;
            int down = top + 36;
            guiGraphics.blit(
                SLIDER,
                left, top + (int) ((down - top - 12) * this.scrollOffs),
                0, 0, 4, 12, 8, 12
            );
        }
    }

    @Override
    public void resize(@NotNull Minecraft minecraft, int width, int height) {
        this.scrollOffs = this.menu.getScrollForRowIndex(this.menu.getRowIndexForScroll(this.scrollOffs));

        this.init(minecraft, width, height);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (this.insideScrollbar(mouseX, mouseY)) {
                this.scrolling = this.menu.canScroll();
                return true;
            }
            for (int i = 0; i < Math.min(this.menu.getEnchantments().size() - this.lastRowIndex * 3, 6); i++) {
                int x = this.leftPos + 65 + 18 * (i % 3);
                int y = this.topPos + 23 + 18 * (i / 3);

                if (MathUtil.isInRange(mouseX, mouseY, x, y, x + 18, y + 18)) {
                    int thisIndex = i + this.lastRowIndex * 3;
                    if (this.menu.getSelectedIndex() == thisIndex) {
                        this.menu.setSelectedEnchantment(-1);
                        PacketDistributor.sendToServer(new SyncEmberGrindstonePacket(-1));
                    } else {
                        this.menu.setSelectedEnchantment(thisIndex);
                        PacketDistributor.sendToServer(new SyncEmberGrindstonePacket(thisIndex));
                    }
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.scrolling = false;
            if (this.insideScrollbar(mouseX, mouseY)) {
                int top = this.topPos + 23;
                int down = top + 36;
                this.scrollOffs = (float) (mouseY - top - 6) / (down - top - 12);
                this.scrollOffs = Mth.clamp(this.scrollOffs, 0.0F, 1.0F);
                this.lastRowIndex = this.menu.getRowIndexForScroll(this.scrollOffs);
            }
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!this.menu.canScroll()) {
            return false;
        } else {
            this.scrollOffs = this.menu.subtractInputFromScroll(this.scrollOffs, scrollY / 1.2);
            this.lastRowIndex = this.menu.getRowIndexForScroll(this.scrollOffs);
            return true;
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.scrolling) {
            int top = this.topPos + 23;
            int down = top + 36;
            this.scrollOffs = (float) (mouseY - top - 6) / (down - top - 12);
            this.scrollOffs = Mth.clamp(this.scrollOffs, 0.0F, 1.0F);
            this.lastRowIndex = this.menu.getRowIndexForScroll(this.scrollOffs);
            return true;
        } else {
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
    }

    protected boolean insideScrollbar(double mouseX, double mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        int left = x + 122;
        int top = y + 23;
        int right = left + 4;
        int down = top + 36;
        return MathUtil.isInRange(mouseX, mouseY, left, top, right, down);
    }
}
