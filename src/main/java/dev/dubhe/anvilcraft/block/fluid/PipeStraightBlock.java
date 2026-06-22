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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 直管道，沿单一轴向（{@link PipeBlock#AXIS X/Y/Z}）延伸。
 *
 * <p>两端通过 {@link PipeBlock#HAS_END_START} / {@link PipeBlock#HAS_END_END}
 * 控制端头开关：
 * <ul>
 *   <li>{@code true} — 有端头（封闭）</li>
 *   <li>{@code false} — 无端头（开放，连接至管道）</li>
 * </ul>
 *
 * <p>当管道出现在侧面（垂直方向）时，自动转为
 * {@link PipeNodeBlock}，由节点的 {@code onPlace}+{@code trySimplify}
 * 决定最终形态（弯管或节点）。
 */
public class PipeStraightBlock extends PipeBlock {

    /**
     * 构造直管，默认沿 X 轴，两端均有端头。
     */
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

    /**
     * 放置时沿点击面的轴向放置。
     */
    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
            .setValue(AXIS, context.getClickedFace().getAxis())
            .setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
    }

    /**
     * 碰撞箱：中心体 + 两端按端头状态拼接 {@code noEnd} 或 {@code end} arm。
     */
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
     *   <li><b>侧面方向</b>（非轴方向）：有管道对准时转为 {@link PipeNodeBlock}，
     *       由节点扫描全方向并自动退化</li>
     *   <li><b>轴端方向</b>：邻居是管道则开端口（无端头），否则关端口（有端头）</li>
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
        Direction.Axis axis = state.getValue(AXIS);

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

        if (neighborDir.getAxis() != axis) {
            // 侧面连接：检查邻居管道是否对准本方块，或邻居是泵
            BlockState neighborState = level.getBlockState(neighborPos);
            boolean neighborIsPipeToward = isNeighborPipeToward(level, pos, neighborDir);
            boolean neighborIsPump = neighborState.getBlock() instanceof PumpBlock;
            if (!neighborIsPipeToward && !neighborIsPump) {
                return;
            }

            // 转为节点 → 扫描全方向 → 自动退化
            BlockState nodeState = ModBlocks.PIPE_NODE.get().defaultBlockState().setValue(WATERLOGGED, state.getValue(WATERLOGGED));
            for (Direction dir : Direction.values()) {
                nodeState = nodeState.setValue(getPropertyForDirection(dir), PipeNodeBlock.evaluateNeighbor(level, pos, dir));
            }
            level.setBlockAndUpdate(pos, nodeState);
            return;
        }

        // 轴端更新：开/关端头
        Direction startDir = getDirectionFromAxis(axis, Direction.AxisDirection.NEGATIVE);
        Direction ignore = getDirectionFromAxis(axis, Direction.AxisDirection.POSITIVE);
        boolean neighborIsPipe = level.getBlockState(neighborPos).getBlock() instanceof PipeBlock;
        this.changePipeState(level, pos, state, startDir, neighborDir, neighborIsPipe);
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
