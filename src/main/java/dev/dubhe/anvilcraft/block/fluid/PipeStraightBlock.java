package dev.dubhe.anvilcraft.block.fluid;

import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * 直管道，沿单一轴向（X/Y/Z）延伸。
 * 当管道出现在侧面（垂直方向）时，自动转为 PipeNodeBlock。
 */
public class PipeStraightBlock extends PipeBlock {

    public PipeStraightBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.getStateDefinition()
            .any()
            .setValue(PipeBlock.WATERLOGGED, false)
            .setValue(PipeBlock.HAS_CHECK_VALVE, false)
            .setValue(PipeBlock.AXIS, Direction.Axis.X)
            .setValue(PipeBlock.HAS_END_START, true)
            .setValue(PipeBlock.HAS_END_END, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PipeBlock.AXIS);
        builder.add(PipeBlock.HAS_END_START);
        builder.add(PipeBlock.HAS_END_END);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
            .setValue(PipeBlock.AXIS, context.getClickedFace().getAxis())
            .setValue(PipeBlock.WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        Direction.Axis axis = state.getValue(PipeBlock.AXIS);
        Direction startDir = PipeBlock.getDirectionFromAxis(axis, Direction.AxisDirection.NEGATIVE);
        Direction endDir = PipeBlock.getDirectionFromAxis(axis, Direction.AxisDirection.POSITIVE);
        return this.getShape(state, startDir, endDir);
    }

    /**
     * 邻居更新：
     * <ul>
     *   <li><b>侧面（非轴向）</b>出现对准的管道或连接面正对的泵 → 升级为节点
     *       （由 {@link PipeNodeBlock#trySimplify} 决定最终形态）</li>
     *   <li><b>无侧面连接</b> → 保持直管形态，仅按轴端邻居刷新端头开关，
     *       断开连接也不会塌成节点</li>
     * </ul>
     *
     * <p>26.1 的 {@code neighborChanged} 不再传入变更来源方向，故每次扫描全部方向，
     * 但以本方块自身轴向 {@link PipeBlock#AXIS} 为准，避免断连时丢失形状。
     */
    @Override
    protected void neighborChanged(
        BlockState state, Level level, BlockPos pos,
        Block neighborBlock, @Nullable Orientation orientation, boolean movedByPiston
    ) {
        if (level.isClientSide()) return;
        this.updateCheckValvePower(level, pos, state);
        Direction.Axis axis = state.getValue(PipeBlock.AXIS);

        // 侧面（非轴向）出现对准的管道或连接面正对的泵 → 升级为节点
        for (Direction dir : Direction.values()) {
            if (dir.getAxis() == axis) {
                continue;
            }
            BlockState neighborState = level.getBlockState(pos.relative(dir));
            boolean sidePump = neighborState.getBlock() instanceof PumpBlock
                && PumpBlock.isConnectableFace(neighborState, dir.getOpposite());
            if (PipeBlock.isNeighborPipeToward(level, pos, dir) || sidePump) {
                BlockState nodeState = ModBlocks.PIPE_NODE.get().defaultBlockState()
                    .setValue(PipeBlock.WATERLOGGED, state.getValue(PipeBlock.WATERLOGGED));
                for (Direction d : Direction.values()) {
                    nodeState = nodeState.setValue(
                        PipeBlock.getPropertyForDirection(d),
                        PipeNodeBlock.evaluateNeighbor(level, pos, d));
                }
                BlockState simplified = PipeNodeBlock.trySimplify(nodeState);
                if (!simplified.equals(state)) {
                    PipeBlock.setBlockPreservingValve(level, pos, simplified);
                }
                return;
            }
        }

        // 无侧面连接 → 保持直管，仅按轴端邻居刷新端头（断连只封头，不变节点）
        Direction startDir = PipeBlock.getDirectionFromAxis(axis, Direction.AxisDirection.NEGATIVE);
        Direction endDir = PipeBlock.getDirectionFromAxis(axis, Direction.AxisDirection.POSITIVE);
        BlockState newState = state
            .setValue(PipeBlock.HAS_END_START, !PipeBlock.isNeighborPipeToward(level, pos, startDir))
            .setValue(PipeBlock.HAS_END_END, !PipeBlock.isNeighborPipeToward(level, pos, endDir));
        if (!newState.equals(state)) {
            PipeBlock.setBlockPreservingValve(level, pos, newState);
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.PIPE.create(pos, state);
    }
}
