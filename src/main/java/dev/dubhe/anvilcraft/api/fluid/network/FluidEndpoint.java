package dev.dubhe.anvilcraft.api.fluid.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

/**
 * 流体网络中的一个端点容器。
 *
 * @param containerPos    容器方块的位置
 * @param fromPipePos     发现该端点的管道位置
 * @param sideToPipe      从容器看向管道的方向
 * @param handler         容器的流体处理器
 * @param effectiveHeight 等效高度 = 容器 Y + 沿路径累计的泵势场偏移
 */
public record FluidEndpoint(
    BlockPos containerPos,
    BlockPos fromPipePos,
    Direction sideToPipe,
    ResourceHandler<FluidResource> handler,
    int effectiveHeight
) {
}
