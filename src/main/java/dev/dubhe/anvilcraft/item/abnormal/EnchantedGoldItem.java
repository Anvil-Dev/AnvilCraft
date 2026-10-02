package dev.dubhe.anvilcraft.item.abnormal;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class EnchantedGoldItem extends Item implements IEnchantedGold {
    public EnchantedGoldItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        IEnchantedGold.super.inventoryTick(stack, level, entity, slot);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
