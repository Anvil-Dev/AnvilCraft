package dev.dubhe.anvilcraft.api.depository;

import net.minecraft.world.item.ItemStack;

public class ItemDepositoryHelper {
    private ItemDepositoryHelper() {
    }

    public static ItemStack copyStackWithSize(ItemStack stack, int size) {
        if (size == 0) {
            return ItemStack.EMPTY;
        } else {
            ItemStack copy = stack.copy();
            copy.setCount(size);
            return copy;
        }
    }
}
