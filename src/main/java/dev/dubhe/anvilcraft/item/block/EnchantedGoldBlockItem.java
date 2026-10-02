package dev.dubhe.anvilcraft.item.block;

import dev.dubhe.anvilcraft.item.abnormal.IEnchantedGold;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

public class EnchantedGoldBlockItem extends BlockItem implements IEnchantedGold {
    public EnchantedGoldBlockItem(Block block, Properties properties) {
        super(block, properties);
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
