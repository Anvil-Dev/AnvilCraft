package dev.dubhe.anvilcraft.block.utility.redstone;


import net.minecraft.world.level.block.state.BlockBehaviour;
import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.util.ShapeUtil;
import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.state.FacingWithAxis;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class BlockComparatorBlock extends Block implements IHammerRemovable, IHammerChangeable {

    public static final MapCodec<BlockComparatorBlock> CODEC = BlockBehaviour.simpleCodec(BlockComparatorBlock::new);

    public static final EnumProperty<FacingWithAxis> FACING_WITH_AXIS =
        EnumProperty.create("facing_with_axis", FacingWithAxis.class);
    public static final BooleanProperty PRECISE = BooleanProperty.create("precise");
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    private static final VoxelShape SHAPE_NORTH_X = ShapeUtil.merge(
        new AABB(4.0, 0, 0, 12.0, 16, 16),
        new AABB(2.0, 5.0, 1.0, 14.0, 11.0, 9.0),
        new AABB(0, 4.0, 0, 2.0, 12.0, 10.0),
        new AABB(14.0, 4.0, 0, 16, 12.0, 10.0)
    );
    private static final VoxelShape SHAPE_SOUTH_X = ShapeUtil.rotate(Direction.Axis.Y, 180, BlockComparatorBlock.SHAPE_NORTH_X);

    private static final VoxelShape SHAPE_WEST_Z = ShapeUtil.rotate(Direction.Axis.Y, 90, BlockComparatorBlock.SHAPE_NORTH_X);
    private static final VoxelShape SHAPE_EAST_Z = ShapeUtil.rotate(Direction.Axis.Y, 270, BlockComparatorBlock.SHAPE_NORTH_X);

    private static final VoxelShape SHAPE_NORTH_Y = ShapeUtil.rotate(Direction.Axis.Z, 90, BlockComparatorBlock.SHAPE_NORTH_X);
    private static final VoxelShape SHAPE_SOUTH_Y = ShapeUtil.rotate(Direction.Axis.Y, 180, BlockComparatorBlock.SHAPE_NORTH_Y);
    private static final VoxelShape SHAPE_WEST_Y = ShapeUtil.rotate(Direction.Axis.Y, 90, BlockComparatorBlock.SHAPE_NORTH_Y);
    private static final VoxelShape SHAPE_EAST_Y = ShapeUtil.rotate(Direction.Axis.Y, 270, BlockComparatorBlock.SHAPE_NORTH_Y);

    private static final VoxelShape SHAPE_UP_X = ShapeUtil.rotate(Direction.Axis.X, 270, BlockComparatorBlock.SHAPE_NORTH_X);
    private static final VoxelShape SHAPE_DOWN_X = ShapeUtil.rotate(Direction.Axis.X, 90, BlockComparatorBlock.SHAPE_NORTH_X);

    private static final VoxelShape SHAPE_UP_Z = ShapeUtil.rotate(Direction.Axis.Y, 90, BlockComparatorBlock.SHAPE_UP_X);
    private static final VoxelShape SHAPE_DOWN_Z = ShapeUtil.rotate(Direction.Axis.Y, 90, BlockComparatorBlock.SHAPE_DOWN_X);

    public BlockComparatorBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
            this.stateDefinition
                .any()
                .setValue(BlockComparatorBlock.FACING_WITH_AXIS, FacingWithAxis.NORTH_X)
                .setValue(BlockComparatorBlock.PRECISE, false)
                .setValue(BlockComparatorBlock.POWERED, false)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockComparatorBlock.FACING_WITH_AXIS).add(BlockComparatorBlock.PRECISE).add(BlockComparatorBlock.POWERED);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return BlockComparatorBlock.CODEC;
    }

    @Override
    public VoxelShape getShape(
        BlockState state,
        BlockGetter level,
        BlockPos pos,
        CollisionContext context
    ) {
        return BlockComparatorBlock.getShapeFor(state.getValue(BlockComparatorBlock.FACING_WITH_AXIS));
    }

    private static VoxelShape getShapeFor(FacingWithAxis fwa) {
        return switch (fwa) {
            case NORTH_X -> BlockComparatorBlock.SHAPE_NORTH_X;
            case SOUTH_X -> BlockComparatorBlock.SHAPE_SOUTH_X;
            case WEST_Z -> BlockComparatorBlock.SHAPE_WEST_Z;
            case EAST_Z -> BlockComparatorBlock.SHAPE_EAST_Z;
            case NORTH_Y -> BlockComparatorBlock.SHAPE_NORTH_Y;
            case SOUTH_Y -> BlockComparatorBlock.SHAPE_SOUTH_Y;
            case WEST_Y -> BlockComparatorBlock.SHAPE_WEST_Y;
            case EAST_Y -> BlockComparatorBlock.SHAPE_EAST_Y;
            case UP_X -> BlockComparatorBlock.SHAPE_UP_X;
            case UP_Z -> BlockComparatorBlock.SHAPE_UP_Z;
            case DOWN_X -> BlockComparatorBlock.SHAPE_DOWN_X;
            case DOWN_Z -> BlockComparatorBlock.SHAPE_DOWN_Z;
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getNearestLookingDirection();
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            facing = facing.getOpposite();
        }
        Direction.Axis axis;
        if (facing.getAxis() == Direction.Axis.Y) {
            axis = context.getHorizontalDirection().getClockWise().getAxis();
        } else {
            axis = facing.getClockWise().getAxis();
        }
        return this.defaultBlockState().setValue(BlockComparatorBlock.FACING_WITH_AXIS, FacingWithAxis.of(facing, axis));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (
            level.isClientSide()
            || (oldState.is(this)
                && state.getValue(BlockComparatorBlock.FACING_WITH_AXIS) == oldState.getValue(BlockComparatorBlock.FACING_WITH_AXIS))
        ) {
            return;
        }
        boolean newPowered = this.checkBlocks(level, pos, state);
        level.setBlock(pos, state.setValue(BlockComparatorBlock.POWERED, newPowered), 3);
        this.updateNeighborsInFront(level, pos, state);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        BlockState newState = level.getBlockState(pos);
        if (
            level.isClientSide()
            || (state.is(newState.getBlock())
                && state.getValue(BlockComparatorBlock.FACING_WITH_AXIS) == newState.getValue(BlockComparatorBlock.FACING_WITH_AXIS))
        ) {
            return;
        }
        if (state.getValue(BlockComparatorBlock.POWERED)) {
            this.updateNeighborsInFront(level, pos, state);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        BlockHitResult hitResult
    ) {
        if (!player.getAbilities().mayBuild) {
            return InteractionResult.PASS;
        } else {
            BlockState newState = state.cycle(BlockComparatorBlock.PRECISE);
            level.setBlock(pos, newState.setValue(BlockComparatorBlock.POWERED, this.checkBlocks(level, pos, newState)), 2);
            this.updateNeighborsInFront(level, pos, state);
            return InteractionResult.SUCCESS;
        }
    }

    private boolean checkBlocks(LevelAccessor level, BlockPos pos, BlockState blockState) {
        FacingWithAxis fwa = blockState.getValue(BlockComparatorBlock.FACING_WITH_AXIS);
        Direction.Axis axis = fwa.getAxis();
        Direction[] dirs = BlockComparatorBlock.getCompareDirections(axis);
        BlockState state1 = level.getBlockState(pos.relative(dirs[0]));
        BlockState state2 = level.getBlockState(pos.relative(dirs[1]));
        return blockState.getValue(BlockComparatorBlock.PRECISE)
               ? state1.equals(state2)
               : state1.getBlock() == state2.getBlock();
    }

    private static Direction[] getCompareDirections(Direction.Axis axis) {
        return new Direction[]{
            Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE),
            Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE)
        };
    }

    @Override
    protected BlockState updateShape(
        BlockState state,
        LevelReader level,
        ScheduledTickAccess ticks,
        BlockPos pos,
        Direction directionToNeighbour,
        BlockPos neighbourPos,
        BlockState neighbourState,
        RandomSource random
    ) {
        FacingWithAxis fwa = state.getValue(BlockComparatorBlock.FACING_WITH_AXIS);
        Direction facing = fwa.getFacing();
        Direction.Axis compareAxis = fwa.getAxis();
        if (directionToNeighbour.getAxis() == facing.getAxis()) return state;
        if (directionToNeighbour.getAxis() != compareAxis) return state;
        if (!level.isClientSide() && !ticks.getBlockTicks().hasScheduledTick(pos, this)) {
            ticks.scheduleTick(pos, this, 2);
        }
        return state;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean same = this.checkBlocks(level, pos, state);
        if (same != state.getValue(BlockComparatorBlock.POWERED)) {
            level.setBlock(pos, state.setValue(BlockComparatorBlock.POWERED, same), 2);
            this.updateNeighborsInFront(level, pos, state);
        }
    }

    protected void updateNeighborsInFront(Level level, BlockPos pos, BlockState state) {
        Direction direction = state.getValue(BlockComparatorBlock.FACING_WITH_AXIS).getFacing();
        BlockPos blockpos = pos.relative(direction.getOpposite());
        Orientation orientation = ExperimentalRedstoneUtils.initialOrientation(level, direction.getOpposite(), null);
        level.neighborChanged(blockpos, this, orientation);
        level.updateNeighborsAtExceptFromFacing(blockpos, this, direction, orientation);
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return direction == state.getValue(BlockComparatorBlock.FACING_WITH_AXIS).getFacing();
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getDirectSignal(
        BlockState blockState,
        BlockGetter blockAccess,
        BlockPos pos,
        Direction side
    ) {
        return blockState.getSignal(blockAccess, pos, side);
    }

    @Override
    protected int getSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side) {
        return blockState.getValue(BlockComparatorBlock.POWERED) && blockState.getValue(BlockComparatorBlock.FACING_WITH_AXIS).getFacing() == side ? 15 : 0;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    public boolean change(Player player, BlockPos blockPos, Level level, ItemStack anvilHammer) {
        BlockState state = level.getBlockState(blockPos);
        FacingWithAxis fwa = state.getValue(BlockComparatorBlock.FACING_WITH_AXIS);
        FacingWithAxis newFwa = fwa.toggleAxis();
        level.setBlockAndUpdate(blockPos, state.setValue(BlockComparatorBlock.FACING_WITH_AXIS, newFwa));
        return true;
    }

    @Override
    public Property<?> getChangeableProperty(BlockState blockState) {
        return BlockComparatorBlock.FACING_WITH_AXIS;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(BlockComparatorBlock.FACING_WITH_AXIS, state.getValue(BlockComparatorBlock.FACING_WITH_AXIS).rotate(rotation));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(BlockComparatorBlock.FACING_WITH_AXIS, state.getValue(BlockComparatorBlock.FACING_WITH_AXIS).mirror(mirror));
    }
}
