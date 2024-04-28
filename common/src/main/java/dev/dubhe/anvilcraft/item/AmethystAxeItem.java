package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.init.ModEnchantments;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class AmethystAxeItem extends AxeItem {
    public AmethystAxeItem(Properties properties) {
        super(ModTiers.AMETHYST, 7, -3.2f, properties);
    }

    @Override
    public @NotNull ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        stack.enchant(ModEnchantments.FELLING.get(), 1);
        return stack;
    }
}
