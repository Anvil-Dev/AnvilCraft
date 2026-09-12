package dev.dubhe.anvilcraft.api;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import dev.dubhe.anvilcraft.block.container.storage.CrateBlock;
import dev.dubhe.anvilcraft.block.container.storage.LargeCrateBlock;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 记录世界中与已加载存储关联的板条箱方块，供内容变化时刷新相邻比较器信号。
 *
 * <p>板条箱的物品存放在全局 {@code Storages} 中，方块内容变化不会像原版容器那样
 * 自动触发方块更新，因此需要一张 存储 ID → 世界内方块 的表，在
 * {@code StorageServerStub.onContentsChanged} 时主动通知比较器重新读取。</p>
 */
public final class StorageComparatorManager {
    /** （维度 × 存储 ID）→ 方块位置集合 */
    private static final Table<ResourceKey<Level>, UUID, Set<BlockPos>> STORAGE_BLOCKS = HashBasedTable.create();

    private StorageComparatorManager() {
    }

    /**
     * 板条箱 / 大型板条箱方块获得存储 ID 或区块加载时注册其主方块位置。
     */
    public static void registerIfApplicable(StorageBlockEntity be) {
        Level level = be.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof CrateBlock) && !(state.getBlock() instanceof LargeCrateBlock)) {
            return;
        }
        UUID id = be.getId();
        if (id == null) {
            return;
        }
        Set<BlockPos> positions = StorageComparatorManager.STORAGE_BLOCKS.get(level.dimension(), id);
        if (positions == null) {
            positions = new HashSet<>();
            StorageComparatorManager.STORAGE_BLOCKS.put(level.dimension(), id, positions);
        }
        positions.add(StorageComparatorManager.mainPos(be));
    }

    /**
     * 方块被移除或所在区块卸载时注销。
     */
    public static void unregisterIfApplicable(StorageBlockEntity be) {
        Level level = be.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        UUID id = be.getId();
        if (id == null) {
            return;
        }
        Set<BlockPos> positions = StorageComparatorManager.STORAGE_BLOCKS.get(level.dimension(), id);
        if (positions == null) {
            return;
        }
        positions.remove(StorageComparatorManager.mainPos(be));
        if (positions.isEmpty()) {
            StorageComparatorManager.STORAGE_BLOCKS.remove(level.dimension(), id);
        }
    }

    /**
     * 存储内容变化时通知所有关联方块，让相邻比较器重新计算输出信号。
     *
     * @param storageId 发生变化的存储 ID
     */
    public static void notifyContentsChanged(UUID storageId) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        for (Map.Entry<ResourceKey<Level>, Set<BlockPos>> entry
            : StorageComparatorManager.STORAGE_BLOCKS.column(storageId).entrySet()) {
            Set<BlockPos> positions = entry.getValue();
            if (positions.isEmpty()) {
                continue;
            }
            ServerLevel level = server.getLevel(entry.getKey());
            if (level == null) {
                continue;
            }
            for (BlockPos pos : positions) {
                if (level.getBlockEntity(pos) instanceof StorageBlockEntity storage) {
                    storage.refreshComparatorSignal();
                }
            }
        }
    }

    /**
     * 多方块方块取主方块位置，普通方块取自身位置。
     */
    private static BlockPos mainPos(StorageBlockEntity be) {
        BlockState state = be.getBlockState();
        if (state.getBlock() instanceof AbstractMultiPartBlock<?> multipart) {
            return multipart.getMainPartPos(be.getBlockPos(), state);
        }
        return be.getBlockPos();
    }
}
