package dev.dubhe.anvilcraft.api.world.load;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

public class RandomChuckTickLoadManager {
    private static final Map<BlockPos, LoadChuckData> RANDOM_TICK_LOAD_CHUCK_AREA_MAP = new HashMap<>();

    public static void register(BlockPos centerPos, LoadChuckData loadChuckData) {
        if (RANDOM_TICK_LOAD_CHUCK_AREA_MAP.containsKey(centerPos)) unregister(centerPos);
        RANDOM_TICK_LOAD_CHUCK_AREA_MAP.put(centerPos, loadChuckData);
    }

    public static void unregister(BlockPos centerPos) {
        if (!RANDOM_TICK_LOAD_CHUCK_AREA_MAP.containsKey(centerPos)) return;
        RANDOM_TICK_LOAD_CHUCK_AREA_MAP.remove(centerPos);
    }

    public static void tick(ServerLevel serverLevel) {
        ChunkMap chunkMap = serverLevel.getChunkSource().chunkMap;
        for (LoadChuckData loadChuckData : RANDOM_TICK_LOAD_CHUCK_AREA_MAP.values()) {
            for (ChunkPos chunkPos : loadChuckData.getChunkPosList()) {

            }
        }
    }
}
