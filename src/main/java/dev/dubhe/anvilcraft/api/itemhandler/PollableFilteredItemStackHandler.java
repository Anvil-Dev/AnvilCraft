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
}
