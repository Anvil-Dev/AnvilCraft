package dev.dubhe.anvilcraft.api.world.load;

import lombok.AccessLevel;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
public class LoadChunkData {
    private final BlockPos centerPos;
    private final int level;
    private final List<ChunkPos> chunkPosList;
    private final ServerLevel serverLevel;
    private boolean removed = false;
    private final Source source;

    @Getter(AccessLevel.NONE)
    private boolean applied = false;

    public enum Source {
        OVERSEER,
        SIMPLE
    }

    private final Set<ChunkPos> noRandomTickChunks;
    private final Set<ChunkPos> allowFireSpreadChunks;
    private final Set<ChunkPos> allowNaturalSpawnChunks;
    private final Set<ChunkPos> allowSpawnerSpawnChunks;

    private LoadChunkData(BlockPos centerPos, List<ChunkPos> chunkPosList,
        int level, ServerLevel serverLevel, Source source,
        Set<ChunkPos> noRandomTickChunks,
        Set<ChunkPos> allowFireSpreadChunks,
        Set<ChunkPos> allowNaturalSpawnChunks,
        Set<ChunkPos> allowSpawnerSpawnChunks) {
        this.centerPos = centerPos;
        this.chunkPosList = chunkPosList;
        this.level = level;
        this.serverLevel = serverLevel;
        this.source = source;
        this.noRandomTickChunks = noRandomTickChunks;
        this.allowFireSpreadChunks = allowFireSpreadChunks;
        this.allowNaturalSpawnChunks = allowNaturalSpawnChunks;
        this.allowSpawnerSpawnChunks = allowSpawnerSpawnChunks;
    }

    public record BlockOffsetMapping(int chunkOffsetX, int chunkOffsetZ, int tier, int sourceFlags) {}

    public static final class SourceFlags {
        public static final int NO_RANDOM_TICK = 1;
        public static final int ALLOW_FIRE_SPREAD = 1 << 1;
        public static final int ALLOW_NATURAL_SPAWN = 1 << 2;
        public static final int ALLOW_SPAWNER_SPAWN = 1 << 3;

        public static final int DEFAULT = 0;
        public static final int FROST = NO_RANDOM_TICK;
        public static final int FIRE = ALLOW_FIRE_SPREAD;
        public static final int MULTIPHASE = NO_RANDOM_TICK | ALLOW_FIRE_SPREAD;
        public static final int TRANSCENDIUM = NO_RANDOM_TICK | ALLOW_NATURAL_SPAWN | ALLOW_SPAWNER_SPAWN;
    }

    public static LoadChunkData createLoadChunkData(
        int level, BlockPos centerPos, ServerLevel serverLevel, List<BlockOffsetMapping> offsetMappings
    ) {
        ChunkPos centerChunk = ChunkPos.containing(centerPos);

        Map<ChunkPos, List<Integer>> chunkSources = new HashMap<>();

        for (BlockOffsetMapping m : offsetMappings) {
            ChunkPos targetChunk = new ChunkPos(centerChunk.x() + m.chunkOffsetX(), centerChunk.z() + m.chunkOffsetZ());
            chunkSources.computeIfAbsent(targetChunk, k -> new ArrayList<>()).add(m.sourceFlags());
        }

        chunkSources.computeIfAbsent(centerChunk, k -> new ArrayList<>()).add(0);

        Set<ChunkPos> noRandomTick = new HashSet<>();
        Set<ChunkPos> allowFireSpread = new HashSet<>();
        Set<ChunkPos> allowNaturalSpawn = new HashSet<>();
        Set<ChunkPos> allowSpawnerSpawn = new HashSet<>();
        Set<ChunkPos> allChunks = new HashSet<>(chunkSources.keySet());

        for (Map.Entry<ChunkPos, List<Integer>> entry : chunkSources.entrySet()) {
            ChunkPos cp = entry.getKey();
            List<Integer> sourceFlagsList = entry.getValue();

            for (int flags : sourceFlagsList) {
                if ((flags & SourceFlags.NO_RANDOM_TICK) != 0) {
                    noRandomTick.add(cp);
                    break;
                }
            }

            for (int flags : sourceFlagsList) {
                if ((flags & SourceFlags.ALLOW_FIRE_SPREAD) != 0) {
                    allowFireSpread.add(cp);
                    break;
                }
            }

            for (int flags : sourceFlagsList) {
                if ((flags & SourceFlags.ALLOW_NATURAL_SPAWN) != 0) {
                    allowNaturalSpawn.add(cp);
                    break;
                }
            }

            for (int flags : sourceFlagsList) {
                if ((flags & SourceFlags.ALLOW_SPAWNER_SPAWN) != 0) {
                    allowSpawnerSpawn.add(cp);
                    break;
                }
            }
        }

        return new LoadChunkData(
            centerPos,
            new ArrayList<>(allChunks),
            level,
            serverLevel,
            Source.OVERSEER,
            noRandomTick,
            allowFireSpread,
            allowNaturalSpawn,
            allowSpawnerSpawn
        );
    }

    /// 基于半径创建简单加载数据
    /// 默认：只运算随机刻（不火焰蔓延，不生物刷新）
    public static LoadChunkData createSimpleLoadChunkData(
        int radius, BlockPos centerPos, ServerLevel serverLevel
    ) {
        return createSimpleLoadChunkData(radius, centerPos, serverLevel, 0);
    }

    /// 基于半径创建简单加载数据（指定来源标志）
    public static LoadChunkData createSimpleLoadChunkData(
        int radius, BlockPos centerPos, ServerLevel serverLevel, int sourceFlags
    ) {
        ChunkPos centerChunk = ChunkPos.containing(centerPos);
        List<ChunkPos> chunks = new ArrayList<>();
        Set<ChunkPos> noRandomTick = new HashSet<>();
        Set<ChunkPos> allowFireSpread = new HashSet<>();
        Set<ChunkPos> allowNaturalSpawn = new HashSet<>();
        Set<ChunkPos> allowSpawnerSpawn = new HashSet<>();

        for (int x = centerChunk.x() - radius; x <= centerChunk.x() + radius; x++) {
            for (int z = centerChunk.z() - radius; z <= centerChunk.z() + radius; z++) {
                ChunkPos cp = new ChunkPos(x, z);
                chunks.add(cp);

                if ((sourceFlags & SourceFlags.NO_RANDOM_TICK) != 0) noRandomTick.add(cp);
                if ((sourceFlags & SourceFlags.ALLOW_FIRE_SPREAD) != 0) allowFireSpread.add(cp);
                if ((sourceFlags & SourceFlags.ALLOW_NATURAL_SPAWN) != 0) allowNaturalSpawn.add(cp);
                if ((sourceFlags & SourceFlags.ALLOW_SPAWNER_SPAWN) != 0) allowSpawnerSpawn.add(cp);
            }
        }

        return new LoadChunkData(centerPos, chunks, radius, serverLevel,Source.SIMPLE,
            noRandomTick, allowFireSpread, allowNaturalSpawn, allowSpawnerSpawn);
    }

    public void markRemoved() {
        this.removed = true;
    }

    public boolean isApplied() {
        return this.applied;
    }

    public void apply(ServerLevel level) {
        if (this.applied) return;
        this.applied = true;
        LevelLoadManager.lazy(() -> {
            var dimension = level.dimension();
            for (ChunkPos cp : this.chunkPosList) {
                LevelLoadManager.forceChunk(cp.x(), cp.z(), true, level);
                ChunkFeatureManager.registerChunkFeatures(dimension, cp, this.centerPos, this);
            }
        });
    }

    public void discard(ServerLevel level) {
        if (!this.applied) return;
        this.applied = false;
        LevelLoadManager.lazy(() -> {
            var dimension = level.dimension();
            for (ChunkPos cp : this.chunkPosList) {
                LevelLoadManager.forceChunk(cp.x(), cp.z(), false, level);
                ChunkFeatureManager.unregisterChunkFeatures(dimension, cp, this.centerPos);
            }
        });
    }

    public boolean shouldSkipRandomTick(ChunkPos pos) {
        return this.noRandomTickChunks.contains(pos);
    }

    public boolean shouldAllowFireSpread(ChunkPos pos) {
        return this.allowFireSpreadChunks.contains(pos);
    }

    public boolean shouldAllowNaturalSpawn(ChunkPos pos) {
        return this.allowNaturalSpawnChunks.contains(pos);
    }

    public boolean shouldAllowSpawnerSpawn(ChunkPos pos) {
        return this.allowSpawnerSpawnChunks.contains(pos);
    }
}