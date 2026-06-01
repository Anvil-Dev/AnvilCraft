package dev.dubhe.anvilcraft.block.fluid;

import dev.dubhe.anvilcraft.block.entity.fluid.PipeNodeBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;

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
        if (state.is(oldState.getBlock())) {
            return;
        }
        BlockState updated = scanAllDirections(state, level, pos);
        updated = trySimplify(updated);
        if (updated != state) {
            level.setBlockAndUpdate(pos, updated);
        }
    }

    @Override
    protected void neighborChanged(
        BlockState state, Level level, BlockPos pos, Block neighborBlock,
        BlockPos neighborPos, boolean movedByPiston
    ) {
        if (level.isClientSide()) {
            return;
        }

        Direction neighborDir = null;
        for (Direction dir : Direction.values()) {
            if (pos.relative(dir).equals(neighborPos)) {
                neighborDir = dir;
                break;
            }
        }
        if (neighborDir == null) {
            return;
        }

        EnumProperty<NodePipe> prop = getPropertyForDirection(neighborDir);
        NodePipe newValue = evaluateNeighbor(level, pos, neighborDir);
        if (state.getValue(prop) == newValue) {
            return;
        }

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
        List<Direction> endDirs = new ArrayList<>();
        for (Direction dir : Direction.values()) {
            NodePipe value = state.getValue(getPropertyForDirection(dir));
            if (value == NodePipe.PIPE) {
                pipeDirs.add(dir);
            } else if (value == NodePipe.END) {
                endDirs.add(dir);
            }
        }

        int total = pipeDirs.size() + endDirs.size();
        if (total > 2) {
            return state;
        }
        if (total == 0) {
            return state;
        }

        if (total == 2) {
            java.util.List<Direction> all = new ArrayList<>();
            all.addAll(pipeDirs);
            all.addAll(endDirs);
            Direction pipe1 = all.get(0);
            Direction pipe2 = all.get(1);
            boolean pipe1IsPipe = pipeDirs.contains(pipe1);
            boolean pipe2IsPipe = pipeDirs.contains(pipe2);
            if (pipe1.getAxis() == pipe2.getAxis()) {
                Direction.Axis ax = pipe1.getAxis();
                Direction neg = getDirectionFromAxis(ax, Direction.AxisDirection.NEGATIVE);
                return ModBlocks.PIPE_STRAIGHT.get().defaultBlockState()
                    .setValue(AXIS, ax)
                    .setValue(HAS_END_START, neg == pipe1 ? !pipe1IsPipe : !pipe2IsPipe)
                    .setValue(HAS_END_END, neg == pipe1 ? !pipe2IsPipe : !pipe1IsPipe)
                    .setValue(WATERLOGGED, state.getValue(WATERLOGGED));
            } else {
                CornerEnded corner = CornerEnded.fromDirections(pipe1, pipe2);
                boolean firstIsA = corner.getFirstDirection() == pipe1;
                return ModBlocks.PIPE_CORNER.get().defaultBlockState()
                    .setValue(CORNER_ENDED, corner)
                    .setValue(HAS_END_START, firstIsA ? !pipe1IsPipe : !pipe2IsPipe)
                    .setValue(HAS_END_END, firstIsA ? !pipe2IsPipe : !pipe1IsPipe)
                    .setValue(WATERLOGGED, state.getValue(WATERLOGGED));
            }
        }

        // total == 1: single pipe or end → straight pipe on that axis
        Direction only = !pipeDirs.isEmpty() ? pipeDirs.getFirst() : endDirs.getFirst();
        boolean onlyIsPipe = !pipeDirs.isEmpty();
        Direction.Axis ax = only.getAxis();
        Direction neg = getDirectionFromAxis(ax, Direction.AxisDirection.NEGATIVE);
        return ModBlocks.PIPE_STRAIGHT.get().defaultBlockState()
            .setValue(AXIS, ax)
            .setValue(HAS_END_START, neg != only || !onlyIsPipe)
            .setValue(HAS_END_END, neg == only || !onlyIsPipe)
            .setValue(WATERLOGGED, state.getValue(WATERLOGGED));
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

        Direction armDir = getArmDirection(pos, hitResult);
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

    private static @Nullable Direction getArmDirection(BlockPos pos, BlockHitResult hitResult) {
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
        return armDir;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.PIPE_NODE.create(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level,
        BlockState state,
        BlockEntityType<T> blockEntityType
    ) {
        return (l, p, s, ignore) -> PipeNodeBlockEntity.tick(l, p, s);
    }
}
