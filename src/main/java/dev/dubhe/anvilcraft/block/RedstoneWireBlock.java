package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.block.entity.RedstoneWireBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
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
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/** A surface-mounted, non-attenuating redstone network with electrical contacts at its visible open ends. */
public class RedstoneWireBlock extends Block implements EntityBlock {
    public static final EnumProperty<RedstoneSide> NORTH = BlockStateProperties.NORTH_REDSTONE;
    public static final EnumProperty<RedstoneSide> EAST = BlockStateProperties.EAST_REDSTONE;
    public static final EnumProperty<RedstoneSide> SOUTH = BlockStateProperties.SOUTH_REDSTONE;
    public static final EnumProperty<RedstoneSide> WEST = BlockStateProperties.WEST_REDSTONE;
    public static final List<EnumProperty<RedstoneSide>> CONNECTION_PROPERTIES = List.of(NORTH, EAST, SOUTH, WEST);
    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    public static final DirectionProperty ATTACHMENT = DirectionProperty.create("attachment");
    public static final BooleanProperty DOT = BooleanProperty.create("dot");

    private static final ThreadLocal<Boolean> UPDATING = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> SUPPRESS_SIGNAL = ThreadLocal.withInitial(() -> false);
    private static final int MAX_NETWORK_SIZE = 65536;
    private static final Map<Direction, VoxelShape> DOT_SHAPES = new EnumMap<>(Direction.class);
    private static final Map<Direction, List<VoxelShape>> SIDE_SHAPES = new EnumMap<>(Direction.class);
    private static final Map<Direction, List<VoxelShape>> UP_SHAPES = new EnumMap<>(Direction.class);

    static {
        for (Direction attachment : Direction.values()) {
            Direction north = getLocalDirection(attachment, 0);
            DOT_SHAPES.put(attachment, transformedBox(attachment, north, 4.0, 0.0, 4.0, 12.0, 2.5, 12.0));
            List<VoxelShape> sides = new ArrayList<>(4);
            List<VoxelShape> ups = new ArrayList<>(4);
            for (int index = 0; index < 4; index++) {
                Direction tangent = getLocalDirection(attachment, index);
                sides.add(transformedBox(attachment, tangent, 5.0, 0.0, 0.0, 11.0, 2.0, 8.0));
                if (attachment.getAxis().isHorizontal() && tangent == Direction.UP) {
                    ups.add(transformedBox(Direction.DOWN, attachment, 5.0, 0.0, -0.1, 11.0, 18.0, 2.0));
                } else if (attachment.getAxis().isHorizontal() && tangent == Direction.DOWN) {
                    ups.add(transformedBox(Direction.DOWN, attachment, 5.0, -1.0, -0.1, 11.0, 17.0, 2.0));
                } else {
                    ups.add(transformedBox(attachment, tangent, 5.0, 1.0, -0.1, 11.0, 18.0, 2.0));
                }
            }
            SIDE_SHAPES.put(attachment, List.copyOf(sides));
            UP_SHAPES.put(attachment, List.copyOf(ups));
        }
    }

    public RedstoneWireBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(NORTH, RedstoneSide.SIDE)
            .setValue(EAST, RedstoneSide.NONE)
            .setValue(SOUTH, RedstoneSide.SIDE)
            .setValue(WEST, RedstoneSide.NONE)
            .setValue(POWER, 0)
            .setValue(ATTACHMENT, Direction.DOWN)
            .setValue(DOT, false));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RedstoneWireBlockEntity(ModBlockEntities.REDSTONE_WIRE.get(), pos, state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction attachment = context.getClickedFace().getOpposite();
        BlockState state = emptyState(this.defaultBlockState().setValue(ATTACHMENT, attachment));
        Direction preferred = getLocalDirection(attachment, 0);
        for (Direction direction : context.getNearestLookingDirections()) {
            if (direction.getAxis() != attachment.getAxis()) {
                preferred = direction;
                break;
            }
        }
        int index = getLocalIndex(attachment, preferred);
        state = state.setValue(CONNECTION_PROPERTIES.get(index), RedstoneSide.SIDE)
            .setValue(CONNECTION_PROPERTIES.get((index + 2) % 4), RedstoneSide.SIDE);
        return this.connectionState(context.getLevel(), context.getClickedPos(), state);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction attachment = state.getValue(ATTACHMENT);
        BlockPos supportPos = pos.relative(attachment);
        BlockState support = level.getBlockState(supportPos);
        return support.isFaceSturdy(level, supportPos, attachment.getOpposite())
            || attachment == Direction.DOWN && support.is(Blocks.HOPPER);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction attachment = state.getValue(ATTACHMENT);
        VoxelShape shape = state.getValue(DOT) ? DOT_SHAPES.get(attachment) : Shapes.empty();
        for (int index = 0; index < 4; index++) {
            RedstoneSide side = state.getValue(CONNECTION_PROPERTIES.get(index));
            if (side.isConnected()) {
                shape = Shapes.or(shape, SIDE_SHAPES.get(attachment).get(index));
            }
            if (side == RedstoneSide.UP) {
                shape = Shapes.or(shape, UP_SHAPES.get(attachment).get(index));
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
        if (direction == null || !direction.getAxis().isHorizontal()) {
            return false;
        }
        Direction terminalDirection = direction.getOpposite();
        int index = getLocalIndex(state.getValue(ATTACHMENT), terminalDirection);
        return index >= 0
            && state.getValue(CONNECTION_PROPERTIES.get(index)).isConnected()
            && findConnection(level, pos, state, index) == null;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 0;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (SUPPRESS_SIGNAL.get()) {
            return 0;
        }
        Direction outputDirection = direction.getOpposite();
        int index = getLocalIndex(state.getValue(ATTACHMENT), outputDirection);
        if (index < 0
            || !state.getValue(CONNECTION_PROPERTIES.get(index)).isConnected()
            || findConnection(level, pos, state, index) != null) {
            return 0;
        }
        BlockState receiver = level.getBlockState(pos.relative(outputDirection));
        if (receiver.is(Blocks.REDSTONE_WIRE)) {
            return level.getBlockEntity(pos) instanceof RedstoneWireBlockEntity wire ? wire.getNonDustPower() : 0;
        }
        return state.getValue(POWER);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, POWER, ATTACHMENT, DOT);
    }

    private BlockState connectionState(BlockGetter level, BlockPos pos, BlockState oldState) {
        BlockState result = emptyState(this.defaultBlockState()
            .setValue(POWER, oldState.getValue(POWER))
            .setValue(ATTACHMENT, oldState.getValue(ATTACHMENT)));
        int connections = 0;
        int first = -1;
        int second = -1;
        for (int index = 0; index < 4; index++) {
            RedstoneSide side = getConnection(level, pos, result, index);
            result = result.setValue(CONNECTION_PROPERTIES.get(index), side);
            if (side.isConnected()) {
                if (first < 0) {
                    first = index;
                } else if (second < 0) {
                    second = index;
                }
                connections++;
            }
        }

        if (connections == 0) {
            boolean eastWest = oldState.getValue(EAST).isConnected() || oldState.getValue(WEST).isConnected();
            result = result.setValue(eastWest ? EAST : NORTH, RedstoneSide.SIDE)
                .setValue(eastWest ? WEST : SOUTH, RedstoneSide.SIDE);
        } else if (connections == 1) {
            result = result.setValue(CONNECTION_PROPERTIES.get((first + 2) % 4), RedstoneSide.SIDE);
        }

        boolean dot = connections >= 3 || connections == 2 && second != (first + 2) % 4;
        return result.setValue(DOT, dot);
    }

    private static BlockState emptyState(BlockState state) {
        for (EnumProperty<RedstoneSide> property : CONNECTION_PROPERTIES) {
            state = state.setValue(property, RedstoneSide.NONE);
        }
        return state.setValue(DOT, false);
    }

    private static RedstoneSide getConnection(BlockGetter level, BlockPos pos, BlockState state, int index) {
        Connection connection = findConnection(level, pos, state, index);
        if (connection != null) {
            return connection.side();
        }
        Direction tangent = getLocalDirection(state.getValue(ATTACHMENT), index);
        BlockPos adjacentPos = pos.relative(tangent);
        BlockState adjacent = level.getBlockState(adjacentPos);
        return canAttachTo(level, adjacentPos, adjacent, tangent) ? RedstoneSide.SIDE : RedstoneSide.NONE;
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
    private static Connection findConnection(BlockGetter level, BlockPos pos, BlockState state, int index) {
        Direction attachment = state.getValue(ATTACHMENT);
        Direction tangent = getLocalDirection(attachment, index);
        BlockPos endpoint = endpoint(pos, attachment, tangent);
        BlockPos raisedEndpoint = endpoint.relative(attachment.getOpposite(), 2);
        Connection fallback = null;

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos candidatePos = pos.offset(x, y, z);
                    if (candidatePos.equals(pos)) {
                        continue;
                    }
                    BlockState candidate = level.getBlockState(candidatePos);
                    if (!(candidate.getBlock() instanceof RedstoneWireBlock)) {
                        continue;
                    }
                    Direction candidateAttachment = candidate.getValue(ATTACHMENT);
                    for (int candidateIndex = 0; candidateIndex < 4; candidateIndex++) {
                        Direction candidateTangent = getLocalDirection(candidateAttachment, candidateIndex);
                        BlockPos candidateEndpoint = endpoint(candidatePos, candidateAttachment, candidateTangent);
                        if (candidateEndpoint.equals(endpoint)) {
                            RedstoneSide side = attachment.getAxis().isHorizontal()
                                && candidateAttachment.getAxis() == Direction.Axis.Y
                                ? RedstoneSide.UP
                                : RedstoneSide.SIDE;
                            return new Connection(candidatePos.immutable(), side);
                        }
                        if (candidateEndpoint.equals(raisedEndpoint) && canClimb(level, pos, attachment, tangent)) {
                            fallback = new Connection(candidatePos.immutable(), RedstoneSide.UP);
                        } else if (endpoint.equals(candidateEndpoint.relative(candidateAttachment.getOpposite(), 2))
                            && canClimb(level, candidatePos, candidateAttachment, candidateTangent)) {
                            fallback = new Connection(candidatePos.immutable(), RedstoneSide.SIDE);
                        }
                    }
                }
            }
        }
        return fallback;
    }

    private static boolean canClimb(BlockGetter level, BlockPos pos, Direction attachment, Direction tangent) {
        Direction outward = attachment.getOpposite();
        BlockPos bridgePos = pos.relative(tangent);
        BlockState bridge = level.getBlockState(bridgePos);
        return !level.getBlockState(pos.relative(outward)).isRedstoneConductor(level, pos.relative(outward))
            && isFullHeightSupport(level, bridgePos, bridge, tangent.getOpposite());
    }

    private static boolean isFullHeightSupport(
        BlockGetter level, BlockPos pos, BlockState state, Direction side
    ) {
        return !(state.getBlock() instanceof SlabBlock)
            && !(state.getBlock() instanceof StairBlock)
            && state.isFaceSturdy(level, pos, side);
    }

    private static BlockPos endpoint(BlockPos pos, Direction attachment, Direction tangent) {
        return new BlockPos(
            pos.getX() * 2 + 1 + attachment.getStepX() + tangent.getStepX(),
            pos.getY() * 2 + 1 + attachment.getStepY() + tangent.getStepY(),
            pos.getZ() * 2 + 1 + attachment.getStepZ() + tangent.getStepZ()
        );
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

    /** Replaces this wire with the placement state for another supporting face without consuming an item. */
    public boolean reattach(Level level, BlockPos pos, BlockState state) {
        if (!state.is(this) || !state.canSurvive(level, pos) || !level.setBlock(pos, state, Block.UPDATE_ALL)) {
            return false;
        }
        updateNetworksAround(level, pos);
        return true;
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

        InputPower inputPower = getInputPower(level, network);
        for (BlockPos pos : network) {
            BlockState state = level.getBlockState(pos);
            boolean powerChanged = state.getValue(POWER) != inputPower.total();
            boolean sourceChanged = level.getBlockEntity(pos) instanceof RedstoneWireBlockEntity wire
                && wire.setNonDustPower(inputPower.nonDust());
            if (powerChanged) {
                level.setBlock(pos, state.setValue(POWER, inputPower.total()), Block.UPDATE_CLIENTS);
            }
            if (powerChanged || sourceChanged) {
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
            BlockState state = level.getBlockState(pos);
            for (int index = 0; index < 4; index++) {
                Connection connection = findConnection(level, pos, state, index);
                if (connection != null && !network.contains(connection.pos())) {
                    queue.addLast(connection.pos());
                }
            }
        }
        return network;
    }

    private static InputPower getInputPower(Level level, Set<BlockPos> network) {
        int totalPower = 0;
        int nonDustPower = 0;
        SUPPRESS_SIGNAL.set(true);
        try {
            for (BlockPos pos : network) {
                BlockState state = level.getBlockState(pos);
                Direction attachment = state.getValue(ATTACHMENT);
                for (int index = 0; index < 4; index++) {
                    if (!state.getValue(CONNECTION_PROPERTIES.get(index)).isConnected()
                        || findConnection(level, pos, state, index) != null) {
                        continue;
                    }
                    Direction tangent = getLocalDirection(attachment, index);
                    BlockPos inputPos = pos.relative(tangent);
                    BlockState inputState = level.getBlockState(inputPos);
                    int inputPower = level.getSignal(inputPos, tangent);
                    totalPower = Math.max(totalPower, inputPower);
                    if (!inputState.is(Blocks.REDSTONE_WIRE)) {
                        nonDustPower = Math.max(nonDustPower, inputPower);
                    }
                }
            }
        } finally {
            SUPPRESS_SIGNAL.set(false);
        }
        return new InputPower(totalPower, nonDustPower);
    }

    private static boolean isWire(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof RedstoneWireBlock;
    }

    public static Direction getLocalDirection(Direction attachment, int index) {
        Direction north = attachment == Direction.DOWN
            ? Direction.NORTH
            : attachment == Direction.UP ? Direction.SOUTH : Direction.UP;
        Direction outward = attachment.getOpposite();
        Direction east = cross(north, outward);
        return switch (index) {
            case 0 -> north;
            case 1 -> east;
            case 2 -> north.getOpposite();
            case 3 -> east.getOpposite();
            default -> throw new IllegalArgumentException("Invalid local direction index: " + index);
        };
    }

    private static int getLocalIndex(Direction attachment, Direction worldDirection) {
        for (int index = 0; index < 4; index++) {
            if (getLocalDirection(attachment, index) == worldDirection) {
                return index;
            }
        }
        return -1;
    }

    private static Direction cross(Direction first, Direction second) {
        int x = first.getStepY() * second.getStepZ() - first.getStepZ() * second.getStepY();
        int y = first.getStepZ() * second.getStepX() - first.getStepX() * second.getStepZ();
        int z = first.getStepX() * second.getStepY() - first.getStepY() * second.getStepX();
        return Direction.fromDelta(x, y, z);
    }

    public static float[] transformBox(
        Direction attachment,
        Direction tangent,
        double minX,
        double minY,
        double minZ,
        double maxX,
        double maxY,
        double maxZ
    ) {
        Direction outward = attachment.getOpposite();
        Direction right = cross(tangent, outward);
        double[] bounds = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
        for (double x : new double[]{minX, maxX}) {
            for (double y : new double[]{minY, maxY}) {
                for (double z : new double[]{minZ, maxZ}) {
                    double localX = x - 8.0;
                    double localY = y - 8.0;
                    double localZ = z - 8.0;
                    double worldX = 8.0 + right.getStepX() * localX + outward.getStepX() * localY
                        - tangent.getStepX() * localZ;
                    double worldY = 8.0 + right.getStepY() * localX + outward.getStepY() * localY
                        - tangent.getStepY() * localZ;
                    double worldZ = 8.0 + right.getStepZ() * localX + outward.getStepZ() * localY
                        - tangent.getStepZ() * localZ;
                    bounds[0] = Math.min(bounds[0], worldX);
                    bounds[1] = Math.min(bounds[1], worldY);
                    bounds[2] = Math.min(bounds[2], worldZ);
                    bounds[3] = Math.max(bounds[3], worldX);
                    bounds[4] = Math.max(bounds[4], worldY);
                    bounds[5] = Math.max(bounds[5], worldZ);
                }
            }
        }
        return new float[]{(float) bounds[0], (float) bounds[1], (float) bounds[2],
            (float) bounds[3], (float) bounds[4], (float) bounds[5]};
    }

    public static Direction transformDirection(
        Direction attachment, Direction tangent, Direction localDirection
    ) {
        Direction outward = attachment.getOpposite();
        Direction right = cross(tangent, outward);
        int x = right.getStepX() * localDirection.getStepX()
            + outward.getStepX() * localDirection.getStepY()
            - tangent.getStepX() * localDirection.getStepZ();
        int y = right.getStepY() * localDirection.getStepX()
            + outward.getStepY() * localDirection.getStepY()
            - tangent.getStepY() * localDirection.getStepZ();
        int z = right.getStepZ() * localDirection.getStepX()
            + outward.getStepZ() * localDirection.getStepY()
            - tangent.getStepZ() * localDirection.getStepZ();
        return Direction.fromDelta(x, y, z);
    }

    private static VoxelShape transformedBox(
        Direction attachment,
        Direction tangent,
        double minX,
        double minY,
        double minZ,
        double maxX,
        double maxY,
        double maxZ
    ) {
        float[] box = transformBox(attachment, tangent, minX, minY, minZ, maxX, maxY, maxZ);
        return Block.box(box[0], box[1], box[2], box[3], box[4], box[5]);
    }

    private record Connection(BlockPos pos, RedstoneSide side) {
    }

    private record InputPower(int total, int nonDust) {
    }
}
