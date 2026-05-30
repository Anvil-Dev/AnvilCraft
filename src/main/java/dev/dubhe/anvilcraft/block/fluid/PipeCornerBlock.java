package dev.dubhe.anvilcraft.block.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PipeCornerBlock extends PipeBlock {
    public PipeCornerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.getStateDefinition()
            .any()
            .setValue(CORNER_ENDED, CornerEnded.UP_NORTH)
            .setValue(HAS_END_START, true)
            .setValue(HAS_END_END, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(CORNER_ENDED);
        builder.add(HAS_END_START);
        builder.add(HAS_END_END);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        CornerEnded corner = state.getValue(CORNER_ENDED);
        Direction startDir = corner.getFirstDirection();
        Direction endDir = corner.getSecondDirection();
        return this.getShape(state, startDir, endDir);
    }

    @Override
    protected void neighborChanged(
        BlockState state,
        Level level,
        BlockPos pos,
        Block neighborBlock,
        BlockPos neighborPos,
        boolean movedByPiston
    ) {
        if (level.isClientSide) return;
        CornerEnded corner = state.getValue(CORNER_ENDED);

        Direction neighborDir = null;
        for (Direction dir : Direction.values()) {
            if (pos.relative(dir).equals(neighborPos)) {
                neighborDir = dir;
                break;
            }
        }
        if (neighborDir == null) return;

        if (!corner.containsDirection(neighborDir)) return;

        boolean neighborIsPipeToward = isNeighborPipeToward(level, pos, neighborDir);

        Direction startDir = corner.getFirstDirection();
        this.changePipeState(level, pos, state, startDir, neighborDir, neighborIsPipeToward);
    }
}
