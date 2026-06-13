package dev.dubhe.anvilcraft.block.entity.container;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class HyperdimensionStorageStationBlockEntity extends BlockEntity {
    public HyperdimensionStorageStationBlockEntity(BlockEntityType<?> type, BlockPos worldPosition, BlockState blockState) {
        super(type, worldPosition, blockState);
    }
}
