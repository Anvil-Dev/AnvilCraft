package dev.dubhe.anvilcraft.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class EmberAnvilHammerItem extends AnvilHammerItem {
    /**
     * 初始化铁砧锤
     *
     * @param properties 物品属性
     */
    public EmberAnvilHammerItem(Properties properties) {
        super(properties.durability(0));
    }

    @Override
    protected float getAttackDamageModifierAmount() {
        return 9;
    }

    @Override
    protected float calculateFallDamageBonus(float fallDistance) {
        return Math.min(120, fallDistance * 2);
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
