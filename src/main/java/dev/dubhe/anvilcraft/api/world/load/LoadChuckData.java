package dev.dubhe.anvilcraft.api.world.load;

import dev.dubhe.anvilcraft.network.OverseerSyncPacket;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
public class LoadChuckData {
    private final BlockPos centerPos;
    private final int level;
    private final List<ChunkPos> chunkPosList;
    private final boolean isNeedRandomTick;
    private final ServerLevel serverLevel;
    private final boolean overseer;

    private LoadChuckData(
        BlockPos centerPos,
        List<ChunkPos> chunkPosList,
        boolean isNeedRandomTick,
        int level,
        ServerLevel serverLevel,
        boolean overseer
    ) {
        this.centerPos = centerPos;
        this.chunkPosList = chunkPosList;
        this.isNeedRandomTick = isNeedRandomTick;
        this.level = level;
        this.serverLevel = serverLevel;
        this.overseer = overseer;
    }

    /**
     * 创建强加载区块区域数据
     *
     * @param level            加载区域等级, 既边长为(level*2+1)区块的加载区域
     * @param centerPos        加载区中心
     * @param isNeedRandomTick 是否需要随机刻加载
     * @return 强加载区块区域数据
     */
    public static LoadChuckData createLoadChuckData(
        int level,
        BlockPos centerPos,
        boolean isNeedRandomTick,
        ServerLevel serverLevel
    ) {
        return createLoadChuckData(level, centerPos, isNeedRandomTick, serverLevel, false);
    }

    private static LoadChuckData createLoadChuckData(
        int level, BlockPos centerPos, boolean isNeedRandomTick, ServerLevel serverLevel, boolean overseer
    ) {
        List<ChunkPos> chunkPosList = new ArrayList<>();
        ChunkPos centerChunkPos = new ChunkPos(centerPos);
        for (int x = centerChunkPos.x - level; x <= centerChunkPos.x + level; x++) {
            for (int z = centerChunkPos.z - level; z <= centerChunkPos.z + level; z++) {
                chunkPosList.add(new ChunkPos(x, z));
            }
        }
        return new LoadChuckData(centerPos, chunkPosList, isNeedRandomTick, level, serverLevel, overseer);
    }

    public static LoadChuckData createOverseerLoadChuckData(
        int level, BlockPos centerPos, boolean isNeedRandomTick, ServerLevel serverLevel
    ) {
        return createLoadChuckData(level, centerPos, isNeedRandomTick, serverLevel, true);
    }

    /**
     * 加载区块
     */
    public void apply(ServerLevel level) {
        LevelLoadManager.lazy(() -> {
            if (this.isNeedRandomTick) {
                RandomChuckTickLoadManager.register(this.centerPos, this);
            }
            for (ChunkPos chunkPos : this.chunkPosList) {
                LevelLoadManager.addRef(level, chunkPos);
            }
            if (this.overseer) PacketDistributor.sendToAllPlayers(OverseerSyncPacket.of(this));
        });
    }

    /**
     * 取消加载区块
     */
    public void discard(ServerLevel level) {
        LevelLoadManager.lazy(() -> {
            if (this.isNeedRandomTick) {
                RandomChuckTickLoadManager.unregister(this.centerPos);
            }
            for (ChunkPos chunkPos : this.chunkPosList) {
                LevelLoadManager.removeRef(level, chunkPos);
            }
            if (this.overseer) {
                PacketDistributor.sendToAllPlayers(new OverseerSyncPacket(level.dimension(), this.centerPos, Map.of()));
            }
        });
    }
}
