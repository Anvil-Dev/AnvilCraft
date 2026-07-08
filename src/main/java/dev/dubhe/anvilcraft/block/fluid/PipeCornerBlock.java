package dev.dubhe.anvilcraft.block.fluid;

import dev.dubhe.anvilcraft.block.entity.fluid.PipeBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * 弯管道，通过 {@link PipeBlock#CORNER_ENDED} 指定两个垂直方向的拐角。
 * 当管道出现在非弯管方向的侧面时，自动转为 {@link PipeNodeBlock}。
 */
public class PipeCornerBlock extends PipeBlock {

    public PipeCornerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.getStateDefinition()
            .any()
            .setValue(WATERLOGGED, false)
            .setValue(HAS_CHECK_VALVE, false)
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

    /**
     * 邻居更新：
     * <ul>
     *   <li><b>非弯管方向（侧面）</b>出现对准的管道或连接面正对的泵 → 升级为节点</li>
     *   <li><b>无侧面连接</b> → 保持弯管形态，仅按两弯管臂的邻居刷新端头开关，
     *       断开一端只封头，不会塌成直管</li>
     * </ul>
     *
     * <p>26.1 的 {@code neighborChanged} 不再传入变更来源方向，故每次扫描全部方向，
     * 但以本方块自身 {@link PipeBlock#CORNER_ENDED} 为准，避免断连时丢失形状。
     */
    @Override
    protected void neighborChanged(
        BlockState state,
        Level level,
        BlockPos pos,
        Block neighborBlock,
        @Nullable Orientation orientation,
        boolean movedByPiston
    ) {
        if (level.isClientSide()) return;
        this.updateCheckValvePower(level, pos, state);
        CornerEnded corner = state.getValue(CORNER_ENDED);

        // 非弯管方向（侧面）出现对准的管道或连接面正对的泵 → 升级为节点
        for (Direction dir : Direction.values()) {
            if (corner.containsDirection(dir)) {
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
                    setBlockPreservingValve(level, pos, simplified);
                }
                return;
            }
        }

        // 无侧面连接 → 保持弯管，仅按两弯管臂的邻居刷新端头（断连只封头，不变直管）
        Direction first = corner.getFirstDirection();
        Direction second = corner.getSecondDirection();
        BlockState newState = state
            .setValue(HAS_END_START, !isNeighborPipeToward(level, pos, first))
            .setValue(HAS_END_END, !isNeighborPipeToward(level, pos, second));
        if (!newState.equals(state)) {
            setBlockPreservingValve(level, pos, newState);
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
