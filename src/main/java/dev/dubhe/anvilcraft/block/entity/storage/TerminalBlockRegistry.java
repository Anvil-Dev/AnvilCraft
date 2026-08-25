package dev.dubhe.anvilcraft.block.entity.storage;

import dev.dubhe.anvilcraft.block.container.storage.LargeCrateBlock;
import dev.dubhe.anvilcraft.block.container.storage.ShulkerContainerBlock;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * 世界中可被本地终端 / 潜影终端连接的大型板条箱与潜影集装箱注册表。
 *
 * <p>仓储方块在世界中放置（获得存储 ID 或从存档载入）时注册主方块位置，
 * 终端打开 / 刷新时直接按表检索最近的候选，避免每帧全量扫描区块；
 * 表内条目可能因方块被破坏而过期，查询方需用方块实体做最终校验。</p>
 */
public final class TerminalBlockRegistry {
    private static final Map<ResourceKey<Level>, Set<BlockPos>> LARGE_CRATES = new HashMap<>();
    private static final Map<ResourceKey<Level>, Set<BlockPos>> SHULKER_CONTAINERS = new HashMap<>();

    private TerminalBlockRegistry() {
    }

    /** 仓储方块获得存储 ID 或载入时调用；仅登记大型板条箱 / 潜影集装箱的主方块。 */
    public static void registerIfApplicable(StorageBlockEntity be) {
        Level level = be.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockState state = be.getBlockState();
        if (state.getBlock() instanceof LargeCrateBlock) {
            TerminalBlockRegistry.LARGE_CRATES
                .computeIfAbsent(level.dimension(), ignored -> new HashSet<>())
                .add(TerminalBlockRegistry.mainPos(be));
        } else if (state.getBlock() instanceof ShulkerContainerBlock) {
            TerminalBlockRegistry.SHULKER_CONTAINERS
                .computeIfAbsent(level.dimension(), ignored -> new HashSet<>())
                .add(TerminalBlockRegistry.mainPos(be));
        }
    }

    /** 返回范围内最近的大型板条箱主方块坐标；没有则返回 null。 */
    public static @Nullable BlockPos nearestLargeCrate(ServerLevel level, double x, double y, double z, int range) {
        return TerminalBlockRegistry.nearest(
            TerminalBlockRegistry.LARGE_CRATES.get(level.dimension()),
            x,
            y,
            z,
            range
        );
    }

    /** 返回范围内最近的潜影集装箱主方块坐标；没有则返回 null。 */
    public static @Nullable BlockPos nearestShulkerContainer(
        ServerLevel level,
        double x,
        double y,
        double z,
        int range
    ) {
        return TerminalBlockRegistry.nearest(
            TerminalBlockRegistry.SHULKER_CONTAINERS.get(level.dimension()),
            x,
            y,
            z,
            range
        );
    }

    private static @Nullable BlockPos nearest(@Nullable Set<BlockPos> entries, double x, double y, double z, int range) {
        if (entries == null || entries.isEmpty()) {
            return null;
        }
        double rangeSqr = (double) range * range;
        BlockPos nearest = null;
        double nearestSqr = Double.MAX_VALUE;
        for (BlockPos pos : entries) {
            double sqr = pos.distToCenterSqr(x, y, z);
            if (sqr <= rangeSqr && sqr < nearestSqr) {
                nearestSqr = sqr;
                nearest = pos;
            }
        }
        return nearest;
    }

    /** 多方块方块取其主方块坐标，普通方块取自身坐标。 */
    private static BlockPos mainPos(StorageBlockEntity be) {
        BlockState state = be.getBlockState();
        if (state.getBlock() instanceof AbstractMultiPartBlock<?> multipart) {
            return multipart.getMainPartPos(be.getBlockPos(), state);
        }
        return be.getBlockPos();
    }
}