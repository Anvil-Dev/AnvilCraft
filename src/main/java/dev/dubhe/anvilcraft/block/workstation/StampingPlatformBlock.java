package dev.dubhe.anvilcraft.block.workstation;

import dev.anvilcraft.lib.v2.recipe.util.IRecipeResultOffsetBlock;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class StampingPlatformBlock extends Block implements SimpleWaterloggedBlock, IHammerRemovable, IRecipeResultOffsetBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape REDUCE_AABB = Shapes.or(
        Block.box(2.0, 12.0, 2.0, 14.0, 16.0, 14.0),
        Block.box(2.0, 0.0, 2.0, 14.0, 10.0, 14.0),
        Block.box(4.0, 0.0, 0.0, 12.0, 10.0, 16.0),
        Block.box(0.0, 0.0, 4.0, 16.0, 10.0, 12.0));
    private static final VoxelShape REDUCE_AABB_INTERACTION = Shapes.or(
        Block.box(2.0, 0.0, 2.0, 14.0, 10.0, 14.0),
        Block.box(4.0, 0.0, 0.0, 12.0, 10.0, 16.0),
        Block.box(0.0, 0.0, 4.0, 16.0, 10.0, 12.0));
    private static final VoxelShape AABB = Shapes.join(Shapes.block(), StampingPlatformBlock.REDUCE_AABB, BooleanOp.ONLY_FIRST);
    private static final VoxelShape INTERACTION_BOX = Shapes.join(Shapes.block(), StampingPlatformBlock.REDUCE_AABB_INTERACTION, BooleanOp.ONLY_FIRST);

    public StampingPlatformBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
            this.stateDefinition.any()
                .setValue(StampingPlatformBlock.WATERLOGGED, false)
                .setValue(StampingPlatformBlock.FACING, Direction.NORTH)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(StampingPlatformBlock.WATERLOGGED);
        builder.add(StampingPlatformBlock.FACING);
    }

    @Override
    public VoxelShape getShape(
        BlockState blockState,
        BlockGetter blockGetter,
        BlockPos blockPos,
        CollisionContext collisionContext
    ) {
        return StampingPlatformBlock.AABB;
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return StampingPlatformBlock.INTERACTION_BOX;
    }

    @Override

    public boolean useShapeForLightOcclusion(BlockState blockState) {
        return true;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos blockPos = context.getClickedPos();
        FluidState fluidState = context.getLevel().getFluidState(blockPos);
        BlockState state = super.getStateForPlacement(context);
        state = null != state ? state : this.defaultBlockState();
        Direction facing = context.getHorizontalDirection().getOpposite();
        return state.setValue(StampingPlatformBlock.WATERLOGGED, fluidState.getType() == Fluids.WATER).setValue(StampingPlatformBlock.FACING, facing);
    }

    @Override

    public FluidState getFluidState(BlockState blockState) {
        return blockState.getValue(StampingPlatformBlock.WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(blockState);
    }

    @Override

    protected BlockState updateShape(
        BlockState blockState,
        LevelReader levelReader,
        ScheduledTickAccess ticks,
        BlockPos blockPos,
        Direction direction,
        BlockPos blockPos2,
        BlockState blockState2,
        RandomSource random
    ) {
        if (blockState.getValue(StampingPlatformBlock.WATERLOGGED)) {
            ticks.scheduleTick(blockPos, Fluids.WATER, Fluids.WATER.getTickDelay(levelReader));
        }
        return super.updateShape(blockState, levelReader, ticks, blockPos, direction, blockPos2, blockState2, random);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    public Vec3 getOffset(Level level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof StampingPlatformBlock)) return Vec3.ZERO;
        Vec3i normal = state.getValue(StampingPlatformBlock.FACING).getUnitVec3i();
        return new Vec3(normal.getX(), normal.getY(), normal.getZ()).scale(0.7);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(StampingPlatformBlock.FACING, rotation.rotate(state.getValue(StampingPlatformBlock.FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(StampingPlatformBlock.FACING, mirror.mirror(state.getValue(StampingPlatformBlock.FACING)));
    }
}
