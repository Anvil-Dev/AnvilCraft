package dev.dubhe.anvilcraft.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

public class RedstoneWireBlock extends Block {
    public static final EnumProperty<RedstoneSide> NORTH = BlockStateProperties.NORTH_REDSTONE;
    public static final EnumProperty<RedstoneSide> EAST = BlockStateProperties.EAST_REDSTONE;
    public static final EnumProperty<RedstoneSide> SOUTH = BlockStateProperties.SOUTH_REDSTONE;
    public static final EnumProperty<RedstoneSide> WEST = BlockStateProperties.WEST_REDSTONE;
    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    public static final BooleanProperty DOT = BooleanProperty.create("dot");
    public static final Map<Direction, EnumProperty<RedstoneSide>> PROPERTY_BY_DIRECTION = ImmutableMap.copyOf(
        Maps.newEnumMap(Map.of(Direction.NORTH, NORTH, Direction.EAST, EAST, Direction.SOUTH, SOUTH, Direction.WEST, WEST))
    );

    private static final ThreadLocal<Boolean> UPDATING = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> SUPPRESS_SIGNAL = ThreadLocal.withInitial(() -> false);
    private static final int MAX_NETWORK_SIZE = 65536;
    private static final VoxelShape DOT_SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 2.5, 12.0);
    private static final Map<Direction, VoxelShape> SIDE_SHAPES = Map.of(
        Direction.NORTH, Block.box(5.0, 0.0, 0.0, 11.0, 2.0, 8.0),
        Direction.EAST, Block.box(8.0, 0.0, 5.0, 16.0, 2.0, 11.0),
        Direction.SOUTH, Block.box(5.0, 0.0, 8.0, 11.0, 2.0, 16.0),
        Direction.WEST, Block.box(0.0, 0.0, 5.0, 8.0, 2.0, 11.0)
    );
    private static final Map<Direction, VoxelShape> UP_SHAPES = Map.of(
        Direction.NORTH, Block.box(5.0, 1.0, -0.1, 11.0, 18.0, 2.0),
        Direction.EAST, Block.box(14.0, 1.0, 5.0, 16.1, 18.0, 11.0),
        Direction.SOUTH, Block.box(5.0, 1.0, 14.0, 11.0, 18.0, 16.1),
        Direction.WEST, Block.box(-0.1, 1.0, 5.0, 2.0, 18.0, 11.0)
    );

    public RedstoneWireBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(NORTH, RedstoneSide.SIDE)
            .setValue(EAST, RedstoneSide.NONE)
            .setValue(SOUTH, RedstoneSide.SIDE)
            .setValue(WEST, RedstoneSide.NONE)
            .setValue(POWER, 0)
            .setValue(DOT, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = this.defaultBlockState();
        if (context.getHorizontalDirection().getAxis() == Direction.Axis.X) {
            state = state.setValue(NORTH, RedstoneSide.NONE)
                .setValue(SOUTH, RedstoneSide.NONE)
                .setValue(EAST, RedstoneSide.SIDE)
                .setValue(WEST, RedstoneSide.SIDE);
        }
        return this.connectionState(context.getLevel(), context.getClickedPos(), state);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        return belowState.isFaceSturdy(level, below, Direction.UP) || belowState.is(Blocks.HOPPER);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = state.getValue(DOT) ? DOT_SHAPE : Shapes.empty();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            RedstoneSide side = state.getValue(PROPERTY_BY_DIRECTION.get(direction));
            if (side.isConnected()) {
                shape = Shapes.or(shape, SIDE_SHAPES.get(direction));
            }
            if (side == RedstoneSide.UP) {
                shape = Shapes.or(shape, UP_SHAPES.get(direction));
            }
        }
        return shape;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!oldState.is(this)) {
            updateNetworksAround(level, pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!newState.is(this)) {
            updateNetworksAround(level, pos);
        }
    }

    @Override
    protected void neighborChanged(
        BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston
    ) {
        if (level.isClientSide()) {
            return;
        }
        if (!state.canSurvive(level, pos)) {
            dropResources(state, level, pos);
            level.removeBlock(pos, false);
        } else if (UPDATING.get()) {
            level.scheduleTick(pos, this, 1);
        } else {
            updateNetworksAround(level, pos);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        updateNetworksAround(level, pos);
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return !SUPPRESS_SIGNAL.get();
    }

    @Override
    public boolean canConnectRedstone(
        BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction
    ) {
        return false;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 0;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (SUPPRESS_SIGNAL.get() || !direction.getAxis().isHorizontal()) {
            return 0;
        }
        Direction outputDirection = direction.getOpposite();
        BlockState receiver = level.getBlockState(pos.relative(outputDirection));
        if (!state.getValue(PROPERTY_BY_DIRECTION.get(outputDirection)).isConnected()
            || receiver.is(Blocks.REDSTONE_WIRE)
            || findConnectedWire(level, pos, outputDirection) != null) {
            return 0;
        }
        return state.getValue(POWER);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, POWER, DOT);
    }

    private BlockState connectionState(BlockGetter level, BlockPos pos, BlockState oldState) {
        BlockState result = this.defaultBlockState().setValue(POWER, oldState.getValue(POWER));
        int connections = 0;
        Direction first = null;
        Direction second = null;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            RedstoneSide side = getConnection(level, pos, direction);
            result = result.setValue(PROPERTY_BY_DIRECTION.get(direction), side);
            if (side.isConnected()) {
                if (first == null) {
                    first = direction;
                } else if (second == null) {
                    second = direction;
                }
                connections++;
            }
        }

        if (connections == 0) {
            boolean eastWest = oldState.getValue(EAST).isConnected() || oldState.getValue(WEST).isConnected();
            result = result.setValue(eastWest ? EAST : NORTH, RedstoneSide.SIDE)
                .setValue(eastWest ? WEST : SOUTH, RedstoneSide.SIDE);
        } else if (connections == 1 && first != null) {
            result = result.setValue(PROPERTY_BY_DIRECTION.get(first.getOpposite()), RedstoneSide.SIDE);
        }

        boolean dot = connections >= 3 || connections == 2 && first != null && second != first.getOpposite();
        return result.setValue(DOT, dot);
    }

    private static RedstoneSide getConnection(BlockGetter level, BlockPos pos, Direction direction) {
        BlockPos wirePos = findConnectedWire(level, pos, direction);
        if (wirePos != null) {
            return wirePos.getY() > pos.getY() ? RedstoneSide.UP : RedstoneSide.SIDE;
        }

        BlockPos adjacentPos = pos.relative(direction);
        BlockState adjacent = level.getBlockState(adjacentPos);
        return canAttachTo(level, adjacentPos, adjacent, direction) ? RedstoneSide.SIDE : RedstoneSide.NONE;
    }

    private static boolean canAttachTo(BlockGetter level, BlockPos pos, BlockState state, Direction direction) {
        if (state.is(Blocks.REDSTONE_WIRE) || state.getBlock() instanceof RedstoneWireBlock) {
            return false;
        }
        return state.canRedstoneConnectTo(level, pos, direction)
            || state.is(Blocks.TARGET)
            || state.is(Blocks.REDSTONE_TORCH)
            || state.is(Blocks.REDSTONE_WALL_TORCH);
    }

    @Nullable
    private static BlockPos findConnectedWire(BlockGetter level, BlockPos pos, Direction direction) {
        BlockPos same = pos.relative(direction);
        if (isWire(level, same)) {
            return same;
        }

        BlockState aboveCurrent = level.getBlockState(pos.above());
        BlockState bridge = level.getBlockState(same);
        BlockPos above = same.above();
        if (!aboveCurrent.isRedstoneConductor(level, pos.above())
            && isFullHeightSupport(level, same, bridge, direction.getOpposite())
            && isWire(level, above)) {
            return above;
        }

        BlockPos supportPos = pos.below();
        BlockState support = level.getBlockState(supportPos);
        BlockPos below = same.below();
        if (!bridge.isRedstoneConductor(level, same)
            && isFullHeightSupport(level, supportPos, support, direction)
            && isWire(level, below)) {
            return below;
        }
        return null;
    }

    private static boolean isFullHeightSupport(
        BlockGetter level, BlockPos pos, BlockState state, Direction side
    ) {
        return !(state.getBlock() instanceof SlabBlock)
            && !(state.getBlock() instanceof StairBlock)
            && state.isFaceSturdy(level, pos, side);
    }

    private static boolean isWire(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof RedstoneWireBlock;
    }

    private static void updateNetworksAround(Level level, BlockPos origin) {
        if (level.isClientSide() || UPDATING.get()) {
            return;
        }
        UPDATING.set(true);
        try {
            Set<BlockPos> visited = new HashSet<>();
            for (int y = -1; y <= 1; y++) {
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        BlockPos seed = origin.offset(x, y, z);
                        if (isWire(level, seed) && !visited.contains(seed)) {
                            updateNetwork(level, seed, visited);
                        }
                    }
                }
            }
        } finally {
            UPDATING.set(false);
        }
    }

    private static void updateNetwork(Level level, BlockPos seed, Set<BlockPos> visited) {
        Set<BlockPos> network = collectNetwork(level, seed, visited);
        Set<BlockPos> changed = new HashSet<>();
        for (BlockPos pos : network) {
            BlockState state = level.getBlockState(pos);
            RedstoneWireBlock block = (RedstoneWireBlock) state.getBlock();
            BlockState connected = block.connectionState(level, pos, state);
            if (connected != state) {
                level.setBlock(pos, connected, Block.UPDATE_CLIENTS);
                changed.add(pos);
            }
        }

        int power = getInputPower(level, network);
        for (BlockPos pos : network) {
            BlockState state = level.getBlockState(pos);
            if (state.getValue(POWER) != power) {
                level.setBlock(pos, state.setValue(POWER, power), Block.UPDATE_CLIENTS);
                changed.add(pos);
            }
        }
        for (BlockPos pos : changed) {
            level.updateNeighborsAt(pos, level.getBlockState(pos).getBlock());
        }
    }

    private static Set<BlockPos> collectNetwork(Level level, BlockPos seed, Set<BlockPos> visited) {
        Set<BlockPos> network = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(seed.immutable());
        while (!queue.isEmpty() && network.size() < MAX_NETWORK_SIZE) {
            BlockPos pos = queue.removeFirst();
            if (!network.add(pos)) {
                continue;
            }
            visited.add(pos);
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos connected = findConnectedWire(level, pos, direction);
                if (connected != null && !network.contains(connected)) {
                    queue.addLast(connected.immutable());
                }
            }
        }
        return network;
    }

    private static int getInputPower(Level level, Set<BlockPos> network) {
        int power = 0;
        SUPPRESS_SIGNAL.set(true);
        try {
            for (BlockPos pos : network) {
                BlockState state = level.getBlockState(pos);
                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    if (!state.getValue(PROPERTY_BY_DIRECTION.get(direction)).isConnected()
                        || findConnectedWire(level, pos, direction) != null) {
                        continue;
                    }
                    BlockPos inputPos = pos.relative(direction);
                    BlockState inputState = level.getBlockState(inputPos);
                    if (!inputState.is(Blocks.REDSTONE_WIRE)) {
                        power = Math.max(power, level.getSignal(inputPos, direction));
                    }
                }
            }
        } finally {
            SUPPRESS_SIGNAL.set(false);
        }
        return power;
    }
}
