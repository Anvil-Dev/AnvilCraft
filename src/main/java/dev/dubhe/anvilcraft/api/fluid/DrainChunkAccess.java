package dev.dubhe.anvilcraft.api.fluid;

import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

/** 排水口按访问位置加载有限区块，超出抽取距离的位置作为流体边界。 */
public final class DrainChunkAccess {
    private static final int MAX_DISTANCE = 512;
    private static final int MAX_LOADED_CHUNKS = 8;
    private static final int MAX_PENDING_CHUNKS = 2;
    private static final TicketType<BlockPos> TICKET = TicketType.create("anvilcraft:drain", BlockPos::compareTo, 100);
    private final BlockPos drainPos;
    private final long drainChunk;
    private final LongLinkedOpenHashSet tickets = new LongLinkedOpenHashSet();
    private final LongOpenHashSet pending = new LongOpenHashSet();
    private long requestTick = Long.MIN_VALUE;
    private int requests;

    public DrainChunkAccess(BlockPos drainPos) {
        this.drainPos = drainPos.immutable();
        this.drainChunk = ChunkPos.asLong(drainPos.getX() >> 4, drainPos.getZ() >> 4);
    }

    public boolean isInRange(BlockPos pos) {
        long dx = (long) pos.getX() - this.drainPos.getX();
        long dz = (long) pos.getZ() - this.drainPos.getZ();
        return dx * dx + dz * dz <= MAX_DISTANCE * MAX_DISTANCE;
    }

    public @Nullable BlockState read(ServerLevel level, BlockPos pos) {
        if (!this.isInRange(pos) || !level.isInWorldBounds(pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        long key = ChunkPos.asLong(chunkX, chunkZ);
        LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
        if (key == this.drainChunk) {
            return chunk == null ? null : chunk.getBlockState(pos);
        }
        if (this.tickets.contains(key)) {
            this.tickets.addAndMoveToLast(key);
            level.getChunkSource().addRegionTicket(TICKET, new ChunkPos(key), 0, this.drainPos);
        }
        if (chunk != null) {
            this.pending.remove(key);
            return chunk.getBlockState(pos);
        }
        if (this.pending.contains(key)) {
            return null;
        }
        if (this.requestTick != level.getGameTime()) {
            this.requestTick = level.getGameTime();
            this.requests = 0;
        }
        if (this.pending.size() >= MAX_PENDING_CHUNKS || this.requests >= MAX_PENDING_CHUNKS) {
            return null;
        }
        if (!this.tickets.contains(key) && this.tickets.size() >= MAX_LOADED_CHUNKS) {
            long oldest = this.tickets.removeFirstLong();
            this.pending.remove(oldest);
            level.getChunkSource().removeRegionTicket(TICKET, new ChunkPos(oldest), 0, this.drainPos);
        }
        this.requests++;
        this.tickets.add(key);
        this.pending.add(key);
        level.getChunkSource().addRegionTicket(TICKET, new ChunkPos(key), 0, this.drainPos);
        return null;
    }

    public void clear(ServerLevel level) {
        for (long key : this.tickets) {
            level.getChunkSource().removeRegionTicket(TICKET, new ChunkPos(key), 0, this.drainPos);
        }
        this.tickets.clear();
        this.pending.clear();
    }
}
