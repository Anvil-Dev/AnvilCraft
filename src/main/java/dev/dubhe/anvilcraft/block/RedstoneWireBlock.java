package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 可附着在任意方块表面的无衰减红石导线。
 *
 * <p>四个方向属性描述的是附着面内的局部方向，而不是固定的世界水平面方向。相邻导线通过几何端点组成网络，只有断口才会与外部红石元件交换信号。</p>
 */
public class RedstoneWireBlock extends Block implements IHammerRemovable {
    /** 附着面内四个局部方向的连接外观。 */
    public static final EnumProperty<ConnectionType> NORTH = EnumProperty.create("north", ConnectionType.class);
    public static final EnumProperty<ConnectionType> EAST = EnumProperty.create("east", ConnectionType.class);
    public static final EnumProperty<ConnectionType> SOUTH = EnumProperty.create("south", ConnectionType.class);
    public static final EnumProperty<ConnectionType> WEST = EnumProperty.create("west", ConnectionType.class);
    public static final List<EnumProperty<ConnectionType>> CONNECTION_PROPERTIES = List.of(NORTH, EAST, SOUTH, WEST);
    /** 从导线位置指向其支撑方块的方向。 */
    public static final EnumProperty<Direction> ATTACHMENT = EnumProperty.create("attachment", Direction.class);
    /** 是否在中心绘制接线点。 */
    public static final BooleanProperty DOT = BooleanProperty.create("dot");

    private static final Map<Direction, VoxelShape> DOT_SHAPES = new EnumMap<>(Direction.class);
    private static final Map<Direction, List<VoxelShape>> SIDE_SHAPES = new EnumMap<>(Direction.class);
    private static final Map<Direction, List<VoxelShape>> CORNER_SHAPES = new EnumMap<>(Direction.class);
    private static final Map<Direction, List<VoxelShape>> CORNER_SP_SHAPES = new EnumMap<>(Direction.class);
    private static final Map<Direction, List<VoxelShape>> UP_SHAPES = new EnumMap<>(Direction.class);

    static {
        // 碰撞/选取形状只由附着方向和连接类型决定，预计算可避免每次光线检测都重复坐标变换与形状合并。
        for (Direction attachment : Direction.values()) {
            Direction north = getLocalDirection(attachment, 0);
            DOT_SHAPES.put(attachment, transformedBox(attachment, north, 4.0, 0.0, 4.0, 12.0, 2.5, 12.0));
            List<VoxelShape> sides = new ArrayList<>(4);
            List<VoxelShape> corners = new ArrayList<>(4);
            List<VoxelShape> specialCorners = new ArrayList<>(4);
            List<VoxelShape> ups = new ArrayList<>(4);
            for (int index = 0; index < 4; index++) {
                Direction tangent = getLocalDirection(attachment, index);
                sides.add(transformedBox(attachment, tangent, 5.0, 0.0, 0.0, 11.0, 2.0, 8.0));
                corners.add(transformedBox(attachment, tangent, 5.0, 0.0, -2.0, 11.0, 2.0, 8.0));
                specialCorners.add(transformedBox(attachment, tangent, 5.0, 0.0, -1.0, 11.0, 2.0, 8.0));
                ups.add(transformedBox(attachment, tangent, 5.0, 1.0, -0.1, 11.0, 18.0, 2.0));
            }
            SIDE_SHAPES.put(attachment, List.copyOf(sides));
            CORNER_SHAPES.put(attachment, List.copyOf(corners));
            CORNER_SP_SHAPES.put(attachment, List.copyOf(specialCorners));
            UP_SHAPES.put(attachment, List.copyOf(ups));
        }
    }

    public RedstoneWireBlock(Properties properties) {
        super(properties);
        // 默认保留一条南北向直线，使孤立导线刚放下时也有可见且可重新定向的形状。
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(NORTH, ConnectionType.SIDE)
            .setValue(EAST, ConnectionType.NONE)
            .setValue(SOUTH, ConnectionType.SIDE)
            .setValue(WEST, ConnectionType.NONE)
            .setValue(ATTACHMENT, Direction.DOWN)
            .setValue(DOT, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction attachment = context.getClickedFace().getOpposite();
        BlockState state = emptyState(this.defaultBlockState().setValue(ATTACHMENT, attachment));
        Direction preferred = getLocalDirection(attachment, 0);
        // 用玩家视线在附着面上的主要方向决定孤立导线朝向，避免墙面导线总沿固定世界轴放置。
        for (Direction direction : context.getNearestLookingDirections()) {
            if (direction.getAxis() != attachment.getAxis()) {
                preferred = direction;
                break;
            }
        }
        int index = getLocalIndex(attachment, preferred);
        // 先生成一条直线，再让 connectionState 根据真实邻居扩展为拐角、分叉或爬升形态。
        state = state.setValue(CONNECTION_PROPERTIES.get(index), ConnectionType.SIDE)
            .setValue(CONNECTION_PROPERTIES.get((index + 2) % 4), ConnectionType.SIDE);
        return this.connectionState(context.getLevel(), context.getClickedPos(), state);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction attachment = state.getValue(ATTACHMENT);
        BlockPos supportPos = pos.relative(attachment);
        BlockState support = level.getBlockState(supportPos);
        // 原版漏斗顶面可放红石粉，但其面坚固性判定不满足这里的通用条件，因此显式兼容。
        return support.isFaceSturdy(level, supportPos, attachment.getOpposite())
            || attachment == Direction.DOWN && support.is(Blocks.HOPPER);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction attachment = state.getValue(ATTACHMENT);
        VoxelShape shape = state.getValue(DOT) ? DOT_SHAPES.get(attachment) : Shapes.empty();
        for (int index = 0; index < 4; index++) {
            ConnectionType side = state.getValue(CONNECTION_PROPERTIES.get(index));
            if (side.isConnected() && side != ConnectionType.CORNER && side != ConnectionType.CORNER_SP) {
                shape = Shapes.or(shape, SIDE_SHAPES.get(attachment).get(index));
            }
            if (side == ConnectionType.CORNER) {
                shape = Shapes.or(shape, CORNER_SHAPES.get(attachment).get(index));
            }
            if (side == ConnectionType.CORNER_SP) {
                shape = Shapes.or(shape, CORNER_SP_SHAPES.get(attachment).get(index));
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
            // 外观状态的内部改写不改变网络成员，只有真正新增导线时才使拓扑缓存失效。
            RedstoneWireNetworkManager.topologyChanged(level, pos);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(
        BlockState state,
        ServerLevel level,
        BlockPos pos,
        boolean movedByPiston
    ) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        // 先完成方块替换再重建，连接搜索才能看到移除后的真实世界状态。
        RedstoneWireNetworkManager.wireRemoved(level, pos);
        RedstoneWireNetworkManager.topologyChanged(level, pos);
    }

    @Override
    protected void neighborChanged(
        BlockState state,
        Level level,
        BlockPos pos,
        Block neighborBlock,
        @Nullable Orientation orientation,
        boolean movedByPiston
    ) {
        if (level.isClientSide()) {
            return;
        }
        if (!state.canSurvive(level, pos)) {
            dropResources(state, level, pos);
            level.removeBlock(pos, false);
        } else {
            RedstoneWireNetworkManager.neighborChanged(level, pos, neighborBlock, null);
        }
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        // 网络采样外部输入时临时关闭自身输出，否则上一轮功率会被重新读作输入并造成自激锁存。
        return !RedstoneWireNetworkManager.isSuppressingSignal();
    }

    @Override
    public boolean canConnectRedstone(
        BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction
    ) {
        Direction terminalDirection;
        if (direction == null) {
            // 原版红石粉检查斜向下方的方块时不带方向；此时只暴露侧面导线的向上开放断口。
            terminalDirection = Direction.UP;
        } else if (!direction.getAxis().isHorizontal()) {
            return false;
        } else {
            terminalDirection = direction.getOpposite();
        }
        int index = getLocalIndex(state.getValue(ATTACHMENT), terminalDirection);
        if (index < 0 || !state.getValue(CONNECTION_PROPERTIES.get(index)).isConnected()) {
            return false;
        }
        // 内部接线端只负责连通网络；仅开放端点应被外部元件视为红石接口。
        return this.isOpenTerminal(level, pos, state, index);
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        // 导线模拟原版红石粉的弱充能，不直接强充能相邻方块。
        return 0;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (RedstoneWireNetworkManager.isSuppressingSignal()) {
            // 与 isSignalSource 同时兜底，确保不同红石查询路径在采样阶段都读不到导线自身。
            return 0;
        }
        Direction outputDirection = direction.getOpposite();
        int index = getLocalIndex(state.getValue(ATTACHMENT), outputDirection);
        if (index < 0
            || !state.getValue(CONNECTION_PROPERTIES.get(index)).isConnected()
            || isManuallyHidden(level, pos, state, index)
            || this.hasWireConnection(level, pos, state, index)) {
            return 0;
        }
        BlockState receiver = level.getBlockState(pos.relative(outputDirection));
        int power = RedstoneWireNetworkManager.getPower(level, pos);
        if (receiver.is(Blocks.REDSTONE_WIRE)) {
            // 不把来自原版红石粉的输入再输出给红石粉，使导线整体像一个完整方块，
            // 且避免无衰减网络与粉线组成正反馈回路。
            return RedstoneWireNetworkManager.getNonDustPower(level, pos, power);
        }
        return power;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, ATTACHMENT, DOT);
    }

    private boolean hasWireConnection(BlockGetter level, BlockPos pos, BlockState state, int index) {
        Connection[] cached = RedstoneWireNetworkManager.getConnections(level, pos);
        // 客户端和缓存尚未建立的服务端仍需即时计算，保证外观与信号查询不会依赖事件执行顺序。
        return (cached == null ? findConnection(level, pos, state, index) : cached[index]) != null;
    }

    private boolean isOpenTerminal(BlockGetter level, BlockPos pos, BlockState state, int index) {
        Connection[] cached = RedstoneWireNetworkManager.getConnections(level, pos);
        return (cached == null ? findConnection(level, pos, state, index) : cached[index]) == null
            && !isManuallyHidden(level, pos, state, index);
    }

    /** 供原版红石粉采样斜下方导线的非粉线信号，避免该特殊连接只改变外观。 */
    public static int getUpwardDustSignal(BlockGetter level, BlockPos dustPos) {
        int power = 0;
        for (Direction towardWire : Direction.Plane.HORIZONTAL) {
            BlockPos wirePos = dustPos.relative(towardWire).below();
            BlockState wireState = level.getBlockState(wirePos);
            if (!(wireState.getBlock() instanceof RedstoneWireBlock wire)) {
                continue;
            }
            Direction attachment = towardWire.getOpposite();
            if (wireState.getValue(ATTACHMENT) != attachment) {
                continue;
            }
            int index = getLocalIndex(attachment, Direction.UP);
            if (index < 0
                || !wireState.getValue(CONNECTION_PROPERTIES.get(index)).isConnected()
                || !hasUpwardDustConnection(level, wirePos, wireState, index)
                || !wire.isOpenTerminal(level, wirePos, wireState, index)) {
                continue;
            }
            int totalPower = RedstoneWireNetworkManager.getPower(level, wirePos);
            power = Math.max(power, RedstoneWireNetworkManager.getNonDustPower(level, wirePos, totalPower));
        }
        return power;
    }

    /** 根据当前世界重新计算指定导线的四向外观状态。 */
    BlockState connectionState(BlockGetter level, BlockPos pos, BlockState oldState) {
        return this.connectionState(level, pos, oldState, findConnections(level, pos, oldState));
    }

    /** 使用已经求出的连接关系生成方块状态，供网络重建时避免重复搜索。 */
    BlockState connectionState(
        BlockGetter level, BlockPos pos, BlockState oldState, Connection[] connections
    ) {
        // 从空状态开始可以清除已经断开的旧方向，同时保留附着面。
        BlockState result = emptyState(this.defaultBlockState()
            .setValue(ATTACHMENT, oldState.getValue(ATTACHMENT)));
        int forcedTerminalMask = RedstoneWireNetworkManager.getForcedTerminalMask(level, pos);
        int hiddenTerminalMask = RedstoneWireNetworkManager.getHiddenConnectionMask(level, pos);
        for (int index = 0; index < 4; index++) {
            Connection connection = connections[index];
            EnumProperty<ConnectionType> property = CONNECTION_PROPERTIES.get(index);
            ConnectionType side = getConnection(
                level,
                pos,
                result,
                index,
                connection,
                (forcedTerminalMask & (1 << index)) != 0,
                (hiddenTerminalMask & (1 << index)) != 0
            );
            result = result.setValue(property, side);
        }

        int visibleMask = connectedMask(result);
        int connectionCount = Integer.bitCount(visibleMask);
        if (connectionCount == 0) {
            // 孤立导线优先保留旧直线轴；被玩家隐藏一端时改用另一条可用轴，避免隐藏端口被回退逻辑画回。
            int fallbackMask = fallbackMask(oldState, hiddenTerminalMask);
            for (int index = 0; index < 4; index++) {
                if ((fallbackMask & (1 << index)) != 0) {
                    result = result.setValue(CONNECTION_PROPERTIES.get(index), ConnectionType.SIDE);
                }
            }
        } else if (connectionCount == 1) {
            // 单端连接优先补齐反方向；若该端被隐藏，则补一条其它可用方向，保留可见的非点状形态。
            int first = Integer.numberOfTrailingZeros(visibleMask);
            int opposite = (first + 2) % 4;
            int fallback = (hiddenTerminalMask & (1 << opposite)) == 0
                ? opposite
                : firstAvailableDirection(hiddenTerminalMask, 1 << first);
            if (fallback >= 0) {
                result = result.setValue(CONNECTION_PROPERTIES.get(fallback), ConnectionType.SIDE);
            }
        }

        visibleMask = connectedMask(result);
        connectionCount = Integer.bitCount(visibleMask);
        int first = connectionCount == 0 ? -1 : Integer.numberOfTrailingZeros(visibleMask);
        int secondMask = first < 0 ? 0 : visibleMask & ~(1 << first);
        int second = secondMask == 0 ? -1 : Integer.numberOfTrailingZeros(secondMask);
        // 直线不需要中心贴图；单端、拐角或三岔以上需要中心点遮住各段模型的接缝。
        boolean dot = connectionCount == 1
            || connectionCount >= 3
            || connectionCount == 2 && second != (first + 2) % 4;
        return result.setValue(DOT, dot);
    }

    private static int connectedMask(BlockState state) {
        int mask = 0;
        for (int index = 0; index < 4; index++) {
            if (state.getValue(CONNECTION_PROPERTIES.get(index)).isConnected()) {
                mask |= 1 << index;
            }
        }
        return mask;
    }

    private static int fallbackMask(BlockState oldState, int hiddenMask) {
        boolean eastWest = oldState.getValue(EAST).isConnected() || oldState.getValue(WEST).isConnected();
        int preferred = eastWest ? 0b1010 : 0b0101;
        int alternate = eastWest ? 0b0101 : 0b1010;
        int available = (~hiddenMask) & 0x0F;
        if ((preferred & available) == preferred) {
            return preferred;
        }
        if ((alternate & available) == alternate) {
            return alternate;
        }
        int first = Integer.numberOfTrailingZeros(available);
        if (first >= 4) {
            return 0;
        }
        int remaining = available & ~(1 << first);
        int second = Integer.numberOfTrailingZeros(remaining);
        return second >= 4 ? 1 << first : (1 << first) | (1 << second);
    }

    private static int firstAvailableDirection(int hiddenMask, int excludedMask) {
        int available = (~hiddenMask) & ~excludedMask & 0x0F;
        return available == 0 ? -1 : Integer.numberOfTrailingZeros(available);
    }

    private static BlockState emptyState(BlockState state) {
        for (EnumProperty<ConnectionType> property : CONNECTION_PROPERTIES) {
            state = state.setValue(property, ConnectionType.NONE);
        }
        return state.setValue(DOT, false);
    }

    private static ConnectionType getConnection(
        BlockGetter level,
        BlockPos pos,
        BlockState state,
        int index,
        @Nullable Connection connection,
        boolean forcedTerminal,
        boolean hiddenTerminal
    ) {
        if (hiddenTerminal) {
            return ConnectionType.NONE;
        }
        if (connection != null) {
            // 导线到导线的几何关系决定 SIDE、CORNER 或 UP，应优先于普通红石接口外观。
            return connection.side();
        }
        if (forcedTerminal) {
            // 强制端口不要求相邻已有红石元件；墙面向上的粉线连接仍使用专用短拐角。
            return hasUpwardDustConnection(level, pos, state, index)
                ? ConnectionType.CORNER_SP
                : ConnectionType.SIDE;
        }
        if (hasDustConnection(level, pos, state, index)) {
            // 原版粉线也是真实接入端；墙面向上的粉线使用专用短拐角避免模型插入粉线。
            return hasUpwardDustConnection(level, pos, state, index)
                ? ConnectionType.CORNER_SP
                : ConnectionType.SIDE;
        }
        Direction tangent = getLocalDirection(state.getValue(ATTACHMENT), index);
        BlockPos adjacentPos = pos.relative(tangent);
        BlockState adjacent = level.getBlockState(adjacentPos);
        return canAttachTo(level, adjacentPos, adjacent, tangent) ? ConnectionType.SIDE : ConnectionType.NONE;
    }

    private static boolean canAttachTo(BlockGetter level, BlockPos pos, BlockState state, Direction direction) {
        if (state.is(Blocks.REDSTONE_WIRE) || state.getBlock() instanceof RedstoneWireBlock) {
            // 两类导线都有专门的信号反馈规则，不能再把彼此误判为普通开放端设备。
            return false;
        }
        // 标靶和红石火把的 NeoForge 通用连接判定不覆盖所有方向，因此与原版粉线规则保持显式兼容。
        return state.canRedstoneConnectTo(level, pos, direction)
            || state.is(Blocks.TARGET)
            || state.is(Blocks.REDSTONE_TORCH)
            || state.is(Blocks.REDSTONE_WALL_TORCH);
    }

    /** 侧面导线向上断开时，检查支撑方块顶面的原版红石粉斜角连接。 */
    private static boolean hasUpwardDustConnection(BlockGetter level, BlockPos pos, BlockState state, int index) {
        Direction attachment = state.getValue(ATTACHMENT);
        if (!attachment.getAxis().isHorizontal() || getLocalDirection(attachment, index) != Direction.UP) {
            return false;
        }
        return level.getBlockState(pos.relative(attachment).above()).is(Blocks.REDSTONE_WIRE);
    }

    /** 原版红石粉只有与导线处于同一附着面，或位于墙面向上拐角时才算真实接入。 */
    private static boolean hasDustConnection(BlockGetter level, BlockPos pos, BlockState state, int index) {
        if (hasUpwardDustConnection(level, pos, state, index)) {
            return true;
        }
        Direction attachment = state.getValue(ATTACHMENT);
        Direction tangent = getLocalDirection(attachment, index);
        if (attachment != Direction.DOWN || !tangent.getAxis().isHorizontal()) {
            return false;
        }
        BlockState dust = level.getBlockState(pos.relative(tangent));
        return dust.is(Blocks.REDSTONE_WIRE)
            && dust.getValue(RedStoneWireBlock.PROPERTY_BY_DIRECTION.get(tangent.getOpposite())).isConnected();
    }

    /** 该端口是否接入了自定义导线、原版粉线或普通红石元件，而不是单纯的直线外观回退。 */
    static boolean hasConnectionTarget(BlockGetter level, BlockPos pos, BlockState state, int index) {
        if (findConnection(level, pos, state, index) != null || hasDustConnection(level, pos, state, index)) {
            return true;
        }
        Direction tangent = getLocalDirection(state.getValue(ATTACHMENT), index);
        BlockPos adjacentPos = pos.relative(tangent);
        return canAttachTo(level, adjacentPos, level.getBlockState(adjacentPos), tangent);
    }

    /** 返回开放端点实际对应的外部方块位置；斜角红石粉位于支撑方块的顶面。 */
    static BlockPos terminalTarget(BlockGetter level, BlockPos pos, BlockState state, Direction tangent) {
        int index = getLocalIndex(state.getValue(ATTACHMENT), tangent);
        if (index >= 0 && hasUpwardDustConnection(level, pos, state, index)) {
            return pos.relative(state.getValue(ATTACHMENT)).above();
        }
        return pos.relative(tangent);
    }

    /**
     * 查找指定局部方向上与当前端点重合或可爬升相连的自定义导线。
     *
     * @return 连接到的方块位置及其显示类型；开放端点返回 {@code null}
     */
    @Nullable
    static Connection findConnection(BlockGetter level, BlockPos pos, BlockState state, int index) {
        Connection geometric = findGeometricConnection(level, pos, state, index);
        if (geometric == null || isPortHidden(level, pos, index)) {
            return null;
        }

        int connectedCount = 0;
        for (long neighbor : geometric.positions()) {
            if (!isNeighborPortHidden(level, pos, BlockPos.of(neighbor))) {
                connectedCount++;
            }
        }
        if (connectedCount == 0) {
            return null;
        }
        if (connectedCount == geometric.positions().length) {
            return geometric;
        }
        long[] connectedNeighbors = new long[connectedCount];
        int targetIndex = 0;
        for (long neighbor : geometric.positions()) {
            if (!isNeighborPortHidden(level, pos, BlockPos.of(neighbor))) {
                connectedNeighbors[targetIndex++] = neighbor;
            }
        }
        return new Connection(connectedNeighbors, geometric.side());
    }

    /** 返回忽略玩家手动断开标记的原始几何连接。 */
    @Nullable
    static Connection findGeometricConnection(BlockGetter level, BlockPos pos, BlockState state, int index) {
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

        // 端点用二倍整数坐标表示，枚举 6 个附着面 x 4 个切向即可精确反解所有可能与其重合的导线。
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
                    // 墙面导线绕支撑方块边缘转向时需要向负局部坐标延伸，用专门模型覆盖拐角。
                    directSide = ConnectionType.CORNER;
                }
            }
        }
        if (directCount > 0) {
            // 共享同一物理端点的直接连接优先，避免同时把附近可爬升导线错误并入网络。
            return new Connection(copyOf(directNeighbors, directCount), directSide);
        }

        boolean canClimbFromCurrent = canClimb(level, pos, attachment, tangent);
        // 没有直接连接时才检查隔着一格高度的上下坡关系，复现红石粉沿完整碰撞面爬升的行为。
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
                        // 从当前导线向外爬升时，当前这一段需要额外绘制竖直模型。
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

    /** 查找另一根导线中几何上连接到目标位置的局部端口。 */
    static int findGeometricConnectionIndex(
        BlockGetter level, BlockPos pos, BlockState state, BlockPos targetPos
    ) {
        long target = targetPos.asLong();
        for (int index = 0; index < 4; index++) {
            Connection connection = findGeometricConnection(level, pos, state, index);
            if (connection == null) {
                continue;
            }
            for (long neighbor : connection.positions()) {
                if (neighbor == target) {
                    return index;
                }
            }
        }
        return -1;
    }

    private static boolean isPortHidden(BlockGetter level, BlockPos pos, int index) {
        return (RedstoneWireNetworkManager.getHiddenConnectionMask(level, pos) & (1 << index)) != 0;
    }

    private static boolean isNeighborPortHidden(BlockGetter level, BlockPos pos, BlockPos neighborPos) {
        int hiddenMask = RedstoneWireNetworkManager.getHiddenConnectionMask(level, neighborPos);
        if (hiddenMask == 0) {
            return false;
        }
        BlockState neighborState = level.getBlockState(neighborPos);
        if (!(neighborState.getBlock() instanceof RedstoneWireBlock)) {
            return false;
        }
        int neighborIndex = findGeometricConnectionIndex(level, neighborPos, neighborState, pos);
        return neighborIndex >= 0 && (hiddenMask & (1 << neighborIndex)) != 0;
    }

    /** 当前端口或其几何邻线的对应端口是否被玩家关闭。 */
    static boolean isManuallyHidden(
        BlockGetter level, BlockPos pos, BlockState state, int index
    ) {
        if (isPortHidden(level, pos, index)) {
            return true;
        }
        Connection geometric = findGeometricConnection(level, pos, state, index);
        if (geometric == null) {
            return false;
        }
        for (long neighbor : geometric.positions()) {
            if (isNeighborPortHidden(level, pos, BlockPos.of(neighbor))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasDirectConnectionAtEndpoint(
        BlockGetter level, BlockPos pos, BlockState state, int index
    ) {
        Direction attachment = state.getValue(ATTACHMENT);
        Direction tangent = getLocalDirection(attachment, index);
        BlockPos endpoint = endpoint(pos, attachment, tangent);
        BlockPos supportPos = pos.relative(attachment);

        // 一个端点最多只有 24 种朝向表示（6 个附着面 x 4 个切向），枚举比维护额外空间索引更便宜。
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

    /** 将二倍坐标中的几何端点反解为指定朝向的方块坐标。 */
    @Nullable
    private static BlockPos positionForEndpoint(
        BlockPos endpoint, Direction attachment, Direction tangent
    ) {
        int x = endpoint.getX() - 1 - attachment.getStepX() - tangent.getStepX();
        int y = endpoint.getY() - 1 - attachment.getStepY() - tangent.getStepY();
        int z = endpoint.getZ() - 1 - attachment.getStepZ() - tangent.getStepZ();
        if ((x & 1) != 0 || (y & 1) != 0 || (z & 1) != 0) {
            // 出现奇数说明该朝向的方块中心不落在整数格点上，不可能存在对应导线。
            return null;
        }
        return new BlockPos(x / 2, y / 2, z / 2);
    }

    private static int addUnique(long[] values, int size, long value) {
        // 候选只来自相邻 3x3x3 范围，固定小数组可避开热路径上的 HashSet 分配。
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
        // 同轴且相反的附着面隔着整个方块，几何上不可能共享同一条爬升边。
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
        // 跨面连接必须共同依附于同一个支撑方块，且拐角空间不能被实体导体占据。
        return candidatePos.relative(candidateAttachment).equals(supportPos)
            && !isCornerBlocked(level, pos, tangent);
    }

    private static boolean isCornerBlocked(BlockGetter level, BlockPos pos, Direction tangent) {
        BlockPos diagonalPos = pos.relative(tangent);
        return level.getBlockState(diagonalPos).isRedstoneConductor(level, diagonalPos);
    }

    /** 按局部北、东、南、西顺序计算一根导线的全部内部连接。 */
    static Connection[] findConnections(BlockGetter level, BlockPos pos, BlockState state) {
        // 数组索引与 CONNECTION_PROPERTIES 共享同一局部方向约定，Manager 可以直接缓存并复用。
        Connection[] connections = new Connection[4];
        for (int index = 0; index < connections.length; index++) {
            connections[index] = findConnection(level, pos, state, index);
        }
        return connections;
    }

    /** 可爬墙逻辑，判断导线前上方是不是同侧平面的导线或上方是不是贴墙斜面上的导线 */
    private static boolean canClimb(BlockGetter level, BlockPos pos, Direction attachment, Direction tangent) {
        Direction outward = attachment.getOpposite();
        BlockPos bridgePos = pos.relative(tangent);
        BlockState bridge = level.getBlockState(bridgePos);
        return !level.getBlockState(pos.relative(outward)).isRedstoneConductor(level, pos.relative(outward))
            && hasFullCollisionFace(level, bridgePos, bridge, tangent.getOpposite());
    }

    /** 只有朝向导线的碰撞面完整时才允许爬升，与支撑方块的具体类型无关。 */
    private static boolean hasFullCollisionFace(
        BlockGetter level, BlockPos pos, BlockState state, Direction side
    ) {
        return Block.isFaceFull(state.getCollisionShape(level, pos), side);
    }

    /**
     * 返回某一导线端点的二倍整数坐标。
     *
     * <p>使用整数保存半格端点，既避免浮点误差，也让不同附着面的同一物理端点可以直接比较。</p>
     */
    private static BlockPos endpoint(BlockPos pos, Direction attachment, Direction tangent) {
        return new BlockPos(
            pos.getX() * 2 + 1 + attachment.getStepX() + tangent.getStepX(),
            pos.getY() * 2 + 1 + attachment.getStepY() + tangent.getStepY(),
            pos.getZ() * 2 + 1 + attachment.getStepZ() + tangent.getStepZ()
        );
    }

    /** 不消耗物品，将已有导线改挂到玩家新点击的支撑面。 */
    public boolean reattach(Level level, BlockPos pos, BlockState state) {
        if (!state.is(this) || !state.canSurvive(level, pos) || !level.setBlock(pos, state, Block.UPDATE_ALL)) {
            return false;
        }
        RedstoneWireNetworkManager.clearConnectionOverrides(level, pos);
        // 位置没有变化，但附着面会改变几何端点，所以必须按拓扑变化而不是普通状态变化处理。
        RedstoneWireNetworkManager.topologyChanged(level, pos);
        return true;
    }

    /** 根据附着面内的点击位置编辑最近的端口，不消耗手中的导线。 */
    public boolean editConnection(
        Level level, BlockPos pos, BlockState state, Vec3 hitLocation, boolean directHit
    ) {
        ClickedPort clicked = getClickedPort(pos, state, hitLocation, directHit);
        return RedstoneWireNetworkManager.editConnection(
            level, pos, state, clicked.index(), clicked.onConnectionSegment(), clicked.turnIndex()
        );
    }

    /** 将点击投影到附着面，并区分实际线段区域和该方向上的空白区域。 */
    private static ClickedPort getClickedPort(
        BlockPos pos, BlockState state, Vec3 hitLocation, boolean directHit
    ) {
        double dx = hitLocation.x - pos.getX() - 0.5;
        double dy = hitLocation.y - pos.getY() - 0.5;
        double dz = hitLocation.z - pos.getZ() - 0.5;
        Direction attachment = state.getValue(ATTACHMENT);
        int result = directHit ? getHitConnectionIndex(state, attachment, dx, dy, dz) : -1;
        boolean onConnectionSegment = result >= 0;
        if (result < 0) {
            result = getClosestConnectionIndex(attachment, dx, dy, dz);
        }
        Direction perpendicular = getLocalDirection(attachment, (result + 1) % 4);
        double transverse = dx * perpendicular.getStepX()
            + dy * perpendicular.getStepY()
            + dz * perpendicular.getStepZ();
        int turnIndex = transverse >= 0.0 ? (result + 1) % 4 : (result + 3) % 4;
        return new ClickedPort(result, onConnectionSegment, turnIndex);
    }

    /** 直接命中模型时，只从命中点覆盖到的可见线段中选择端口。 */
    private static int getHitConnectionIndex(
        BlockState state, Direction attachment, double dx, double dy, double dz
    ) {
        int result = -1;
        double bestProjection = Double.NEGATIVE_INFINITY;
        for (int index = 0; index < 4; index++) {
            if (!state.getValue(CONNECTION_PROPERTIES.get(index)).isConnected()) {
                continue;
            }
            Direction tangent = getLocalDirection(attachment, index);
            double projection = dx * tangent.getStepX() + dy * tangent.getStepY() + dz * tangent.getStepZ();
            Direction perpendicular = getLocalDirection(attachment, (index + 1) % 4);
            double transverse = dx * perpendicular.getStepX()
                + dy * perpendicular.getStepY()
                + dz * perpendicular.getStepZ();
            if (projection >= -1.0E-6
                && Math.abs(transverse) <= 4.0 / 16.0 + 1.0E-6
                && projection > bestProjection) {
                bestProjection = projection;
                result = index;
            }
        }
        return result;
    }

    /** 点击支撑面空白或中心接点时，按附着面内投影选择最近方向。 */
    private static int getClosestConnectionIndex(Direction attachment, double dx, double dy, double dz) {
        int result = 0;
        double bestProjection = Double.NEGATIVE_INFINITY;
        for (int index = 0; index < 4; index++) {
            Direction tangent = getLocalDirection(attachment, index);
            double projection = dx * tangent.getStepX() + dy * tangent.getStepY() + dz * tangent.getStepZ();
            if (projection > bestProjection) {
                bestProjection = projection;
                result = index;
            }
        }
        return result;
    }

    private record ClickedPort(int index, boolean onConnectionSegment, int turnIndex) {
    }

    /** 将附着面内的方向索引转换为世界方向。 */
    public static Direction getLocalDirection(Direction attachment, int index) {
        // 为每个附着面构造稳定的局部北向：地面沿世界北，天花板反向，墙面统一朝世界上方。
        Direction north = attachment == Direction.DOWN
            ? Direction.NORTH
            : attachment == Direction.UP ? Direction.SOUTH : Direction.UP;
        Direction outward = attachment.getOpposite();
        // 叉积得到局部东向，保证四个方向在从导线外侧观察时始终保持一致的环绕顺序。
        Direction east = cross(north, outward);
        return switch (index) {
            case 0 -> north;
            case 1 -> east;
            case 2 -> north.getOpposite();
            case 3 -> east.getOpposite();
            default -> throw new IllegalArgumentException("Invalid local direction index: " + index);
        };
    }

    /** 将世界方向转换为附着面内的方向索引；方向不在该平面内时返回 -1。 */
    static int getLocalIndex(Direction attachment, Direction worldDirection) {
        // 方向只有四个，线性查找比维护 6x6 的静态映射更直观，且只发生在局部连接计算中。
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
        // 两个输入始终互相垂直，叉积必然落在六个方块方向之一；null 表示调用约束被破坏。
        return Objects.requireNonNull(Direction.getNearest(x, y, z, null));
    }

    /**
     * 将附着面局部坐标中的轴对齐盒转换为世界方块坐标中的包围盒。
     *
     * @return 依次为最小 XYZ 和最大 XYZ 的六元素数组
     */
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
        // 变换后的轴可能交换或反向，因此遍历八个角点重新求 min/max，不能只转换两个对角点。
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

    /** 将附着面局部方向转换为世界方向。 */
    public static Direction transformDirection(
        Direction attachment, Direction tangent, Direction localDirection
    ) {
        Direction outward = attachment.getOpposite();
        Direction right = cross(tangent, outward);
        // 与 transformBox 使用完全相同的局部基，确保模型面、剔除方向和碰撞盒不会互相错位。
        int x = right.getStepX() * localDirection.getStepX()
            + outward.getStepX() * localDirection.getStepY()
            - tangent.getStepX() * localDirection.getStepZ();
        int y = right.getStepY() * localDirection.getStepX()
            + outward.getStepY() * localDirection.getStepY()
            - tangent.getStepY() * localDirection.getStepZ();
        int z = right.getStepZ() * localDirection.getStepX()
            + outward.getStepZ() * localDirection.getStepY()
            - tangent.getStepZ() * localDirection.getStepZ();
        return Objects.requireNonNull(Direction.getNearest(x, y, z, null));
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

    /** 导线某一局部方向的连接外观与连通状态。 */
    public enum ConnectionType implements StringRepresentable {
        /** 没有内部导线或外部红石接口。 */
        NONE("none", false),
        /** 沿当前附着面延伸。 */
        SIDE("side", true),
        /** 沿前方方块的完整碰撞面向上爬升。 */
        UP("up", true),
        /** 绕同一支撑方块的边缘连接到另一个附着面。 */
        CORNER("corner", true),
        /** 向上的开放断口连接支撑方块顶面的原版红石粉。 */
        CORNER_SP("corner_sp", true);

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

    /** 一端连接到的导线位置，以及当前端应采用的显示形态。 */
    record Connection(long[] positions, ConnectionType side) {
    }
}
