package dev.dubhe.anvilcraft.api;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import dev.dubhe.anvilcraft.block.container.storage.HyperdimensionStorageStationBlock;
import dev.dubhe.anvilcraft.block.container.storage.ShulkerContainerBlock;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 仓储端口管理器：登记各存储名下的端口，并提供端口相连关系的扫描。
 *
 * <p>三种端口（仓储端口、仓储流体端口、仓储端口整合器）在连通关系重校验时都会重新登记，
 * 表按「维度 × 存储 ID → 端口位置」记录，供端口按存储归属登记与注销。
 * 维度先分层才能避免不同维度的同坐标互相干扰（注册与注销都必须带上维度）。</p>
 *
 * <p>相连关系由端口自身决定：面相邻的端口可互相延伸，遇到集装箱 / 存储站不穿过，
 * 因此集装箱两侧未直接相连的端口阵列不会互通。端口周期性重校验并重新登记，
 * 失效条目会被自然修正。</p>
 */
public final class StoragePortManager {
    /**
     * 流体伪槽位的逻辑编号起点。
     *
     * <p>流体不占存储槽位，用一段远高于真实槽位数的编号与物品槽位区分；
     * 服务端排序与客户端渲染、同步均以此判定「这是流体而非物品」。</p>
     */
    public static final int FLUID_SLOT_BASE = 1 << 24;

    /** 相连关系扫描的端口访问上限，防止极端链式摆放造成性能问题 */
    private static final int CONNECTIVITY_LIMIT = 512;

    /** （维度 × 存储 ID）→ 端口位置集合 */
    private static final Table<ResourceKey<Level>, UUID, Set<BlockPos>> PORTS = HashBasedTable.create();

    private StoragePortManager() {
    }

    /**
     * 端口相连关系的扫描结果。
     *
     * @param core  连通组件接触到的唯一核心主方块坐标；没有核心或接触多个核心时为 null
     * @param ports 该组件中除起点以外的所有端口
     */
    public record LinkedPorts(@Nullable BlockPos core, List<IStoragePort> ports) {
    }

    /**
     * 从起点沿面相邻的端口扫描相连关系，一次遍历同时得到核心与端口。
     *
     * @param level 世界
     * @param start 起点（任意端口方块位置）
     * @return 连通组件的核心与端口；未加载的相邻方块不参与扫描
     */
    public static LinkedPorts scan(Level level, BlockPos start) {
        Set<BlockPos> cores = new HashSet<>();
        List<IStoragePort> ports = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.addLast(start);
        visited.add(start);
        int visitedPorts = 0;
        while (!queue.isEmpty() && visitedPorts < StoragePortManager.CONNECTIVITY_LIMIT) {
            BlockPos pos = queue.removeFirst();
            visitedPorts++;
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = pos.relative(direction);
                if (visited.contains(neighbor) || !level.isLoaded(neighbor)) {
                    continue;
                }
                BlockState state = level.getBlockState(neighbor);
                Block block = state.getBlock();
                BlockPos coreMain = null;
                if (block instanceof ShulkerContainerBlock shulker) {
                    coreMain = shulker.getMainPartPos(neighbor, state);
                } else if (block instanceof HyperdimensionStorageStationBlock station) {
                    coreMain = station.getMainPartPos(neighbor, state);
                }
                if (coreMain != null) {
                    if (level.getBlockEntity(coreMain) instanceof StorageBlockEntity storage
                        && storage.getId() != null) {
                        cores.add(coreMain);
                    }
                    continue;
                }
                if (level.getBlockEntity(neighbor) instanceof IStoragePort port) {
                    visited.add(neighbor);
                    queue.addLast(neighbor);
                    ports.add(port);
                }
            }
        }
        // 连通组件必须恰好接触一个核心（紧贴两个核心则整条链不工作）
        return new LinkedPorts(cores.size() == 1 ? cores.iterator().next() : null, List.copyOf(ports));
    }

    /**
     * 从起点扫描相连关系，解析连通组件接触到的那个唯一核心。
     *
     * @param level 世界
     * @param start 起点（任意端口方块位置）
     * @return 唯一有效核心的主方块坐标；没有核心或接触多个核心时返回 {@code null}
     */
    @Nullable
    public static BlockPos findSoleCore(Level level, BlockPos start) {
        return StoragePortManager.scan(level, start).core();
    }

    /**
     * 登记一个已连接到某存储的端口。
     *
     * @param storageId 存储 ID
     * @param level     端口所在维度
     * @param pos       端口位置
     */
    public static void register(UUID storageId, Level level, BlockPos pos) {
        Set<BlockPos> positions = StoragePortManager.PORTS.get(level.dimension(), storageId);
        if (positions == null) {
            positions = new HashSet<>();
            StoragePortManager.PORTS.put(level.dimension(), storageId, positions);
        }
        positions.add(pos.immutable());
    }

    /**
     * 注销某端口在「指定存储 × 指定维度」下的登记。
     *
     * <p>只清这一个存储名下的条目：端口从 A 存储改挂到 B 存储时，必须先把 A 的旧条目清掉，
     * 否则 A 的界面仍会显示该端口的流体、{@code drain(A)} 还会从属于 B 的端口抽走流体。
     * 只清这一个维度：不同维度的同坐标是两个不同的端口。</p>
     *
     * @param storageId 存储 ID；为 null 时无操作
     * @param dimension 端口所在维度
     * @param pos       端口位置
     */
    public static void unregister(@Nullable UUID storageId, ResourceKey<Level> dimension, BlockPos pos) {
        if (storageId == null) {
            return;
        }
        Set<BlockPos> positions = StoragePortManager.PORTS.get(dimension, storageId);
        if (positions == null) {
            return;
        }
        positions.remove(pos);
        if (positions.isEmpty()) {
            StoragePortManager.PORTS.remove(dimension, storageId);
        }
    }

    /** 服务端停止时清表，避免静态表在下次进入世界时残留上次的登记。 */
    public static void clear() {
        StoragePortManager.PORTS.clear();
    }

    /**
     * 某存储当前登记的全部端口位置（仓储端口 / 流体端口 / 整合器都算），供测试与调试使用。
     *
     * @param storageId 存储 ID
     * @return 端口位置集合（跨维度取并集）
     */
    public static Set<BlockPos> positions(UUID storageId) {
        Set<BlockPos> result = new HashSet<>();
        for (Set<BlockPos> positions : StoragePortManager.PORTS.column(storageId).values()) {
            result.addAll(positions);
        }
        return Set.copyOf(result);
    }
}
