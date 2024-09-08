package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.init.ModEnchantments;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import org.jetbrains.annotations.NotNull;

public class AmethystSwordItem extends SwordItem {
    public AmethystSwordItem(Properties properties) {
        super(ModTiers.AMETHYST, 3, -2.4f, properties);
    }

    @Override
    public @NotNull ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        stack.enchant(ModEnchantments.BEHEADING, 1);
        return stack;
    }
}
