package dev.dubhe.anvilcraft.block.item;

import dev.dubhe.anvilcraft.item.abnormal.ICursed;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

public class CursedBlockItem extends BlockItem implements ICursed {
    public CursedBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void inventoryTick(ItemStack ignored, ServerLevel level, Entity entity, @Nullable EquipmentSlot ignored1) {
        super.inventoryTick(ignored, level, entity, ignored1);
        ICursed.super.inventoryTick(ignored, level, entity, ignored1);
    }
}
