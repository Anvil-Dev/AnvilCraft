package dev.dubhe.anvilcraft.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class EmberMetalHoeItem extends HoeItem {
    public EmberMetalHoeItem(Properties properties) {
        super(ModTiers.EMBER_METAL, 1, 0, properties.durability(0));
    }

    @Override
    public @NotNull ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putBoolean("Unbreakable", true);
        compoundTag.putInt("HideFlags", 8);
        stack.setTag(compoundTag);
        return stack;
    }
}
