package dev.dubhe.anvilcraft.api.itemhandler;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class SlotItemHandlerWithFilter extends SlotItemHandler {
    public SlotItemHandlerWithFilter(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
        super(itemHandler, index, xPosition, yPosition);
    }

    /**
     * 判断槽位是否支持过滤
     *
     * @return 是否支持过滤
     */
    public boolean isFilter() {
        return this.getItemHandler() instanceof FilteredItemStackHandler;
    }

    public boolean mayPlace(@NotNull ItemStack stack) {
        return true;
    }

    /**
     * 获取指定槽位上的过滤器要过滤的物品
     *
     * @param slotIndex 槽位的索引
     * @return 如果指定槽位是过滤器，返回过滤器要过滤的物品，否则返回空物品
     */
    public ItemStack getFilterItem(int slotIndex) {
        if (this.getItemHandler() instanceof FilteredItemStackHandler filtered) {
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
        if (this.getItemHandler() instanceof FilteredItemStackHandler filtered) {
            return filtered.isSlotDisabled(slot);
        }
        return false;
    }
}
