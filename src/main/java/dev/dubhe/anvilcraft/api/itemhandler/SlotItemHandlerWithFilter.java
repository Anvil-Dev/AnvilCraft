package dev.dubhe.anvilcraft.api.itemhandler;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.IndexModifier;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;

public class SlotItemHandlerWithFilter extends ResourceHandlerSlot {
    public SlotItemHandlerWithFilter(
        ResourceHandler<ItemResource> handler,
        IndexModifier<ItemResource> modifier,
        int index,
        int posX,
        int posY
    ) {
        super(handler, modifier, index, posX, posY);
    }

    /**
     * 判断槽位是否支持过滤
     *
     * @return 是否支持过滤
     */
    public boolean isFilter() {
        return this.getResourceHandler() instanceof FilteredItemStackHandler;
    }

    public boolean mayPlace(ItemStack stack) {
        return true;
    }

    /**
     * 获取指定槽位上的过滤器要过滤的物品
     *
     * @param slotIndex 槽位的索引
     * @return 如果指定槽位是过滤器，返回过滤器要过滤的物品，否则返回空物品
     */
    public ItemStack getFilterItem(int slotIndex) {
        if (this.getResourceHandler() instanceof FilteredItemStackHandler filtered) {
            return filtered.getFilter(slotIndex);
        }
        return ItemStack.EMPTY;
    }

    /**
     * 判断指定槽位是否被禁用
     *
     * @param slot 槽位
     * @return 指定槽位是否被禁用
     */
    public boolean isSlotDisabled(int slot) {
        if (this.getResourceHandler() instanceof FilteredItemStackHandler filtered) {
            return filtered.isSlotDisabled(slot);
        }
        return false;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return Math.min(this.getMaxStackSize(), stack.getMaxStackSize());
    }
}
