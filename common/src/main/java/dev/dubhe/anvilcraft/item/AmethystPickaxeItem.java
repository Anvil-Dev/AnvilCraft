package dev.dubhe.anvilcraft.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.enchantment.Enchantments;
import org.jetbrains.annotations.NotNull;

public class AmethystPickaxeItem extends PickaxeItem {
    public AmethystPickaxeItem(Properties properties) {
        super(ModTiers.AMETHYST, 1, -2.8f, properties);
    }

    @Override
    public @NotNull ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        stack.enchant(Enchantments.BLOCK_FORTUNE, 3);
        return stack;
    }
}
