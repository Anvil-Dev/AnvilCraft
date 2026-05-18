package dev.dubhe.anvilcraft.item.abnormal;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

public class CursedItem extends Item implements ICursed {
    public CursedItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        ICursed.super.inventoryTick(stack, level, entity, slot);
    }
}
