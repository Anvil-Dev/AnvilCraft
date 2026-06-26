package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.entity.ExpCollectorBlockEntity;
import dev.dubhe.anvilcraft.client.support.FluidRenderHelper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public class ExpCollectorBlockEntityRenderer implements BlockEntityRenderer<ExpCollectorBlockEntity> {
    public ExpCollectorBlockEntityRenderer(BlockEntityRendererProvider.Context ignore) {
    }

    @Override
    public void render(
        ExpCollectorBlockEntity blockEntity,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedLight,
        int packedOverlay
    ) {
        IFluidHandler fluidHandler = blockEntity.getFluidHandler();
        FluidStack fluidStack = fluidHandler.getFluidInTank(0);
        if (fluidStack.isEmpty()) {
            return;
        }

        float fillRatio = (float) fluidStack.getAmount() / fluidHandler.getTankCapacity(0);
        float fill = 0.15f + 0.6f * fillRatio;

        FluidRenderHelper.INSTANCE.renderFluidBox(
            fluidStack,
            0.07f, 0.15f, 0.07f,
            0.93f, fill, 0.93f,
            bufferSource, poseStack, packedLight,
            true, false
        );
    }
}
