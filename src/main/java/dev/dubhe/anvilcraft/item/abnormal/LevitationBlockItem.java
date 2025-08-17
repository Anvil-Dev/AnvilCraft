package dev.dubhe.anvilcraft.item.abnormal;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class LevitationBlockItem extends BlockItem implements ILevitation {
    public LevitationBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        ILevitation.super.inventoryTick(stack, level, entity, slotId, isSelected);
    }
}
