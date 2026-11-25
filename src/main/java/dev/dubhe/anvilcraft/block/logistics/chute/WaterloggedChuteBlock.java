package dev.dubhe.anvilcraft.block.logistics.chute;

import dev.dubhe.anvilcraft.block.better.BetterBaseEntityBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/** 为带方块实体的溜槽提供原版含水状态与流体更新。 */
abstract class WaterloggedChuteBlock extends BetterBaseEntityBlock implements SimpleWaterloggedBlock {
    protected static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    protected WaterloggedChuteBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WATERLOGGED);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected BlockState updateShape(
        BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction side,
        BlockPos neighborPos, BlockState neighbor, RandomSource random
    ) {
        if (state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, level, ticks, pos, side, neighborPos, neighbor, random);
    }
}
