package dev.dubhe.anvilcraft.api.fluid;

import dev.dubhe.anvilcraft.init.ModTicketTypes;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

/** 排水口按读取位置移动区块加载窗口，防止搜索范围内的区块同时常驻。 */
public final class DrainChunkAccess {
    private static final int MAX_DISTANCE = 512;
    private static final int MAX_LOADED_CHUNKS = 8;
    private static final int MAX_PENDING_CHUNKS = 2;
    private static final Map<ServerLevel, Long2IntOpenHashMap> TICKET_USERS = new WeakHashMap<>();
    private static final Map<ServerLevel, Long2ObjectOpenHashMap<Revision>> OBSERVED_CHUNKS = new WeakHashMap<>();
    private final BlockPos drainPos;
    private final long drainChunk;
    private final Long2ObjectOpenHashMap<Snapshot> chunks = new Long2ObjectOpenHashMap<>();
    private final LongLinkedOpenHashSet tickets = new LongLinkedOpenHashSet();
    private final LongOpenHashSet pending = new LongOpenHashSet();
    private long requestTick = Long.MIN_VALUE;
    private int requests;

    public DrainChunkAccess(BlockPos drainPos) {
        this.drainPos = drainPos.immutable();
        this.drainChunk = ChunkPos.pack(drainPos.getX() >> 4, drainPos.getZ() >> 4);
    }

    public void tick(ServerLevel level) {
        if (this.requestTick != level.getGameTime()) {
            this.requestTick = level.getGameTime();
            this.requests = 0;
        }
        this.pending.removeIf(key -> level.getChunkSource().getChunkNow(ChunkPos.getX(key), ChunkPos.getZ(key)) != null);
        if (level.getGameTime() % 20 == 0) {
            for (long pos : this.tickets) {
                this.keepLoaded(level, pos);
            }
        }
    }

    public boolean isValid() {
        for (Snapshot snapshot : this.chunks.values()) {
            if (snapshot.revision().value != snapshot.value()) {
                return false;
            }
        }
        return true;
    }

    /** 已访问区块的版本由搜索共同持有，保证卸载重载后的流体变化也能使旧连通关系失效。 */
    public static void onFluidChanged(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        Long2ObjectOpenHashMap<Revision> observed = OBSERVED_CHUNKS.get(serverLevel);
        if (observed != null) {
            Revision revision = observed.get(ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4));
            if (revision != null) {
                revision.value++;
            }
        }
    }

    public @Nullable BlockState read(ServerLevel level, BlockPos pos) {
        long dx = (long) pos.getX() - this.drainPos.getX();
        long dz = (long) pos.getZ() - this.drainPos.getZ();
        if (!level.isInValidBounds(pos) || dx * dx + dz * dz > (long) MAX_DISTANCE * MAX_DISTANCE) {
            return Blocks.AIR.defaultBlockState();
        }
        long key = ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4);
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null) {
            if (key == this.drainChunk || this.pending.contains(key)
                || this.pending.size() >= MAX_PENDING_CHUNKS || this.requests >= MAX_PENDING_CHUNKS) {
                return null;
            }
            this.requests++;
            this.pending.add(key);
            this.acquire(level, key);
            return null;
        }
        this.pending.remove(key);
        this.acquire(level, key);
        if (!this.chunks.containsKey(key)) {
            Revision revision = OBSERVED_CHUNKS.computeIfAbsent(level, ignored -> new Long2ObjectOpenHashMap<>())
                .computeIfAbsent(key, ignored -> new Revision());
            revision.users++;
            this.chunks.put(key, new Snapshot(revision, revision.value));
        }
        return chunk.getBlockState(pos);
    }

    public boolean acceptRemoval(BlockPos pos) {
        long key = ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4);
        Snapshot snapshot = this.chunks.get(key);
        if (snapshot == null || snapshot.revision().value != snapshot.value() + 1) {
            return false;
        }
        this.chunks.put(key, new Snapshot(snapshot.revision(), snapshot.revision().value));
        return true;
    }

    public void release(ServerLevel level) {
        for (long key : this.tickets) {
            this.releaseTicket(level, key);
        }
        this.tickets.clear();
        this.pending.clear();
        Long2ObjectOpenHashMap<Revision> observed = OBSERVED_CHUNKS.get(level);
        if (observed != null) {
            for (var entry : this.chunks.long2ObjectEntrySet()) {
                if (--entry.getValue().revision().users == 0) {
                    observed.remove(entry.getLongKey());
                }
            }
            if (observed.isEmpty()) {
                OBSERVED_CHUNKS.remove(level);
            }
        }
        this.chunks.clear();
    }

    private void acquire(ServerLevel level, long key) {
        if (key == this.drainChunk || !this.tickets.addAndMoveToLast(key)) {
            return;
        }
        if (this.tickets.size() > MAX_LOADED_CHUNKS) {
            long oldest = this.tickets.removeFirstLong();
            this.pending.remove(oldest);
            this.releaseTicket(level, oldest);
        }
        TICKET_USERS.computeIfAbsent(level, ignored -> new Long2IntOpenHashMap()).addTo(key, 1);
        this.keepLoaded(level, key);
    }

    private void releaseTicket(ServerLevel level, long key) {
        Long2IntOpenHashMap users = TICKET_USERS.get(level);
        if (users != null && users.addTo(key, -1) == 1) {
            users.remove(key);
            level.getChunkSource().removeTicketWithRadius(ModTicketTypes.DRAIN.get(), ChunkPos.unpack(key), 0);
            if (users.isEmpty()) {
                TICKET_USERS.remove(level);
            }
        }
    }

    private void keepLoaded(ServerLevel level, long key) {
        // FULL 级别只允许读取目标区块，既不让自身区块保持 ticking，也不加载完整邻域。
        level.getChunkSource().addTicketWithRadius(ModTicketTypes.DRAIN.get(), ChunkPos.unpack(key), 0);
    }

    private record Snapshot(Revision revision, long value) {
    }

    private static final class Revision {
        private long value;
        private int users;
    }
}
