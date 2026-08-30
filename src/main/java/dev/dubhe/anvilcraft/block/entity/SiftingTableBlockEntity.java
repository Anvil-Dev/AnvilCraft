package dev.dubhe.anvilcraft.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 过筛台方块实体，用于存储原料，并在铁坷砸落时执行过筛配方。
 */
public class SiftingTableBlockEntity extends ProcessingTableBlockEntity {
    public SiftingTableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
}
