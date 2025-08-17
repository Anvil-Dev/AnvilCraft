package dev.dubhe.anvilcraft.item.abnormal;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class RadiationBlockItem extends BlockItem implements IRadiation {
    public RadiationBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        IRadiation.super.inventoryTick(stack, level, entity, slotId, isSelected);
    }
}
