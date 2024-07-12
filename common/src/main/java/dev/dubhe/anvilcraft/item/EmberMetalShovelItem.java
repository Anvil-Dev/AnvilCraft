package dev.dubhe.anvilcraft.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import org.jetbrains.annotations.NotNull;

public class EmberMetalShovelItem extends ShovelItem {
    public EmberMetalShovelItem(Properties properties) {
        super(ModTiers.EMBER_METAL, 6.5f, -3f, properties.durability(0));
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
