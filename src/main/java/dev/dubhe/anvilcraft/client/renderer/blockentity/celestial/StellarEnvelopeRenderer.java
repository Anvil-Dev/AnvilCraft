package dev.dubhe.anvilcraft.client.renderer.blockentity.celestial;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.block.entity.celestial.StellarEventProfile;
import dev.dubhe.anvilcraft.block.entity.celestial.StellarVisualState;
import dev.dubhe.anvilcraft.client.init.ModRenderPipelines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

/** 与恒星本体独立缩放的立方体风层、核心层和抛射壳。 */
public final class StellarEnvelopeRenderer {
    private StellarEnvelopeRenderer() {
    }

    public static void submit(
        PoseStack pose, SubmitNodeCollector collector, StellarVisualState visual,
        @Nullable StellarEventProfile event, float progress
    ) {
        wind(pose, collector, visual);
        if (event != null) event(pose, collector, visual, event, progress);
    }

    private static void wind(PoseStack pose, SubmitNodeCollector collector, StellarVisualState visual) {
        float strength = visual.windStrength();
        boolean convective = visual.surfaceStyle().equals("fully_convective_main_sequence");
        boolean radiative = visual.surfaceStyle().equals("radiative_core_main_sequence");
        if (strength <= 0 && !convective && !radiative) return;
        int shells = strength > 0 ? 4 : convective ? 3 : 1;
        for (int shell = 0; shell < shells; shell++) {
            float flow = (visual.flowProgress() * (2 + strength * 5) + shell / (float) shells) % 1;
            float scale = strength > 0 ? 1.05F + flow * (0.5F + strength) : 1.012F + shell * 0.014F;
            final float alpha = strength > 0 ? strength * 0.16F * (1 - flow) : convective ? 0.075F : 0.045F;
            pose.pushPose();
            pose.translate(0.5, 0.5, 0.5);
            pose.scale(scale, scale, scale);
            if (convective) pose.mulPose(Axis.YP.rotationDegrees((flow - 0.5F) * 3));
            pose.translate(-0.5, -0.5, -0.5);
            cube(pose, collector, visual.red(), visual.green(), visual.blue(), alpha);
            pose.popPose();
        }
    }

    private static void event(
        PoseStack pose, SubmitNodeCollector collector, StellarVisualState visual, StellarEventProfile profile, float progress
    ) {
        float coreRatio = Math.clamp(profile.coreRadius(progress), 0.05F, 2.0F);
        float ejecta = Math.max(0, profile.ejectaRadius(progress));
        int rgb = profile.color(progress);
        float red = ((rgb >> 16) & 255) / 255.0F;
        float green = ((rgb >> 8) & 255) / 255.0F;
        float blue = (rgb & 255) / 255.0F;
        float intensity = profile.emission(progress);
        if (coreRatio < 0.98F) {
            scaledCube(pose, collector, coreRatio, Math.min(1, red * 1.2F), Math.min(1, green * 1.2F), Math.min(1, blue * 1.2F),
                Math.min(0.85F, 0.20F + intensity * 0.08F));
        }
        if (ejecta > 0.01F) {
            int layers = Math.min(6, Math.max(1, profile.shellCount()));
            float outer = Math.clamp(visual.ejectaRadius() / Math.max(visual.radius(), 0.01F), 1.0F, 12.0F);
            for (int index = layers; index >= 1; index--) {
                float layer = index / (float) layers;
                float scale = 1.0F + (outer - 1.0F) * layer;
                float alpha = Math.min(0.55F, (0.12F + intensity * 0.035F) * (1.0F - layer * 0.55F));
                scaledCube(pose, collector, scale, Math.min(1, red + 0.08F), Math.min(1, green + 0.08F),
                    Math.min(1, blue + 0.08F), alpha);
            }
        }
    }

    private static void scaledCube(
        PoseStack pose, SubmitNodeCollector collector, float scale, float red, float green, float blue, float alpha
    ) {
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.scale(scale, scale, scale);
        pose.translate(-0.5, -0.5, -0.5);
        cube(pose, collector, red, green, blue, alpha);
        pose.popPose();
    }

    private static void cube(PoseStack pose, SubmitNodeCollector collector, float red, float green, float blue, float alpha) {
        var model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(Blocks.WHITE_CONCRETE.defaultBlockState());
        StellarEmissionRenderer.drawModel(model, pose, collector, Layers.TYPE, ARGB.colorFromFloat(alpha, red, green, blue));
    }

    private static final class Layers {
        private static final RenderType TYPE = RenderType.create("anvilcraft:stellar_envelope",
            RenderSetup.builder(ModRenderPipelines.STELLAR_ENVELOPE).withTexture("Sampler0", Sheets.BLOCKS_MAPPER.sheet())
                .useLightmap().sortOnUpload().createRenderSetup());
    }
}
