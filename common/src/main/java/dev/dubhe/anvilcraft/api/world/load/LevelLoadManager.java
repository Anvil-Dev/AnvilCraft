package dev.dubhe.anvilcraft.api.world.load;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public class LevelLoadManager {
    private static final Map<BlockPos, LoadChuckData> LEVEL_LOAD_CHUCK_AREA_MAP = new HashMap<>();

    public static void register(BlockPos centerPos, LoadChuckData loadChuckData, ServerLevel level) {
        if (LEVEL_LOAD_CHUCK_AREA_MAP.containsKey(centerPos)) unregister(centerPos, level);
        LEVEL_LOAD_CHUCK_AREA_MAP.put(centerPos, loadChuckData);
        loadChuckData.load(level);
    }

    public static void unregister(BlockPos centerPos, ServerLevel level) {
        if (!LEVEL_LOAD_CHUCK_AREA_MAP.containsKey(centerPos)) return;
        LEVEL_LOAD_CHUCK_AREA_MAP.get(centerPos).unLoad(level);
        LEVEL_LOAD_CHUCK_AREA_MAP.remove(centerPos);
        reload(level);
    }

    public static void reload(ServerLevel serverLevel) {
        for (LoadChuckData loadChuckData : LEVEL_LOAD_CHUCK_AREA_MAP.values()) loadChuckData.load(serverLevel);
    }
}
