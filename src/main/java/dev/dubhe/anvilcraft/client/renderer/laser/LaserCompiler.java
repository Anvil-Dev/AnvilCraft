package dev.dubhe.anvilcraft.client.renderer.laser;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import java.util.function.Function;

public class LaserCompiler {
    public static final float[] LASER_WIDTH;
    public static final float PIXEL = 1 / 16f;
    public static final float HALF_PIXEL = PIXEL / 2f;

    static {
        float[] array = new float[65];
        for (int i = 1; i <= 64; i++) {
            array[i] = (float) Math.sqrt(i) / 2f * PIXEL;
        }
        LASER_WIDTH = array;
    }

    public static float laserWidth(LaserState state) {
        return LASER_WIDTH[Math.clamp(state.blockEntity().getLaserLevel(), 1, 64)] + 0.001f;
    }

    public static void compile(
        LaserState state,
        Function<RenderType, VertexConsumer> bufferBuilderFunction
    ) {
        if (state.laserLevel() <= 0) return;
        float width = laserWidth(state);
        renderBox(
            bufferBuilderFunction.apply(RenderType.solid()),
            state.pose(),
            -width,
            -state.offset() - 0.001f,
            -width,
            width,
            state.length() + 0.501f,
            width,
            1f,
            state.laserAtlasSprite(),
            state.concreteAtlasSprite()
        );
        float haloWidth = width + HALF_PIXEL;
        renderBox(
            bufferBuilderFunction.apply(ModRenderTypes.LASER),
            state.pose(),
            -haloWidth,
            -state.offset(),
            -haloWidth,
            haloWidth,
            state.length() + 0.5f + HALF_PIXEL,
            haloWidth,
            0.6f,
            state.laserAtlasSprite(),
            state.concreteAtlasSprite()
        );
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
        float a,
        TextureAtlasSprite sprite,
        TextureAtlasSprite endSprite) {
        renderQuadX(consumer, pose, maxX, maxX, minY, minZ, maxY, maxZ, a, sprite);
        renderQuadX(consumer, pose, minX, minX, minY, maxZ, maxY, minZ, a, sprite);
        renderQuadY(consumer, pose, maxY, maxY, minX, minZ, maxX, maxZ, a - 0.25f, endSprite);
        //renderQuadY(consumer, pose, minY, minY, maxX, minZ, minX, maxZ, a, endSprite);
        renderQuadZ(consumer, pose, maxZ, maxZ, minX, maxY, maxX, minY, a, sprite);
        renderQuadZ(consumer, pose, minZ, minZ, minX, minY, maxX, maxY, a, sprite);
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
        float a,
        TextureAtlasSprite sprite) {
        addVertex(consumer, pose, minX, maxY, minZ, sprite.getU1(), sprite.getV1(), a);
        addVertex(consumer, pose, minX, maxY, maxZ, sprite.getU0(), sprite.getV1(), a);
        addVertex(consumer, pose, maxX, minY, maxZ, sprite.getU0(), sprite.getV0(), a);
        addVertex(consumer, pose, maxX, minY, minZ, sprite.getU1(), sprite.getV0(), a);
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
        float a,
        TextureAtlasSprite sprite) {
        addVertex(consumer, pose, minX, minY, minZ, sprite.getU1(), sprite.getV1(), a);
        addVertex(consumer, pose, minX, minY, maxZ, sprite.getU0(), sprite.getV1(), a);
        addVertex(consumer, pose, maxX, maxY, maxZ, sprite.getU0(), sprite.getV0(), a);
        addVertex(consumer, pose, maxX, maxY, minZ, sprite.getU1(), sprite.getV0(), a);
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
        float a,
        TextureAtlasSprite sprite) {
        addVertex(consumer, pose, minX, maxY, minZ, sprite.getU1(), sprite.getV1(), a);
        addVertex(consumer, pose, maxX, maxY, minZ, sprite.getU0(), sprite.getV1(), a);
        addVertex(consumer, pose, maxX, minY, maxZ, sprite.getU0(), sprite.getV0(), a);
        addVertex(consumer, pose, minX, minY, maxZ, sprite.getU1(), sprite.getV0(), a);
    }

    private static void addVertex(
        VertexConsumer consumer,
        PoseStack.Pose pose,
        float x,
        float y,
        float z,
        float u,
        float v,
        float a) {
        consumer.addVertex(pose.pose(), x, y, z)
            .setColor(1f, .05f, .05f, a)
            .setUv(u, v)
            .setUv1(0, 0)
            .setUv2(240, 240)
            .setNormal(1, 0, 0);
    }


}
