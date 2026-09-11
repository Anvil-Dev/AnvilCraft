package dev.dubhe.anvilcraft.block.entity.storage;

import dev.dubhe.anvilcraft.block.entity.StorageFluidPortBlockEntity;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * 记录各存储连接到的仓储流体端口，供仓储 UI 查询该存储可显示的流体。
 *
 * <p>流体存放在端口自身（拆除随掉落物保留），因此仓储本体只存物品；UI 需要显示流体时
 * 由本表反查端口并汇总。端口每 {@code VALIDATE_INTERVAL} 重校验连通关系时重新登记，
 * 因此失效条目会被自然修正。</p>
 *
 * <p>表按「维度 → 存储 ID → 端口位置」分层，与 {@link StorageBlockRegistry} 一致：
 * 维度先分层才能避免不同维度的同坐标互相干扰（注册与注销都必须带上维度）。</p>
 */
public final class StorageFluidRegistry {
    /**
     * 流体伪槽位的逻辑编号起点。
     *
     * <p>流体不占存储槽位，用一段远高于真实槽位数的编号与物品槽位区分；
     * 服务端排序与客户端渲染、同步均以此判定「这是流体而非物品」。</p>
     */
    public static final int FLUID_SLOT_BASE = 1 << 24;

    /** 维度 → （存储 ID → 端口位置集合） */
    private static final Map<ResourceKey<Level>, Map<UUID, Set<BlockPos>>> PORTS = new HashMap<>();

    private StorageFluidRegistry() {
    }

    /**
     * 登记一个已连接到某存储的流体端口。
     *
     * @param storageId 存储 ID
     * @param level     端口所在维度
     * @param pos       端口位置
     */
    public static void register(UUID storageId, ServerLevel level, BlockPos pos) {
        StorageFluidRegistry.PORTS
            .computeIfAbsent(level.dimension(), ignored -> new HashMap<>())
            .computeIfAbsent(storageId, ignored -> new HashSet<>())
            .add(pos.immutable());
    }

    /**
     * 注销某端口在「指定存储 × 指定维度」下的登记。
     *
     * <p>只清这一个存储名下的条目：端口从 A 存储改挂到 B 存储时，必须先把 A 的旧条目清掉，
     * 否则 A 的 UI 仍会显示该端口的流体、{@code drain(A)} 还会从属于 B 的端口抽走流体。
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
        Map<UUID, Set<BlockPos>> byId = StorageFluidRegistry.PORTS.get(dimension);
        if (byId == null) {
            return;
        }
        Set<BlockPos> positions = byId.get(storageId);
        if (positions == null) {
            return;
        }
        positions.remove(pos);
        if (positions.isEmpty()) {
            byId.remove(storageId);
        }
        if (byId.isEmpty()) {
            StorageFluidRegistry.PORTS.remove(dimension);
        }
    }

    /** 服务端停止时清表，避免静态表在下次进入世界时残留上次的登记。 */
    public static void clear() {
        StorageFluidRegistry.PORTS.clear();
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
        for (StorageFluidPortBlockEntity port : StorageFluidRegistry.livePorts(storageId)) {
            FluidStack fluid = port.getFluid();
            boolean drained = fluid.isEmpty();
            if (drained) {
                // 取空：用记忆的流体类型占位，数量记 0
                fluid = port.getRememberedFluid();
                if (fluid.isEmpty()) {
                    continue;
                }
            }
            int index = StorageFluidRegistry.indexOfSame(result, fluid);
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
        for (StorageServerStub.FluidEntry entry : StorageFluidRegistry.collect(storageId)) {
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
        return StorageFluidRegistry.drain(storageId, fluid, amountMb, false);
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
        for (StorageFluidPortBlockEntity port : StorageFluidRegistry.livePorts(storageId)) {
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
        for (StorageFluidPortBlockEntity port : StorageFluidRegistry.livePorts(storageId)) {
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
        for (StorageFluidPortBlockEntity port : StorageFluidRegistry.livePorts(storageId)) {
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
     * 取出该存储名下当前已加载的端口方块实体。
     *
     * <p>存储可跨维度（超维存储站），故遍历所有维度查找该存储 ID 的登记。</p>
     */
    private static List<StorageFluidPortBlockEntity> livePorts(UUID storageId) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || StorageFluidRegistry.PORTS.isEmpty()) {
            return List.of();
        }
        List<StorageFluidPortBlockEntity> result = new ArrayList<>();
        for (Map.Entry<ResourceKey<Level>, Map<UUID, Set<BlockPos>>> byDimension
            : StorageFluidRegistry.PORTS.entrySet()) {
            Set<BlockPos> positions = byDimension.getValue().get(storageId);
            if (positions == null || positions.isEmpty()) {
                continue;
            }
            ServerLevel level = server.getLevel(byDimension.getKey());
            if (level == null) {
                continue;
            }
            for (BlockPos pos : Set.copyOf(positions)) {
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
     * 某存储当前登记的全部端口位置，供测试与调试使用。
     *
     * @param storageId 存储 ID
     * @return 端口位置集合（跨维度取并集）
     */
    public static Set<BlockPos> positions(UUID storageId) {
        Set<BlockPos> result = new HashSet<>();
        for (Map<UUID, Set<BlockPos>> byId : StorageFluidRegistry.PORTS.values()) {
            Set<BlockPos> positions = byId.get(storageId);
            if (positions != null) {
                result.addAll(positions);
            }
        }
        return Set.copyOf(result);
    }
}
