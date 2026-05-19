package dev.dubhe.anvilcraft.client.renderer.laser;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.LaserRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ARGB;

public class LaserCompiler {
    public static final float[] LASER_WIDTH;
    public static final float PIXEL = 1 / 16F;
    public static final float HALF_PIXEL = PIXEL / 2F;

    static {
        float[] array = new float[65];
        for (int i = 1; i <= 64; i++) {
            array[i] = (float) Math.sqrt(i) / 2F * PIXEL;
        }
        LASER_WIDTH = array;
    }

    public static float laserWidth(LaserRenderState state) {
        return LASER_WIDTH[Math.clamp(state.laserLevel, 1, 64)] + 0.001F;
    }

    public static void submit(
        PoseStack poseStack,
        LaserRenderState state,
        SubmitNodeCollector nodeCollector,
        boolean bloomed
    ) {
        if (state.laserLevel <= 0) return;
        float width = laserWidth(state);
        nodeCollector.submitCustomGeometry(
            poseStack,
            ModRenderTypes.LASER_SOLID,
            (pose, buffer) -> renderBox(
                buffer,
                pose,
                -width,
                -state.offset - 0.001F,
                -width,
                width,
                state.length + 0.501F,
                width,
                ARGB.color(1, state.color),
                state.laserAtlasSprite,
                state.solidAtlasSprite
            )
        );
        nodeCollector.submitCustomGeometry(
            poseStack,
            bloomed ? ModRenderTypes.LASER_TRANSLUCENT_BLOOM : ModRenderTypes.LASER_TRANSLUCENT,
            ((pose, buffer) -> {
            float outerWidth = width + HALF_PIXEL;
            renderBox(
                buffer,
                pose,
                -outerWidth,
                -state.offset,
                -outerWidth,
                outerWidth,
                state.length + 0.5F + HALF_PIXEL,
                outerWidth,
                ARGB.color(0.6f, state.color),
                state.laserAtlasSprite,
                state.solidAtlasSprite
            );
        }));
    }

    private static void renderBox(
        VertexConsumer consumer,
        PoseStack.Pose pose,
        float minX,
        float minY,
        float minZ,
        float maxX,
        float maxY,
        float maxZ,
        int color,
        TextureAtlasSprite sprite,
        TextureAtlasSprite endSprite
    ) {
        renderQuadX(consumer, pose, maxX, maxX, minY, minZ, maxY, maxZ, color, sprite);
        renderQuadX(consumer, pose, minX, minX, minY, maxZ, maxY, minZ, color, sprite);
        renderQuadY(consumer, pose, maxY, maxY, minX, minZ, maxX, maxZ, ARGB.color(0.35f, color), endSprite);
        // renderQuadY(consumer, pose, minY, minY, maxX, minZ, minX, maxZ, color, endSprite);
        renderQuadZ(consumer, pose, maxZ, maxZ, minX, maxY, maxX, minY, color, sprite);
        renderQuadZ(consumer, pose, minZ, minZ, minX, minY, maxX, maxY, color, sprite);
    }

    private static void renderQuadX(
        VertexConsumer consumer,
        PoseStack.Pose pose,
        float minX,
        float maxX,
        float minY,
        float minZ,
        float maxY,
        float maxZ,
        int color,
        TextureAtlasSprite sprite
    ) {
        addVertex(consumer, pose, minX, maxY, minZ, sprite.getU1(), sprite.getV1(), color);
        addVertex(consumer, pose, minX, maxY, maxZ, sprite.getU0(), sprite.getV1(), color);
        addVertex(consumer, pose, maxX, minY, maxZ, sprite.getU0(), sprite.getV0(), color);
        addVertex(consumer, pose, maxX, minY, minZ, sprite.getU1(), sprite.getV0(), color);
    }

    private static void renderQuadY(
        VertexConsumer consumer,
        PoseStack.Pose pose,
        float minY,
        float maxY,
        float minX,
        float minZ,
        float maxX,
        float maxZ,
        int color,
        TextureAtlasSprite sprite
    ) {
        addVertex(consumer, pose, minX, minY, minZ, sprite.getU1(), sprite.getV1(), color);
        addVertex(consumer, pose, minX, minY, maxZ, sprite.getU0(), sprite.getV1(), color);
        addVertex(consumer, pose, maxX, maxY, maxZ, sprite.getU0(), sprite.getV0(), color);
        addVertex(consumer, pose, maxX, maxY, minZ, sprite.getU1(), sprite.getV0(), color);
    }

    private static void renderQuadZ(
        VertexConsumer consumer,
        PoseStack.Pose pose,
        float minZ,
        float maxZ,
        float minX,
        float minY,
        float maxX,
        float maxY,
        int color,
        TextureAtlasSprite sprite
    ) {
        addVertex(consumer, pose, minX, maxY, minZ, sprite.getU1(), sprite.getV1(), color);
        addVertex(consumer, pose, maxX, maxY, minZ, sprite.getU0(), sprite.getV1(), color);
        addVertex(consumer, pose, maxX, minY, maxZ, sprite.getU0(), sprite.getV0(), color);
        addVertex(consumer, pose, minX, minY, maxZ, sprite.getU1(), sprite.getV0(), color);
    }

    private static void addVertex(
        VertexConsumer consumer,
        PoseStack.Pose pose,
        float x,
        float y,
        float z,
        float u,
        float v,
        int color
    ) {
        consumer.addVertex(pose.pose(), x, y, z)
            .setColor(color)
            .setUv(u, v)
            .setUv1(0, 0)
            .setUv2(240, 240)
            .setNormal(1, 0, 0);
    }
}
