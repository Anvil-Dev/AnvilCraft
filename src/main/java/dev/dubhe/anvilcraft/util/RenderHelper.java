package dev.dubhe.anvilcraft.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.joml.Vector3f;

public class RenderHelper {
    private static final Vector3f L1 = new Vector3f(0.4F, 0.0F, 1.0F).normalize();
    private static final Vector3f L2 = new Vector3f(-0.4F, 1.0F, -0.2F).normalize();

    public static final BlockRenderFunction SINGLE_BLOCK = (block, poseStack, buffers) -> Minecraft.getInstance()
            .getBlockRenderer()
            .renderSingleBlock(block, poseStack, buffers, 0xF000F0, OverlayTexture.NO_OVERLAY);

    public static void renderBlock(
            GuiGraphics guiGraphics, BlockState block, float x, float y, float z, float scale, BlockRenderFunction fn) {
        PoseStack poseStack = guiGraphics.pose();

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.scale(-scale, -scale, -scale);
        poseStack.translate(-0.5f, -0.5f, 0);
        poseStack.mulPose(Axis.XP.rotationDegrees(-30F));
        poseStack.translate(0.5F, 0, -0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(45f));
        poseStack.translate(-0.5F, 0, 0.5F);

        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        poseStack.translate(0, 0, -1);

        FluidState fluidState = block.getFluidState();
        if (fluidState.isEmpty()) {
            MultiBufferSource.BufferSource buffers =
                    Minecraft.getInstance().renderBuffers().bufferSource();

            RenderSystem.setupGui3DDiffuseLighting(L1, L2);
            fn.renderBlock(block, poseStack, buffers);

            buffers.endBatch();
        }

        poseStack.popPose();
    }

    @FunctionalInterface
    public interface BlockRenderFunction {
        void renderBlock(BlockState block, PoseStack poseStack, MultiBufferSource.BufferSource buffers);
    }
}
