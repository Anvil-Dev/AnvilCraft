package dev.dubhe.anvilcraft.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.enchantment.Enchantments;

import org.jetbrains.annotations.NotNull;

public class AmethystShovelItem extends ShovelItem {
    public AmethystShovelItem(Properties properties) {
        super(ModTiers.AMETHYST, 1.5f, -3.0f, properties);
    }

    @Override
    public @NotNull ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        stack.enchant(Enchantments.BLOCK_EFFICIENCY, 3);
        return stack;
    }
}
