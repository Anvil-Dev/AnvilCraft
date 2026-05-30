package dev.dubhe.anvilcraft.block.fluid;

import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.Tags;

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
        if (isFluidHandler(level, neighborPos)) {
            return NodePipe.END;
        }
        return NodePipe.NONE;
    }

    private static BlockState trySimplify(BlockState state) {
        List<Direction> pipeDirs = new ArrayList<>();
        boolean hasEnd = false;
        for (Direction dir : Direction.values()) {
            NodePipe value = state.getValue(getPropertyForDirection(dir));
            if (value == NodePipe.PIPE) {
                pipeDirs.add(dir);
            } else if (value == NodePipe.END) {
                hasEnd = true;
            }
        }

        if (pipeDirs.size() <= 2 && !hasEnd) {
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

    @Override
    protected ItemInteractionResult useItemOn(
        ItemStack stack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hitResult
    ) {
        if (!stack.is(Tags.Items.TOOLS_WRENCH)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        }
        if (level.isClientSide()) {
            return ItemInteractionResult.sidedSuccess(true);
        }

        Vec3 loc = hitResult.getLocation();
        double bx = loc.x - pos.getX();
        double by = loc.y - pos.getY();
        double bz = loc.z - pos.getZ();

        Direction armDir = null;
        double maxDist = 0;
        for (Direction dir : Direction.values()) {
            double dist = switch (dir) {
                case NORTH -> bz < 3.0 / 16 ? 3.0 / 16 - bz : 0;
                case SOUTH -> bz > 13.0 / 16 ? bz - 13.0 / 16 : 0;
                case WEST -> bx < 3.0 / 16 ? 3.0 / 16 - bx : 0;
                case EAST -> bx > 13.0 / 16 ? bx - 13.0 / 16 : 0;
                case DOWN -> by < 3.0 / 16 ? 3.0 / 16 - by : 0;
                case UP -> by > 13.0 / 16 ? by - 13.0 / 16 : 0;
            };
            if (dist > maxDist) {
                maxDist = dist;
                armDir = dir;
            }
        }
        if (armDir == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        EnumProperty<NodePipe> prop = getPropertyForDirection(armDir);
        NodePipe current = state.getValue(prop);
        if (current == NodePipe.NONE) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (current == NodePipe.PIPE) {
            BlockPos neighborPos = pos.relative(armDir);
            BlockState neighborState = level.getBlockState(neighborPos);
            if (!(neighborState.getBlock() instanceof PipeNodeBlock)) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
        }

        BlockState newState = state.setValue(prop, NodePipe.NONE);
        newState = trySimplify(newState);
        level.setBlockAndUpdate(pos, newState);
        return ItemInteractionResult.sidedSuccess(false);
    }
}
