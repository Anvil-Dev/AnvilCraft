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

/** Maintains cached topology and coalesced signal updates for non-attenuating redstone wire networks. */
public final class RedstoneWireNetworkManager {
    private static final int MAX_NETWORK_SIZE = 65536;
    private static final int MAX_SETTLING_PASSES = 16;
    private static final long OVERFLOW_WARNING_INTERVAL = 1200;
    private static final Map<ServerLevel, LevelNetworks> LEVELS = new IdentityHashMap<>();
    private static final ThreadLocal<Boolean> SUPPRESS_SIGNAL = ThreadLocal.withInitial(() -> false);

    private RedstoneWireNetworkManager() {
    }

    public static void topologyChanged(Level level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            state(serverLevel).topologySeeds.add(pos.asLong());
        }
    }

    public static void neighborChanged(Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        LevelNetworks state = state(serverLevel);
        if (state.applyingTopology && neighborBlock instanceof RedstoneWireBlock) {
            return;
        }
        long packedPos = pos.asLong();
        Network network = state.byWire.get(packedPos);
        if (network == null || !network.valid) {
            state.topologySeeds.add(packedPos);
            return;
        }
        Node node = network.nodes.get(packedPos);
        BlockState blockState = level.getBlockState(pos);
        if (node == null || !(blockState.getBlock() instanceof RedstoneWireBlock block)) {
            state.topologySeeds.add(packedPos);
            return;
        }

        if (block.connectionState(level, pos, blockState, node.connections) != blockState) {
            state.topologySeeds.add(packedPos);
            return;
        }
        if (!mayAffectWireTopology(pos, neighborPos, blockState, node.connections)) {
            if (!network.overflow) {
                state.dirtySignals.add(network);
            }
            return;
        }

        RedstoneWireBlock.Connection[] current = RedstoneWireBlock.findConnections(level, pos, blockState);
        if (!connectionsEqual(node.connections, current)) {
            state.topologySeeds.add(packedPos);
        } else if (!network.overflow) {
            state.dirtySignals.add(network);
        }
    }

    public static void tick() {
        for (LevelNetworks state : LEVELS.values()) {
            state.tick();
        }
    }

    public static void clear(ServerLevel level) {
        LEVELS.remove(level);
    }

    public static void chunkLoaded(ServerLevel level, ChunkAccess chunk) {
        LevelNetworks state = state(level);
        chunk.findBlocks(
            blockState -> blockState.getBlock() instanceof RedstoneWireBlock,
            (pos, blockState) -> state.topologySeeds.add(pos.asLong())
        );
    }

    public static void chunkUnloaded(ServerLevel level, ChunkPos chunkPos) {
        LevelNetworks state = LEVELS.get(level);
        if (state != null) {
            state.chunkUnloaded(chunkPos.toLong());
        }
    }

    public static boolean isSuppressingSignal() {
        return SUPPRESS_SIGNAL.get();
    }

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
        Node node = network == null || !network.valid || network.overflow ? null : network.nodes.get(pos.asLong());
        return node == null ? null : node.connections;
    }

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

    public static int getNonDustPower(ServerLevel level, BlockPos pos) {
        LevelNetworks state = state(level);
        long packedPos = pos.asLong();
        Network network = state.byWire.get(packedPos);
        if (network == null && level.getBlockState(pos).getBlock() instanceof RedstoneWireBlock) {
            state.rebuildFromSeeds(new LongOpenHashSet(new long[]{packedPos}));
            network = state.byWire.get(packedPos);
            if (network != null && !network.overflow) {
                state.recompute(network);
            }
        }
        return network == null || !network.valid || network.overflow ? 0 : network.nonDustPower;
    }

    private static LevelNetworks state(ServerLevel level) {
        return LEVELS.computeIfAbsent(level, LevelNetworks::new);
    }

    private static boolean connectionsEqual(
        RedstoneWireBlock.Connection[] first, RedstoneWireBlock.Connection[] second
    ) {
        for (int index = 0; index < first.length; index++) {
            RedstoneWireBlock.Connection a = first[index];
            RedstoneWireBlock.Connection b = second[index];
            if (a == b) {
                continue;
            }
            if (a == null || b == null || a.side() != b.side() || !Arrays.equals(a.positions(), b.positions())) {
                return false;
            }
        }
        return true;
    }

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
            return true;
        }
        Direction neighborDirection = Direction.fromDelta(dx, dy, dz);
        if (neighborDirection == null) {
            return true;
        }
        Direction attachment = state.getValue(RedstoneWireBlock.ATTACHMENT);
        if (neighborDirection.getAxis() == attachment.getAxis()) {
            return true;
        }
        int index = RedstoneWireBlock.getLocalIndex(attachment, neighborDirection);
        return index < 0 || connections[index] != null;
    }

    private static final class LevelNetworks {
        private final ServerLevel level;
        private final Long2ObjectOpenHashMap<Network> byWire = new Long2ObjectOpenHashMap<>();
        private final Long2ObjectOpenHashMap<ObjectOpenHashSet<Network>> byChunk = new Long2ObjectOpenHashMap<>();
        private final LongOpenHashSet topologySeeds = new LongOpenHashSet();
        private final ObjectOpenHashSet<Network> dirtySignals = new ObjectOpenHashSet<>();
        private boolean applyingTopology;
        private long lastOverflowWarning = Long.MIN_VALUE;

        private LevelNetworks(ServerLevel level) {
            this.level = level;
        }

        private void tick() {
            int pass = 0;
            while ((!this.topologySeeds.isEmpty() || !this.dirtySignals.isEmpty()) && pass++ < MAX_SETTLING_PASSES) {
                if (!this.topologySeeds.isEmpty()) {
                    LongOpenHashSet seeds = new LongOpenHashSet(this.topologySeeds);
                    this.topologySeeds.clear();
                    this.rebuildFromSeeds(seeds);
                }
                if (!this.dirtySignals.isEmpty()) {
                    ObjectOpenHashSet<Network> dirtyNetworks = new ObjectOpenHashSet<>(this.dirtySignals);
                    this.dirtySignals.clear();
                    for (Network network : dirtyNetworks) {
                        if (network.valid && !network.overflow) {
                            this.recompute(network);
                        }
                    }
                }
            }
        }

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
                this.invalidate(network, rebuildSeeds, Long.MIN_VALUE);
            }

            this.applyingTopology = true;
            try {
                for (LongIterator iterator = rebuildSeeds.iterator(); iterator.hasNext();) {
                    long seed = iterator.nextLong();
                    if (this.byWire.containsKey(seed)) {
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
                    this.byChunk.computeIfAbsent(chunkPos, ignored -> new ObjectOpenHashSet<>()).add(network);
                }
            }
            if (overflow) {
                this.warnOverflow(seed);
                return;
            }

            LongOpenHashSet topologyChanged = new LongOpenHashSet();
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
                        network.terminalWires.add(entry.getLongKey());
                        Direction terminalDirection = RedstoneWireBlock.getLocalDirection(attachment, index);
                        network.terminalDirections.add((byte) terminalDirection.ordinal());
                    }
                }
            }
            this.notifyTopologyChanges(topologyChanged);
            this.dirtySignals.add(network);
        }

        private void recompute(Network network) {
            if (!network.valid || network.overflow) {
                return;
            }
            int totalPower = 0;
            int nonDustPower = 0;
            boolean[] dustTerminals = new boolean[network.terminalWires.size()];
            SUPPRESS_SIGNAL.set(true);
            try {
                for (int index = 0; index < network.terminalWires.size(); index++) {
                    BlockPos wirePos = BlockPos.of(network.terminalWires.getLong(index));
                    Direction tangent = Direction.values()[network.terminalDirections.getByte(index)];
                    BlockPos inputPos = wirePos.relative(tangent);
                    BlockState inputState = this.level.getBlockState(inputPos);
                    int inputPower = this.level.getSignal(inputPos, tangent);
                    totalPower = Math.max(totalPower, inputPower);
                    dustTerminals[index] = inputState.is(Blocks.REDSTONE_WIRE);
                    if (!dustTerminals[index]) {
                        nonDustPower = Math.max(nonDustPower, inputPower);
                    }
                }
            } finally {
                SUPPRESS_SIGNAL.set(false);
            }

            boolean totalChanged = network.totalPower != totalPower;
            boolean nonDustChanged = network.nonDustPower != nonDustPower;
            if (!totalChanged && !nonDustChanged && !network.needsPowerNormalization) {
                return;
            }
            network.totalPower = totalPower;
            network.nonDustPower = nonDustPower;
            if (totalChanged || network.needsPowerNormalization) {
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
            this.notifyTerminalChanges(network, dustTerminals, totalChanged, nonDustChanged);
        }

        private void notifyTerminalChanges(
            Network network, boolean[] dustTerminals, boolean totalChanged, boolean nonDustChanged
        ) {
            Long2ByteOpenHashMap excludedFaces = new Long2ByteOpenHashMap();
            Long2LongOpenHashMap sources = new Long2LongOpenHashMap();
            for (int index = 0; index < network.terminalWires.size(); index++) {
                if (dustTerminals[index] ? !nonDustChanged : !totalChanged) {
                    continue;
                }
                long source = network.terminalWires.getLong(index);
                BlockPos sourcePos = BlockPos.of(source);
                Direction tangent = Direction.values()[network.terminalDirections.getByte(index)];
                long target = sourcePos.relative(tangent).asLong();
                byte mask = (byte) (excludedFaces.get(target) | 1 << tangent.getOpposite().ordinal());
                excludedFaces.put(target, mask);
                sources.putIfAbsent(target, source);
            }
            for (Long2ByteOpenHashMap.Entry entry : excludedFaces.long2ByteEntrySet()) {
                BlockPos target = BlockPos.of(entry.getLongKey());
                BlockPos source = BlockPos.of(sources.get(entry.getLongKey()));
                Block sourceBlock = this.level.getBlockState(source).getBlock();
                this.level.neighborChanged(target, sourceBlock, source);
                int mask = Byte.toUnsignedInt(entry.getByteValue());
                if (Integer.bitCount(mask) == 1) {
                    this.level.updateNeighborsAtExceptFromFacing(
                        target, sourceBlock, Direction.values()[Integer.numberOfTrailingZeros(mask)]
                    );
                } else {
                    this.level.updateNeighborsAt(target, sourceBlock);
                }
            }
        }

        private void notifyTopologyChanges(LongOpenHashSet changed) {
            for (LongIterator iterator = changed.iterator(); iterator.hasNext();) {
                BlockPos pos = BlockPos.of(iterator.nextLong());
                this.level.updateNeighborsAt(pos, this.level.getBlockState(pos).getBlock());
            }
        }

        private void warnOverflow(long seed) {
            long gameTime = this.level.getGameTime();
            if (this.lastOverflowWarning != Long.MIN_VALUE
                && gameTime - this.lastOverflowWarning < OVERFLOW_WARNING_INTERVAL) {
                return;
            }
            this.lastOverflowWarning = gameTime;
            AnvilCraft.LOGGER.warn(
                "Redstone wire network at {} exceeds {} nodes; update was skipped to preserve its previous state",
                BlockPos.of(seed),
                MAX_NETWORK_SIZE
            );
        }

        private void chunkUnloaded(long chunkPos) {
            removeChunkPositions(this.topologySeeds, chunkPos);
            ObjectOpenHashSet<Network> affected = this.byChunk.remove(chunkPos);
            if (affected == null) {
                return;
            }
            LongOpenHashSet rebuildSeeds = new LongOpenHashSet();
            for (Network network : affected) {
                this.invalidate(network, rebuildSeeds, chunkPos);
            }
            this.topologySeeds.addAll(rebuildSeeds);
        }

        private static void removeChunkPositions(LongOpenHashSet positions, long chunkPos) {
            for (LongIterator iterator = positions.iterator(); iterator.hasNext();) {
                long pos = iterator.nextLong();
                if (ChunkPos.asLong(BlockPos.getX(pos) >> 4, BlockPos.getZ(pos) >> 4) == chunkPos) {
                    iterator.remove();
                }
            }
        }

        private void invalidate(Network network, LongOpenHashSet rebuildSeeds, long excludedChunk) {
            if (!network.valid) {
                return;
            }
            network.valid = false;
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
                    this.byWire.remove(nodePos);
                }
                long nodeChunk = ChunkPos.asLong(BlockPos.getX(nodePos) >> 4, BlockPos.getZ(nodePos) >> 4);
                if (nodeChunk != excludedChunk) {
                    rebuildSeeds.add(nodePos);
                }
            }
        }

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

    private static final class Network {
        private final Long2ObjectLinkedOpenHashMap<Node> nodes;
        private final boolean overflow;
        private final LongArrayList terminalWires = new LongArrayList();
        private final ByteArrayList terminalDirections = new ByteArrayList();
        private final LongOpenHashSet chunks = new LongOpenHashSet();
        private boolean valid = true;
        private boolean needsPowerNormalization = true;
        private int totalPower;
        private int nonDustPower;

        private Network(Long2ObjectLinkedOpenHashMap<Node> nodes, boolean overflow, int totalPower) {
            this.nodes = nodes;
            this.overflow = overflow;
            this.totalPower = totalPower;
        }
    }

    private record Node(RedstoneWireBlock.Connection[] connections) {
    }
}
