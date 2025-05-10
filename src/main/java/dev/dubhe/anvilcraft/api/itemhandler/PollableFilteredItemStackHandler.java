package dev.dubhe.anvilcraft.api.itemhandler;

import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.world.item.ItemStack;

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

    public ItemStack insertItemNoPolling(int slot, ItemStack stack, boolean simulate) {
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
