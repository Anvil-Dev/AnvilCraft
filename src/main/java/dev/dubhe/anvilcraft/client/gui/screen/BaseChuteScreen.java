package dev.dubhe.anvilcraft.client.gui.screen;

import dev.dubhe.anvilcraft.api.itemhandler.SlotItemHandlerWithFilter;
import dev.dubhe.anvilcraft.block.entity.BaseChuteBlockEntity;
import dev.dubhe.anvilcraft.client.gui.component.EnableFilterButton;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.BaseChuteMenu;
import dev.dubhe.anvilcraft.item.FilterItem;
import dev.dubhe.anvilcraft.network.SlotDisableChangePacket;
import dev.dubhe.anvilcraft.network.SlotFilterChangePacket;
import dev.dubhe.anvilcraft.network.SlotFilterMaxStackSizeChangePacket;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * 溜槽屏幕基类
 */
public abstract class BaseChuteScreen<T extends BaseChuteBlockEntity, M extends BaseChuteMenu<T>> extends BaseMachineScreen<M>
    implements IFilterScreen<M> {
    private static final ResourceLocation BACKGROUND = SharedTextures.bg("machine", "chute");

    BiFunction<Integer, Integer, EnableFilterButton> enableFilterButtonSupplier = this.getEnableFilterButtonSupplier(134, 36);

    @Getter
    private EnableFilterButton enableFilterButton = null;

    private final M menu;

    public BaseChuteScreen(M menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.menu = menu;
    }

    @Override
    protected void init() {
        super.init();
        this.enableFilterButton = this.enableFilterButtonSupplier.apply(this.leftPos, this.topPos);
        for (Direction value : Direction.values()) {
            if (this.shouldSkipDirection(value)) {
                Objects.requireNonNull(this.getDirectionButton()).skip(value);
            }
        }
        this.addRenderableWidget(this.enableFilterButton);
    }

    abstract boolean shouldSkipDirection(Direction direction);

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
        this.renderFilterItemTooltip(guiGraphics, x, y);
        this.renderSlotTooltip(guiGraphics, x, y);
    }

    private void renderFilterItemTooltip(GuiGraphics guiGraphics, int x, int y) {
        if (!this.menu.getCarried().isEmpty()) return;
        ItemStack filter = this.getHoveredFilterItem(this.hoveredSlot);
        if (filter.isEmpty()) return;
        guiGraphics.renderTooltip(
            this.font,
            this.getTooltipFromContainerItem(filter),
            filter.getTooltipImage(),
            filter,
            x,
            y
        );
    }

    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> components = super.getTooltipFromContainerItem(stack);
        if (this.hoveredSlot instanceof SlotItemHandlerWithFilter filterSlot
                && filterSlot.isFilter()
                && (!filterSlot.getItem().isEmpty()
                || !this.getFilter(filterSlot.getContainerSlot()).isEmpty())) {
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
    public M getFilterMenu() {
        return this.menu;
    }

    @Override
    public void flush() {
        this.enableFilterButton.flush();
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {
        if (slot instanceof SlotItemHandlerWithFilter) {
            int realSlotId = slot.getContainerSlot();
            ItemStack carriedItem = this.menu.getCarried().copy();
            boolean slotEmpty = slot.getItem().isEmpty();
            // 实际会放入槽位的物品：PICKUP 取光标，SWAP（数字键 / 副手键）取快捷栏 / 副手
            ItemStack incoming = this.getIncomingStack(this.minecraft.player, type, mouseButton, carriedItem);
            // 左右键 PICKUP 与 SWAP 在放入不同物品时都会替换槽内物品，故都要同步过滤；
            // QUICK_MOVE（Shift 点击）用于启用/禁用槽位，有意排除
            boolean replacing = (type == ClickType.PICKUP || type == ClickType.SWAP) && !slotEmpty
                && !incoming.isEmpty()
                && !ItemStack.isSameItemSameComponents(slot.getItem(), incoming);
            if (!incoming.isEmpty() && this.menu.isFilterEnabled() && (slotEmpty || replacing)) {
                final ItemStack filter = this.menu.getFilter(realSlotId);
                if (this.menu.isSlotDisabled(realSlotId)) {
                    PacketDistributor.sendToServer(new SlotDisableChangePacket(realSlotId, false));
                    this.menu.setSlotDisabled(realSlotId, false);
                }
                PacketDistributor.sendToServer(new SlotFilterChangePacket(realSlotId, incoming));
                this.menu.setFilter(realSlotId, incoming);
                if (incoming.is(ModItems.FILTER) && (filter.isEmpty() || !FilterItem.filter(filter, incoming))) return;
            } else if (slotEmpty && Screen.hasShiftDown()) {
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