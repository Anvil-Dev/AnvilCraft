package dev.dubhe.anvilcraft.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import org.jetbrains.annotations.NotNull;

public class EmberMetalSwordItem extends SwordItem {
    public EmberMetalSwordItem(Properties properties) {
        super(ModTiers.EMBER_METAL, 8, -2.4f, properties.durability(0));
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
