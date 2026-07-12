package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;

/** A surface-mounted, non-attenuating redstone network with electrical contacts at its visible open ends. */
public class RedstoneWireBlock extends Block implements IHammerRemovable {
    public static final EnumProperty<ConnectionType> NORTH = EnumProperty.create("north", ConnectionType.class);
    public static final EnumProperty<ConnectionType> EAST = EnumProperty.create("east", ConnectionType.class);
    public static final EnumProperty<ConnectionType> SOUTH = EnumProperty.create("south", ConnectionType.class);
    public static final EnumProperty<ConnectionType> WEST = EnumProperty.create("west", ConnectionType.class);
    public static final List<EnumProperty<ConnectionType>> CONNECTION_PROPERTIES = List.of(NORTH, EAST, SOUTH, WEST);
    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    public static final DirectionProperty ATTACHMENT = DirectionProperty.create("attachment");
    public static final BooleanProperty DOT = BooleanProperty.create("dot");

    private static final Map<Direction, VoxelShape> DOT_SHAPES = new EnumMap<>(Direction.class);
    private static final Map<Direction, List<VoxelShape>> SIDE_SHAPES = new EnumMap<>(Direction.class);
    private static final Map<Direction, List<VoxelShape>> CORNER_SHAPES = new EnumMap<>(Direction.class);
    private static final Map<Direction, List<VoxelShape>> UP_SHAPES = new EnumMap<>(Direction.class);

    static {
        for (Direction attachment : Direction.values()) {
            Direction north = getLocalDirection(attachment, 0);
            DOT_SHAPES.put(attachment, transformedBox(attachment, north, 4.0, 0.0, 4.0, 12.0, 2.5, 12.0));
            List<VoxelShape> sides = new ArrayList<>(4);
            List<VoxelShape> corners = new ArrayList<>(4);
            List<VoxelShape> ups = new ArrayList<>(4);
            for (int index = 0; index < 4; index++) {
                Direction tangent = getLocalDirection(attachment, index);
                sides.add(transformedBox(attachment, tangent, 5.0, 0.0, 0.0, 11.0, 2.0, 8.0));
                corners.add(transformedBox(attachment, tangent, 5.0, 0.0, -2.0, 11.0, 2.0, 8.0));
                ups.add(transformedBox(attachment, tangent, 5.0, 1.0, -0.1, 11.0, 18.0, 2.0));
            }
            SIDE_SHAPES.put(attachment, List.copyOf(sides));
            CORNER_SHAPES.put(attachment, List.copyOf(corners));
            UP_SHAPES.put(attachment, List.copyOf(ups));
        }
    }

    public RedstoneWireBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(NORTH, ConnectionType.SIDE)
            .setValue(EAST, ConnectionType.NONE)
            .setValue(SOUTH, ConnectionType.SIDE)
            .setValue(WEST, ConnectionType.NONE)
            .setValue(POWER, 0)
            .setValue(ATTACHMENT, Direction.DOWN)
            .setValue(DOT, false));
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
        state = state.setValue(CONNECTION_PROPERTIES.get(index), ConnectionType.SIDE)
            .setValue(CONNECTION_PROPERTIES.get((index + 2) % 4), ConnectionType.SIDE);
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
            ConnectionType side = state.getValue(CONNECTION_PROPERTIES.get(index));
            if (side.isConnected() && side != ConnectionType.CORNER) {
                shape = Shapes.or(shape, SIDE_SHAPES.get(attachment).get(index));
            }
            if (side == ConnectionType.CORNER) {
                shape = Shapes.or(shape, CORNER_SHAPES.get(attachment).get(index));
            }
            if (side == ConnectionType.UP) {
                shape = Shapes.or(shape, UP_SHAPES.get(attachment).get(index));
            }
        }
        return shape;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!oldState.is(this)) {
            RedstoneWireNetworkManager.topologyChanged(level, pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!newState.is(this)) {
            RedstoneWireNetworkManager.topologyChanged(level, pos);
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
        } else {
            RedstoneWireNetworkManager.neighborChanged(level, pos, neighborBlock, neighborPos);
        }
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return !RedstoneWireNetworkManager.isSuppressingSignal();
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
        if (index < 0 || !state.getValue(CONNECTION_PROPERTIES.get(index)).isConnected()) {
            return false;
        }
        Connection[] cached = RedstoneWireNetworkManager.getConnections(level, pos);
        return (cached == null ? findConnection(level, pos, state, index) : cached[index]) == null;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 0;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (RedstoneWireNetworkManager.isSuppressingSignal()) {
            return 0;
        }
        Direction outputDirection = direction.getOpposite();
        int index = getLocalIndex(state.getValue(ATTACHMENT), outputDirection);
        if (index < 0
            || !state.getValue(CONNECTION_PROPERTIES.get(index)).isConnected()
            || hasWireConnection(level, pos, state, index)) {
            return 0;
        }
        BlockState receiver = level.getBlockState(pos.relative(outputDirection));
        if (receiver.is(Blocks.REDSTONE_WIRE)) {
            return RedstoneWireNetworkManager.getNonDustPower(level, pos, state.getValue(POWER));
        }
        return state.getValue(POWER);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, POWER, ATTACHMENT, DOT);
    }

    private boolean hasWireConnection(BlockGetter level, BlockPos pos, BlockState state, int index) {
        Connection[] cached = RedstoneWireNetworkManager.getConnections(level, pos);
        return (cached == null ? findConnection(level, pos, state, index) : cached[index]) != null;
    }

    BlockState connectionState(BlockGetter level, BlockPos pos, BlockState oldState) {
        return this.connectionState(level, pos, oldState, findConnections(level, pos, oldState));
    }

    BlockState connectionState(
        BlockGetter level, BlockPos pos, BlockState oldState, Connection[] connections
    ) {
        BlockState result = emptyState(this.defaultBlockState()
            .setValue(POWER, oldState.getValue(POWER))
            .setValue(ATTACHMENT, oldState.getValue(ATTACHMENT)));
        int connectionCount = 0;
        int first = -1;
        int second = -1;
        for (int index = 0; index < 4; index++) {
            Connection connection = connections[index];
            ConnectionType side = getConnection(level, pos, result, index, connection);
            result = result.setValue(CONNECTION_PROPERTIES.get(index), side);
            if (side.isConnected()) {
                if (first < 0) {
                    first = index;
                } else if (second < 0) {
                    second = index;
                }
                connectionCount++;
            }
        }

        if (connectionCount == 0) {
            boolean eastWest = oldState.getValue(EAST).isConnected() || oldState.getValue(WEST).isConnected();
            result = result.setValue(eastWest ? EAST : NORTH, ConnectionType.SIDE)
                .setValue(eastWest ? WEST : SOUTH, ConnectionType.SIDE);
        } else if (connectionCount == 1) {
            result = result.setValue(CONNECTION_PROPERTIES.get((first + 2) % 4), ConnectionType.SIDE);
        }

        boolean dot = connectionCount >= 3 || connectionCount == 2 && second != (first + 2) % 4;
        return result.setValue(DOT, dot);
    }

    private static BlockState emptyState(BlockState state) {
        for (EnumProperty<ConnectionType> property : CONNECTION_PROPERTIES) {
            state = state.setValue(property, ConnectionType.NONE);
        }
        return state.setValue(DOT, false);
    }

    private static ConnectionType getConnection(
        BlockGetter level, BlockPos pos, BlockState state, int index, @Nullable Connection connection
    ) {
        if (connection != null) {
            return connection.side();
        }
        Direction tangent = getLocalDirection(state.getValue(ATTACHMENT), index);
        BlockPos adjacentPos = pos.relative(tangent);
        BlockState adjacent = level.getBlockState(adjacentPos);
        return canAttachTo(level, adjacentPos, adjacent, tangent) ? ConnectionType.SIDE : ConnectionType.NONE;
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
    static Connection findConnection(BlockGetter level, BlockPos pos, BlockState state, int index) {
        Direction attachment = state.getValue(ATTACHMENT);
        Direction tangent = getLocalDirection(attachment, index);
        BlockPos endpoint = endpoint(pos, attachment, tangent);
        BlockPos raisedEndpoint = endpoint.relative(attachment.getOpposite(), 2);
        BlockPos supportPos = pos.relative(attachment);
        long[] directNeighbors = new long[8];
        int directCount = 0;
        long[] climbingNeighbors = new long[8];
        int climbingCount = 0;
        ConnectionType directSide = ConnectionType.SIDE;
        ConnectionType climbingSide = ConnectionType.SIDE;

        for (Direction candidateAttachment : Direction.values()) {
            for (int candidateIndex = 0; candidateIndex < 4; candidateIndex++) {
                Direction candidateTangent = getLocalDirection(candidateAttachment, candidateIndex);
                BlockPos candidatePos = positionForEndpoint(endpoint, candidateAttachment, candidateTangent);
                if (candidatePos == null || candidatePos.equals(pos)) {
                    continue;
                }
                BlockState candidate = level.getBlockState(candidatePos);
                if (!(candidate.getBlock() instanceof RedstoneWireBlock)
                    || candidate.getValue(ATTACHMENT) != candidateAttachment
                    || !canConnectDirectly(
                        level, pos, attachment, tangent, supportPos, candidatePos, candidateAttachment
                    )) {
                    continue;
                }
                directCount = addUnique(directNeighbors, directCount, candidatePos.asLong());
                boolean crossesSurface = candidateAttachment != attachment;
                if (attachment.getAxis().isHorizontal() && crossesSurface) {
                    directSide = ConnectionType.CORNER;
                }
            }
        }
        if (directCount > 0) {
            return new Connection(copyOf(directNeighbors, directCount), directSide);
        }

        boolean canClimbFromCurrent = canClimb(level, pos, attachment, tangent);
        for (Direction candidateAttachment : Direction.values()) {
            if (!canShareClimbingEdge(attachment, candidateAttachment)) {
                continue;
            }
            for (int candidateIndex = 0; candidateIndex < 4; candidateIndex++) {
                Direction candidateTangent = getLocalDirection(candidateAttachment, candidateIndex);
                BlockPos candidatePos = positionForEndpoint(
                    raisedEndpoint, candidateAttachment, candidateTangent
                );
                if (isNearby(pos, candidatePos)) {
                    BlockState candidate = level.getBlockState(candidatePos);
                    if (candidate.getBlock() instanceof RedstoneWireBlock
                        && candidate.getValue(ATTACHMENT) == candidateAttachment
                        && canClimbFromCurrent
                        && !hasDirectConnectionAtEndpoint(level, candidatePos, candidate, candidateIndex)) {
                        climbingCount = addUnique(climbingNeighbors, climbingCount, candidatePos.asLong());
                        climbingSide = ConnectionType.UP;
                    }
                }

                BlockPos lowerEndpoint = endpoint.relative(candidateAttachment, 2);
                candidatePos = positionForEndpoint(lowerEndpoint, candidateAttachment, candidateTangent);
                if (isNearby(pos, candidatePos)) {
                    BlockState candidate = level.getBlockState(candidatePos);
                    if (candidate.getBlock() instanceof RedstoneWireBlock
                        && candidate.getValue(ATTACHMENT) == candidateAttachment
                        && canClimb(level, candidatePos, candidateAttachment, candidateTangent)
                        && !hasDirectConnectionAtEndpoint(level, candidatePos, candidate, candidateIndex)) {
                        climbingCount = addUnique(climbingNeighbors, climbingCount, candidatePos.asLong());
                    }
                }
            }
        }
        if (climbingCount > 0) {
            return new Connection(copyOf(climbingNeighbors, climbingCount), climbingSide);
        }
        return null;
    }

    private static boolean hasDirectConnectionAtEndpoint(
        BlockGetter level, BlockPos pos, BlockState state, int index
    ) {
        Direction attachment = state.getValue(ATTACHMENT);
        Direction tangent = getLocalDirection(attachment, index);
        BlockPos endpoint = endpoint(pos, attachment, tangent);
        BlockPos supportPos = pos.relative(attachment);

        // An endpoint has at most 24 oriented representations: six attachment faces by four tangents.
        for (Direction candidateAttachment : Direction.values()) {
            for (int candidateIndex = 0; candidateIndex < 4; candidateIndex++) {
                Direction candidateTangent = getLocalDirection(candidateAttachment, candidateIndex);
                BlockPos candidatePos = positionForEndpoint(endpoint, candidateAttachment, candidateTangent);
                if (candidatePos == null || candidatePos.equals(pos)) {
                    continue;
                }
                BlockState candidate = level.getBlockState(candidatePos);
                if (!(candidate.getBlock() instanceof RedstoneWireBlock)
                    || candidate.getValue(ATTACHMENT) != candidateAttachment) {
                    continue;
                }
                if (canConnectDirectly(
                    level, pos, attachment, tangent, supportPos, candidatePos, candidateAttachment
                )) {
                    return true;
                }
            }
        }
        return false;
    }

    @Nullable
    private static BlockPos positionForEndpoint(
        BlockPos endpoint, Direction attachment, Direction tangent
    ) {
        int x = endpoint.getX() - 1 - attachment.getStepX() - tangent.getStepX();
        int y = endpoint.getY() - 1 - attachment.getStepY() - tangent.getStepY();
        int z = endpoint.getZ() - 1 - attachment.getStepZ() - tangent.getStepZ();
        if ((x & 1) != 0 || (y & 1) != 0 || (z & 1) != 0) {
            return null;
        }
        return new BlockPos(x / 2, y / 2, z / 2);
    }

    private static int addUnique(long[] values, int size, long value) {
        for (int index = 0; index < size; index++) {
            if (values[index] == value) {
                return size;
            }
        }
        values[size] = value;
        return size + 1;
    }

    private static long[] copyOf(long[] values, int size) {
        long[] result = new long[size];
        System.arraycopy(values, 0, result, 0, size);
        return result;
    }

    private static boolean canShareClimbingEdge(Direction attachment, Direction candidateAttachment) {
        return candidateAttachment == attachment
            || candidateAttachment.getAxis() != attachment.getAxis();
    }

    private static boolean isNearby(BlockPos origin, @Nullable BlockPos candidate) {
        return candidate != null
            && !candidate.equals(origin)
            && Math.abs(candidate.getX() - origin.getX()) <= 1
            && Math.abs(candidate.getY() - origin.getY()) <= 1
            && Math.abs(candidate.getZ() - origin.getZ()) <= 1;
    }

    private static boolean canConnectDirectly(
        BlockGetter level,
        BlockPos pos,
        Direction attachment,
        Direction tangent,
        BlockPos supportPos,
        BlockPos candidatePos,
        Direction candidateAttachment
    ) {
        if (candidateAttachment == attachment) {
            return true;
        }
        return candidatePos.relative(candidateAttachment).equals(supportPos)
            && !isCornerBlocked(level, pos, tangent);
    }

    private static boolean isCornerBlocked(BlockGetter level, BlockPos pos, Direction tangent) {
        BlockPos diagonalPos = pos.relative(tangent);
        return level.getBlockState(diagonalPos).isRedstoneConductor(level, diagonalPos);
    }

    static Connection[] findConnections(BlockGetter level, BlockPos pos, BlockState state) {
        Connection[] connections = new Connection[4];
        for (int index = 0; index < connections.length; index++) {
            connections[index] = findConnection(level, pos, state, index);
        }
        return connections;
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
        return (!(state.getBlock() instanceof SlabBlock) || state.getValue(SlabBlock.TYPE) == SlabType.DOUBLE)
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

    /** Replaces this wire with the placement state for another supporting face without consuming an item. */
    public boolean reattach(Level level, BlockPos pos, BlockState state) {
        if (!state.is(this) || !state.canSurvive(level, pos) || !level.setBlock(pos, state, Block.UPDATE_ALL)) {
            return false;
        }
        RedstoneWireNetworkManager.topologyChanged(level, pos);
        return true;
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

    static int getLocalIndex(Direction attachment, Direction worldDirection) {
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
        return Objects.requireNonNull(Direction.fromDelta(x, y, z));
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
        return Objects.requireNonNull(Direction.fromDelta(x, y, z));
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

    public enum ConnectionType implements StringRepresentable {
        NONE("none", false),
        SIDE("side", true),
        UP("up", true),
        CORNER("corner", true);

        private final String name;
        @Getter
        private final boolean connected;

        ConnectionType(String name, boolean connected) {
            this.name = name;
            this.connected = connected;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    record Connection(long[] positions, ConnectionType side) {
    }
}
