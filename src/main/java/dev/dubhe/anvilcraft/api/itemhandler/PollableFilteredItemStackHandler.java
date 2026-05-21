package dev.dubhe.anvilcraft.api.itemhandler;

import dev.dubhe.anvilcraft.mixin.accessor.StacksResourceHandlerAccessor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.List;
import java.util.Objects;

public class PollableFilteredItemStackHandler extends FilteredItemStackHandler {
    public PollableFilteredItemStackHandler(int size) {
        super(size);
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return this.getEmptyOrSmallerSlot(resource) == index && super.isValid(index, resource);
    }

    private int getEmptyOrSmallerSlot(ItemResource resource) {
        int size = this.size();
        int valid = -1;
        int countInSlot = Integer.MAX_VALUE;
        for (int slot = size - 1; slot >= 0; slot--) {
            if (this.isSlotDisabled(slot)) continue;
            ItemResource resourceIn = this.getResource(slot);
            if (!this.isFiltered(slot, resourceIn.toStack())) continue;
            if (resourceIn.isEmpty()) return slot;
            if (!resourceIn.equals(resource)) continue;
            int stackInSlotCount = this.getAmountAsInt(slot);
            if (stackInSlotCount <= countInSlot && stackInSlotCount < this.getSlotLimit(slot)) {
                valid = slot;
                countInSlot = stackInSlotCount;
            }
        }
        return valid;
    }

    public boolean canCompletelyInsert(List<ItemStack> items) {
        List<ItemStack> copyItems = items.stream().map(ItemStack::copy).toList();
        for (int slot = 0; slot < this.size(); slot++) {
            for (ItemStack stack : copyItems) {
                if (stack.isEmpty()) continue;
                ItemStack existing = this.stacks.get(slot);
                if (!ItemStack.isSameItemSameComponents(stack, existing) && !existing.isEmpty()) continue;
                int limit = this.getCapacity(slot, ItemResource.of(stack));
                int shrink = Math.min(stack.getCount(), limit - existing.getCount());
                stack.shrink(shrink);
                if (!stack.isEmpty() || limit == shrink) break;
            }
        }
        return copyItems.stream().allMatch(ItemStack::isEmpty);
    }

    public int insertNoPolling(ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);

        int inserted = 0;
        int size = this.size();
        for (int index = 0; index < size; index++) {
            inserted += this.insertNoPolling(index, resource, amount - inserted, transaction);
            if (inserted == amount) break;
        }
        return inserted;
    }

    public int insertNoPolling(int index, ItemResource resource, int amount, TransactionContext transaction) {
        Objects.checkIndex(index, this.size());
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);

        ItemStack currentStack = this.stacks.get(index);
        int currentAmount = this.getAmountFrom(currentStack);

        if ((currentAmount == 0 || this.matches(currentStack, resource)) && super.isValid(index, resource)) {
            int inserted = Math.min(amount, this.getCapacity(index, resource) - currentAmount);

            if (inserted > 0) {
                ((StacksResourceHandlerAccessor) this).getSnapshotJournals().get(index).updateSnapshots(transaction);
                this.stacks.set(index, this.getStackFrom(resource, currentAmount + inserted));
                return inserted;
            }
        }

        return 0;
    }
}
