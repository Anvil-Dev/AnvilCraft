package dev.dubhe.anvilcraft.block.fluid;

import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.Tags;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 管道节点（多向连接器），最多支持六个方向的连接。
 * 支持自动退化（≤2连接时简化为直管/弯管）和扳手断开。
 */
public class PipeNodeBlock extends PipeBlock {

    public PipeNodeBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.getStateDefinition()
            .any()
            .setValue(WATERLOGGED, false)
            .setValue(HAS_CHECK_VALVE, false)
            .setValue(DOWN, NodePipe.NONE)
            .setValue(UP, NodePipe.NONE)
            .setValue(NORTH, NodePipe.NONE)
            .setValue(SOUTH, NodePipe.NONE)
            .setValue(WEST, NodePipe.NONE)
            .setValue(EAST, NodePipe.NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(DOWN).add(UP).add(NORTH).add(SOUTH).add(WEST).add(EAST);
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
    protected boolean hasArmToward(BlockState state, Direction dir) {
        return state.getValue(getPropertyForDirection(dir)) != NodePipe.NONE;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (state.is(oldState.getBlock())) return;
        BlockState updated = scanAllDirections(state, level, pos);
        updated = trySimplify(updated);
        if (!updated.equals(state)) setBlockPreservingValve(level, pos, updated);
    }

    @Override
    protected void neighborChanged(
        BlockState state, Level level, BlockPos pos,
        Block neighborBlock, @Nullable Orientation orientation, boolean movedByPiston
    ) {
        if (level.isClientSide()) return;
        this.updateCheckValvePower(level, pos, state);
        BlockState updated = scanAllDirections(state, level, pos);
        BlockState simplified = trySimplify(updated);
        if (!simplified.equals(state)) setBlockPreservingValve(level, pos, simplified);
    }

    public static NodePipe evaluateNeighbor(Level level, BlockPos pos, Direction dir) {
        BlockPos neighborPos = pos.relative(dir);
        BlockState neighborState = level.getBlockState(neighborPos);
        if (neighborState.getBlock() instanceof PipeBlock && hasConnectionToward(neighborState, dir.getOpposite())) {
            return NodePipe.PIPE;
        }
        if (neighborState.getBlock() instanceof PumpBlock) {
            // 泵仅在其连接面（朝向轴两端）正对节点时才形成端头连接
            return PumpBlock.isConnectableFace(neighborState, dir.getOpposite()) ? NodePipe.END : NodePipe.NONE;
        }
        if (neighborState.getBlock() instanceof ControlValveBlock) {
            // 控制阀仅在其连接面（朝向轴两端）正对节点时才形成端头连接
            return ControlValveBlock.isConnectableFace(neighborState, dir.getOpposite()) ? NodePipe.END : NodePipe.NONE;
        }
        if (isFluidHandler(level, neighborPos)) {
            return NodePipe.END;
        }
        return NodePipe.NONE;
    }

    static BlockState trySimplify(BlockState state) {
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
        if (total > 2 || total == 0) return state;

        if (total == 2) {
            List<Direction> all = new ArrayList<>();
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
        // total == 1
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

    /**
     * 扳手交互：右键节点的臂可断开连接。
     */
    @Override
    protected InteractionResult useItemOn(
        ItemStack stack, BlockState state, Level level, BlockPos pos,
        Player player, InteractionHand hand, BlockHitResult hitResult
    ) {
        InteractionResult result = super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        if (
            result != InteractionResult.PASS && result != InteractionResult.TRY_WITH_EMPTY_HAND
            || !(stack.is(Tags.Items.TOOLS_WRENCH) || stack.is(ModItemTags.ANVIL_HAMMER))
        ) {
            return result;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        Direction armDir = getNodeArmDirection(pos, hitResult);
        if (armDir == null) return InteractionResult.PASS;
        EnumProperty<NodePipe> prop = getPropertyForDirection(armDir);
        NodePipe current = state.getValue(prop);
        if (current == NodePipe.NONE) return InteractionResult.PASS;
        if (current == NodePipe.PIPE) {
            BlockPos neighborPos = pos.relative(armDir);
            BlockState neighborState = level.getBlockState(neighborPos);
            if (!(neighborState.getBlock() instanceof PipeNodeBlock)) {
                return InteractionResult.PASS;
            }
        }
        BlockState newState = state.setValue(prop, NodePipe.NONE);
        newState = trySimplify(newState);
        setBlockPreservingValve(level, pos, newState);
        return InteractionResult.CONSUME;
    }

    private static @Nullable Direction getNodeArmDirection(BlockPos pos, BlockHitResult hitResult) {
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
                case WEST  -> bx < 3.0 / 16 ? 3.0 / 16 - bx : 0;
                case EAST  -> bx > 13.0 / 16 ? bx - 13.0 / 16 : 0;
                case DOWN  -> by < 3.0 / 16 ? 3.0 / 16 - by : 0;
                case UP    -> by > 13.0 / 16 ? by - 13.0 / 16 : 0;
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
}
