package dev.dubhe.anvilcraft.client.gui.screen;

import dev.dubhe.anvilcraft.api.itemhandler.SlotItemHandlerWithFilter;
import dev.dubhe.anvilcraft.client.gui.component.EnableFilterButton;
import dev.dubhe.anvilcraft.client.gui.component.ItemCollectorButton;
import dev.dubhe.anvilcraft.client.gui.component.TextWidget;
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
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.function.BiFunction;

public class ItemCollectorScreen extends AbstractContainerScreen<ItemCollectorMenu> implements IFilterScreen<ItemCollectorMenu> {
    private static final Identifier BACKGROUND = SharedTextures.bg("machine", "item_collector");
    BiFunction<Integer, Integer, EnableFilterButton> enableFilterButtonSupplier = this.getEnableFilterButtonSupplier(75, 54);

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
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
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
        this.addRenderableWidget(new ItemCollectorButton(
            this.leftPos + 43, this.topPos + 23, "minus", b -> {
            this.menu.getBlockEntity().getRangeRadius().previous();
            this.menu.getBlockEntity().getRangeRadius().notifyServer();
        }
        ));
        this.addRenderableWidget(new ItemCollectorButton(
            this.leftPos + 81, this.topPos + 23, "add", b -> {
            this.menu.getBlockEntity().getRangeRadius().next();
            this.menu.getBlockEntity().getRangeRadius().notifyServer();
        }
        ));
        // cooldown - +
        this.addRenderableWidget(new ItemCollectorButton(
            this.leftPos + 43, this.topPos + 37, "minus", b -> {
            this.menu.getBlockEntity().getCooldown().previous();
            this.menu.getBlockEntity().getCooldown().notifyServer();
        }
        ));
        this.addRenderableWidget(new ItemCollectorButton(
            leftPos + 81, topPos + 37, "add", b -> {
            this.menu.getBlockEntity().getCooldown().next();
            this.menu.getBlockEntity().getCooldown().notifyServer();
        }
        ));
    }

    @Override
    protected void renderBg(GuiGraphicsExtractor graphics, float partialTick, int mouseX, int mouseY) {
        int i = (this.width - this.getImageWidth()) / 2;
        int j = (this.height - this.getImageHeight()) / 2;
        graphics.blit(BACKGROUND, i, j, 0, 0, this.getImageWidth(), this.getImageHeight());
    }

    @Override
    public void extractSlot(GuiGraphicsExtractor graphics, Slot slot) {
        super.renderSlot(graphics, slot);
        IFilterScreen.super.extractSlot(graphics, slot);
    }

    @Override
    protected void renderTooltip(GuiGraphicsExtractor graphics, int x, int y) {
        super.renderTooltip(graphics, x, y);
        this.renderSlotTooltip(graphics, x, y);
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

    protected void renderSlotTooltip(GuiGraphicsExtractor graphics, int x, int y) {
        if (this.hoveredSlot == null) return;
        if (!(this.hoveredSlot instanceof SlotItemHandlerWithFilter)) return;
        if (!((SlotItemHandlerWithFilter) this.hoveredSlot).isFilter()) return;
        if (!this.isFilterEnabled()) return;
        if (!this.isSlotDisabled(this.hoveredSlot.getContainerSlot())) return;
        graphics.renderTooltip(this.font, Component.translatable("screen.anvilcraft.slot.disable.tooltip"), x, y);
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
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
                    ClientPacketDistributor.sendToServer(new SlotDisableChangePacket(realSlotId, false));
                    this.menu.setSlotDisabled(realSlotId, false);
                }
                ClientPacketDistributor.sendToServer(new SlotFilterChangePacket(realSlotId, carriedItem));
                this.menu.setFilter(realSlotId, carriedItem);
                if (carriedItem.is(ModItems.FILTER) && (filter.isEmpty() || !FilterItem.filter(filter, carriedItem))) return;
                slot.set(carriedItem);
            } else if (Screen.hasShiftDown()) {
                ClientPacketDistributor.sendToServer(new SlotDisableChangePacket(
                    realSlotId,
                    carriedItem.isEmpty() && !this.menu.isSlotDisabled(realSlotId)
                ));
            }
        }
        super.slotClicked(slot, slotId, mouseButton, type);
    }

    @Override
    public int getOffsetX() {
        return (this.width - this.getImageWidth()) / 2;
    }

    @Override
    public int getOffsetY() {
        return (this.height - this.getImageHeight()) / 2;
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
                ClientPacketDistributor.sendToServer(new SlotFilterMaxStackSizeChangePacket(slotIndex, newLimit));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
