package dev.dubhe.anvilcraft.util;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.Iterator;

public class BlockStateRender {
    /**
     * 渲染方块状态
     */

    public static void renderBlocks(
            BlockState[] states,
            Vec2[] vec2s,
            @NotNull GuiGraphics draw,
            float p) {
        if (states.length != vec2s.length) return;
        PoseStack poseStack = draw.pose();
        poseStack.pushPose();
        poseStack.translate(0, 0, 150.0f);
        poseStack.scale(p, p, p);
        poseStack.mulPoseMatrix(new Matrix4f().scaling(1.0f, -1.0f, 1.0f));
        ItemTransform transform = new ItemTransform(
                new Vector3f(30, 210, 0), new Vector3f(0.925f, -0.8125f, 0), new Vector3f(0.625f)
        );
        transform.apply(false, poseStack);
        Iterator<Vec2> blockPosesIterator = Arrays.stream(vec2s).iterator();
        Vec2 vec2;
        for (BlockState state : states) {
            vec2 = blockPosesIterator.next();
            poseStack.translate(vec2.x, vec2.y, 0);
            Minecraft minecraft = Minecraft.getInstance();
            BlockRenderDispatcher blockRenderer = minecraft.getBlockRenderer();
            blockRenderer.renderSingleBlock(
                    state,
                    poseStack,
                    minecraft.renderBuffers().bufferSource(),
                    0xF000F0,
                    OverlayTexture.NO_OVERLAY
            );
            poseStack.translate(-vec2.x, -vec2.y, 0);
        }
        poseStack.popPose();
        draw.flush();
    }

    public static void renderBlock(BlockState state, @NotNull GuiGraphics draw, int x, int y, float p) {
        renderBlocks(new BlockState[]{state}, new Vec2[]{Vec2.ZERO}, draw, p);
    }
}
