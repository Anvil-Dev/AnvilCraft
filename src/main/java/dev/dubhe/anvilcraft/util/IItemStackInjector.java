package dev.dubhe.anvilcraft.util;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

public interface IItemStackInjector {
    default ItemStack dataCopy(ItemStack stack) {
        return ItemStack.EMPTY;
    }

    default boolean removeEnchant(Enchantment enchantment) {
        return false;
    }
}
