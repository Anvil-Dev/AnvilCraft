package dev.dubhe.anvilcraft.client.gui.screen;

import dev.dubhe.anvilcraft.api.itemhandler.SlotItemHandlerWithFilter;
import dev.dubhe.anvilcraft.client.gui.component.EnableFilterButton;
import dev.dubhe.anvilcraft.client.gui.component.TextWidget;
import dev.dubhe.anvilcraft.client.gui.component.Texture10xButton;
import dev.dubhe.anvilcraft.constant.Constant;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.ItemCollectorMenu;
import dev.dubhe.anvilcraft.item.FilterItem;
import dev.dubhe.anvilcraft.network.SlotDisableChangePacket;
import dev.dubhe.anvilcraft.network.SlotFilterChangePacket;
import dev.dubhe.anvilcraft.network.SlotFilterMaxStackSizeChangePacket;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.function.BiFunction;

public class ItemCollectorScreen extends AbstractContainerScreen<ItemCollectorMenu> implements IFilterScreen<ItemCollectorMenu> {
    private static final ResourceLocation BACKGROUND = SharedTextures.bg("machine", "item_collector");
    BiFunction<Integer, Integer, EnableFilterButton> enableFilterButtonSupplier = this.getEnableFilterButtonSupplier(75, 54);
    private static final String TEXTURES_PREFIX = "machine/item_collector/button_";

    @Getter
    private EnableFilterButton enableFilterButton = null;

    private final ItemCollectorMenu menu;

    /**
     * 物品收集器 Screen
     */
    public ItemCollectorScreen(ItemCollectorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.menu = menu;
        this.minecraft = Minecraft.getInstance();
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelY = Constant.SCREEN_TITLE_Y;
        this.enableFilterButton = this.enableFilterButtonSupplier.apply(this.leftPos, this.topPos);
        this.addRenderableWidget(this.enableFilterButton);
        if (this.minecraft == null) return;
        // range
        this.addRenderableWidget(new TextWidget(
            this.leftPos + 57,
            this.topPos + 24,
            20,
            8,
            this.minecraft.font,
            () -> Component.literal(this.menu.getBlockEntity().getRangeRadius().get().toString())
        ));
        // cooldown
        this.addRenderableWidget(new TextWidget(
            this.leftPos + 57,
            this.topPos + 38,
            20,
            8,
            this.minecraft.font,
            () -> Component.literal(this.menu.getBlockEntity().getCooldown().get().toString())
        ));
        // power cost
        this.addRenderableWidget(new TextWidget(
            this.leftPos + 43,
            this.topPos + 51,
            20,
            8,
            this.minecraft.font,
            () -> Component.literal(Integer.toString(this.menu.getBlockEntity().getInputPower()))
        ));
        // range - +
        this.addRenderableWidget(
            new Texture10xButton(
                this.leftPos + 43,
                this.topPos + 23,
                TEXTURES_PREFIX + "minus",
                (ignore) -> Component.literal("Reduce Item Collect Range"),
                (btn) -> {
                    this.menu.getBlockEntity().getRangeRadius().previous();
                    this.menu.getBlockEntity().getRangeRadius().notifyServer();
                }
            )
        );
        this.addRenderableWidget(
            new Texture10xButton(
                this.leftPos + 81,
                this.topPos + 23,
                TEXTURES_PREFIX + "add",
                (ignore) -> Component.literal("Add Item Collect Range"),
                (btn) -> {
                    this.menu.getBlockEntity().getRangeRadius().next();
                    this.menu.getBlockEntity().getRangeRadius().notifyServer();
                }
            )
        );
        // cooldown - +
        this.addRenderableWidget(
            new Texture10xButton(
                this.leftPos + 43,
                this.topPos + 37,
                 TEXTURES_PREFIX + "minus",
                (ignore) -> Component.literal("Reduce Item Collect Cooldown"),
                (btn) -> {
                    this.menu.getBlockEntity().getCooldown().previous();
                    this.menu.getBlockEntity().getCooldown().notifyServer();
                }
            )
        );
        this.addRenderableWidget(
            new Texture10xButton(
                leftPos + 81,
                topPos + 37,
                 TEXTURES_PREFIX + "add",
                (ignore) -> Component.literal("Add Item Collect Cooldown"),
                (btn) -> {
                    this.menu.getBlockEntity().getCooldown().next();
                    this.menu.getBlockEntity().getCooldown().notifyServer();
                }
            )
        );
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(BACKGROUND, i, j, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void renderSlot(GuiGraphics guiGraphics, Slot slot) {
        super.renderSlot(guiGraphics, slot);
        IFilterScreen.super.renderSlot(guiGraphics, slot);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        super.renderTooltip(guiGraphics, x, y);
        this.renderSlotTooltip(guiGraphics, x, y);
    }

    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> components = super.getTooltipFromContainerItem(stack);
        if (this.hoveredSlot instanceof SlotItemHandlerWithFilter filterSlot 
                && filterSlot.isFilter() 
                && !filterSlot.getItem().isEmpty()) {
            components.add(SCROLL_WHEEL_TO_CHANGE_STACK_LIMIT_TOOLTIP);
            components.add(SHIFT_TO_SCROLL_FASTER_TOOLTIP);
        }
        return components;
    }

    protected void renderSlotTooltip(GuiGraphics guiGraphics, int x, int y) {
        if (this.hoveredSlot == null) return;
        if (!(this.hoveredSlot instanceof SlotItemHandlerWithFilter)) return;
        if (!((SlotItemHandlerWithFilter) this.hoveredSlot).isFilter()) return;
        if (!this.isFilterEnabled()) return;
        if (!this.isSlotDisabled(this.hoveredSlot.getContainerSlot())) return;
        guiGraphics.renderTooltip(this.font, Component.translatable("screen.anvilcraft.slot.disable.tooltip"), x, y);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public ItemCollectorMenu getFilterMenu() {
        return this.menu;
    }

    @Override
    public void flush() {
        this.enableFilterButton.flush();
    }

    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {
        if (slot instanceof SlotItemHandlerWithFilter && slot.getItem().isEmpty()) {
            ItemStack carriedItem = this.menu.getCarried().copy();
            int realSlotId = slot.getContainerSlot();
            if (!carriedItem.isEmpty() && this.menu.isFilterEnabled()) {
                final ItemStack filter = this.menu.getFilter(realSlotId);
                if (this.menu.isSlotDisabled(realSlotId)) {
                    PacketDistributor.sendToServer(new SlotDisableChangePacket(realSlotId, false));
                    this.menu.setSlotDisabled(realSlotId, false);
                }
                PacketDistributor.sendToServer(new SlotFilterChangePacket(realSlotId, carriedItem));
                this.menu.setFilter(realSlotId, carriedItem);
                if (carriedItem.is(ModItems.FILTER) && (filter.isEmpty() || !FilterItem.filter(filter, carriedItem))) return;
                slot.set(carriedItem);
            } else if (Screen.hasShiftDown()) {
                PacketDistributor.sendToServer(new SlotDisableChangePacket(
                    realSlotId,
                    carriedItem.isEmpty() && !this.menu.isSlotDisabled(realSlotId)
                ));
            }
        }
        super.slotClicked(slot, slotId, mouseButton, type);
    }

    @Override
    public int getOffsetX() {
        return (this.width - this.imageWidth) / 2;
    }

    @Override
    public int getOffsetY() {
        return (this.height - this.imageHeight) / 2;
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        Slot slot = this.hoveredSlot;
        if (slot instanceof SlotItemHandlerWithFilter filterSlot && filterSlot.isFilter() && scrollY != 0) {
            int slotIndex = slot.getContainerSlot();
            int currentLimit = this.getSlotLimit(slotIndex);
            int scrollSpeed = Screen.hasShiftDown() ? 5 : 1;
            int newLimit = currentLimit + (scrollY > 0 ? scrollSpeed : -scrollSpeed);
            newLimit = Mth.clamp(newLimit, 1, 64);
            
            if (newLimit != currentLimit) {
                this.setSlotLimit(slotIndex, newLimit);
                PacketDistributor.sendToServer(new SlotFilterMaxStackSizeChangePacket(slotIndex, newLimit));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}