package dev.dubhe.anvilcraft.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class EmberMetalAxeItem extends AxeItem {
    public EmberMetalAxeItem(Properties properties) {
        super(ModTiers.EMBER_METAL, 10, -3f, properties.durability(0));
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
