package dev.dubhe.anvilcraft.item.abnormal;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class SuperHeavyItem extends Item implements ISuperHeavy {
    public SuperHeavyItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        ISuperHeavy.super.inventoryTick(stack, level, entity, slotId, isSelected);
    }
}
