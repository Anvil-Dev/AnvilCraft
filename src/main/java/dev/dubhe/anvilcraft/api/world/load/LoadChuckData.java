package dev.dubhe.anvilcraft.api.world.load;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.List;

@Getter
public class LoadChuckData {
    private final BlockPos centerPos;
    private final int level;
    private final List<ChunkPos> chunkPosList;
    private final boolean isNeedRandomTick;
    private final ServerLevel serverLevel;

    @Getter
    private boolean removed = false;

    private LoadChuckData(
        BlockPos centerPos,
        List<ChunkPos> chunkPosList,
        boolean isNeedRandomTick,
        int level,
        ServerLevel serverLevel
    ) {
        this.centerPos = centerPos;
        this.chunkPosList = chunkPosList;
        this.isNeedRandomTick = isNeedRandomTick;
        this.level = level;
        this.serverLevel = serverLevel;
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
        List<ChunkPos> chunkPosList = new ArrayList<>();
        ChunkPos centerChunkPos = new ChunkPos(centerPos);
        for (int x = centerChunkPos.x - level; x < centerChunkPos.x + level; x++) {
            for (int z = centerChunkPos.z - level; z < centerChunkPos.z + level; z++) {
                chunkPosList.add(new ChunkPos(x, z));
            }
        }
        return new LoadChuckData(centerPos, chunkPosList, isNeedRandomTick, level, serverLevel);
    }

    /**
     * 加载区块
     */
    public void apply(ServerLevel level) {
        LevelLoadManager.lazy(() -> {
            if (this.isNeedRandomTick) {
                RandomChuckTickLoadManager.register(this.centerPos, this);
            }
            for (ChunkPos chunkPos : chunkPosList) {
                level.setChunkForced(chunkPos.x, chunkPos.z, true);
            }
        });
    }

    public void markRemoved() {
        this.removed = true;
    }

    /**
     * 取消加载区块
     */
    public void discard(ServerLevel level) {
        LevelLoadManager.lazy(() -> {
            if (this.isNeedRandomTick) {
                RandomChuckTickLoadManager.unregister(this.centerPos);
            }
            for (ChunkPos chunkPos : chunkPosList) {
                level.setChunkForced(chunkPos.x, chunkPos.z, false);
            }
        });
    }
}
