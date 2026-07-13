package dev.dubhe.anvilcraft.api.fluid.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 流体管道网络的全局管理器（每服务器一个）。
 *
 * <h3>缓存 + 脏标记</h3>
 * 管道拓扑很少变，故网络对象<b>缓存复用</b>：只有拓扑/端点/势场变化时才重扫。
 * 变化通过 {@link #markDirty} 标记（管道放置/破坏/推动/邻居变、泵工作翻转、容器增删）。
 * 端点的流体量每 tick 变但结构不变——直接每 tick 复用缓存网络做分配即可。
 *
 * <h3>pos→网络索引</h3>
 * 缓存时建 {@code partIndex}（管道位置→所属网络），tick 时直接遍历缓存网络，
 * 免去"逐容器找相邻管道 + flood-fill 去重"的探测开销。
 *
 * <h3>空闲降频</h3>
 * 网络连续 {@value #IDLE_THRESHOLD} tick 无流体转移则视为空闲，改为每
 * {@value #IDLE_INTERVAL} tick 才分配一次；一旦发生转移立即恢复每 tick。
 */
public final class FluidNetworkManager {
    public static final FluidNetworkManager INSTANCE = new FluidNetworkManager();

    /**
     * 连续无转移达到此 tick 数则进入空闲降频
     */
    public static final int IDLE_THRESHOLD = 1200;
    /**
     * 空闲网络的分配间隔（tick）
     */
    public static final int IDLE_INTERVAL = 20;

    private final Map<Level, LevelData> byLevel = Collections.synchronizedMap(new HashMap<>());

    private FluidNetworkManager() {
    }

    /**
     * 每个世界的网络缓存与容器登记。
     */
    private static final class LevelData {
        final Set<BlockPos> containers = Collections.synchronizedSet(new HashSet<>());
        List<FluidPipeNetwork> networks = new ArrayList<>();
        final Map<BlockPos, FluidPipeNetwork> partIndex = new HashMap<>();
        boolean dirty = true;
    }

    private LevelData data(Level level) {
        return this.byLevel.computeIfAbsent(level, _ -> new LevelData());
    }

    // ---- 容器登记（容器 BE 的 onLoad/setRemoved 调用） ----

    /**
     * 容器加载时注册。管道部件不注册（它们无 BlockEntity）。
     */
    public void addContainer(Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return;
        }
        LevelData d = this.data(level);
        d.containers.add(pos.immutable());
        d.dirty = true;
    }

    /**
     * 容器移除时注销。
     */
    public void removeContainer(Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return;
        }
        LevelData d = this.byLevel.get(level);
        if (d != null) {
            d.containers.remove(pos);
            d.dirty = true;
        }
    }

    /**
     * 标记某世界的网络缓存失效，下 tick 重扫。
     * 由管道方块放置/破坏/推动/邻居变、泵工作翻转等拓扑变化调用。
     */
    public void markDirty(Level level) {
        if (level.isClientSide()) {
            return;
        }
        LevelData d = this.byLevel.get(level);
        if (d != null) {
            d.dirty = true;
        }
    }

    // ---- tick ----

    /**
     * 每服务器 tick 调用：必要时重扫网络，然后按活跃度分配。
     */
    public void tick() {
        synchronized (this.byLevel) {
            for (Map.Entry<Level, LevelData> entry : this.byLevel.entrySet()) {
                this.tickLevel(entry.getKey(), entry.getValue());
            }
        }
    }

    private void tickLevel(Level level, LevelData d) {
        // 遵守 /tick freeze（以及 sprint/step）：世界未正常运行时不推进流体
        if (!level.tickRateManager().runsNormally()) {
            return;
        }
        if (d.dirty) {
            this.rebuild(level, d);
            d.dirty = false;
        }
        long gameTime = level.getGameTime();
        for (FluidPipeNetwork network : d.networks) {
            // #4 空闲降频：连续 IDLE_THRESHOLD tick 无转移后，每 IDLE_INTERVAL tick 才分配一次
            if (network.getIdleTicks() >= IDLE_THRESHOLD && gameTime % IDLE_INTERVAL != 0) {
                continue;
            }
            network.tick();
            network.updateIdle();
        }
    }

    /**
     * 重建某世界的全部网络缓存与 {@code partIndex}（#1/#2）。
     * 从每个已登记容器相邻的、尚未归入某张网络的管道部件出发 flood-fill；
     * 顺带剔除已失效（方块不在或不再是容器）的登记项。
     */
    private void rebuild(Level level, LevelData d) {
        d.networks = new ArrayList<>();
        d.partIndex.clear();

        for (BlockPos containerPos : new HashSet<>(d.containers)) {
            if (!level.isLoaded(containerPos)) {
                continue; // 区块未加载：暂不处理，保留登记
            }
            if (FluidNetworkScanner.isPipePart(level.getBlockState(containerPos))
                || !FluidNetworkScanner.isContainer(level, containerPos)) {
                d.containers.remove(containerPos); // 已失效 → 注销
                continue;
            }
            BlockPos seed = findUnindexedAdjacentPipe(level, containerPos, d.partIndex);
            if (seed == null) {
                continue; // 无相邻管道，或相邻管道所属网络已在本次重建中建好
            }
            FluidPipeNetwork network = FluidNetworkScanner.scan(level, seed);
            if (network == null) {
                continue;
            }
            d.networks.add(network);
            for (BlockPos part : network.getParts()) {
                d.partIndex.put(part, network);
            }
        }
    }

    /**
     * 在容器六个相邻方向找一个<b>尚未归入本次重建任何网络</b>的管道部件作为扫描种子。
     * 已在 {@code partIndex} 中的管道说明其网络已建好，跳过。
     */
    private static BlockPos findUnindexedAdjacentPipe(
        Level level, BlockPos containerPos, Map<BlockPos, FluidPipeNetwork> partIndex
    ) {
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = containerPos.relative(dir);
            if (partIndex.containsKey(neighborPos)) {
                continue;
            }
            if (!level.isLoaded(neighborPos)) {
                continue;
            }
            BlockState neighborState = level.getBlockState(neighborPos);
            if (FluidNetworkScanner.isPipePart(neighborState)) {
                return neighborPos;
            }
        }
        return null;
    }

    public void clear() {
        this.byLevel.clear();
    }
}
