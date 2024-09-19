package dev.dubhe.anvilcraft.api.itemhandler;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

public class PollableItemHandler extends ItemStackHandler {
    public PollableItemHandler(int size) {
        super(size);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot == getEmptyOrSmallerSlot(stack);
    }

    private int getEmptyOrSmallerSlot(ItemStack stack) {
        int slotCount = this.getSlots();
        int slot = -1;
        int countInSlot = Integer.MAX_VALUE;
        for (int i = slotCount - 1; i >= 0; i--) {
            ItemStack stackInSlot = this.getStackInSlot(i);
            if (!stackInSlot.isEmpty() && !ItemStack.isSameItemSameComponents(stackInSlot, stack)) continue;
            int stackInSlotCount = stackInSlot.getCount();
            if (stackInSlotCount <= countInSlot && stackInSlotCount < this.getSlotLimit(i)) {
                slot = i;
                countInSlot = stackInSlotCount;
            }
        }
        return slot;
    }
}
