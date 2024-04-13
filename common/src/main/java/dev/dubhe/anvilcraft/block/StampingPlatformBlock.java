package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class StampingPlatformBlock extends Block implements SimpleWaterloggedBlock, IHammerRemovable {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final VoxelShape REDUCE_AABB = Shapes.or(
            Block.box(2.0, 12.0, 2.0, 14.0, 16.0, 14.0),
            Block.box(2.0, 0.0, 2.0, 14.0, 10.0, 14.0),
            Block.box(4.0, 0.0, 0.0, 12.0, 10.0, 16.0),
            Block.box(0.0, 0.0, 4.0, 16.0, 10.0, 12.0)
    );
    private static final VoxelShape AABB = Shapes.join(Shapes.block(), REDUCE_AABB, BooleanOp.NOT_SAME);

    public StampingPlatformBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(@NotNull StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WATERLOGGED);
    }

    @Override
    @SuppressWarnings("deprecation")
    public @NotNull VoxelShape getShape(
        @NotNull BlockState blockState, @NotNull BlockGetter blockGetter,
        @NotNull BlockPos blockPos, @NotNull CollisionContext collisionContext
    ) {
        return AABB;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean useShapeForLightOcclusion(@NotNull BlockState blockState) {
        return true;
    }

    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext blockPlaceContext) {
        BlockPos blockPos = blockPlaceContext.getClickedPos();
        FluidState fluidState = blockPlaceContext.getLevel().getFluidState(blockPos);
        BlockState state = super.getStateForPlacement(blockPlaceContext);
        state = null != state ? state : this.defaultBlockState();
        return state.setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
    }

    @Override
    @SuppressWarnings("deprecation")
    public @NotNull FluidState getFluidState(@NotNull BlockState blockState) {
        return blockState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(blockState);
    }

    @Override
    @SuppressWarnings("deprecation")
    public @NotNull BlockState updateShape(
        @NotNull BlockState blockState, @NotNull Direction direction, @NotNull BlockState blockState2,
        @NotNull LevelAccessor levelAccessor, @NotNull BlockPos blockPos, @NotNull BlockPos blockPos2
    ) {
        if (blockState.getValue(WATERLOGGED)) {
            levelAccessor.scheduleTick(blockPos, Fluids.WATER, Fluids.WATER.getTickDelay(levelAccessor));
        }
        return super.updateShape(blockState, direction, blockState2, levelAccessor, blockPos, blockPos2);
    }
}
