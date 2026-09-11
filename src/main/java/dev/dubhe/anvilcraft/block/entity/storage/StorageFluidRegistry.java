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
 */
public final class StorageFluidRegistry {
    /**
     * 流体伪槽位的逻辑编号起点。
     *
     * <p>流体不占存储槽位，用一段远高于真实槽位数的编号与物品槽位区分；
     * 服务端排序与客户端渲染、同步均以此判定「这是流体而非物品」。</p>
     */
    public static final int FLUID_SLOT_BASE = 1 << 24;

    /** 存储 ID → （端口位置 → 维度） */
    private static final Map<UUID, Map<BlockPos, ResourceKey<Level>>> PORTS = new HashMap<>();

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
            .computeIfAbsent(storageId, ignored -> new HashMap<>())
            .put(pos.immutable(), level.dimension());
    }

    /**
     * 注销某位置在所有存储下的登记（端口失效、被拆除或断开连接时调用）。
     *
     * @param pos 端口位置
     */
    public static void unregister(BlockPos pos) {
        for (Map<BlockPos, ResourceKey<Level>> ports : StorageFluidRegistry.PORTS.values()) {
            ports.remove(pos);
        }
        StorageFluidRegistry.PORTS.entrySet().removeIf(entry -> entry.getValue().isEmpty());
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
        int remaining = amountMb;
        for (StorageFluidPortBlockEntity port : StorageFluidRegistry.livePorts(storageId)) {
            if (remaining <= 0) {
                break;
            }
            FluidStack stored = port.getFluid();
            if (stored.isEmpty() || !FluidStack.isSameFluidSameComponents(stored, fluid)) {
                continue;
            }
            FluidStack drained = port.getFluidHandler().drain(remaining, IFluidHandler.FluidAction.EXECUTE);
            remaining -= drained.getAmount();
        }
        return amountMb - remaining;
    }

    /**
     * 找一个能接收该流体的端口处理器，用于把桶装流体自动倾倒进仓储。
     *
     * <p>优先返回已在存放同种流体的端口；否则退回任意空端口。</p>
     *
     * @param storageId 存储 ID
     * @param fluid     待倾入的流体
     * @return 可接收的流体处理器；没有合适端口时返回 {@code null}
     */
    @Nullable
    public static IFluidHandler findAcceptor(UUID storageId, FluidStack fluid) {
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
     */
    private static List<StorageFluidPortBlockEntity> livePorts(UUID storageId) {
        Map<BlockPos, ResourceKey<Level>> ports = StorageFluidRegistry.PORTS.get(storageId);
        if (ports == null || ports.isEmpty()) {
            return List.of();
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return List.of();
        }
        List<StorageFluidPortBlockEntity> result = new ArrayList<>();
        for (Map.Entry<BlockPos, ResourceKey<Level>> entry : Map.copyOf(ports).entrySet()) {
            ServerLevel level = server.getLevel(entry.getValue());
            if (level == null) {
                continue;
            }
            BlockPos pos = entry.getKey();
            if (!level.isLoaded(pos)) {
                continue;
            }
            if (level.getBlockEntity(pos) instanceof StorageFluidPortBlockEntity port) {
                result.add(port);
            }
        }
        return result;
    }

    /**
     * 某存储当前登记的全部端口位置，供测试与调试使用。
     *
     * @param storageId 存储 ID
     * @return 端口位置集合
     */
    public static Set<BlockPos> positions(UUID storageId) {
        Map<BlockPos, ResourceKey<Level>> ports = StorageFluidRegistry.PORTS.get(storageId);
        return ports == null ? Set.of() : Set.copyOf(ports.keySet());
    }
}
