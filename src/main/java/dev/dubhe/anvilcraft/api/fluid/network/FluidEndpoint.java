package dev.dubhe.anvilcraft.api.fluid.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 流体网络中的一个端点容器。
 *
 * <p>端点是网络的叶子节点：实际存储流体的容器（储罐、鱼缸、门格海绵、创造流体储罐、
 * 其它模组容器等）。管道部件本身不是端点，它们只负责把端点连接成网络。
 *
 * @param containerPos    容器方块的位置（贴着管道的那一格；巨型储罐即其相连 part 的位置）
 * @param fromPipePos     发现该端点的管道位置
 * @param sideToPipe      从容器看向管道的方向（即查询 capability 时使用的 side）
 * @param handler         容器的 {@link IFluidHandler}
 * @param effectiveHeight 等效高度 = 容器 Y + 沿路径累计的泵势场偏移
 * @param cauldron        该端点是否为 NeoForge 注册的炼药锅
 * @param entity          提供该端点的实体；方块容器为 {@code null}
 */
public record FluidEndpoint(
    BlockPos containerPos,
    BlockPos fromPipePos,
    Direction sideToPipe,
    IFluidHandler handler,
    int effectiveHeight,
    boolean cauldron,
    @Nullable Entity entity
) {
    public FluidEndpoint(
        BlockPos containerPos,
        BlockPos fromPipePos,
        Direction sideToPipe,
        IFluidHandler handler,
        int effectiveHeight,
        boolean cauldron
    ) {
        this(containerPos, fromPipePos, sideToPipe, handler, effectiveHeight, cauldron, null);
    }
}
