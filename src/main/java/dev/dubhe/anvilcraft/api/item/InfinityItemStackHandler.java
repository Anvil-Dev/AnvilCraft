package dev.dubhe.anvilcraft.api.item;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

public class InfinityItemStackHandler extends ItemStackHandler {
    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        ItemStack stackInSlot = this.getStackInSlot(0);
        if (stackInSlot.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return stackInSlot.copyWithCount(amount);
    }
}
