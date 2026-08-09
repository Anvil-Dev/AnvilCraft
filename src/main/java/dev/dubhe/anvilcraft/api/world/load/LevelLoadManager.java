package dev.dubhe.anvilcraft.api.world.load;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class LevelLoadManager {
    private static final Map<BlockPos, LoadChunkData> LOAD_DATA_MAP = new HashMap<>();
    private static final Deque<Runnable> deferredTasks = new ArrayDeque<>();
    private static boolean serverStarted = false;

    private static final Map<ResourceKey<Level>, Map<ChunkPos, Integer>> CHUNK_REF_COUNT = new HashMap<>();

    public static void register(BlockPos centerPos, LoadChunkData data, ServerLevel level) {
        if (LevelLoadManager.LOAD_DATA_MAP.containsKey(centerPos)) return;
        LevelLoadManager.LOAD_DATA_MAP.put(centerPos, data);
        LevelLoadManager.reload(level);
    }

    public static boolean checkRegistered(BlockPos pos) {
        return LevelLoadManager.LOAD_DATA_MAP.containsKey(pos);
    }

    public static void unregister(BlockPos centerPos, Level level) {
        LoadChunkData data = LevelLoadManager.LOAD_DATA_MAP.get(centerPos);
        if (data == null) return;
        data.markRemoved();
        if (level instanceof ServerLevel serverLevel) {
            LevelLoadManager.reload(serverLevel);
        }
    }

    static void lazy(Runnable task) {
        if (LevelLoadManager.serverStarted) {
            task.run();
        } else {
            LevelLoadManager.deferredTasks.add(task);
        }
    }

    public static void notifyServerStarted() {
        LevelLoadManager.serverStarted = true;
        while (!LevelLoadManager.deferredTasks.isEmpty()) {
            LevelLoadManager.deferredTasks.poll().run();
        }
    }

    public static void forceChunk(int chunkX, int chunkZ, boolean load, ServerLevel level) {
        ChunkPos cp = new ChunkPos(chunkX, chunkZ);
        ResourceKey<Level> dim = level.dimension();
        Map<ChunkPos, Integer> refMap = LevelLoadManager.CHUNK_REF_COUNT.computeIfAbsent(dim, k -> new HashMap<>());
        int count = refMap.getOrDefault(cp, 0);

        if (load) {
            refMap.put(cp, count + 1);
            if (count == 0) level.setChunkForced(chunkX, chunkZ, true);
        } else {
            if (count <= 1) {
                refMap.remove(cp);
                level.setChunkForced(chunkX, chunkZ, false);
            } else {
                refMap.put(cp, count - 1);
            }
        }
    }

    public static void reload(ServerLevel level) {
        LevelLoadManager.LOAD_DATA_MAP.values().stream()
            .filter(LoadChunkData::isRemoved)
            .forEach(d -> d.discard(level));
        LevelLoadManager.LOAD_DATA_MAP.values().stream()
            .filter(d -> !d.isRemoved())
            .forEach(d -> d.apply(level));
        LevelLoadManager.LOAD_DATA_MAP.values().removeIf(LoadChunkData::isRemoved);
    }

    public static void removeAll(ServerLevel level) {
        LevelLoadManager.LOAD_DATA_MAP.values().forEach(d -> {
            d.markRemoved();
            d.discard(level);
        });
        LevelLoadManager.LOAD_DATA_MAP.clear();
        LevelLoadManager.CHUNK_REF_COUNT.clear();
    }

    public static int getOverseerChunkCount(BlockPos centerPos) {
        LoadChunkData data = LevelLoadManager.LOAD_DATA_MAP.get(centerPos);
        return (data != null
                && !data.isRemoved()
                && data.getSource() == LoadChunkData.Source.OVERSEER)
               ? data.getChunkPosList().size()
               : 0;
    }

    public static int getAllOverseerForcedChunkCount(ServerLevel level) {
        return LevelLoadManager.LOAD_DATA_MAP.values().stream()
            .filter(data -> !data.isRemoved())
            .filter(data -> data.getSource() == LoadChunkData.Source.OVERSEER)
            .filter(data -> data.getServerLevel().dimension().equals(level.dimension()))
            .flatMap(data -> data.getChunkPosList().stream())
            .distinct()
            .mapToInt(cp -> 1)
            .sum();
    }
}
