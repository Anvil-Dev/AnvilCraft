package dev.dubhe.anvilcraft.util;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class BlockStateRender {
    /**
     * 渲染方块状态
     */
    public static void render(BlockState state, @NotNull GuiGraphics draw, int x, int y) {
        PoseStack poseStack = draw.pose();
        poseStack.pushPose();
        poseStack.translate(x, y, 150.0f);
        float p = 16.0f;
        poseStack.scale(p, p, p);
        poseStack.mulPoseMatrix(new Matrix4f().scaling(1.0f, -1.0f, 1.0f));
        ItemTransform transform = new ItemTransform(
            new Vector3f(30, 210, 0), new Vector3f(0.925f, -0.8125f, 0), new Vector3f(0.625f)
        );
        transform.apply(false, poseStack);
        Minecraft minecraft = Minecraft.getInstance();
        BlockRenderDispatcher blockRenderer = minecraft.getBlockRenderer();
        blockRenderer.renderSingleBlock(
            state,
            poseStack,
            minecraft.renderBuffers().bufferSource(),
            0xF000F0,
            OverlayTexture.NO_OVERLAY
        );
        poseStack.popPose();
        draw.flush();
    }
}
