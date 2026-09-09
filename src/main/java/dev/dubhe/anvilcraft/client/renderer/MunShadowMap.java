package dev.dubhe.anvilcraft.client.renderer;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/** 按高度位图保存完整遮光方块，保留悬空结构下面的空气。 */
public final class MunShadowMap implements AutoCloseable {
    private static final int SIZE = 128;
    private static final int CHUNKS_PER_SIDE = SIZE / 16;
    private static final int MAX_CHUNK_BUILDS = 4;
    private static final long BUILD_BUDGET_NS = 2_000_000;
    private final Map<Long, ChunkData> chunks = new HashMap<>();
    private final Set<Long> invalidated = new HashSet<>();
    private List<ChunkPos> updateOrder = List.of();
    private @Nullable ClientLevel level;
    private @Nullable DynamicTexture texture;
    private int originX = Integer.MIN_VALUE;
    private int originY;
    private int originZ = Integer.MIN_VALUE;
    private int bands;

    public void prepare(ClientLevel level, Vec3 camera) {
        if (this.level != level) {
            this.close();
            this.level = level;
            this.originY = level.getMinBuildHeight();
            this.bands = (level.getHeight() + 31) / 32;
            this.texture = new DynamicTexture(SIZE, SIZE * this.bands + 1, true);
        }
        if (this.texture == null) return;
        NativeImage pixels = this.texture.getPixels();
        if (pixels == null) return;
        int originX = (((int) Math.floor(camera.x)) & ~15) - SIZE / 2;
        int originZ = (((int) Math.floor(camera.z)) & ~15) - SIZE / 2;
        boolean moved = originX != this.originX || originZ != this.originZ;
        if (moved) {
            this.moveWindow(originX, originZ);
            pixels.fillRect(0, 0, SIZE, 1, 0);
        }
        boolean changed = moved;
        List<ChunkPos> order = this.updateOrder;
        if (!this.invalidated.isEmpty()) {
            order = new ArrayList<>(order);
            order.sort(Comparator.comparingInt(pos -> this.invalidated.contains(pos.toLong()) ? 0 : 1));
        }
        int builds = 0;
        long started = System.nanoTime();
        for (ChunkPos pos : order) {
            long key = pos.toLong();
            LevelChunk chunk = level.getChunkSource().getChunk(pos.x, pos.z, ChunkStatus.FULL, false);
            ChunkData data = this.chunks.get(key);
            if (this.invalidated.contains(key) || data != null && data.chunk != chunk) {
                this.chunks.remove(key);
                data = null;
                pixels.setPixelRGBA(this.tileIndex(pos), 0, 0);
                changed = true;
            }
            if (chunk != null && data == null && builds < MAX_CHUNK_BUILDS
                && (builds == 0 || System.nanoTime() - started < BUILD_BUDGET_NS)) {
                data = new ChunkData(chunk, this.readChunk(level, chunk));
                this.chunks.put(key, data);
                builds++;
            }
            if (data != null || chunk == null) this.invalidated.remove(key);
            if (data != null && (moved || data.dirty)) {
                this.publish(pixels, pos, data.columns);
                data.dirty = false;
                changed = true;
            }
        }
        if (changed) this.texture.upload();
    }

    private void moveWindow(int x, int z) {
        this.originX = x;
        this.originZ = z;
        this.chunks.values().removeIf(data -> !this.contains(data.chunk.getPos()));
        this.invalidated.removeIf(key -> !this.contains(new ChunkPos(key)));
        List<ChunkPos> order = new ArrayList<>(CHUNKS_PER_SIDE * CHUNKS_PER_SIDE);
        for (int dz = 0; dz < CHUNKS_PER_SIDE; dz++) {
            for (int dx = 0; dx < CHUNKS_PER_SIDE; dx++) order.add(new ChunkPos((x >> 4) + dx, (z >> 4) + dz));
        }
        order.sort(Comparator.comparingInt(pos -> {
            int dx = pos.x - (x >> 4) - CHUNKS_PER_SIDE / 2;
            int dz = pos.z - (z >> 4) - CHUNKS_PER_SIDE / 2;
            return dx * dx + dz * dz;
        }));
        this.updateOrder = order;
    }

    private int[] readChunk(ClientLevel level, LevelChunk chunk) {
        int[] columns = new int[this.bands * 256];
        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();
        LevelChunkSection[] sections = chunk.getSections();
        for (int index = 0; index < sections.length; index++) {
            LevelChunkSection section = sections[index];
            if (section.hasOnlyAir()) continue;
            if (!section.getStates().maybeHas(state -> state.getBlock().hasDynamicShape() || !state.isSolidRender(level, BlockPos.ZERO))) {
                int mask = 0xFFFF << ((index & 1) * 16);
                for (int column = 0; column < 256; column++) columns[(index >> 1) * 256 + column] |= mask;
                continue;
            }
            for (int y = 0; y < 16; y++) {
                int height = index * 16 + y;
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState state = section.getBlockState(x, y, z);
                        position.set(chunk.getPos().getMinBlockX() + x, this.originY + height, chunk.getPos().getMinBlockZ() + z);
                        if (state.isSolidRender(level, position)) columns[(height >> 5) * 256 + z * 16 + x] |= 1 << (height & 31);
                    }
                }
            }
        }
        return columns;
    }

    public void blockChanged(ClientLevel level, BlockPos pos, BlockState state) {
        if (this.level != level) return;
        ChunkData data = this.chunks.get(ChunkPos.asLong(pos));
        if (data == null) {
            this.chunkChanged(new ChunkPos(pos));
            return;
        }
        int y = pos.getY() - this.originY;
        if (y < 0 || y >= this.bands * 32) return;
        int column = (y >> 5) * 256 + (pos.getZ() & 15) * 16 + (pos.getX() & 15);
        int mask = 1 << (y & 31);
        if (state.isSolidRender(level, pos)) {
            data.columns[column] |= mask;
        } else {
            data.columns[column] &= ~mask;
        }
        data.dirty = true;
    }

    public void chunkChanged(ChunkPos pos) {
        if (this.level != null && this.contains(pos)) this.invalidated.add(pos.toLong());
    }

    private void publish(NativeImage pixels, ChunkPos pos, int[] columns) {
        int offsetX = pos.getMinBlockX() - this.originX;
        int offsetZ = pos.getMinBlockZ() - this.originZ;
        for (int band = 0; band < this.bands; band++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    pixels.setPixelRGBA(offsetX + x, 1 + band * SIZE + offsetZ + z, columns[band * 256 + z * 16 + x]);
                }
            }
        }
        pixels.setPixelRGBA(this.tileIndex(pos), 0, 1);
    }

    private int tileIndex(ChunkPos pos) {
        return pos.x - (this.originX >> 4) + (pos.z - (this.originZ >> 4)) * CHUNKS_PER_SIDE;
    }

    private boolean contains(ChunkPos pos) {
        int x = pos.x - (this.originX >> 4);
        int z = pos.z - (this.originZ >> 4);
        return x >= 0 && x < CHUNKS_PER_SIDE && z >= 0 && z < CHUNKS_PER_SIDE;
    }

    public Vec3 relativePosition(Vec3 position) {
        return position.add(-this.originX, -this.originY, -this.originZ);
    }

    public int textureId() {
        return this.texture == null ? 0 : this.texture.getId();
    }

    @Override
    public void close() {
        if (this.texture != null) this.texture.close();
        this.texture = null;
        this.level = null;
        this.originX = Integer.MIN_VALUE;
        this.originZ = Integer.MIN_VALUE;
        this.chunks.clear();
        this.invalidated.clear();
        this.updateOrder = List.of();
    }

    private static final class ChunkData {
        private final LevelChunk chunk;
        private final int[] columns;
        private boolean dirty = true;

        private ChunkData(LevelChunk chunk, int[] columns) {
            this.chunk = chunk;
            this.columns = columns;
        }
    }
}
