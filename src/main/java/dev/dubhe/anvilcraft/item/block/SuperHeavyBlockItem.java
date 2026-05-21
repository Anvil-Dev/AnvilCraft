package dev.dubhe.anvilcraft.item.block;

import dev.dubhe.anvilcraft.item.abnormal.ISuperHeavy;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

public class SuperHeavyBlockItem extends BlockItem implements ISuperHeavy {
    public SuperHeavyBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void inventoryTick(ItemStack itemStack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        super.inventoryTick(itemStack, level, owner, slot);
        ISuperHeavy.super.inventoryTick(itemStack, level, owner, slot);
    }
}
