package dev.dubhe.anvilcraft.api.world.load;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class LevelLoadManager {
    private static final Map<BlockPos, LoadChuckData> LEVEL_LOAD_CHUCK_AREA_MAP = new HashMap<>();
    private static final Deque<Runnable> lazyCalls = new ArrayDeque<>();
    private static boolean serverStarted = false;

    /**
     * 注册区块区域
     *
     * @param centerPos     中心坐标
     * @param loadChuckData 区块区域数据
     * @param level         世界
     */
    public static void register(BlockPos centerPos, LoadChuckData loadChuckData, ServerLevel level) {
        if (LEVEL_LOAD_CHUCK_AREA_MAP.containsKey(centerPos)) return;
        LEVEL_LOAD_CHUCK_AREA_MAP.put(centerPos, loadChuckData);
        reload(level);
    }

    public static boolean checkRegistered(BlockPos pos) {
        return LEVEL_LOAD_CHUCK_AREA_MAP.containsKey(pos);
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
    public static void unregister(BlockPos centerPos, ServerLevel level) {
        if (!LEVEL_LOAD_CHUCK_AREA_MAP.containsKey(centerPos)) return;
        LEVEL_LOAD_CHUCK_AREA_MAP.get(centerPos).markRemoved();
        reload(level);
    }

    public static void reload(ServerLevel serverLevel) {
        LEVEL_LOAD_CHUCK_AREA_MAP.values().stream()
            .filter(it -> !it.isRemoved())
            .forEach(it -> it.apply(serverLevel));
        LEVEL_LOAD_CHUCK_AREA_MAP.values().stream()
            .filter(LoadChuckData::isRemoved)
            .forEach(it -> it.discard(serverLevel));
        LEVEL_LOAD_CHUCK_AREA_MAP.values()
            .removeIf(LoadChuckData::isRemoved);
    }

    public static void removeAll(ServerLevel level) {
        LEVEL_LOAD_CHUCK_AREA_MAP.values().forEach(it -> {
            it.markRemoved();
            it.discard(level);
        });
        LEVEL_LOAD_CHUCK_AREA_MAP.clear();
    }
}
