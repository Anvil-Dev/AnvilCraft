package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.laser.LaserBlockManager;
import dev.dubhe.anvilcraft.api.laser.LevelLaserBlockManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public abstract class BaseLaserBlockEntity extends BlockEntity {
    public BaseLaserBlockEntity(BlockEntityType<?> type,
        BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public void emitLaser(Direction direction) {
        LevelLaserBlockManager levelLaserBlockManager = LaserBlockManager.getLevelLaserBlockManager(level);
        BaseLaserBlockEntity irradiateLaserBlockEntity =  levelLaserBlockManager.getIrradiateLaserBlockEntity(getBlockPos());
        BlockPos trackBoundBox = levelLaserBlockManager.getIrradiateBlockPos(128, direction, getBlockPos());

    }

    public void onIrradiated() {
    }
}
