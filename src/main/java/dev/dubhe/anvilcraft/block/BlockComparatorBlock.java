package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.util.ShapeUtil;
import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class BlockComparatorBlock extends Block implements IHammerRemovable, IHammerChangeable {

    public static final MapCodec<BlockComparatorBlock> CODEC = simpleCodec(BlockComparatorBlock::new);

    public static final EnumProperty<FacingWithAxis> FACING_WITH_AXIS =
        EnumProperty.create("facing_with_axis", FacingWithAxis.class);
    public static final BooleanProperty PRECISE = BooleanProperty.create("precise");
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    private static final VoxelShape BASE_SHAPE = makeShape();

    private static VoxelShape makeShape() {
        VoxelShape shape = Shapes.empty();
        shape = Shapes.join(shape, Shapes.box(0.25, 0, 0, 0.75, 1, 1), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.125, 0.3125, 0.0625, 0.875, 0.6875, 0.5625), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0, 0.25, 0, 0.125, 0.75, 0.625), BooleanOp.OR);
        shape = Shapes.join(shape, Shapes.box(0.875, 0.25, 0, 1, 0.75, 0.625), BooleanOp.OR);
        return shape;
    }

    private static final VoxelShape SHAPE_NORTH_X = BASE_SHAPE;
    private static final VoxelShape SHAPE_SOUTH_X = ShapeUtil.rotate(Direction.Axis.Y, 180, BASE_SHAPE);

    private static final VoxelShape SHAPE_WEST_Z = ShapeUtil.rotate(Direction.Axis.Y, 90, BASE_SHAPE);
    private static final VoxelShape SHAPE_EAST_Z = ShapeUtil.rotate(Direction.Axis.Y, 270, BASE_SHAPE);

    private static final VoxelShape SHAPE_NORTH_Y = ShapeUtil.rotate(Direction.Axis.Z, 90, BASE_SHAPE);
    private static final VoxelShape SHAPE_SOUTH_Y = ShapeUtil.rotate(Direction.Axis.Y, 180, SHAPE_NORTH_Y);
    private static final VoxelShape SHAPE_WEST_Y = ShapeUtil.rotate(Direction.Axis.Y, 90, SHAPE_NORTH_Y);
    private static final VoxelShape SHAPE_EAST_Y = ShapeUtil.rotate(Direction.Axis.Y, 270, SHAPE_NORTH_Y);

    private static final VoxelShape SHAPE_UP_X = ShapeUtil.rotate(Direction.Axis.X, 270, BASE_SHAPE);
    private static final VoxelShape SHAPE_DOWN_X = ShapeUtil.rotate(Direction.Axis.X, 90, BASE_SHAPE);

    private static final VoxelShape SHAPE_UP_Z = ShapeUtil.rotate(Direction.Axis.Y, 90, SHAPE_UP_X);
    private static final VoxelShape SHAPE_DOWN_Z = ShapeUtil.rotate(Direction.Axis.Y, 90, SHAPE_DOWN_X);

    public BlockComparatorBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
            this.stateDefinition
                .any()
                .setValue(FACING_WITH_AXIS, FacingWithAxis.NORTH_X)
                .setValue(PRECISE, false)
                .setValue(POWERED, false)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING_WITH_AXIS).add(PRECISE).add(POWERED);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public VoxelShape getShape(
        BlockState state,
        BlockGetter level,
        BlockPos pos,
        CollisionContext context
    ) {
        return getShapeFor(state.getValue(FACING_WITH_AXIS));
    }

    private static VoxelShape getShapeFor(FacingWithAxis fwa) {
        return switch (fwa) {
            case NORTH_X -> SHAPE_NORTH_X;
            case SOUTH_X -> SHAPE_SOUTH_X;
            case WEST_Z -> SHAPE_WEST_Z;
            case EAST_Z -> SHAPE_EAST_Z;
            case NORTH_Y -> SHAPE_NORTH_Y;
            case SOUTH_Y -> SHAPE_SOUTH_Y;
            case WEST_Y -> SHAPE_WEST_Y;
            case EAST_Y -> SHAPE_EAST_Y;
            case UP_X -> SHAPE_UP_X;
            case UP_Z -> SHAPE_UP_Z;
            case DOWN_X -> SHAPE_DOWN_X;
            case DOWN_Z -> SHAPE_DOWN_Z;
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
            axis = context.getHorizontalDirection().getAxis();
        } else {
            axis = facing.getClockWise().getAxis();
        }
        return defaultBlockState().setValue(FACING_WITH_AXIS, FacingWithAxis.of(facing, axis));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (level.isClientSide
            || (oldState.is(this)
                && state.getValue(FACING_WITH_AXIS) == oldState.getValue(FACING_WITH_AXIS))
        ) {
            return;
        }
        boolean newPowered = checkBlocks(level, pos, state);
        level.setBlock(pos, state.setValue(POWERED, newPowered), 3);
        this.updateNeighborsInFront(level, pos, state);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (level.isClientSide
            || (state.is(newState.getBlock())
                && state.getValue(FACING_WITH_AXIS) == newState.getValue(FACING_WITH_AXIS))
        ) {
            return;
        }
        if (state.getValue(POWERED)) {
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
            BlockState newState = state.cycle(PRECISE);
            level.setBlock(pos, newState.setValue(POWERED, checkBlocks(level, pos, newState)), 2);
            this.updateNeighborsInFront(level, pos, state);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
    }

    private boolean checkBlocks(LevelAccessor level, BlockPos pos, BlockState blockState) {
        FacingWithAxis fwa = blockState.getValue(FACING_WITH_AXIS);
        Direction facing = fwa.getFacing();
        Direction.Axis axis = fwa.getAxis();
        Direction[] dirs = getCompareDirections(facing, axis);
        BlockState state1 = level.getBlockState(pos.relative(dirs[0]));
        BlockState state2 = level.getBlockState(pos.relative(dirs[1]));
        return blockState.getValue(PRECISE)
            ? state1.equals(state2)
            : state1.getBlock() == state2.getBlock();
    }

    private static Direction[] getCompareDirections(Direction facing, Direction.Axis axis) {
        return new Direction[]{
            Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE),
            Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE)
        };
    }

    @Override
    public BlockState updateShape(
        BlockState blockState,
        Direction direction,
        BlockState blockState2,
        LevelAccessor level,
        BlockPos pos,
        BlockPos pos2
    ) {
        FacingWithAxis fwa = blockState.getValue(FACING_WITH_AXIS);
        Direction facing = fwa.getFacing();
        Direction.Axis compareAxis = fwa.getAxis();
        if (direction.getAxis() == facing.getAxis()) return blockState;
        if (direction.getAxis() != compareAxis) return blockState;
        if (!level.isClientSide() && !level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, 2);
        }
        return blockState;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean same = checkBlocks(level, pos, state);
        if (same != state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, same), 2);
            this.updateNeighborsInFront(level, pos, state);
        }
    }

    protected void updateNeighborsInFront(Level level, BlockPos pos, BlockState state) {
        Direction direction = state.getValue(FACING_WITH_AXIS).getFacing();
        BlockPos blockpos = pos.relative(direction.getOpposite());
        level.neighborChanged(blockpos, this, pos);
        level.updateNeighborsAtExceptFromFacing(blockpos, this, direction);
    }

    @Override
    public boolean canConnectRedstone(
        BlockState state,
        BlockGetter level,
        BlockPos pos,
        @Nullable Direction direction
    ) {
        return direction == state.getValue(FACING_WITH_AXIS).getFacing();
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
    protected int getSignal(
        BlockState blockState,
        BlockGetter blockAccess,
        BlockPos pos,
        Direction side
    ) {
        return blockState.getValue(POWERED)
            && blockState.getValue(FACING_WITH_AXIS).getFacing() == side
            ? 15 : 0;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    public boolean change(Player player, BlockPos blockPos, Level level, ItemStack anvilHammer) {
        BlockState state = level.getBlockState(blockPos);
        FacingWithAxis fwa = state.getValue(FACING_WITH_AXIS);
        level.setBlockAndUpdate(blockPos, state.setValue(FACING_WITH_AXIS, fwa.toggleAxis()));
        return true;
    }

    @Override
    public @Nullable Property<?> getChangeableProperty(BlockState blockState) {
        return FACING_WITH_AXIS;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING_WITH_AXIS, state.getValue(FACING_WITH_AXIS).rotate(rotation));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING_WITH_AXIS, state.getValue(FACING_WITH_AXIS).mirror(mirror));
    }

    public enum FacingWithAxis implements StringRepresentable {
        UP_X(Direction.UP, Direction.Axis.X),
        UP_Z(Direction.UP, Direction.Axis.Z),
        DOWN_X(Direction.DOWN, Direction.Axis.X),
        DOWN_Z(Direction.DOWN, Direction.Axis.Z),
        NORTH_X(Direction.NORTH, Direction.Axis.X),
        NORTH_Y(Direction.NORTH, Direction.Axis.Y),
        SOUTH_X(Direction.SOUTH, Direction.Axis.X),
        SOUTH_Y(Direction.SOUTH, Direction.Axis.Y),
        WEST_Z(Direction.WEST, Direction.Axis.Z),
        WEST_Y(Direction.WEST, Direction.Axis.Y),
        EAST_Z(Direction.EAST, Direction.Axis.Z),
        EAST_Y(Direction.EAST, Direction.Axis.Y);

        @Getter
        private final Direction facing;
        @Getter
        private final Direction.Axis axis;
        private final String name;

        FacingWithAxis(Direction facing, Direction.Axis axis) {
            this.facing = facing;
            this.axis = axis;
            this.name = facing.getSerializedName() + "_" + axis.getSerializedName();
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        public static FacingWithAxis of(Direction facing, Direction.Axis axis) {
            for (FacingWithAxis fwa : values()) {
                if (fwa.facing == facing && fwa.axis == axis) {
                    return fwa;
                }
            }
            return NORTH_X;
        }

        public FacingWithAxis rotate(Rotation rotation) {
            Direction newFacing = rotation.rotate(this.facing);
            Direction.Axis newAxis = this.axis;
            if (this.facing.getAxis() == Direction.Axis.Y) {
                Direction axisDir = Direction.fromAxisAndDirection(this.axis, Direction.AxisDirection.POSITIVE);
                newAxis = rotation.rotate(axisDir).getAxis();
            }
            return of(newFacing, newAxis);
        }

        public FacingWithAxis mirror(Mirror mirror) {
            return of(mirror.mirror(this.facing), this.axis);
        }

        public FacingWithAxis toggleAxis() {
            return switch (this) {
                case NORTH_X -> NORTH_Y;
                case NORTH_Y -> NORTH_X;
                case SOUTH_X -> SOUTH_Y;
                case SOUTH_Y -> SOUTH_X;
                case EAST_Z -> EAST_Y;
                case EAST_Y -> EAST_Z;
                case WEST_Z -> WEST_Y;
                case WEST_Y -> WEST_Z;
                case UP_X -> UP_Z;
                case UP_Z -> UP_X;
                case DOWN_X -> DOWN_Z;
                case DOWN_Z -> DOWN_X;
            };
        }
    }
}
