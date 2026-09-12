package dev.dubhe.anvilcraft.api;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import dev.dubhe.anvilcraft.block.container.storage.HyperdimensionStorageStationBlock;
import dev.dubhe.anvilcraft.block.container.storage.ShulkerContainerBlock;
import dev.dubhe.anvilcraft.block.entity.StorageFluidPortBlockEntity;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * 仓储端口管理器：登记各存储名下的端口，并提供端口相连关系的扫描与流体读写。
 *
 * <p>三种端口（仓储端口、仓储流体端口、仓储端口整合器）在连通关系重校验时都会重新登记，
 * 表按「维度 × 存储 ID → 端口位置」记录；流体相关的查询只取其中的流体端口。
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
     * 汇总某存储连接到的所有流体端口中的流体，同种流体合并数量。
     *
     * @param storageId 存储 ID
     * @return 流体列表；没有流体时为空列表
     */
    public static List<StorageServerStub.FluidEntry> collect(UUID storageId) {
        // 用「首次出现」顺序累计：取空的端口以 0 数量占位，
        // 这样流体槽位编号不会因某个流体被取空而整体前移（否则点击会指到别的流体）
        List<StorageServerStub.FluidEntry> result = new ArrayList<>();
        for (StorageFluidPortBlockEntity port : StoragePortManager.liveFluidPorts(storageId)) {
            FluidStack fluid = port.getFluid();
            boolean drained = fluid.isEmpty();
            if (drained) {
                // 取空：用记忆的流体类型占位，数量记 0
                fluid = port.getRememberedFluid();
                if (fluid.isEmpty()) {
                    continue;
                }
            }
            int index = StoragePortManager.indexOfSame(result, fluid);
            if (index < 0) {
                result.add(new StorageServerStub.FluidEntry(fluid.copy(), drained ? 0 : fluid.getAmount()));
            } else if (!drained) {
                StorageServerStub.FluidEntry old = result.get(index);
                result.set(index, new StorageServerStub.FluidEntry(old.icon(), old.amount() + fluid.getAmount()));
            }
        }
        return result;
    }

    /**
     * 按流体身份查找该存储中的条目。
     *
     * <p>供交互使用：客户端与点击之间列表可能变化（端口被拆 / 区块卸载 / 新流体接入），
     * 按下标定位会取到别的流体，故改按 {@link FluidStack#isSameFluidSameComponents} 匹配。</p>
     *
     * @param storageId 存储 ID
     * @param fluid     目标流体
     * @return 匹配的条目；不存在时返回 null
     */
    @Nullable
    public static StorageServerStub.FluidEntry find(UUID storageId, FluidStack fluid) {
        for (StorageServerStub.FluidEntry entry : StoragePortManager.collect(storageId)) {
            if (FluidStack.isSameFluidSameComponents(entry.icon(), fluid)) {
                return entry;
            }
        }
        return null;
    }

    /** 在条目列表中查找同一流体的下标；不存在时返回 -1。 */
    private static int indexOfSame(List<StorageServerStub.FluidEntry> entries, FluidStack fluid) {
        for (int i = 0; i < entries.size(); i++) {
            if (FluidStack.isSameFluidSameComponents(entries.get(i).icon(), fluid)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 从该存储名下持有该流体的端口中抽取指定量，可跨多个端口凑足。
     *
     * @param storageId 存储 ID
     * @param fluid     目标流体（按流体与组件匹配）
     * @param amountMb  期望抽取量（mB）
     * @return 实际抽取量（mB）
     */
    public static int drain(UUID storageId, FluidStack fluid, int amountMb) {
        return StoragePortManager.drain(storageId, fluid, amountMb, false);
    }

    /**
     * {@link #drain(UUID, FluidStack, int)} 的模拟重载：{@code simulate} 为 true 时只统计
     * 可抽取量、不改动任何端口。
     *
     * <p>供「先模拟确认够量、再实际抽取」的调用方使用，避免抽到一半失败留下已抽走的流体。</p>
     *
     * @param simulate 是否只模拟
     * @return 可抽取量 / 实际抽取量（mB）
     */
    public static int drain(UUID storageId, FluidStack fluid, int amountMb, boolean simulate) {
        IFluidHandler.FluidAction action = simulate
            ? IFluidHandler.FluidAction.SIMULATE
            : IFluidHandler.FluidAction.EXECUTE;
        int remaining = amountMb;
        for (StorageFluidPortBlockEntity port : StoragePortManager.liveFluidPorts(storageId)) {
            if (remaining <= 0) {
                break;
            }
            FluidStack stored = port.getFluid();
            if (stored.isEmpty() || !FluidStack.isSameFluidSameComponents(stored, fluid)) {
                continue;
            }
            FluidStack drained = port.getFluidHandler().drain(remaining, action);
            remaining -= drained.getAmount();
        }
        return amountMb - remaining;
    }

    /**
     * 找一个<b>已在存放同种流体</b>的端口处理器，用于把桶装流体自动倾倒进仓储。
     *
     * <p>按 #4792：只倾倒入「有相同流体」的端口；没有对应端口时返回 {@code null}，
     * 由调用方把桶作为普通物品存入。因此本方法<b>不</b>回退到空端口——否则空端口会把
     * 桶装流体直接吃掉，桶再也无法以物品形式入库。</p>
     *
     * @param storageId 存储 ID
     * @param fluid     待倾入的流体
     * @return 可接收的流体处理器；没有存放同种流体的端口时返回 {@code null}
     */
    @Nullable
    public static IFluidHandler findAcceptor(UUID storageId, FluidStack fluid) {
        for (StorageFluidPortBlockEntity port : StoragePortManager.liveFluidPorts(storageId)) {
            FluidStack stored = port.getFluid();
            if (!stored.isEmpty() && FluidStack.isSameFluidSameComponents(stored, fluid)) {
                return port.getFluidHandler();
            }
        }
        return null;
    }

    /**
     * 回滚专用：找一个能装下该流体的端口（同种流体优先，否则任意空端口）。
     *
     * <p>与 {@link #findAcceptor} 的区别在于允许空端口：这里灌回的是先前从端口抽出、
     * 因后续步骤失败而必须归还的流体，其原端口可能已被抽空，若同样只认同种流体，
     * 归还就会失败并凭空丢失流体。</p>
     *
     * @param storageId 存储 ID
     * @param fluid     待灌回的流体
     * @return 可接收的流体处理器；没有合适端口时返回 {@code null}
     */
    @Nullable
    public static IFluidHandler findRefillTarget(UUID storageId, FluidStack fluid) {
        IFluidHandler emptyAcceptor = null;
        for (StorageFluidPortBlockEntity port : StoragePortManager.liveFluidPorts(storageId)) {
            FluidStack stored = port.getFluid();
            if (stored.isEmpty()) {
                if (emptyAcceptor == null) {
                    emptyAcceptor = port.getFluidHandler();
                }
                continue;
            }
            if (FluidStack.isSameFluidSameComponents(stored, fluid)) {
                return port.getFluidHandler();
            }
        }
        return emptyAcceptor;
    }

    /**
     * 取出该存储名下当前已加载的流体端口方块实体。
     *
     * <p>存储可跨维度（超维存储站），故按列取该存储在所有维度下的登记。</p>
     */
    private static List<StorageFluidPortBlockEntity> liveFluidPorts(UUID storageId) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || StoragePortManager.PORTS.isEmpty()) {
            return List.of();
        }
        List<StorageFluidPortBlockEntity> result = new ArrayList<>();
        for (Map.Entry<ResourceKey<Level>, Set<BlockPos>> entry
            : StoragePortManager.PORTS.column(storageId).entrySet()) {
            ServerLevel level = server.getLevel(entry.getKey());
            if (level == null) {
                continue;
            }
            for (BlockPos pos : Set.copyOf(entry.getValue())) {
                if (!level.isLoaded(pos)) {
                    continue;
                }
                if (level.getBlockEntity(pos) instanceof StorageFluidPortBlockEntity port) {
                    result.add(port);
                }
            }
        }
        return result;
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
