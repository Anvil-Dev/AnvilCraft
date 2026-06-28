package dev.dubhe.anvilcraft.api.world.load;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

    private static boolean isInPlayerRandomTickRange(ServerLevel serverLevel, ChunkPos chunkPos) {
        double chunkCenterX = (chunkPos.x << 4) + 8;
        double chunkCenterZ = (chunkPos.z << 4) + 8;

        for (ServerPlayer player : serverLevel.players()) {
            if (player.isSpectator()) continue;
            double dx = chunkCenterX - player.getX();
            double dz = chunkCenterZ - player.getZ();
            if (dx * dx + dz * dz < 16384.0) {
                return true;
            }
        }
        return false;
    }

    /**
     * tick
     * 修复1：多个监督者范围重叠时，使用 Set 去重，同一区块每 tick 只执行一次。
     * 修复2：与原版玩家随机刻范围重叠时，监督者跳过，避免额外运算。
     */
    public static void tick() {
        Map<ServerLevel, Set<Long>> tickedChunksByLevel = new HashMap<>();

        for (LoadChuckData loadChuckData : RANDOM_TICK_LOAD_CHUCK_AREA_MAP.values()) {
            ServerLevel serverLevel = loadChuckData.getServerLevel();
            if (!serverLevel.tickRateManager().runsNormally()) {
                continue;
            }
            Set<Long> tickedChunks = tickedChunksByLevel.computeIfAbsent(
                serverLevel, k -> new HashSet<>()
            );

            for (ChunkPos chunkPos : loadChuckData.getChunkPosList()) {
                long chunkKey = chunkPos.toLong();
                if (!tickedChunks.add(chunkKey) || isInPlayerRandomTickRange(serverLevel, chunkPos)) {
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
