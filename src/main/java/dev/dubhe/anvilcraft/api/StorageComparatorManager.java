package dev.dubhe.anvilcraft.api;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import dev.dubhe.anvilcraft.block.entity.storage.CrateBlockEntity;
import dev.dubhe.anvilcraft.block.entity.storage.LargeCrateBlockEntity;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class StorageComparatorManager {
    private static final Table<ServerLevel, UUID, Set<BlockPos>> STORAGE_BLOCKS = HashBasedTable.create();

    private StorageComparatorManager() {
    }

    public static void registerIfApplicable(StorageBlockEntity entity) {
        if (!(entity.getLevel() instanceof ServerLevel level) || entity.isRemoved() || entity.getId() == null
            || !(entity instanceof CrateBlockEntity || entity instanceof LargeCrateBlockEntity)) return;
        var state = entity.getBlockState();
        if (state.getBlock() instanceof AbstractMultiPartBlock<?> block
            && !block.getMainPartPos(entity.getBlockPos(), state).equals(entity.getBlockPos())) return;
        STORAGE_BLOCKS.row(level).computeIfAbsent(entity.getId(), ignored -> new HashSet<>())
            .add(entity.getBlockPos().immutable());
    }

    public static void unregisterIfApplicable(StorageBlockEntity entity) {
        if (!(entity.getLevel() instanceof ServerLevel level) || entity.getId() == null) return;
        Set<BlockPos> positions = STORAGE_BLOCKS.get(level, entity.getId());
        if (positions == null) return;
        positions.remove(entity.getBlockPos());
        if (positions.isEmpty()) STORAGE_BLOCKS.remove(level, entity.getId());
    }

    public static void notifyContentsChanged(UUID storageId) {
        STORAGE_BLOCKS.column(storageId).forEach((level, positions) -> {
            for (BlockPos pos : Set.copyOf(positions)) {
                var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
                if (chunk != null && chunk.getBlockEntity(pos) instanceof StorageBlockEntity entity
                    && !entity.isRemoved() && storageId.equals(entity.getId())) {
                    entity.refreshComparatorSignal();
                }
            }
        });
    }

    public static void clear(ServerLevel level) {
        STORAGE_BLOCKS.row(level).clear();
    }

    public static void clear() {
        STORAGE_BLOCKS.clear();
    }
}
