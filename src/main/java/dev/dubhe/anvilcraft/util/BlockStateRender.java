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

    public static void renderBlock(
        BlockState state,
        int posY,
        @NotNull GuiGraphics draw,
        float size
    ) {
        PoseStack poseStack = draw.pose();
        poseStack.pushPose();
        poseStack.translate(0, 0, 150.0f);
        poseStack.scale(size, size, size);
        poseStack.mulPose(new Matrix4f().scaling(1.0f, -1.0f, 1.0f));
        ItemTransform transform = new ItemTransform(
            new Vector3f(30, 210, 0), new Vector3f(0.925f, -0.8125f, 0), new Vector3f(0.625f)
        );
        transform.apply(false, poseStack);
        poseStack.translate(0, posY, 0);
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

    public static void render(BlockState state, @NotNull GuiGraphics draw, int x, int y) {
        BlockStateRender.render(state, draw, x, y, 16);
    }

    /**
     * 渲染方块状态
     */
    public static void render(BlockState state, @NotNull GuiGraphics draw, int x, int y, float size) {
        PoseStack poseStack = draw.pose();
        poseStack.pushPose();
        poseStack.translate(x, y, 150.0f);
        poseStack.scale(size, size, size);
        poseStack.mulPose(new Matrix4f().scaling(1.0f, -1.0f, 1.0f));
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
