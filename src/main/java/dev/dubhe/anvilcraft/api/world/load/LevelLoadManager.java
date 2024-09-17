package dev.dubhe.anvilcraft.api.world.load;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LevelLoadManager {
    private static final Map<BlockPos, LoadChuckData> LEVEL_LOAD_CHUCK_AREA_MAP = new HashMap<>();
    private static final List<Runnable> lazyCalls = new ArrayList<>();
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
        loadChuckData.load(level);
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
        lazyCalls.forEach(Runnable::run);
    }

    /**
     * 取消注册
     *
     * @param centerPos 中心坐标
     * @param level     世界
     */
    public static void unregister(BlockPos centerPos, ServerLevel level) {
        if (!LEVEL_LOAD_CHUCK_AREA_MAP.containsKey(centerPos)) return;
        LEVEL_LOAD_CHUCK_AREA_MAP.get(centerPos).unLoad(level);
        LEVEL_LOAD_CHUCK_AREA_MAP.remove(centerPos);
        reload(level);
    }

    public static void reload(ServerLevel serverLevel) {
        for (LoadChuckData loadChuckData : LEVEL_LOAD_CHUCK_AREA_MAP.values()) loadChuckData.load(serverLevel);
    }

    public static void removeAll(ServerLevel level) {
        for (LoadChuckData loadChuckData : LEVEL_LOAD_CHUCK_AREA_MAP.values()) loadChuckData.unLoad(level);
        LEVEL_LOAD_CHUCK_AREA_MAP.clear();
    }
}
