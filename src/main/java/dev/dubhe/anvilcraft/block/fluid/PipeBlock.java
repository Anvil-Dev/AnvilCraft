package dev.dubhe.anvilcraft.block.fluid;

import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;

import java.util.Locale;

public abstract class PipeBlock extends Block implements SimpleWaterloggedBlock, IHammerRemovable, EntityBlock {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    public static final EnumProperty<CornerEnded> CORNER_ENDED = EnumProperty.create("corner_ended", CornerEnded.class);
    public static final BooleanProperty HAS_END_START = BooleanProperty.create("has_end_start");
    public static final BooleanProperty HAS_END_END = BooleanProperty.create("has_end_end");
    public static final EnumProperty<NodePipe> DOWN = EnumProperty.create("down", NodePipe.class);
    public static final EnumProperty<NodePipe> UP = EnumProperty.create("up", NodePipe.class);
    public static final EnumProperty<NodePipe> NORTH = EnumProperty.create("north", NodePipe.class);
    public static final EnumProperty<NodePipe> SOUTH = EnumProperty.create("south", NodePipe.class);
    public static final EnumProperty<NodePipe> WEST = EnumProperty.create("west", NodePipe.class);
    public static final EnumProperty<NodePipe> EAST = EnumProperty.create("east", NodePipe.class);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    // pipe_straight model: [4,4,4]→[12,12,12]
    static final VoxelShape PIPE_CENTER = box(4, 4, 4, 12, 12, 12);
    // pipe_node model: [3,3,3]→[13,13,13]
    static final VoxelShape NODE_CENTER = box(3, 3, 3, 13, 13, 13);

    static VoxelShape makeNoEnd(Direction dir) {
        return switch (dir) {
            case DOWN -> box(4, 0, 4, 12, 4, 12);
            case UP -> box(4, 12, 4, 12, 16, 12);
            case NORTH -> box(4, 4, 0, 12, 12, 4);
            case SOUTH -> box(4, 4, 12, 12, 12, 16);
            case WEST -> box(0, 4, 4, 4, 12, 12);
            case EAST -> box(12, 4, 4, 16, 12, 12);
        };
    }

    static VoxelShape makeEnd(Direction dir) {
        VoxelShape ring = switch (dir) {
            case DOWN -> box(4, 2, 4, 12, 4, 12);
            case UP -> box(4, 12, 4, 12, 14, 12);
            case NORTH -> box(4, 4, 2, 12, 12, 4);
            case SOUTH -> box(4, 4, 12, 12, 12, 14);
            case WEST -> box(2, 4, 4, 4, 12, 12);
            case EAST -> box(12, 4, 4, 14, 12, 12);
        };
        VoxelShape cap = switch (dir) {
            case DOWN -> box(3, 0, 3, 13, 2, 13);
            case UP -> box(3, 14, 3, 13, 16, 13);
            case NORTH -> box(3, 3, 0, 13, 13, 2);
            case SOUTH -> box(3, 3, 14, 13, 13, 16);
            case WEST -> box(0, 3, 3, 2, 13, 13);
            case EAST -> box(14, 3, 3, 16, 13, 13);
        };
        return Shapes.or(ring, cap);
    }

    public PipeBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.getStateDefinition().any().setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
    }

    public static Direction getDirectionFromAxis(Direction.Axis axis, Direction.AxisDirection axisDirection) {
        return Direction.get(axisDirection, axis);
    }

    public static EnumProperty<NodePipe> getPropertyForDirection(Direction direction) {
        return switch (direction) {
            case DOWN -> DOWN;
            case UP -> UP;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
        };
    }

    public static boolean hasConnectionToward(BlockState state, Direction toward) {
        Block block = state.getBlock();
        return switch (block) {
            case PipeStraightBlock ignored -> toward.getAxis() == state.getValue(AXIS);
            case PipeCornerBlock ignored -> state.getValue(CORNER_ENDED).containsDirection(toward);
            case PipeNodeBlock ignored -> state.getValue(getPropertyForDirection(toward)) == NodePipe.PIPE;
            default -> false;
        };
    }

    public static boolean isNeighborPipeToward(Level level, BlockPos pos, Direction dir) {
        BlockPos neighborPos = pos.relative(dir);
        BlockState neighborState = level.getBlockState(neighborPos);
        return neighborState.getBlock() instanceof PipeBlock && hasConnectionToward(neighborState, dir.getOpposite());
    }

    public static boolean isFluidHandler(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockEntity be = level.getBlockEntity(pos);
        return level.getCapability(Capabilities.FluidHandler.BLOCK, pos, state, be, null) != null;
    }

    public static boolean isNeighborOccupied(Level level, BlockPos pos, Direction dir) {
        if (isNeighborPipeToward(level, pos, dir)) return true;
        return isFluidHandler(level, pos.relative(dir));
    }

    @Override
    public Item asItem() {
        return ModItems.PIPE.get();
    }

    protected void changePipeState(
        Level level,
        BlockPos pos,
        BlockState state,
        Direction startDir,
        Direction neighborDir,
        boolean neighborIsPipeToward
    ) {
        BlockState newState = state;
        if (neighborDir == startDir) {
            newState = newState.setValue(HAS_END_START, !neighborIsPipeToward);
        } else {
            newState = newState.setValue(HAS_END_END, !neighborIsPipeToward);
        }

        if (newState != state) {
            level.setBlockAndUpdate(pos, newState);
        }
    }

    @Override
    protected BlockState updateShape(
        BlockState state,
        Direction direction,
        BlockState neighborState,
        LevelAccessor level,
        BlockPos pos,
        BlockPos neighborPos
    ) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    public VoxelShape getShape(BlockState state, Direction startDir, Direction endDir) {
        VoxelShape shape = PIPE_CENTER;
        if (state.getValue(HAS_END_START)) {
            shape = Shapes.or(shape, makeEnd(startDir));
        } else {
            shape = Shapes.or(shape, makeNoEnd(startDir));
        }
        if (state.getValue(HAS_END_END)) {
            shape = Shapes.or(shape, makeEnd(endDir));
        } else {
            shape = Shapes.or(shape, makeNoEnd(endDir));
        }
        return shape;
    }

    public enum CornerEnded implements StringRepresentable {
        DOWN_NORTH(Direction.DOWN, Direction.NORTH),
        DOWN_SOUTH(Direction.DOWN, Direction.SOUTH),
        DOWN_WEST(Direction.DOWN, Direction.WEST),
        DOWN_EAST(Direction.DOWN, Direction.EAST),
        UP_NORTH(Direction.UP, Direction.NORTH),
        UP_SOUTH(Direction.UP, Direction.SOUTH),
        UP_WEST(Direction.UP, Direction.WEST),
        UP_EAST(Direction.UP, Direction.EAST),
        NORTH_WEST(Direction.NORTH, Direction.WEST),
        NORTH_EAST(Direction.NORTH, Direction.EAST),
        SOUTH_WEST(Direction.SOUTH, Direction.WEST),
        SOUTH_EAST(Direction.SOUTH, Direction.EAST);

        private final Direction first;
        private final Direction second;

        CornerEnded(Direction first, Direction second) {
            this.first = first;
            this.second = second;
        }

        public Direction getFirstDirection() {
            return first;
        }

        public Direction getSecondDirection() {
            return second;
        }

        public boolean containsDirection(Direction direction) {
            return first == direction || second == direction;
        }

        public static CornerEnded fromDirections(Direction a, Direction b) {
            for (CornerEnded corner : values()) {
                if ((corner.first == a && corner.second == b) || (corner.first == b && corner.second == a)) {
                    return corner;
                }
            }
            return UP_NORTH;
        }

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    public enum NodePipe implements StringRepresentable {
        PIPE,
        END,
        NONE;

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }
}
