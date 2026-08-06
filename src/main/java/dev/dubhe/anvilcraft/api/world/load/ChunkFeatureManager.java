package dev.dubhe.anvilcraft.api.world.load;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ChunkFeatureManager {
    /// 按维度分组的区块源数据，避免不同维度同坐标区块互相影响
    private static final Map<ResourceKey<Level>, Map<ChunkPos, Map<BlockPos, LoadChunkData>>> CHUNK_SOURCES = new HashMap<>();
    public static final ThreadLocal<ChunkPos> CURRENT_SPAWNING_CHUNK = new ThreadLocal<>();
    public static final ThreadLocal<ResourceKey<Level>> CURRENT_SPAWNING_DIMENSION = new ThreadLocal<>();
    public static final int TRANSCENDIUM_DESPAWN_DISTANCE = 32768; //2048 chunks

    public static void registerChunkFeatures(ResourceKey<Level> dimension, ChunkPos pos, BlockPos sourcePos, LoadChunkData data) {
        CHUNK_SOURCES
            .computeIfAbsent(dimension, k -> new HashMap<>())
            .computeIfAbsent(pos, k -> new HashMap<>())
            .put(sourcePos, data);
    }

    public static void unregisterChunkFeatures(ResourceKey<Level> dimension, ChunkPos pos, BlockPos sourcePos) {
        Map<ChunkPos, Map<BlockPos, LoadChunkData>> dimSources = CHUNK_SOURCES.get(dimension);
        if (dimSources == null) return;

        Map<BlockPos, LoadChunkData> sources = dimSources.get(pos);
        if (sources != null) {
            sources.remove(sourcePos);
            if (sources.isEmpty()) {
                dimSources.remove(pos);
                if (dimSources.isEmpty()) {
                    CHUNK_SOURCES.remove(dimension);
                }
            }
        }
    }

    public static boolean isChunkManaged(ResourceKey<Level> dimension, ChunkPos pos) {
        Map<ChunkPos, Map<BlockPos, LoadChunkData>> dimSources = CHUNK_SOURCES.get(dimension);
        return dimSources != null && dimSources.containsKey(pos);
    }

    public static boolean shouldSkipRandomTick(ResourceKey<Level> dimension, ChunkPos pos) {
        Map<ChunkPos, Map<BlockPos, LoadChunkData>> dimSources = CHUNK_SOURCES.get(dimension);
        if (dimSources == null) return false;

        Map<BlockPos, LoadChunkData> sources = dimSources.get(pos);
        if (sources == null || sources.isEmpty()) return false; // 默认运算

        for (LoadChunkData data : sources.values()) {
            if (data.shouldSkipRandomTick(pos)) return true;
        }
        return false;
    }

    public static boolean shouldAllowFireSpread(ResourceKey<Level> dimension, ChunkPos pos) {
        Map<ChunkPos, Map<BlockPos, LoadChunkData>> dimSources = CHUNK_SOURCES.get(dimension);
        if (dimSources == null) return false;

        Map<BlockPos, LoadChunkData> sources = dimSources.get(pos);
        if (sources == null || sources.isEmpty()) return false;

        for (LoadChunkData data : sources.values()) {
            if (data.shouldAllowFireSpread(pos)) return true;
        }
        return false;
    }

    public static boolean shouldAllowNaturalSpawn(ResourceKey<Level> dimension, ChunkPos pos) {
        Map<ChunkPos, Map<BlockPos, LoadChunkData>> dimSources = CHUNK_SOURCES.get(dimension);
        if (dimSources == null) return false;

        Map<BlockPos, LoadChunkData> sources = dimSources.get(pos);
        if (sources == null || sources.isEmpty()) return false;

        for (LoadChunkData data : sources.values()) {
            if (data.shouldAllowNaturalSpawn(pos)) return true;
        }
        return false;
    }

    public static boolean shouldAllowSpawnerSpawn(ResourceKey<Level> dimension, ChunkPos pos) {
        Map<ChunkPos, Map<BlockPos, LoadChunkData>> dimSources = CHUNK_SOURCES.get(dimension);
        if (dimSources == null) return false;

        Map<BlockPos, LoadChunkData> sources = dimSources.get(pos);
        if (sources == null || sources.isEmpty()) return false;

        for (LoadChunkData data : sources.values()) {
            if (data.shouldAllowSpawnerSpawn(pos)) return true;
        }
        return false;
    }

    public static Set<ChunkPos> getAllNaturalSpawnChunks(ResourceKey<Level> dimension) {
        Set<ChunkPos> result = new HashSet<>();
        Map<ChunkPos, Map<BlockPos, LoadChunkData>> dimSources = CHUNK_SOURCES.get(dimension);
        if (dimSources == null) return result;

        for (Map.Entry<ChunkPos, Map<BlockPos, LoadChunkData>> entry : dimSources.entrySet()) {
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