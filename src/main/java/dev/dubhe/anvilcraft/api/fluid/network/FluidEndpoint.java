package dev.dubhe.anvilcraft.api.fluid.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.Nullable;

/**
 * 流体网络中的一个端点容器。
 *
 * @param containerPos    容器方块的位置
 * @param fromPipePos     发现该端点的管道位置
 * @param sideToPipe      从容器看向管道的方向
 * @param handler         容器的流体处理器
 * @param effectiveHeight 等效高度 = 容器 Y + 沿路径累计的泵势场偏移
 * @param cauldron        该端点是否为 NeoForge 注册的炼药锅
 * @param entity          提供该端点的实体；方块容器为 {@code null}
 */
public record FluidEndpoint(
    BlockPos containerPos,
    BlockPos fromPipePos,
    Direction sideToPipe,
    ResourceHandler<FluidResource> handler,
    int effectiveHeight,
    boolean cauldron,
    @Nullable Entity entity
) {
    public FluidEndpoint(
        BlockPos containerPos,
        BlockPos fromPipePos,
        Direction sideToPipe,
        ResourceHandler<FluidResource> handler,
        int effectiveHeight,
        boolean cauldron
    ) {
        this(containerPos, fromPipePos, sideToPipe, handler, effectiveHeight, cauldron, null);
    }
}
