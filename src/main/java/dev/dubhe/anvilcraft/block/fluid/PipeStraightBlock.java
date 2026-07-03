package dev.dubhe.anvilcraft.block.fluid;

import dev.dubhe.anvilcraft.block.entity.fluid.PipeBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
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
        Direction.Axis axis = state.getValue(AXIS);

        // 侧面（非轴向）出现对准的管道或连接面正对的泵 → 升级为节点
        for (Direction dir : Direction.values()) {
            if (dir.getAxis() == axis) {
                continue;
            }
            BlockState neighborState = level.getBlockState(pos.relative(dir));
            boolean sidePump = neighborState.getBlock() instanceof PumpBlock
                && PumpBlock.isConnectableFace(neighborState, dir.getOpposite());
            if (isNeighborPipeToward(level, pos, dir) || sidePump) {
                BlockState nodeState = ModBlocks.PIPE_NODE.get().defaultBlockState()
                    .setValue(WATERLOGGED, state.getValue(WATERLOGGED));
                for (Direction d : Direction.values()) {
                    nodeState = nodeState.setValue(getPropertyForDirection(d),
                        PipeNodeBlock.evaluateNeighbor(level, pos, d));
                }
                BlockState simplified = PipeNodeBlock.trySimplify(nodeState);
                if (!simplified.equals(state)) {
                    level.setBlockAndUpdate(pos, simplified);
                }
                return;
            }
        }

        // 无侧面连接 → 保持直管，仅按轴端邻居刷新端头（断连只封头，不变节点）
        Direction startDir = getDirectionFromAxis(axis, Direction.AxisDirection.NEGATIVE);
        Direction endDir = getDirectionFromAxis(axis, Direction.AxisDirection.POSITIVE);
        BlockState newState = state
            .setValue(HAS_END_START, !isNeighborPipeToward(level, pos, startDir))
            .setValue(HAS_END_END, !isNeighborPipeToward(level, pos, endDir));
        if (!newState.equals(state)) {
            level.setBlockAndUpdate(pos, newState);
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.PIPE.create(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level, BlockState state, BlockEntityType<T> blockEntityType
    ) {
        return (l, p, s, ignore) -> PipeBlockEntity.tick(l, p, s);
    }
}
