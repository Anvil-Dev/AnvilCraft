package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.init.ModEnchantments;

import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.NotNull;

public class AmethystHoeItem extends HoeItem {
    public AmethystHoeItem(Properties properties) {
        super(ModTiers.AMETHYST, -1, -2.0f, properties);
    }

    @Override
    public @NotNull ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        stack.enchant(ModEnchantments.HARVEST.get(), 1);
        return stack;
    }
}
