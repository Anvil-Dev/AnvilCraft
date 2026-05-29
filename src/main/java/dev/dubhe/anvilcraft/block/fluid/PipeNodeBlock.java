package dev.dubhe.anvilcraft.block.fluid;

import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.List;

public class PipeNodeBlock extends PipeBlock {
    public PipeNodeBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
            this.getStateDefinition()
                .any()
                .setValue(DOWN, NodePipe.NONE)
                .setValue(UP, NodePipe.NONE)
                .setValue(NORTH, NodePipe.NONE)
                .setValue(SOUTH, NodePipe.NONE)
                .setValue(WEST, NodePipe.NONE)
                .setValue(EAST, NodePipe.NONE)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(DOWN)
            .add(UP)
            .add(NORTH)
            .add(SOUTH)
            .add(WEST)
            .add(EAST);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        VoxelShape shape = NODE_CENTER;
        for (Direction dir : Direction.values()) {
            NodePipe value = state.getValue(getPropertyForDirection(dir));
            if (value == NodePipe.PIPE) {
                shape = Shapes.or(shape, makeNoEnd(dir));
            } else if (value == NodePipe.END) {
                shape = Shapes.or(shape, makeEnd(dir));
            }
        }
        return shape;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (state.is(oldState.getBlock())) return;
        BlockState updated = scanAllDirections(state, level, pos);
        if (updated != state) {
            level.setBlockAndUpdate(pos, updated);
        }
    }

    @Override
    protected void neighborChanged(
        BlockState state, Level level, BlockPos pos, Block neighborBlock,
        BlockPos neighborPos, boolean movedByPiston
    ) {
        if (level.isClientSide) return;

        Direction neighborDir = null;
        for (Direction dir : Direction.values()) {
            if (pos.relative(dir).equals(neighborPos)) {
                neighborDir = dir;
                break;
            }
        }
        if (neighborDir == null) return;

        EnumProperty<NodePipe> prop = getPropertyForDirection(neighborDir);
        NodePipe newValue = evaluateNeighbor(level, pos, neighborDir);
        if (state.getValue(prop) == newValue) return;

        BlockState newState = state.setValue(prop, newValue);
        BlockState simplified = trySimplify(newState);
        level.setBlockAndUpdate(pos, simplified);
    }

    public static NodePipe evaluateNeighbor(Level level, BlockPos pos, Direction dir) {
        BlockPos neighborPos = pos.relative(dir);
        BlockState neighborState = level.getBlockState(neighborPos);
        if (neighborState.getBlock() instanceof PipeBlock && hasConnectionToward(neighborState, dir.getOpposite())) {
            return NodePipe.PIPE;
        }
        if (level.getBlockEntity(neighborPos) instanceof IFluidHandler) {
            return NodePipe.END;
        }
        return NodePipe.NONE;
    }

    private static BlockState trySimplify(BlockState state) {
        List<Direction> pipeDirs = new ArrayList<>();
        boolean hasEnd = false;
        for (Direction dir : Direction.values()) {
            NodePipe value = state.getValue(getPropertyForDirection(dir));
            if (value == NodePipe.PIPE) pipeDirs.add(dir);
            else if (value == NodePipe.END) hasEnd = true;
        }

        if (pipeDirs.size() == 2 && !hasEnd) {
            Direction a = pipeDirs.get(0);
            Direction b = pipeDirs.get(1);
            if (a.getOpposite() == b) {
                return ModBlocks.PIPE_STRAIGHT.get().defaultBlockState()
                    .setValue(AXIS, a.getAxis())
                    .setValue(HAS_END_START, false)
                    .setValue(HAS_END_END, false)
                    .setValue(WATERLOGGED, state.getValue(WATERLOGGED));
            } else {
                return ModBlocks.PIPE_CORNER.get().defaultBlockState()
                    .setValue(CORNER_ENDED, CornerEnded.fromDirections(a, b))
                    .setValue(HAS_END_START, false)
                    .setValue(HAS_END_END, false)
                    .setValue(WATERLOGGED, state.getValue(WATERLOGGED));
            }
        }

        return state;
    }

    private static BlockState scanAllDirections(BlockState state, Level level, BlockPos pos) {
        BlockState updated = state;
        for (Direction dir : Direction.values()) {
            updated = updated.setValue(getPropertyForDirection(dir), evaluateNeighbor(level, pos, dir));
        }
        return updated;
    }
}
