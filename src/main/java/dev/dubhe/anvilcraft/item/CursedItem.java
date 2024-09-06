package dev.dubhe.anvilcraft.item;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class CursedItem extends Item implements ICursed {
    public CursedItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(
        @NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slotId, boolean isSelected
    ) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        ICursed.super.inventoryTick(stack, level, entity, slotId, isSelected);
    }
}
