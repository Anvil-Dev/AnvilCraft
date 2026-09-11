package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.entity.StorageFluidPortBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.FluidTankRenderUtil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * 仓储流体端口渲染器：在中央玻璃窗口内渲染内部流体，液体按液面高度、气体按不透明度表示余量。
 */
public class StorageFluidPortBlockEntityRenderer implements BlockEntityRenderer<StorageFluidPortBlockEntity> {
    /** 中央玻璃窗口是 2..14，比储罐更小，故在默认内缩基础上再收 1.5 像素 */
    private static final float WINDOW_INSET_PIXELS = 1.5f;

    public StorageFluidPortBlockEntityRenderer(BlockEntityRendererProvider.Context ignore) {
    }

    @Override
    public void render(
        StorageFluidPortBlockEntity port,
        float tickDelta,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int light,
        int overlay
    ) {
        FluidStack fluid = port.getFluid();
        if (fluid.isEmpty()) return;
        float fill = Mth.clamp((float) fluid.getAmount() / StorageFluidPortBlockEntity.CAPACITY_MB, 0, 1);
        FluidTankRenderUtil.drawFluidInTank(
            poseStack, bufferSource, light, fluid, fill, WINDOW_INSET_PIXELS
        );
    }
}
