package dev.dubhe.anvilcraft.client.renderer.laser;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.state.LensType;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

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
        return laserWidth(state.laserLevel());
    }

    public static float laserWidth(int laserLevel) {
        return LASER_WIDTH[Math.clamp(laserLevel, 1, 64)] + 0.001f;
    }

    public static void compile(
        LaserState state,
        Function<RenderType, VertexConsumer> bufferBuilderFunction
    ) {
        if (state.laserLevel() <= 0) return;
        float width = laserWidth(state);

        // Pre-compute colors: core is always red (or purple for gamma), halo depends on lens
        float[] coreColor;
        float[] haloColor;
        if (state.gamma()) {
            coreColor = new float[]{0.6f, 0.1f, 1.0f};
            haloColor = new float[]{0.6f, 0.1f, 1.0f};
        } else {
            coreColor = new float[]{1.0f, 0.05f, 0.05f};
            haloColor = getLensColor(state.lensType());
        }

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
            state.concreteAtlasSprite(),
            coreColor
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
            state.concreteAtlasSprite(),
            haloColor
        );
    }

    @SuppressWarnings("deprecation")
    public static void compileWeaponBeam(
        PoseStack.Pose pose,
        float length,
        int laserLevel,
        Function<RenderType, VertexConsumer> bufferBuilderFunction
    ) {
        if (laserLevel <= 0 || length <= 0.0f) return;
        var spriteGetter = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS);
        TextureAtlasSprite laserSprite = spriteGetter.apply(AnvilCraft.of("block/laser"));
        TextureAtlasSprite endSprite = spriteGetter.apply(ResourceLocation.withDefaultNamespace("block/white_concrete"));
        float width = laserWidth(laserLevel);
        float[] color = new float[]{1.0f, 0.05f, 0.05f};
        VertexConsumer coreConsumer = bufferBuilderFunction.apply(RenderType.solid());
        renderBox(
            coreConsumer,
            pose,
            -width,
            0.0f,
            -width,
            width,
            length,
            width,
            1.0f,
            laserSprite,
            endSprite,
            color
        );
        renderQuadY(
            coreConsumer,
            pose,
            0.0f,
            0.0f,
            width,
            -width,
            -width,
            width,
            1.0f,
            endSprite,
            color
        );
        float haloWidth = width + HALF_PIXEL;
        VertexConsumer haloConsumer = bufferBuilderFunction.apply(ModRenderTypes.LASER);
        renderBox(
            haloConsumer,
            pose,
            -haloWidth,
            0.0f,
            -haloWidth,
            haloWidth,
            length + HALF_PIXEL,
            haloWidth,
            0.6f,
            laserSprite,
            endSprite,
            color
        );
        renderQuadY(
            haloConsumer,
            pose,
            0.0f,
            0.0f,
            haloWidth,
            -haloWidth,
            -haloWidth,
            haloWidth,
            0.6f,
            endSprite,
            color
        );
    }

    private static float[] getLensColor(LensType type) {
        return switch (type) {
            case ROYAL -> new float[]{0.0f, 1.0f, 0.75f};
            case FROST -> new float[]{0.35f, 0.55f, 1.0f};
            case EMBER -> new float[]{1.0f, 0.85f, 0.0f};
            default -> new float[]{1.0f, 0.05f, 0.05f};
        };
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
        TextureAtlasSprite endSprite,
        float[] color) {
        renderQuadX(consumer, pose, maxX, maxX, minY, minZ, maxY, maxZ, a, sprite, color);
        renderQuadX(consumer, pose, minX, minX, minY, maxZ, maxY, minZ, a, sprite, color);
        renderQuadY(consumer, pose, maxY, maxY, minX, minZ, maxX, maxZ, a - 0.25f, endSprite, color);
        // renderQuadY(consumer, pose, minY, minY, maxX, minZ, minX, maxZ, a, endSprite);
        renderQuadZ(consumer, pose, maxZ, maxZ, minX, maxY, maxX, minY, a, sprite, color);
        renderQuadZ(consumer, pose, minZ, minZ, minX, minY, maxX, maxY, a, sprite, color);
    }

    private static void renderQuadX(
        VertexConsumer consumer,
        PoseStack.Pose pose,
        float minX, float maxX, float minY, float minZ, float maxY, float maxZ,
        float a, TextureAtlasSprite sprite, float[] color) {
        addVertex(consumer, pose, minX, maxY, minZ, sprite.getU1(), sprite.getV1(), a, color);
        addVertex(consumer, pose, minX, maxY, maxZ, sprite.getU0(), sprite.getV1(), a, color);
        addVertex(consumer, pose, maxX, minY, maxZ, sprite.getU0(), sprite.getV0(), a, color);
        addVertex(consumer, pose, maxX, minY, minZ, sprite.getU1(), sprite.getV0(), a, color);
    }

    private static void renderQuadY(
        VertexConsumer consumer,
        PoseStack.Pose pose,
        float minY, float maxY, float minX, float minZ, float maxX, float maxZ,
        float a, TextureAtlasSprite sprite, float[] color) {
        addVertex(consumer, pose, minX, minY, minZ, sprite.getU1(), sprite.getV1(), a, color);
        addVertex(consumer, pose, minX, minY, maxZ, sprite.getU0(), sprite.getV1(), a, color);
        addVertex(consumer, pose, maxX, maxY, maxZ, sprite.getU0(), sprite.getV0(), a, color);
        addVertex(consumer, pose, maxX, maxY, minZ, sprite.getU1(), sprite.getV0(), a, color);
    }

    private static void renderQuadZ(
        VertexConsumer consumer,
        PoseStack.Pose pose,
        float minZ, float maxZ, float minX, float minY, float maxX, float maxY,
        float a, TextureAtlasSprite sprite, float[] color) {
        addVertex(consumer, pose, minX, maxY, minZ, sprite.getU1(), sprite.getV1(), a, color);
        addVertex(consumer, pose, maxX, maxY, minZ, sprite.getU0(), sprite.getV1(), a, color);
        addVertex(consumer, pose, maxX, minY, maxZ, sprite.getU0(), sprite.getV0(), a, color);
        addVertex(consumer, pose, minX, minY, maxZ, sprite.getU1(), sprite.getV0(), a, color);
    }

    private static void addVertex(
        VertexConsumer consumer,
        PoseStack.Pose pose,
        float x, float y, float z,
        float u, float v, float a,
        float[] color) {
        consumer.addVertex(pose.pose(), x, y, z)
            .setColor(color[0], color[1], color[2], a)
            .setUv(u, v)
            .setUv1(0, 0)
            .setUv2(240, 240)
            .setNormal(1, 0, 0);
    }
}
