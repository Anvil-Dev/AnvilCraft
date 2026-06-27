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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 弯管道，通过 {@link PipeBlock#CORNER_ENDED} 指定两个垂直方向的拐角。
 *
 * <p>两端各由 {@link PipeBlock#HAS_END_START} / {@link PipeBlock#HAS_END_END}
 * 控制端头开关。当管道出现在非弯管方向的侧面时，自动转为
 * {@link PipeNodeBlock}。
 */
public class PipeCornerBlock extends PipeBlock {

    /**
     * 构造弯管，默认上→北拐角，两端均有端头。
     */
    public PipeCornerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.getStateDefinition()
            .any()
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

    /**
     * 碰撞箱：中心体 + 两弯管方向按端头状态拼接 arm。
     */
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
     *   <li><b>非弯管方向</b>：有管道对准时转为 {@link PipeNodeBlock}，
     *       由节点扫描全方向并自动退化</li>
     *   <li><b>弯管方向</b>：邻居是管道则开端口（无端头），否则关端口（有端头）</li>
     * </ul>
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
        if (level.isClientSide) {
            return;
        }
        CornerEnded corner = state.getValue(CORNER_ENDED);

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

        if (!corner.containsDirection(neighborDir)) {
            // 非弯管方向（侧面）：有对准的管道，或连接面正对本方块的泵 → 转节点
            BlockState neighborState = level.getBlockState(neighborPos);
            boolean neighborIsPump = neighborState.getBlock() instanceof PumpBlock
                && PumpBlock.isConnectableFace(neighborState, neighborDir.getOpposite());
            if (isNeighborPipeToward(level, pos, neighborDir) || neighborIsPump) {
                BlockState nodeState = ModBlocks.PIPE_NODE.get().defaultBlockState().setValue(WATERLOGGED, state.getValue(WATERLOGGED));
                for (Direction dir : Direction.values()) {
                    nodeState = nodeState.setValue(getPropertyForDirection(dir), PipeNodeBlock.evaluateNeighbor(level, pos, dir));
                }
                level.setBlockAndUpdate(pos, nodeState);
            }
            return;
        }

        // 弯管方向：开/关端头
        boolean neighborIsPipeToward = isNeighborPipeToward(level, pos, neighborDir);
        Direction startDir = corner.getFirstDirection();
        this.changePipeState(level, pos, state, startDir, neighborDir, neighborIsPipeToward);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.PIPE.create(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level,
        BlockState state,
        BlockEntityType<T> blockEntityType
    ) {
        return (l, p, s, ignore) -> PipeBlockEntity.tick(l, p, s);
    }
}
