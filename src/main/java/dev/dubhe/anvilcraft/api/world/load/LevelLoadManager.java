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
    private static final Map<ResourceKey<Level>, Map<BlockPos, LoadChuckData>> LEVEL_LOAD_CHUCK_AREA_MAP = new HashMap<>();
    // 新增：ChunkPos -> 引用计数
    private static final Map<ResourceKey<Level>, Map<ChunkPos, Integer>> CHUNK_REFS = new HashMap<>();
    private static final Deque<Runnable> lazyCalls = new ArrayDeque<>();
    private static boolean serverStarted = false;

    private static Map<BlockPos, LoadChuckData> getDimensionMap(Level level) {
        return LEVEL_LOAD_CHUCK_AREA_MAP.computeIfAbsent(
            level.dimension(),
            k -> new HashMap<>()
        );
    }

    private static Map<ChunkPos, Integer> getRefMap(Level level) {
        return CHUNK_REFS.computeIfAbsent(level.dimension(), k -> new HashMap<>());
    }

    /** 增加引用 */
    static void addRef(ServerLevel level, ChunkPos pos) {
        Map<ChunkPos, Integer> refs = getRefMap(level);
        int c = refs.merge(pos, 1, Integer::sum);
        if (c == 1) level.setChunkForced(pos.x, pos.z, true);
    }

    /** 减少引用 */
    static void removeRef(ServerLevel level, ChunkPos pos) {
        Map<ChunkPos, Integer> refs = getRefMap(level);
        if (!refs.containsKey(pos)) return;
        int c = refs.get(pos) - 1;
        if (c <= 0) {
            refs.remove(pos);
            level.setChunkForced(pos.x, pos.z, false);
        } else {
            refs.put(pos, c);
        }
    }

    /**
     * 注册区块区域
     *
     * @param centerPos     中心坐标
     * @param loadChuckData 区块区域数据
     * @param level         世界
     */
    public static void register(BlockPos centerPos, LoadChuckData loadChuckData, ServerLevel level) {
        Map<BlockPos, LoadChuckData> dimMap = getDimensionMap(level);
        if (dimMap.containsKey(centerPos)) return;
        dimMap.put(centerPos, loadChuckData);
        loadChuckData.apply(level);
    }

    public static boolean checkRegistered(BlockPos pos, Level level) {
        Map<BlockPos, LoadChuckData> dimMap = LEVEL_LOAD_CHUCK_AREA_MAP.get(level.dimension());
        return dimMap != null && dimMap.containsKey(pos);
    }

    static void lazy(Runnable runnable) {
        if (serverStarted) {
            runnable.run();
        } else {
            lazyCalls.add(runnable);
        }
    }

    public static void notifyServerStarted() {
        serverStarted = true;
        while (!lazyCalls.isEmpty()) {
            lazyCalls.poll().run();
        }
    }

    /**
     * 取消注册
     *
     * @param centerPos 中心坐标
     * @param level     世界
     */
    public static void unregister(BlockPos centerPos, Level level) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        Map<BlockPos, LoadChuckData> dimMap = LEVEL_LOAD_CHUCK_AREA_MAP.get(level.dimension());
        if (dimMap == null) return;
        LoadChuckData data = dimMap.remove(centerPos);
        if (data != null) data.discard(serverLevel);
    }

    public static void reload(ServerLevel serverLevel, BlockPos centerPos, LoadChuckData newData) {
        Map<BlockPos, LoadChuckData> dimMap = getDimensionMap(serverLevel);
        LoadChuckData old = dimMap.remove(centerPos);
        if (old != null) old.discard(serverLevel);
        dimMap.put(centerPos, newData);
        newData.apply(serverLevel);
    }

    public static void removeAll(ServerLevel level) {
        Map<BlockPos, LoadChuckData> dimMap = LEVEL_LOAD_CHUCK_AREA_MAP.remove(level.dimension());
        if (dimMap != null) dimMap.values().forEach(it -> it.discard(level));
        CHUNK_REFS.remove(level.dimension());
    }
}
