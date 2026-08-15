package dev.dubhe.anvilcraft.api.itemhandler;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/// ??? IItemHandler ????????????????
public class ReadOnlyItemHandlerWrapper implements IItemHandler {
    private final IItemHandler delegate;

    public ReadOnlyItemHandlerWrapper(IItemHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    public int getSlots() {
        return this.delegate.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return this.delegate.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return this.delegate.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return false;
    }

    public static IItemHandler wrap(IItemHandler delegate) {
        return new ReadOnlyItemHandlerWrapper(delegate);
    }
}
