package dev.dubhe.anvilcraft.client.renderer.blockentity.celestial;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyClass;
import dev.dubhe.anvilcraft.block.entity.celestial.StarData;
import dev.dubhe.anvilcraft.block.entity.celestial.StellarVisualState;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.model.data.ModelData;

import javax.annotation.Nullable;

final class VanillaCelestialRenderer {
    private VanillaCelestialRenderer() {
    }

    static void star(
        StarData star, BakedModel model, PoseStack pose, MultiBufferSource buffers, int overlay, @Nullable StellarVisualState visual
    ) {
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffers.getBuffer(RenderType.cutout()),
            null, model, 1, 1, 1, LightTexture.FULL_BRIGHT, overlay);
        if (star.bodyClass() == CelestialBodyClass.NEUTRON_STAR) return;
        float[] color = visual == null ? CelestialBodyTextureBakery.starColor(star) : visual.surfaceColorComponents();
        float emission = visual == null ? 1.0f
            : Math.clamp(0.85f + 0.15f * (float) Math.log10(1.0f + visual.luminosity()), 0.85f, 1.25f);
        shell(pose, buffers, ModRenderTypes.STAR_COLOR_OVERLAY, color, 1.005f, 1.0f, overlay);
        for (int layer = 0; layer < 10; layer++) {
            float progress = layer / 10.0f;
            shell(pose, buffers, ModRenderTypes.CELESTIAL_ATMOSPHERE, color, 1.0f + progress * 0.6f,
                emission * (1.2f - 1.125f * progress) / 10.0f, overlay);
        }
    }

    static void atmosphere(PoseStack pose, MultiBufferSource buffers, float[] color, int overlay) {
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.scale(1.125f, 1.125f, 1.125f);
        pose.translate(-0.5, -0.5, -0.5);
        CelestialBodyRenderer.renderAtmosphere(pose, buffers, color, LightTexture.FULL_BRIGHT, overlay, 42L);
        pose.popPose();
    }

    static void brownDwarf(PoseStack pose, MultiBufferSource buffers, int overlay) {
        float[] color = {1.0f, 0.3f, 0.1f};
        for (int layer = 0; layer < 3; layer++) {
            float progress = layer / 3.0f;
            shell(pose, buffers, ModRenderTypes.CELESTIAL_ATMOSPHERE, color, 1.15f + progress * 0.25f,
                (0.45f - 0.38f * progress) / 3.0f, overlay);
        }
    }

    private static void shell(
        PoseStack pose, MultiBufferSource buffers, RenderType type, float[] color, float scale, float alpha, int overlay
    ) {
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.scale(scale, scale, scale);
        pose.translate(-0.5, -0.5, -0.5);
        BakedModel cube = Minecraft.getInstance().getBlockRenderer().getBlockModel(Blocks.WHITE_CONCRETE.defaultBlockState());
        VertexConsumer consumer = buffers.getBuffer(type);
        RandomSource random = RandomSource.create(42L);
        for (Direction direction : Direction.values()) {
            for (BakedQuad quad : cube.getQuads(null, direction, random, ModelData.EMPTY, null)) {
                consumer.putBulkData(pose.last(), quad, color[0], color[1], color[2], alpha, LightTexture.FULL_BRIGHT, overlay);
            }
        }
        for (BakedQuad quad : cube.getQuads(null, null, random, ModelData.EMPTY, null)) {
            consumer.putBulkData(pose.last(), quad, color[0], color[1], color[2], alpha, LightTexture.FULL_BRIGHT, overlay);
        }
        pose.popPose();
    }
}
