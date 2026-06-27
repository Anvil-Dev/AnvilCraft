package dev.dubhe.anvilcraft.api.world.load;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

    /**
     * tick
     * 修复：多个监督者区块位置重叠时，随机刻会重复计算。
     * 使用 Set 对每个 ServerLevel 的 ChunkPos 去重，确保同一区块每 tick 只执行一次随机刻。
     */
    public static void tick() {
        Map<ServerLevel, Set<Long>> tickedChunksByLevel = new HashMap<>();

        for (LoadChuckData loadChuckData : RANDOM_TICK_LOAD_CHUCK_AREA_MAP.values()) {
            ServerLevel serverLevel = loadChuckData.getServerLevel();
            Set<Long> tickedChunks = tickedChunksByLevel.computeIfAbsent(
                serverLevel, k -> new HashSet<>()
            );

            for (ChunkPos chunkPos : loadChuckData.getChunkPosList()) {
                long chunkKey = chunkPos.toLong();
                if (!tickedChunks.add(chunkKey)) {
                    continue;
                }

                ChunkMap chunkMap = serverLevel.getChunkSource().chunkMap;
                if (chunkMap.updatingChunkMap.containsKey(chunkKey)) {
                    serverLevel.tickChunk(
                        serverLevel.getChunk(chunkPos.x, chunkPos.z),
                        serverLevel.getServer().getGameRules().getInt(GameRules.RULE_RANDOMTICKING)
                    );
                }
            }
        }
    }
}
