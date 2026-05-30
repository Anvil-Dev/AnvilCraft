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

            BlockState nodeState = ModBlocks.PIPE_NODE.get().defaultBlockState()
                .setValue(WATERLOGGED, state.getValue(WATERLOGGED));
            for (Direction dir : Direction.values()) {
                nodeState = nodeState.setValue(getPropertyForDirection(dir),
                    PipeNodeBlock.evaluateNeighbor(level, pos, dir));
            }
            level.setBlockAndUpdate(pos, nodeState);
            return;
        }

        boolean neighborIsPipe = level.getBlockState(neighborPos).getBlock() instanceof PipeBlock;
        this.changePipeState(level, pos, state, startDir, neighborDir, neighborIsPipe);
    }
}
