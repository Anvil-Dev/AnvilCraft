package dev.dubhe.anvilcraft.item;

import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.NotNull;

public class AmethystAxeItem extends AxeItem {
    /**
     *
     */
    public AmethystAxeItem(Properties properties) {
        super(
                ModTiers.AMETHYST,
                properties.attributes(AxeItem.createAttributes(ModTiers.AMETHYST, 7, -3.2f)));
    }

    @Override
    public @NotNull ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        return stack;
    }
}
