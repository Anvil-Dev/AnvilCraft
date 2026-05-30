package dev.dubhe.anvilcraft.block.entity.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class PipeBlockEntity extends AbstractPipeBlockEntity {
    protected PipeBlockEntity(
        BlockEntityType<PipeBlockEntity> type,
        BlockPos pos,
        BlockState blockState
    ) {
        super(type, pos, blockState);
    }

    public static PipeBlockEntity create(BlockEntityType<PipeBlockEntity> type, BlockPos pos, BlockState blockState) {
        return new PipeBlockEntity(type, pos, blockState);
    }
}
