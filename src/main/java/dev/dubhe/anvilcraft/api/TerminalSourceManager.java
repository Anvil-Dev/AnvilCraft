package dev.dubhe.anvilcraft.api;

import dev.dubhe.anvilcraft.block.entity.storage.LargeCrateBlockEntity;
import dev.dubhe.anvilcraft.block.entity.storage.ShulkerContainerBlockEntity;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** 记录已加载的终端仓储源，查询只验证范围内的候选，不触发区块加载。 */
public final class TerminalSourceManager {
    private static final Map<ServerLevel, Set<BlockPos>> LARGE_CRATES = new HashMap<>();
    private static final Map<ServerLevel, Set<BlockPos>> SHULKER_CONTAINERS = new HashMap<>();

    private TerminalSourceManager() {
    }

    public static void registerIfApplicable(StorageBlockEntity entity) {
        if (!(entity.getLevel() instanceof ServerLevel level) || entity.isRemoved()) return;
        Map<ServerLevel, Set<BlockPos>> sources = sources(entity);
        if (sources == null || !entity.getBlockPos().equals(mainPos(entity))) return;
        sources.computeIfAbsent(level, ignored -> new HashSet<>()).add(entity.getBlockPos().immutable());
    }

    public static void unregisterIfApplicable(StorageBlockEntity entity) {
        if (!(entity.getLevel() instanceof ServerLevel level)) return;
        Map<ServerLevel, Set<BlockPos>> sources = sources(entity);
        if (sources == null || !entity.getBlockPos().equals(mainPos(entity))) return;
        Set<BlockPos> entries = sources.get(level);
        if (entries == null) return;
        entries.remove(entity.getBlockPos());
        if (entries.isEmpty()) sources.remove(level);
    }

    public static @Nullable BlockPos nearestLargeCrate(ServerLevel level, double x, double y, double z, int range) {
        return nearest(level, LARGE_CRATES.get(level), x, y, z, range, LargeCrateBlockEntity.class);
    }

    public static @Nullable BlockPos nearestShulkerContainer(ServerLevel level, double x, double y, double z, int range) {
        return nearest(level, SHULKER_CONTAINERS.get(level), x, y, z, range, ShulkerContainerBlockEntity.class);
    }

    private static @Nullable BlockPos nearest(
        ServerLevel level, @Nullable Set<BlockPos> entries, double x, double y, double z,
        int range, Class<? extends StorageBlockEntity> expected
    ) {
        if (range < 0 || entries == null || entries.isEmpty()) return null;
        double rangeSquared = (double) range * range;
        double nearestSquared = Double.MAX_VALUE;
        BlockPos nearest = null;
        var iterator = entries.iterator();
        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();
            double distance = pos.distToCenterSqr(x, y, z);
            if (distance > rangeSquared) continue;
            var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
            if (chunk == null || !(chunk.getBlockEntity(pos) instanceof StorageBlockEntity entity)
                || entity.isRemoved() || !expected.isInstance(entity) || !mainPos(entity).equals(pos)) {
                iterator.remove();
                continue;
            }
            if (distance < nearestSquared) {
                nearest = pos;
                nearestSquared = distance;
            }
        }
        return nearest;
    }

    private static @Nullable Map<ServerLevel, Set<BlockPos>> sources(StorageBlockEntity entity) {
        if (entity instanceof LargeCrateBlockEntity) return LARGE_CRATES;
        if (entity instanceof ShulkerContainerBlockEntity) return SHULKER_CONTAINERS;
        return null;
    }

    private static BlockPos mainPos(StorageBlockEntity entity) {
        var state = entity.getBlockState();
        if (state.getBlock() instanceof AbstractMultiPartBlock<?> multipart) {
            return multipart.getMainPartPos(entity.getBlockPos(), state);
        }
        return entity.getBlockPos();
    }

    public static void clear(ServerLevel level) {
        LARGE_CRATES.remove(level);
        SHULKER_CONTAINERS.remove(level);
    }

    public static void clear() {
        LARGE_CRATES.clear();
        SHULKER_CONTAINERS.clear();
    }
}
