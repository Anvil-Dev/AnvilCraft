package dev.dubhe.anvilcraft.api.fluid;

import dev.dubhe.anvilcraft.init.ModTicketTypes;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

/** Main-thread, non-blocking reads for a drain search. Missing chunks are requested through ordinary loading tickets. */
public final class DrainChunkAccess {
    private static final int MAX_PENDING_CHUNKS = 2;
    private final Long2ObjectOpenHashMap<Snapshot> chunks = new Long2ObjectOpenHashMap<>();
    private final LongOpenHashSet pending = new LongOpenHashSet();

    public void tick(ServerLevel level) {
        if (level.getGameTime() % 20 != 0) return;
        for (long pos : this.chunks.keySet()) this.keepLoaded(level, pos);
        for (long pos : this.pending) this.keepLoaded(level, pos);
    }

    public boolean isValid(ServerLevel level) {
        for (var entry : this.chunks.long2ObjectEntrySet()) {
            Snapshot snapshot = entry.getValue();
            long pos = entry.getLongKey();
            if (level.getChunkSource().getChunkNow(ChunkPos.getX(pos), ChunkPos.getZ(pos)) != snapshot.chunk()
                || ((FluidChangeTracker) snapshot.chunk()).anvilcraft$getFluidRevision() != snapshot.revision()) {
                return false;
            }
        }
        return true;
    }

    public @Nullable BlockState read(ServerLevel level, BlockPos pos) {
        if (!level.isInValidBounds(pos)) return Blocks.AIR.defaultBlockState();
        long key = ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4);
        Snapshot snapshot = this.chunks.get(key);
        if (snapshot != null) return snapshot.chunk().getBlockState(pos);
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null) {
            if (this.pending.contains(key) || this.pending.size() >= MAX_PENDING_CHUNKS) return null;
            this.pending.add(key);
            this.keepLoaded(level, key);
            return null;
        }
        this.pending.remove(key);
        this.keepLoaded(level, key);
        this.chunks.put(key, new Snapshot(chunk, ((FluidChangeTracker) chunk).anvilcraft$getFluidRevision()));
        return chunk.getBlockState(pos);
    }

    public boolean acceptRemoval(BlockPos pos) {
        long key = ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4);
        Snapshot snapshot = this.chunks.get(key);
        if (snapshot == null) return false;
        long revision = ((FluidChangeTracker) snapshot.chunk()).anvilcraft$getFluidRevision();
        if (revision != snapshot.revision() + 1) return false;
        this.chunks.put(key, new Snapshot(snapshot.chunk(), revision));
        return true;
    }

    private void keepLoaded(ServerLevel level, long key) {
        level.getChunkSource().addTicketWithRadius(ModTicketTypes.DRAIN.get(), new ChunkPos(ChunkPos.getX(key), ChunkPos.getZ(key)), 0);
    }

    private record Snapshot(LevelChunk chunk, long revision) {
    }
}
