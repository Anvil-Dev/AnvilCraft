package dev.dubhe.anvilcraft.api.itemhandler;

import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PollableFilteredItemStackHandler extends FilteredItemStackHandler {
    public PollableFilteredItemStackHandler(int size) {
        super(size);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        // 临时解决方案
        if (Util.findCaller("doClick")) {
            return super.isItemValid(slot, stack);
        } else {
            return getEmptyOrSmallerSlot(stack) == slot && super.isItemValid(slot, stack);
        }
    }

    @Override
    public boolean isFiltered(int slot, ItemStack stack) {
        ItemStack filter = this.getFilteredItems().get(slot);
        return filter.isEmpty() || ItemStack.isSameItem(filter, stack);
    }

    private int getEmptyOrSmallerSlot(ItemStack stack) {
        int slotCount = this.getSlots();
        int slot = -1;
        int countInSlot = Integer.MAX_VALUE;
        for (int index = slotCount - 1; index >= 0; index--) {
            if (this.isSlotDisabled(index)) continue;
            ItemStack stackInSlot = this.getStackInSlot(index);
            if (this.isSlotDisabled(index)) continue;
            if (!this.isFiltered(index, stack)) continue;
            if (stackInSlot.isEmpty()) {
                slot = index;
                countInSlot = 0;
                continue;
            } else if (!ItemStack.isSameItemSameComponents(stackInSlot, stack)) continue;
            int stackInSlotCount = stackInSlot.getCount();
            if (stackInSlotCount <= countInSlot && stackInSlotCount < this.getSlotLimit(index)) {
                slot = index;
                countInSlot = stackInSlotCount;
            }
        }
        return slot;
    }

    public boolean canCompletelyInsert(@NotNull List<ItemStack> items) {
        List<ItemStack> copyItems = items.stream().map(ItemStack::copy).toList();
        for (int slot = 0; slot < this.getSlots(); slot++) {
            for (ItemStack stack : copyItems) {
                if (stack.isEmpty()) continue;
                ItemStack existing = this.stacks.get(slot);
                if (!ItemStack.isSameItemSameComponents(stack, existing) && !existing.isEmpty()) continue;
                int limit = this.getStackLimit(slot, stack);
                int shrink = Math.min(stack.getCount(), limit - existing.getCount());
                stack.shrink(shrink);
                if (!stack.isEmpty() || limit == shrink) break;
            }
        }
        return copyItems.stream().allMatch(ItemStack::isEmpty);
    }

    public ItemStack insertItemNoPolling(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        } else if (!super.isItemValid(slot, stack)) {
            return stack;
        } else {
            this.validateSlotIndex(slot);
            ItemStack existing = this.stacks.get(slot);
            int limit = this.getStackLimit(slot, stack);
            if (!existing.isEmpty()) {
                if (!ItemStack.isSameItemSameComponents(stack, existing)) {
                    return stack;
                }

                limit -= existing.getCount();
            }

            if (limit <= 0) {
                return stack;
            } else {
                boolean reachedLimit = stack.getCount() > limit;
                if (!simulate) {
                    if (existing.isEmpty()) {
                        this.stacks.set(slot, reachedLimit ? stack.copyWithCount(limit) : stack);
                    } else {
                        existing.grow(reachedLimit ? limit : stack.getCount());
                    }

                    this.onContentsChanged(slot);
                }

                return reachedLimit ? stack.copyWithCount(stack.getCount() - limit) : ItemStack.EMPTY;
            }
        }
    }
}
