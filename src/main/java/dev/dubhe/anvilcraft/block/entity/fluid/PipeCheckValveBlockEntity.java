package dev.dubhe.anvilcraft.block.entity.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class PipeCheckValveBlockEntity extends AbstractPipeCheckValveBlockEntity {
    public PipeCheckValveBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }
}
