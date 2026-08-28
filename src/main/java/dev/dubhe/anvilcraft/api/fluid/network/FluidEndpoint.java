package dev.dubhe.anvilcraft.api.fluid.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 流体网络中的一个端点容器。
 *
 * <p>端点是网络的叶子节点：实际存储流体的容器（储罐、鱼缸、门格海绵、创造流体储罐、
 * 其它模组容器等）。管道部件本身不是端点，它们只负责把端点连接成网络。
 *
 * @param containerPos 容器方块的位置（贴着管道的那一格；巨型储罐即其相连 part 的位置）
 * @param entries      该容器实际接入管道的所有入口
 * @param handler      容器的 {@link IFluidHandler}
 * @param cauldron     该端点是否为 NeoForge 注册的炼药锅
 * @param entity       提供该端点的实体；方块容器为 {@code null}
 */
public record FluidEndpoint(
    BlockPos containerPos,
    List<Entry> entries,
    IFluidHandler handler,
    boolean cauldron,
    @Nullable Entity entity
) {
    public FluidEndpoint(
        BlockPos containerPos,
        Entry entry,
        IFluidHandler handler,
        boolean cauldron,
        @Nullable Entity entity
    ) {
        this(containerPos, List.of(entry), handler, cauldron, entity);
    }

    public FluidEndpoint(
        BlockPos containerPos,
        BlockPos fromPipePos,
        Direction sideToPipe,
        IFluidHandler handler,
        int effectiveHeight,
        boolean cauldron
    ) {
        this(containerPos, new Entry(fromPipePos, sideToPipe, effectiveHeight), handler, cauldron, null);
    }

    /** 增加一个接入点后返回新的端点，避免改变已有端点对象。 */
    public FluidEndpoint withEntry(BlockPos pipePos, Direction sideToPipe, int effectiveHeight) {
        List<Entry> merged = new ArrayList<>(entries);
        merged.add(new Entry(pipePos, sideToPipe, effectiveHeight));
        return new FluidEndpoint(containerPos, merged, handler, cauldron, entity);
    }

    /** 第一个接入点（外部推送等仍使用单入口语义时的兼容入口）。 */
    public Entry primaryEntry() {
        return entries.get(0);
    }

    /** 兼容便捷方法：取第一个入口的等效高度。 */
    public int effectiveHeight() {
        return entries.get(0).effectiveHeight();
    }

    /** 端点容器的一个实际管道接入点。 */
    public record Entry(BlockPos fromPipePos, Direction sideToPipe, int effectiveHeight) {
    }
}
