package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.AnvilCraft;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * 按维度维护无衰减红石导线的连通网络、连接拓扑和信号缓存。
 *
 * <p>每个 {@link Network} 代表当前已加载区块中的一个连通分量。Manager 只在拓扑改变时重新搜索导线，
 * 普通信号变化则复用缓存的开放端点采样最大输入，再把同一强度写回整张网络。</p>
 *
 * <p>这些数据都是可由世界状态重建的运行时缓存，不写入存档。</p>
 */
public final class RedstoneWireNetworkManager {
    /** 单张网络允许缓存的最大导线数，防止恶意或异常拓扑耗尽服务器时间和内存。 */
    private static final int MAX_NETWORK_SIZE = 65536;
    /** 单次调用允许处理的最大级联轮数，剩余脏数据留到 tick 继续收敛。 */
    private static final int MAX_SETTLING_PASSES = 16;
    /** 超大网络警告的最小间隔，单位为游戏刻。 */
    private static final long OVERFLOW_WARNING_INTERVAL = 1200;
    /** ServerLevel 没有稳定值语义，按对象身份隔离不同维度实例的运行时缓存。 */
    private static final Map<ServerLevel, LevelNetworks> LEVELS = new IdentityHashMap<>();
    /** 标记当前线程正在采样外部输入，使导线在采样期间不返回自己上一轮的输出。 */
    private static final ThreadLocal<Boolean> SUPPRESS_SIGNAL = ThreadLocal.withInitial(() -> false);

    private RedstoneWireNetworkManager() {
    }

    /** 标记某个位置附近的导线连接关系已经变化。 */
    public static void topologyChanged(Level level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            // 客户端只消费同步后的方块状态，拓扑的权威计算必须集中在服务端。
            state(serverLevel).requestTopologyUpdate(pos.asLong());
        }
    }

    /** 根据邻居变化的类型，为该位置安排拓扑重建或信号重算。 */
    public static void neighborChanged(Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        LevelNetworks state = state(serverLevel);
        if (state.applyingTopology && neighborBlock instanceof RedstoneWireBlock) {
            // connectionState 写回外观会触发导线间邻居通知；此时拓扑已由当前重建过程掌握，继续响应会递归重建。
            return;
        }
        long packedPos = pos.asLong();
        Network network = state.byWire.get(packedPos);
        if (network == null || !network.valid) {
            // 新加载、刚放置或已经失效的位置没有可信缓存，只能从该点重新发现连通分量。
            state.requestTopologyUpdate(packedPos);
            return;
        }
        Node node = network.nodes.get(packedPos);
        BlockState blockState = level.getBlockState(pos);
        if (node == null || !(blockState.getBlock() instanceof RedstoneWireBlock block)) {
            state.requestTopologyUpdate(packedPos);
            return;
        }

        if (block.connectionState(level, pos, blockState, node.connections) != blockState) {
            // 方块外观与缓存连接不一致，说明期间发生了未被捕获的几何变化。
            state.requestTopologyUpdate(packedPos);
            return;
        }
        if (!mayAffectWireTopology(pos, neighborPos, blockState, node.connections)) {
            if (!network.overflow) {
                // 与任何内部连接无关的邻居只能改变端点输入，复用拓扑直接重算信号即可。
                state.requestSignalUpdate(network);
            }
            return;
        }

        RedstoneWireBlock.Connection[] current = RedstoneWireBlock.findConnections(level, pos, blockState);
        if (!connectionsEqual(node.connections, current)) {
            state.requestTopologyUpdate(packedPos);
        } else if (!network.overflow) {
            // 邻居位于潜在连接方向，但实际连接数组没有变化，最终仍只需刷新输入值。
            state.requestSignalUpdate(network);
        }
    }

    /** 在服务端 tick 末尾继续处理上一轮因收敛上限而留下的更新。 */
    public static void tick() {
        for (LevelNetworks state : LEVELS.values()) {
            state.tick();
        }
    }

    /** 世界卸载时释放该维度的全部派生缓存。 */
    public static void clear(ServerLevel level) {
        LEVELS.remove(level);
    }

    /** 扫描新加载区块中的导线，并将它们作为待建网种子。 */
    public static void chunkLoaded(ServerLevel level, ChunkAccess chunk) {
        LevelNetworks state = state(level);
        // 只扫描区块自己的方块，避免加载事件为了找网络而同步加载相邻区块。
        chunk.findBlocks(
            blockState -> blockState.getBlock() instanceof RedstoneWireBlock,
            (pos, blockState) -> state.topologySeeds.add(pos.asLong())
        );
    }

    /** 区块卸载时移除其中节点，并重建仍处于加载状态的网络部分。 */
    public static void chunkUnloaded(ServerLevel level, ChunkPos chunkPos) {
        LevelNetworks state = LEVELS.get(level);
        if (state != null) {
            state.chunkUnloaded(chunkPos.toLong());
        }
    }

    /** 返回当前线程是否正在屏蔽自定义导线的信号输出。 */
    public static boolean isSuppressingSignal() {
        return SUPPRESS_SIGNAL.get();
    }

    /** 获取已缓存的四向连接；客户端、溢出网络或尚未建网时返回 {@code null}。 */
    @Nullable
    static RedstoneWireBlock.Connection[] getConnections(BlockGetter level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        LevelNetworks state = LEVELS.get(serverLevel);
        if (state == null) {
            return null;
        }
        Network network = state.byWire.get(pos.asLong());
        // 溢出网络的节点集合不完整，使用其连接缓存会把截断边界误判为开放端点。
        Node node = network == null || !network.valid || network.overflow ? null : network.nodes.get(pos.asLong());
        return node == null ? null : node.connections;
    }

    /**
     * 返回网络可以安全输出给原版红石粉的信号强度。
     *
     * <p>服务端使用网络缓存；客户端没有该缓存时使用同步到方块状态中的强度作为显示期回退值。</p>
     */
    static int getNonDustPower(BlockGetter level, BlockPos pos, int clientFallback) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return clientFallback;
        }
        LevelNetworks state = LEVELS.get(serverLevel);
        if (state == null) {
            return 0;
        }
        Network network = state.byWire.get(pos.asLong());
        return network == null || !network.valid || network.overflow ? 0 : network.nonDustPower;
    }

    /** 为服务端提示查询返回非红石粉输入强度，必要时同步建立当前位置的网络。 */
    public static int getNonDustPower(ServerLevel level, BlockPos pos) {
        LevelNetworks state = state(level);
        long packedPos = pos.asLong();
        Network network = state.byWire.get(packedPos);
        if (network == null && level.getBlockState(pos).getBlock() instanceof RedstoneWireBlock) {
            // 提示请求可能早于 tick 中的区块种子处理，按需建网可避免首帧错误显示为零。
            state.rebuildFromSeeds(new LongOpenHashSet(new long[]{packedPos}));
            network = state.byWire.get(packedPos);
            if (network != null && !network.overflow) {
                state.recompute(network);
            }
        }
        return network == null || !network.valid || network.overflow ? 0 : network.nonDustPower;
    }

    private static LevelNetworks state(ServerLevel level) {
        // 网络没有需要持久化的数据，首次真正访问该维度时再惰性创建即可。
        return LEVELS.computeIfAbsent(level, LevelNetworks::new);
    }

    /** 比较两组连接是否指向相同节点并采用相同显示形态。 */
    private static boolean connectionsEqual(
        RedstoneWireBlock.Connection[] first, RedstoneWireBlock.Connection[] second
    ) {
        for (int index = 0; index < first.length; index++) {
            RedstoneWireBlock.Connection a = first[index];
            RedstoneWireBlock.Connection b = second[index];
            if (a == b) {
                continue;
            }
            // 连接搜索使用固定方向枚举顺序，positions 数组顺序稳定，可以直接逐项比较而无需构造集合。
            if (a == null || b == null || a.side() != b.side() || !Arrays.equals(a.positions(), b.positions())) {
                return false;
            }
        }
        return true;
    }

    /** 快速判断一次邻居通知是否可能改变内部导线连接。 */
    private static boolean mayAffectWireTopology(
        BlockPos pos,
        BlockPos neighborPos,
        BlockState state,
        RedstoneWireBlock.Connection[] connections
    ) {
        int dx = neighborPos.getX() - pos.getX();
        int dy = neighborPos.getY() - pos.getY();
        int dz = neighborPos.getZ() - pos.getZ();
        if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) != 1) {
            // 非直接邻居通知不满足常规假设，保守地允许完整连接复查。
            return true;
        }
        Direction neighborDirection = Direction.fromDelta(dx, dy, dz);
        if (neighborDirection == null) {
            return true;
        }
        Direction attachment = state.getValue(RedstoneWireBlock.ATTACHMENT);
        if (neighborDirection.getAxis() == attachment.getAxis()) {
            // 支撑面和外侧空间会影响存活或爬升条件，即使它们不属于四个切向端点。
            return true;
        }
        int index = RedstoneWireBlock.getLocalIndex(attachment, neighborDirection);
        // 已有内部连接的方向可能因邻居变化断开；开放且无连接的其他方向只可能改变外部输入。
        return index < 0 || connections[index] != null;
    }

    /** 单个服务端维度中的全部导线网络状态。 */
    private static final class LevelNetworks {
        private final ServerLevel level;
        /** 从导线位置直接定位所属网络，避免每次信号查询重新遍历连通分量。 */
        private final Long2ObjectOpenHashMap<Network> byWire = new Long2ObjectOpenHashMap<>();
        /** 反向记录每个区块涉及的网络，用于区块卸载时精确失效。 */
        private final Long2ObjectOpenHashMap<ObjectOpenHashSet<Network>> byChunk = new Long2ObjectOpenHashMap<>();
        /** 需要重新发现拓扑的位置种子。 */
        private final LongOpenHashSet topologySeeds = new LongOpenHashSet();
        /** 拓扑仍有效、只需重新采样输入的网络。 */
        private final ObjectOpenHashSet<Network> dirtySignals = new ObjectOpenHashSet<>();
        /** 防止写回连接外观产生的导线邻居通知再次触发拓扑重建。 */
        private boolean applyingTopology;
        /** 防止邻居通知重入更新循环；重入请求只加入集合，由外层循环合并处理。 */
        private boolean processingUpdates;
        private long lastOverflowWarning = Long.MIN_VALUE;

        private LevelNetworks(ServerLevel level) {
            this.level = level;
        }

        private void requestTopologyUpdate(long pos) {
            this.topologySeeds.add(pos);
            // 通常立即求值以保持红石同步语义；处理过程中产生的重复种子会由集合去重。
            this.runUpdates();
        }

        private void requestSignalUpdate(Network network) {
            this.dirtySignals.add(network);
            // 不延迟到下一 tick，避免观察者等元件在当前邻居更新链中读到旧信号。
            this.runUpdates();
        }

        private void tick() {
            this.runUpdates();
        }

        /** 交替处理拓扑和信号脏集合，直到稳定或达到单次收敛上限。 */
        private void runUpdates() {
            if (this.processingUpdates) {
                // 当前更新引发的回调会把工作加入集合，外层 while 会在下一轮统一消费。
                return;
            }
            this.processingUpdates = true;
            try {
                int pass = 0;
                while ((!this.topologySeeds.isEmpty() || !this.dirtySignals.isEmpty())
                    && pass++ < MAX_SETTLING_PASSES) {
                    if (!this.topologySeeds.isEmpty()) {
                        // 先快照再清空，使重建期间新增的种子自然进入下一轮，而不是干扰当前迭代器。
                        LongOpenHashSet seeds = new LongOpenHashSet(this.topologySeeds);
                        this.topologySeeds.clear();
                        this.rebuildFromSeeds(seeds);
                    }
                    if (!this.dirtySignals.isEmpty()) {
                        // 拓扑优先于信号，确保后续采样使用刚刚建立的端点列表。
                        ObjectOpenHashSet<Network> dirtyNetworks = new ObjectOpenHashSet<>(this.dirtySignals);
                        this.dirtySignals.clear();
                        for (Network network : dirtyNetworks) {
                            if (network.valid && !network.overflow) {
                                this.recompute(network);
                            }
                        }
                    }
                }
            } finally {
                // 即使模组兼容代码在红石查询中抛出异常，也必须解除重入保护，避免该维度永久停止更新。
                this.processingUpdates = false;
            }
        }

        /** 使种子涉及的旧网络失效，并从所有仍加载的旧节点重新构建连通分量。 */
        private void rebuildFromSeeds(LongOpenHashSet changedPositions) {
            ObjectOpenHashSet<Network> affected = new ObjectOpenHashSet<>();
            LongOpenHashSet rebuildSeeds = new LongOpenHashSet(changedPositions);
            for (LongIterator iterator = changedPositions.iterator(); iterator.hasNext();) {
                long packedPos = iterator.nextLong();
                Network oldNetwork = this.byWire.get(packedPos);
                if (oldNetwork != null) {
                    affected.add(oldNetwork);
                }
                BlockPos pos = BlockPos.of(packedPos);
                BlockState state = this.level.getBlockState(pos);
                if (!(state.getBlock() instanceof RedstoneWireBlock) || this.byWire.isEmpty()) {
                    continue;
                }
                // 新放置的导线可能把多个既有网络桥接起来，因此还要收集它当前连接到的所有邻居网络。
                RedstoneWireBlock.Connection[] connections = RedstoneWireBlock.findConnections(this.level, pos, state);
                for (RedstoneWireBlock.Connection connection : connections) {
                    if (connection == null) {
                        continue;
                    }
                    for (long neighbor : connection.positions()) {
                        Network neighborNetwork = this.byWire.get(neighbor);
                        if (neighborNetwork != null) {
                            affected.add(neighborNetwork);
                        }
                    }
                }
            }

            for (Network network : affected) {
                // 连通分量增删一个节点都可能发生拆分或合并，局部修补容易留下错误归属，因此整体失效后重建。
                this.invalidate(network, rebuildSeeds, Long.MIN_VALUE);
            }

            this.applyingTopology = true;
            try {
                for (LongIterator iterator = rebuildSeeds.iterator(); iterator.hasNext();) {
                    long seed = iterator.nextLong();
                    if (this.byWire.containsKey(seed)) {
                        // 先处理的种子已经遍历并覆盖同一连通分量，后续种子无需重复 BFS。
                        continue;
                    }
                    BlockPos pos = BlockPos.of(seed);
                    if (this.level.getBlockState(pos).getBlock() instanceof RedstoneWireBlock) {
                        this.buildNetwork(seed);
                    }
                }
            } finally {
                this.applyingTopology = false;
            }
        }

        /** 从一个导线种子广度优先构建完整连通分量及其四向连接缓存。 */
        private void buildNetwork(long seed) {
            Long2ObjectLinkedOpenHashMap<Node> nodes = new Long2ObjectLinkedOpenHashMap<>();
            LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
            LongOpenHashSet queued = new LongOpenHashSet();
            queue.enqueue(seed);
            queued.add(seed);
            boolean overflow = false;

            while (!queue.isEmpty()) {
                long packedPos = queue.dequeueLong();
                BlockPos pos = BlockPos.of(packedPos);
                BlockState state = this.level.getBlockState(pos);
                if (!(state.getBlock() instanceof RedstoneWireBlock)) {
                    continue;
                }
                RedstoneWireBlock.Connection[] connections = RedstoneWireBlock.findConnections(this.level, pos, state);
                nodes.put(packedPos, new Node(connections));
                if (nodes.size() >= MAX_NETWORK_SIZE) {
                    // 恰好等于上限且没有更多邻居仍是完整网络；只有真正被截断时才标记 overflow。
                    overflow = !queue.isEmpty() || hasUnqueuedConnection(connections, queued);
                    if (overflow) {
                        break;
                    }
                }
                for (RedstoneWireBlock.Connection connection : connections) {
                    if (connection == null) {
                        continue;
                    }
                    for (long neighbor : connection.positions()) {
                        if (queued.add(neighbor)) {
                            // 入队时去重而不是出队时去重，限制环形网络中的队列长度和临时内存。
                            queue.enqueue(neighbor);
                        }
                    }
                }
            }

            int currentPower = this.level.getBlockState(BlockPos.of(seed)).getValue(RedstoneWireBlock.POWER);
            Network network = new Network(nodes, overflow, currentPower);
            for (LongIterator iterator = nodes.keySet().iterator(); iterator.hasNext();) {
                long nodePos = iterator.nextLong();
                this.byWire.put(nodePos, network);
                long chunkPos = ChunkPos.asLong(BlockPos.getX(nodePos) >> 4, BlockPos.getZ(nodePos) >> 4);
                if (network.chunks.add(chunkPos)) {
                    // 一个网络在同一区块可能有大量节点，反向索引只登记一次网络对象。
                    this.byChunk.computeIfAbsent(chunkPos, ignored -> new ObjectOpenHashSet<>()).add(network);
                }
            }
            if (overflow) {
                // 节点集合不完整时无法可靠识别端点或统一信号，保留旧方块状态比输出错误信号更安全。
                this.warnOverflow(seed);
                return;
            }

            LongOpenHashSet topologyChanged = new LongOpenHashSet();
            // BFS 完成后统一写回外观，避免搜索过程被自己尚未完成的状态修改影响。
            for (Long2ObjectMap.Entry<Node> entry : nodes.long2ObjectEntrySet()) {
                BlockPos pos = BlockPos.of(entry.getLongKey());
                BlockState state = this.level.getBlockState(pos);
                RedstoneWireBlock block = (RedstoneWireBlock) state.getBlock();
                BlockState connected = block.connectionState(this.level, pos, state, entry.getValue().connections);
                if (connected != state) {
                    this.level.setBlock(pos, connected, Block.UPDATE_CLIENTS);
                    topologyChanged.add(entry.getLongKey());
                    state = connected;
                }
                Direction attachment = state.getValue(RedstoneWireBlock.ATTACHMENT);
                for (int index = 0; index < 4; index++) {
                    if (state.getValue(RedstoneWireBlock.CONNECTION_PROPERTIES.get(index)).isConnected()
                        && entry.getValue().connections[index] == null) {
                        // 有可见连接形状但没有内部导线的位置才是对外采样和输出的电气端子。
                        network.terminalWires.add(entry.getLongKey());
                        Direction terminalDirection = RedstoneWireBlock.getLocalDirection(attachment, index);
                        network.terminalDirections.add((byte) terminalDirection.ordinal());
                    }
                }
            }
            // 先完成整网状态写回，再通知外部方块，避免它们观察到半更新的连接外观。
            this.notifyTopologyChanges(topologyChanged);
            this.dirtySignals.add(network);
        }

        /** 从全部开放端点采样输入，并把最大强度同步到整个无衰减网络。 */
        private void recompute(Network network) {
            if (!network.valid || network.overflow) {
                return;
            }
            int totalPower = 0;
            int nonDustPower = 0;
            SUPPRESS_SIGNAL.set(true);
            try {
                // 只遍历预计算的端点，不扫描内部节点；长导线的信号采样成本由端子数而非总长度决定。
                for (int index = 0; index < network.terminalWires.size(); index++) {
                    BlockPos wirePos = BlockPos.of(network.terminalWires.getLong(index));
                    Direction tangent = Direction.values()[network.terminalDirections.getByte(index)];
                    BlockState wireState = this.level.getBlockState(wirePos);
                    BlockPos inputPos = wireState.getBlock() instanceof RedstoneWireBlock
                        ? RedstoneWireBlock.terminalTarget(this.level, wirePos, wireState, tangent)
                        : wirePos.relative(tangent);
                    BlockState inputState = this.level.getBlockState(inputPos);
                    int inputPower = this.level.getSignal(inputPos, tangent);
                    totalPower = Math.max(totalPower, inputPower);
                    if (!inputState.is(Blocks.REDSTONE_WIRE)) {
                        // 单独保留非粉线输入，向原版红石粉输出时用它切断粉线经无衰减网络反馈给自身的回路。
                        nonDustPower = Math.max(nonDustPower, inputPower);
                    }
                }
            } finally {
                // ThreadLocal 必须在所有异常路径复位，否则后续红石查询会把整条导线永久视为无信号源。
                SUPPRESS_SIGNAL.set(false);
            }

            boolean totalChanged = network.totalPower != totalPower;
            boolean nonDustChanged = network.nonDustPower != nonDustPower;
            boolean updateTerminals = totalChanged || nonDustChanged || network.needsTerminalUpdate;
            if (!updateTerminals && !network.needsPowerNormalization) {
                // 输入和拓扑均未变化时不写 BlockState，也不制造多余的邻居更新。
                return;
            }
            network.totalPower = totalPower;
            network.nonDustPower = nonDustPower;
            if (totalChanged || network.needsPowerNormalization) {
                // POWER 保存在每个方块状态中供客户端着色和常规红石查询使用，所以变更时必须整网归一化。
                for (LongIterator iterator = network.nodes.keySet().iterator(); iterator.hasNext();) {
                    BlockPos pos = BlockPos.of(iterator.nextLong());
                    BlockState state = this.level.getBlockState(pos);
                    if (state.getBlock() instanceof RedstoneWireBlock
                        && state.getValue(RedstoneWireBlock.POWER) != totalPower) {
                        this.level.setBlock(
                            pos, state.setValue(RedstoneWireBlock.POWER, totalPower), Block.UPDATE_CLIENTS
                        );
                    }
                }
                network.needsPowerNormalization = false;
            }
            network.needsTerminalUpdate = false;
            if (updateTerminals) {
                // 等所有节点都具有新 POWER 后再通知端点，外部元件不会读到同一网络中的混合强度。
                this.notifyTerminalChanges(network);
            }
        }

        /** 合并指向同一外部方块的端点通知，并避开通知来源所在的面。 */
        private void notifyTerminalChanges(Network network) {
            Long2ByteOpenHashMap excludedFaces = new Long2ByteOpenHashMap();
            Long2LongOpenHashMap sources = new Long2LongOpenHashMap();
            for (int index = 0; index < network.terminalWires.size(); index++) {
                long source = network.terminalWires.getLong(index);
                BlockPos sourcePos = BlockPos.of(source);
                Direction tangent = Direction.values()[network.terminalDirections.getByte(index)];
                BlockState sourceState = this.level.getBlockState(sourcePos);
                BlockPos targetPos = sourceState.getBlock() instanceof RedstoneWireBlock
                    ? RedstoneWireBlock.terminalTarget(this.level, sourcePos, sourceState, tangent)
                    : sourcePos.relative(tangent);
                long target = targetPos.asLong();
                byte mask = (byte) (excludedFaces.get(target) | 1 << tangent.getOpposite().ordinal());
                excludedFaces.put(target, mask);
                // 多个端点指向同一方块时只需任选一个真实导线作为邻居变化来源。
                sources.putIfAbsent(target, source);
            }
            for (Long2ByteOpenHashMap.Entry entry : excludedFaces.long2ByteEntrySet()) {
                BlockPos target = BlockPos.of(entry.getLongKey());
                BlockPos source = BlockPos.of(sources.get(entry.getLongKey()));
                Block sourceBlock = this.level.getBlockState(source).getBlock();
                this.level.neighborChanged(target, sourceBlock, source);
                int mask = Byte.toUnsignedInt(entry.getByteValue());
                if (Integer.bitCount(mask) == 1) {
                    // 单端接入时排除回指导线的一面，避免立即把通知反射回刚完成求值的网络。
                    this.level.updateNeighborsAtExceptFromFacing(
                        target, sourceBlock, Direction.values()[Integer.numberOfTrailingZeros(mask)]
                    );
                } else {
                    // 多面同时接入无法用一次 except 调用排除所有来源，退回完整邻居通知以保证兼容性。
                    this.level.updateNeighborsAt(target, sourceBlock);
                }
            }
        }

        /** 通知连接外观发生变化的导线周围方块重新检查邻居。 */
        private void notifyTopologyChanges(LongOpenHashSet changed) {
            for (LongIterator iterator = changed.iterator(); iterator.hasNext();) {
                BlockPos pos = BlockPos.of(iterator.nextLong());
                this.level.updateNeighborsAt(pos, this.level.getBlockState(pos).getBlock());
            }
        }

        /** 以限频日志报告因规模上限而跳过求值的网络。 */
        private void warnOverflow(long seed) {
            long gameTime = this.level.getGameTime();
            if (this.lastOverflowWarning != Long.MIN_VALUE
                && gameTime - this.lastOverflowWarning < OVERFLOW_WARNING_INTERVAL) {
                // 同一维度的大型网络可能连续触发重建，限频可避免日志本身进一步放大性能问题。
                return;
            }
            this.lastOverflowWarning = gameTime;
            AnvilCraft.LOGGER.warn(
                "Redstone wire network at {} exceeds {} nodes; update was skipped to preserve its previous state",
                BlockPos.of(seed),
                MAX_NETWORK_SIZE
            );
        }

        /** 从索引中移除卸载区块涉及的网络，并安排剩余已加载节点重建。 */
        private void chunkUnloaded(long chunkPos) {
            // 卸载区块中的待处理位置已经不可访问，不能保留为下一 tick 的建网种子。
            removeChunkPositions(this.topologySeeds, chunkPos);
            ObjectOpenHashSet<Network> affected = this.byChunk.remove(chunkPos);
            if (affected == null) {
                return;
            }
            LongOpenHashSet rebuildSeeds = new LongOpenHashSet();
            for (Network network : affected) {
                // 跨区块网络必须整体失效；否则剩余节点仍会引用已经卸载的端点与连接。
                this.invalidate(network, rebuildSeeds, chunkPos);
            }
            this.topologySeeds.addAll(rebuildSeeds);
        }

        /** 删除位置集合中属于指定区块的所有元素。 */
        private static void removeChunkPositions(LongOpenHashSet positions, long chunkPos) {
            for (LongIterator iterator = positions.iterator(); iterator.hasNext();) {
                long pos = iterator.nextLong();
                if (ChunkPos.asLong(BlockPos.getX(pos) >> 4, BlockPos.getZ(pos) >> 4) == chunkPos) {
                    iterator.remove();
                }
            }
        }

        /** 从所有正向和反向索引删除网络，并收集可用于重建的仍加载节点。 */
        private void invalidate(Network network, LongOpenHashSet rebuildSeeds, long excludedChunk) {
            if (!network.valid) {
                return;
            }
            network.valid = false;
            // 先从区块反向索引移除，防止后续区块卸载再次处理同一个失效网络。
            for (LongIterator iterator = network.chunks.iterator(); iterator.hasNext();) {
                long chunkPos = iterator.nextLong();
                ObjectOpenHashSet<Network> networks = this.byChunk.get(chunkPos);
                if (networks != null) {
                    networks.remove(network);
                    if (networks.isEmpty()) {
                        this.byChunk.remove(chunkPos);
                    }
                }
            }
            for (LongIterator iterator = network.nodes.keySet().iterator(); iterator.hasNext();) {
                long nodePos = iterator.nextLong();
                if (this.byWire.get(nodePos) == network) {
                    // 身份检查避免误删已经被其他重建过程重新归属的新网络映射。
                    this.byWire.remove(nodePos);
                }
                long nodeChunk = ChunkPos.asLong(BlockPos.getX(nodePos) >> 4, BlockPos.getZ(nodePos) >> 4);
                if (nodeChunk != excludedChunk) {
                    // 排除正在卸载的区块，只从仍可访问的旧节点恢复剩余连通分量。
                    rebuildSeeds.add(nodePos);
                }
            }
        }

        /** 判断达到节点上限时是否仍存在尚未入队的连接，用于区分完整网络和截断网络。 */
        private static boolean hasUnqueuedConnection(
            RedstoneWireBlock.Connection[] connections, LongOpenHashSet queued
        ) {
            for (RedstoneWireBlock.Connection connection : connections) {
                if (connection == null) {
                    continue;
                }
                for (long neighbor : connection.positions()) {
                    if (!queued.contains(neighbor)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /** 一张已加载导线连通分量的拓扑、端点、区块归属和信号缓存。 */
    private static final class Network {
        private final Long2ObjectLinkedOpenHashMap<Node> nodes;
        private final boolean overflow;
        /** 开放端点所在的导线位置，与 terminalDirections 按索引对应。 */
        private final LongArrayList terminalWires = new LongArrayList();
        /** 从端点导线指向外部输入/输出方块的世界方向。 */
        private final ByteArrayList terminalDirections = new ByteArrayList();
        /** 网络跨越的已加载区块，用于快速响应卸载。 */
        private final LongOpenHashSet chunks = new LongOpenHashSet();
        private boolean valid = true;
        /** 新网络继承种子 POWER，仅用于过渡，首次求值必须统一所有节点。 */
        private boolean needsPowerNormalization = true;
        // 重建不会继承旧网络的 nonDustPower，因此首次采样即使总 POWER 相同也必须通知全部端点。
        private boolean needsTerminalUpdate = true;
        /** 所有外部输入（包括原版红石粉）的最大强度。 */
        private int totalPower;
        /** 排除原版红石粉输入后的最大强度，专供向粉线输出时防反馈。 */
        private int nonDustPower;

        private Network(Long2ObjectLinkedOpenHashMap<Node> nodes, boolean overflow, int totalPower) {
            this.nodes = nodes;
            this.overflow = overflow;
            this.totalPower = totalPower;
        }
    }

    /** 单根导线缓存的四向内部连接。 */
    private record Node(RedstoneWireBlock.Connection[] connections) {
    }
}
