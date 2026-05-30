package dev.dubhe.anvilcraft.block.fluid;

import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class PipeStraightBlock extends PipeBlock {
    public PipeStraightBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.getStateDefinition()
            .any()
            .setValue(AXIS, Direction.Axis.X)
            .setValue(HAS_END_START, true)
            .setValue(HAS_END_END, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(AXIS);
        builder.add(HAS_END_START);
        builder.add(HAS_END_END);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
            .setValue(AXIS, context.getClickedFace().getAxis())
            .setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        Direction.Axis axis = state.getValue(AXIS);
        Direction startDir = getDirectionFromAxis(axis, Direction.AxisDirection.NEGATIVE);
        Direction endDir = getDirectionFromAxis(axis, Direction.AxisDirection.POSITIVE);
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
        Direction.Axis axis = state.getValue(AXIS);
        Direction startDir = getDirectionFromAxis(axis, Direction.AxisDirection.NEGATIVE);
        Direction endDir = getDirectionFromAxis(axis, Direction.AxisDirection.POSITIVE);

        Direction neighborDir = null;
        for (Direction dir : Direction.values()) {
            if (pos.relative(dir).equals(neighborPos)) {
                neighborDir = dir;
                break;
            }
        }
        if (neighborDir == null) return;

        if (neighborDir.getAxis() != axis) {
            boolean neighborIsPipeToward = isNeighborPipeToward(level, pos, neighborDir);
            if (!neighborIsPipeToward) return;

            boolean startOccupied = isNeighborOccupied(level, pos, startDir);
            boolean endOccupied = isNeighborOccupied(level, pos, endDir);

            if (startOccupied && endOccupied) {
                BlockState nodeState = ModBlocks.PIPE_NODE.get().defaultBlockState().setValue(WATERLOGGED, state.getValue(WATERLOGGED));
                nodeState = nodeState.setValue(getPropertyForDirection(startDir), PipeNodeBlock.evaluateNeighbor(level, pos, startDir));
                nodeState = nodeState.setValue(getPropertyForDirection(endDir), PipeNodeBlock.evaluateNeighbor(level, pos, endDir));
                nodeState = nodeState.setValue(getPropertyForDirection(neighborDir), NodePipe.PIPE);
                level.setBlockAndUpdate(pos, nodeState);
            } else {
                Direction pipeEnd = startOccupied ? startDir : endDir;
                CornerEnded corner = CornerEnded.fromDirections(pipeEnd, neighborDir);
                boolean pipeEndIsPipe = isNeighborPipeToward(level, pos, pipeEnd);
                boolean firstIsPipeEnd = corner.getFirstDirection() == pipeEnd;
                boolean hasEndPipeEnd = !pipeEndIsPipe;
                BlockState cornerState = ModBlocks.PIPE_CORNER.get()
                    .defaultBlockState()
                    .setValue(WATERLOGGED, state.getValue(WATERLOGGED))
                    .setValue(CORNER_ENDED, corner)
                    .setValue(HAS_END_START, firstIsPipeEnd && hasEndPipeEnd)
                    .setValue(HAS_END_END, !firstIsPipeEnd && hasEndPipeEnd);
                level.setBlockAndUpdate(pos, cornerState);
            }
            return;
        }

        boolean neighborIsPipeToward = isNeighborPipeToward(level, pos, neighborDir);

        this.changePipeState(level, pos, state, startDir, neighborDir, neighborIsPipeToward);
    }
}
