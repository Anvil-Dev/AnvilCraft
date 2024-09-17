package dev.dubhe.anvilcraft.item;

import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;

import org.jetbrains.annotations.NotNull;

public class AmethystShovelItem extends ShovelItem {
    /**
     *
     */
    public AmethystShovelItem(Properties properties) {
        super(ModTiers.AMETHYST, properties.attributes(AxeItem.createAttributes(ModTiers.AMETHYST, 1.5f, -3.0f)));
    }

    @Override
    public @NotNull ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        return stack;
    }
}
