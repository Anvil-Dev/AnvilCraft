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
import org.jetbrains.annotations.Nullable;

/**
 * 弯管道，通过 {@link PipeBlock#CORNER_ENDED} 指定两个垂直方向的拐角。
 * 当管道出现在非弯管方向的侧面时，自动转为 {@link PipeNodeBlock}。
 */
public class PipeCornerBlock extends PipeBlock {

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

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        CornerEnded corner = state.getValue(CORNER_ENDED);
        Direction startDir = corner.getFirstDirection();
        Direction endDir = corner.getSecondDirection();
        return this.getShape(state, startDir, endDir);
    }

    /**
     * 邻居更新：全方向扫描重建连接状态。
     * 参考 1.21.1 风格，不依赖 orientation 参数，每次变更都扫描全部方向。
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
        BlockState nodeState = ModBlocks.PIPE_NODE.get().defaultBlockState()
            .setValue(WATERLOGGED, state.getValue(WATERLOGGED));
        for (Direction dir : Direction.values()) {
            nodeState = nodeState.setValue(getPropertyForDirection(dir),
                PipeNodeBlock.evaluateNeighbor(level, pos, dir));
        }
        BlockState simplified = PipeNodeBlock.trySimplify(nodeState);
        if (!simplified.equals(state)) {
            level.setBlockAndUpdate(pos, simplified);
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
