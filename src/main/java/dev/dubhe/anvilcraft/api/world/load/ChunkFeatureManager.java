package dev.dubhe.anvilcraft.api.world.load;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ChunkFeatureManager {
    private static final Map<ChunkPos, Map<BlockPos, LoadChunkData>> CHUNK_SOURCES = new HashMap<>();
    public static final ThreadLocal<ChunkPos> CURRENT_SPAWNING_CHUNK = new ThreadLocal<>();
    public static final int TRANSCENDIUM_DESPAWN_DISTANCE = 32768; //2048 chunks

    public static void registerChunkFeatures(ChunkPos pos, BlockPos sourcePos, LoadChunkData data) {
        CHUNK_SOURCES.computeIfAbsent(pos, k -> new HashMap<>()).put(sourcePos, data);
    }

    public static void unregisterChunkFeatures(ChunkPos pos, BlockPos sourcePos) {
        Map<BlockPos, LoadChunkData> sources = CHUNK_SOURCES.get(pos);
        if (sources != null) {
            sources.remove(sourcePos);
            if (sources.isEmpty()) {
                CHUNK_SOURCES.remove(pos);
            }
        }
    }

    public static boolean isChunkManaged(ChunkPos pos) {
        return CHUNK_SOURCES.containsKey(pos);
    }

    public static boolean shouldSkipRandomTick(ChunkPos pos) {
        Map<BlockPos, LoadChunkData> sources = CHUNK_SOURCES.get(pos);
        if (sources == null || sources.isEmpty()) return false; // 默认运算

        for (LoadChunkData data : sources.values()) {
            if (data.shouldSkipRandomTick(pos)) return true;
        }
        return false;
    }

    public static boolean shouldAllowFireSpread(ChunkPos pos) {
        Map<BlockPos, LoadChunkData> sources = CHUNK_SOURCES.get(pos);
        if (sources == null || sources.isEmpty()) return false;

        for (LoadChunkData data : sources.values()) {
            if (data.shouldAllowFireSpread(pos)) return true;
        }
        return false;
    }

    public static boolean shouldAllowNaturalSpawn(ChunkPos pos) {
        Map<BlockPos, LoadChunkData> sources = CHUNK_SOURCES.get(pos);
        if (sources == null || sources.isEmpty()) return false;

        for (LoadChunkData data : sources.values()) {
            if (data.shouldAllowNaturalSpawn(pos)) return true;
        }
        return false;
    }

    public static boolean shouldAllowSpawnerSpawn(ChunkPos pos) {
        Map<BlockPos, LoadChunkData> sources = CHUNK_SOURCES.get(pos);
        if (sources == null || sources.isEmpty()) return false;

        for (LoadChunkData data : sources.values()) {
            if (data.shouldAllowSpawnerSpawn(pos)) return true;
        }
        return false;
    }

    public static Set<ChunkPos> getAllNaturalSpawnChunks() {
        Set<ChunkPos> result = new HashSet<>();
        for (Map.Entry<ChunkPos, Map<BlockPos, LoadChunkData>> entry : CHUNK_SOURCES.entrySet()) {
            ChunkPos chunkPos = entry.getKey();
            for (LoadChunkData data : entry.getValue().values()) {
                if (data.shouldAllowNaturalSpawn(chunkPos)) {
                    result.add(chunkPos);
                    break;
                }
            }
        }
        return result;
    }

    public static void clear() {
        CHUNK_SOURCES.clear();
    }
}