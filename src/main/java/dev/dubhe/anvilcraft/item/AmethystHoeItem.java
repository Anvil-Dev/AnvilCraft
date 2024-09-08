package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.init.ModEnchantments;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class AmethystHoeItem extends HoeItem {
    public AmethystHoeItem(Properties properties) {
        super(ModTiers.AMETHYST, properties.attributes(AxeItem.createAttributes(
            ModTiers.AMETHYST, -1, -2.0f
        )));
    }

    @Override
    public @NotNull ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        stack.enchant(ModEnchantments.HARVEST.get(), 1);
        return stack;
    }
}
