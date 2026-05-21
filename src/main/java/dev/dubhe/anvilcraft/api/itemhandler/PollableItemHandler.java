package dev.dubhe.anvilcraft.api.itemhandler;

import com.google.common.primitives.Ints;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

public class PollableItemHandler extends ItemStacksResourceHandler {
    public PollableItemHandler(int size) {
        super(size);
    }

    @Override
    public boolean isValid(int slot, ItemResource resource) {
        return slot == this.getEmptyOrSmallerSlot(resource);
    }

    protected int getEmptyOrSmallerSlot(ItemResource resource) {
        int slotCount = this.size();
        int slot = -1;
        int countInSlot = Integer.MAX_VALUE;
        for (int i = slotCount - 1; i >= 0; i--) {
            ItemResource resourceIn = this.getResourceDirect(i);
            if (!resourceIn.isEmpty() && !resourceIn.equals(resource)) continue;
            int amount = this.getAmountAsInt(i);
            if (amount <= countInSlot && amount < this.getCapacityAsIntDirect(i, resourceIn)) {
                slot = i;
                countInSlot = amount;
            }
        }
        return slot;
    }

    protected int getCapacityAsIntDirect(int index, ItemResource resource) {
        return Ints.saturatedCast(this.getCapacityAsLongDirect(index, resource));
    }

    public long getCapacityAsLongDirect(int index, ItemResource resource) {
        return !resource.isEmpty() ? getCapacity(index, resource) : 0;
    }

    protected ItemResource getResourceDirect(int index) {
        return getResourceFrom(stacks.get(index));
    }
}
