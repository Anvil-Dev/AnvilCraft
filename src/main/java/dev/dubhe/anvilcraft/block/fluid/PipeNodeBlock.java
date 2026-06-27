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

/**
 * 管道节点（多向连接器），最多支持六个方向的连接。
 *
 * <p>每个方向独立记录连接状态（{@link PipeBlock.NodePipe}）：
 * <ul>
 *   <li>{@link PipeBlock.NodePipe#PIPE PIPE} — 无端头，连接至另一管道</li>
 *   <li>{@link PipeBlock.NodePipe#END END}   — 有端头，连接至 IFluidHandler</li>
 *   <li>{@link PipeBlock.NodePipe#NONE NONE} — 无臂，该方向无连接</li>
 * </ul>
 *
 * <p>节点支持<b>自动退化</b>：当总连接数 ≤ 2 时自动简化为
 * {@link PipeStraightBlock}（对向）或 {@link PipeCornerBlock}（垂直）。
 *
 * <p>支持<b>扳手断开</b>：用扳手右键点击节点的臂可断开
 * 节点↔节点 或 节点↔IFluidHandler 的连接。
 */
public class PipeNodeBlock extends PipeBlock {

    /**
     * 节点默认全方向无臂
     */
    public PipeNodeBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.getStateDefinition()
            .any()
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

    /**
     * 碰撞箱：中心体 + 每个非 NONE 方向按状态拼接 arm（PIPE→noEnd, END→end）。
     */
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

    /**
     * 放置或从其他管型转换而来时，扫描全部六个方向并尝试退化。
     * 仅在方块类型发生变化（非同类替换）时执行。
     */
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

    /**
     * 邻居更新：仅计算变化来源方向的新状态并更新，更新后尝试退化。
     */
    @Override
    protected void neighborChanged(
        BlockState state,
        Level level,
        BlockPos pos,
        Block neighborBlock,
        BlockPos neighborPos,
        boolean movedByPiston
    ) {
        if (level.isClientSide()) {
            return;
        }

        // 查找邻居相对于本方块的方向
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

    /**
     * 评估指定方向邻居的连接状态。
     * <ul>
     *   <li>邻居是管道且对准本节点 → {@link NodePipe#PIPE}</li>
     *   <li>邻居是 IFluidHandler 或 PumpBlock → {@link NodePipe#END}</li>
     *   <li>其他 → {@link NodePipe#NONE}</li>
     * </ul>
     *
     * @param level 世界
     * @param pos   本节点位置
     * @param dir   评估方向
     * @return 该方向应设置的连接状态
     */
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
        if (isFluidHandler(level, neighborPos)) {
            return NodePipe.END;
        }
        return NodePipe.NONE;
    }

    /**
     * 尝试将节点简化为更简单的管型。
     * <ul>
     *   <li>总连接数 &gt; 2 或 0 → 保持节点</li>
     *   <li>2 连接同轴 → {@link PipeStraightBlock}</li>
     *   <li>2 连接异轴 → {@link PipeCornerBlock}</li>
     *   <li>1 连接 → {@link PipeStraightBlock}（管端无端头/非管端有端头）</li>
     * </ul>
     *
     * @param state 当前节点状态
     * @return 简化后的方块状态（可能仍是节点）
     */
    private static BlockState trySimplify(BlockState state) {
        // 统计 PIPE 和 END 方向
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
            return state; // 3+ 连接 → 保持节点
        }
        if (total == 0) {
            return state; // 无连接 → 保持节点
        }

        if (total == 2) {
            // 两个连接 → 直管或弯管
            List<Direction> all = new ArrayList<>();
            all.addAll(pipeDirs);
            all.addAll(endDirs);
            Direction pipe1 = all.get(0);
            Direction pipe2 = all.get(1);
            boolean pipe1IsPipe = pipeDirs.contains(pipe1);
            boolean pipe2IsPipe = pipeDirs.contains(pipe2);

            if (pipe1.getAxis() == pipe2.getAxis()) {
                // 同轴 → 直管
                Direction.Axis ax = pipe1.getAxis();
                Direction neg = getDirectionFromAxis(ax, Direction.AxisDirection.NEGATIVE);
                return ModBlocks.PIPE_STRAIGHT.get()
                    .defaultBlockState()
                    .setValue(AXIS, ax)
                    .setValue(HAS_END_START, neg == pipe1 ? !pipe1IsPipe : !pipe2IsPipe)
                    .setValue(HAS_END_END, neg == pipe1 ? !pipe2IsPipe : !pipe1IsPipe)
                    .setValue(WATERLOGGED, state.getValue(WATERLOGGED));
            } else {
                // 异轴 → 弯管
                CornerEnded corner = CornerEnded.fromDirections(pipe1, pipe2);
                boolean firstIsA = corner.getFirstDirection() == pipe1;
                return ModBlocks.PIPE_CORNER.get()
                    .defaultBlockState()
                    .setValue(CORNER_ENDED, corner)
                    .setValue(HAS_END_START, firstIsA ? !pipe1IsPipe : !pipe2IsPipe)
                    .setValue(HAS_END_END, firstIsA ? !pipe2IsPipe : !pipe1IsPipe)
                    .setValue(WATERLOGGED, state.getValue(WATERLOGGED));
            }
        }

        // total == 1：单连接 → 直管（管端无端头，另一端有端头）
        Direction only = !pipeDirs.isEmpty() ? pipeDirs.getFirst() : endDirs.getFirst();
        boolean onlyIsPipe = !pipeDirs.isEmpty();
        Direction.Axis ax = only.getAxis();
        Direction neg = getDirectionFromAxis(ax, Direction.AxisDirection.NEGATIVE);
        return ModBlocks.PIPE_STRAIGHT.get()
            .defaultBlockState()
            .setValue(AXIS, ax)
            .setValue(HAS_END_START, neg != only || !onlyIsPipe)
            .setValue(HAS_END_END, neg == only || !onlyIsPipe)
            .setValue(WATERLOGGED, state.getValue(WATERLOGGED));
    }

    /**
     * 遍历全部六个方向，用 {@link #evaluateNeighbor} 计算每个方向的状态。
     */
    private static BlockState scanAllDirections(BlockState state, Level level, BlockPos pos) {
        BlockState updated = state;
        for (Direction dir : Direction.values()) {
            updated = updated.setValue(getPropertyForDirection(dir), evaluateNeighbor(level, pos, dir));
        }
        return updated;
    }

    /**
     * 扳手交互：右键节点的臂可断开连接。
     * <ul>
     *   <li>节点 ↔ 节点（PIPE）→ 断开</li>
     *   <li>节点 ↔ IFluidHandler（END）→ 断开</li>
     *   <li>节点 ↔ 直管/弯管（PIPE）→ 不断开（放行默认交互）</li>
     * </ul>
     * 断开后自动尝试退化。
     */
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
        // 非扳手 → 放行默认交互
        if (!stack.is(Tags.Items.TOOLS_WRENCH)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        }
        // 客户端直接返回成功，服务端处理实际逻辑
        if (level.isClientSide()) {
            return ItemInteractionResult.sidedSuccess(true);
        }

        // 根据精确点击位置确定被命中的臂方向
        Direction armDir = getArmDirection(pos, hitResult);
        if (armDir == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        EnumProperty<NodePipe> prop = getPropertyForDirection(armDir);
        NodePipe current = state.getValue(prop);
        if (current == NodePipe.NONE) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // PIPE 时仅断开与另一节点的连接，直管/弯管不断开
        if (current == NodePipe.PIPE) {
            BlockPos neighborPos = pos.relative(armDir);
            BlockState neighborState = level.getBlockState(neighborPos);
            if (!(neighborState.getBlock() instanceof PipeNodeBlock)) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
        }

        // 断开连接 + 退化
        BlockState newState = state.setValue(prop, NodePipe.NONE);
        newState = trySimplify(newState);
        level.setBlockAndUpdate(pos, newState);
        return ItemInteractionResult.sidedSuccess(false);
    }

    /**
     * 根据精确点击坐标判断被点击的臂方向。
     * 节点中心为 [3,3,3]→[13,13,13]（相对于 0-1 范围即 3/16→13/16），
     * 点击超出此范围的方向即为命中对应臂。
     *
     * @param pos       节点位置
     * @param hitResult 点击结果（含精确世界坐标）
     * @return 被命中的臂方向，点击在中心区域则返回 {@code null}
     */
    private static @Nullable Direction getArmDirection(BlockPos pos, BlockHitResult hitResult) {
        Vec3 loc = hitResult.getLocation();
        double bx = loc.x - pos.getX(); // 方块内相对坐标 [0, 1]
        double by = loc.y - pos.getY();
        double bz = loc.z - pos.getZ();

        // 找出偏离中心最远的方向
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
