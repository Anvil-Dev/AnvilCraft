package dev.dubhe.anvilcraft.client.renderer.blockentity.celestial;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.rendering.BlockStateModelRenderer;
import dev.dubhe.anvilcraft.api.rendering.BlockStateModelTessellateState;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyClass;
import dev.dubhe.anvilcraft.block.entity.celestial.StarData;
import dev.dubhe.anvilcraft.block.entity.celestial.StellarVisualState;
import dev.dubhe.anvilcraft.client.init.ModRenderPipelines;
import dev.dubhe.anvilcraft.config.AnvilCraftClientConfig.CelestialRenderingMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class StellarEmissionRenderer {
    private static final int HALO_BANDS = 6;
    private static boolean checked;
    private static boolean failed;

    private StellarEmissionRenderer() {
    }

    @SubscribeEvent
    public static void reload(ModelEvent.BakingCompleted event) {
        checked = false;
        failed = false;
    }

    public static boolean standard() {
        if (AnvilCraft.CLIENT_CONFIG.stellarRenderingMode != CelestialRenderingMode.STANDARD) return false;
        if (!checked) {
            checked = true;
            try {
                failed = !RenderSystem.getDevice().precompilePipeline(ModRenderPipelines.STELLAR_SURFACE).isValid()
                    || !RenderSystem.getDevice().precompilePipeline(ModRenderPipelines.STELLAR_CORONA).isValid();
            } catch (RuntimeException exception) {
                failed = true;
                AnvilCraft.LOGGER.warn("Stellar pipeline unavailable; using vanilla stellar rendering", exception);
            }
        }
        return !failed;
    }

    public static boolean submit(
        StarData star, BlockStateModelTessellateState model, PoseStack pose, SubmitNodeCollector collector,
        @Nullable StellarVisualState visual, float eventEmission
    ) {
        if (!standard()) return false;
        float temperature = visual == null
            ? StellarVisualState.temperatureForSurfaceClass(star.bodyClass(), star.energy()) : visual.temperature();
        float luminosity = visual == null ? Math.max(0.01F, star.energy() / 32.0F) : visual.luminosity();
        float emission = visual == null ? 1.0F : visual.emission();
        float exposure = StellarRadiance.exposure(temperature, luminosity, emission);
        if (eventEmission > 0) exposure = StellarRadiance.eventExposure(exposure, eventEmission);
        float[] color = visual == null ? CelestialBodyTextureBakery.starColor(star) : visual.surfaceColorComponents();
        if (color[0] + color[1] + color[2] == 0.0F) {
            int rgb = StellarVisualState.colorForTemperature(temperature);
            color = new float[]{((rgb >> 16) & 255) / 255.0F, ((rgb >> 8) & 255) / 255.0F, (rgb & 255) / 255.0F};
        }
        StellarRadiance.normalizeColor(color);
        float[] core = StellarRadiance.coreColor(color, temperature, exposure);
        float gain = StellarRadiance.surfaceGain(temperature);
        int tint = ARGB.colorFromFloat(StellarRadiance.encodeExposure(exposure), core[0] * gain, core[1] * gain, core[2] * gain);
        BlockStateModel baked = Minecraft.getInstance().getModelManager().getStandaloneModel(model.key());
        if (baked == null) return false;
        drawModel(baked, pose, collector, Layers.SURFACE, tint);
        pose.pushPose();
        if (star.bodyClass() == CelestialBodyClass.NEUTRON_STAR) {
            pose.translate(0.5, 0.5, 0.5);
            pose.scale(0.375F, 0.375F, 0.375F);
            pose.translate(-0.5, -0.5, -0.5);
        }
        submitCorona(pose, collector, color, StellarRadiance.haloScale(exposure) - 1.0F,
            StellarRadiance.haloStrength(exposure) * 0.45F, StellarRadiance.rimBoost(temperature, exposure));
        pose.popPose();
        return true;
    }

    public static boolean submitBrownDwarf(PoseStack pose, SubmitNodeCollector collector) {
        if (!standard()) return false;
        BlockStateModel cube = Minecraft.getInstance().getModelManager().getBlockStateModelSet()
            .get(Blocks.WHITE_CONCRETE.defaultBlockState());
        float[] color = {1.0F, 0.3F, 0.1F};
        drawModel(cube, pose, collector, Layers.CORONA,
            ARGB.colorFromFloat(StellarRadiance.BROWN_DWARF_SURFACE_GLOW, color[0], color[1], color[2]));
        submitCorona(pose, collector, color, StellarRadiance.BROWN_DWARF_HALO_SCALE - 1.0F,
            StellarRadiance.BROWN_DWARF_HALO_ALPHA, 0.0F);
        return true;
    }

    static void drawModel(BlockStateModel model, PoseStack pose, OrderedSubmitNodeCollector collector, RenderType type, int tint) {
        collector.submitCustomGeometry(pose, type, (matrix, vertices) ->
            BlockStateModelRenderer.INSTANCE.getTessellatorNoLighting().tesselateBlock((x, y, z, quad, instance) -> {
                instance.setColor(tint);
                instance.setLightCoords(LightCoordsUtil.FULL_BRIGHT);
                instance.setOverlayCoords(OverlayTexture.NO_OVERLAY);
                vertices.putBakedQuad(matrix, quad, instance);
            }, 0, 0, 0, BlockAndTintGetter.EMPTY, BlockPos.ZERO, Blocks.AIR.defaultBlockState(), model, 42));
    }

    private static void submitCorona(
        PoseStack pose, SubmitNodeCollector collector, float[] color, float reach, float edgeAlpha, float rimBoost
    ) {
        var block = Blocks.WHITE_CONCRETE.defaultBlockState();
        TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(block)
            .particleMaterial(BlockAndTintGetter.EMPTY, BlockPos.ZERO, block).sprite();
        Vector3f camera = new Matrix4f(pose.last().pose()).invert().transformPosition(new Vector3f());
        collector.submitCustomGeometry(pose, Layers.CORONA, (matrix, vertices) -> {
            for (int corner = 0; corner < 8; corner++) {
                for (int axis = 0; axis < 3; axis++) {
                    if ((corner & (1 << axis)) != 0 || !StellarRadiance.silhouetteEdge(camera, corner, axis)) continue;
                    int end = corner | (1 << axis);
                    for (int band = 0; band < HALO_BANDS; band++) {
                        float inner = band / (float) HALO_BANDS;
                        float outer = (band + 1) / (float) HALO_BANDS;
                        inner *= inner;
                        outer *= outer;
                        float innerScale = 1.0F + reach * inner;
                        float outerScale = 1.0F + reach * outer;
                        float innerAlpha = StellarRadiance.haloAlpha(edgeAlpha, rimBoost, inner);
                        float outerAlpha = StellarRadiance.haloAlpha(edgeAlpha, rimBoost, outer);
                        haloVertex(vertices, matrix, sprite, corner, innerScale, color, innerAlpha);
                        haloVertex(vertices, matrix, sprite, end, innerScale, color, innerAlpha);
                        haloVertex(vertices, matrix, sprite, end, outerScale, color, outerAlpha);
                        haloVertex(vertices, matrix, sprite, corner, outerScale, color, outerAlpha);
                    }
                }
            }
        });
    }

    private static void haloVertex(
        VertexConsumer vertices, PoseStack.Pose pose, TextureAtlasSprite sprite, int corner, float scale, float[] color, float alpha
    ) {
        float x = 0.5F + ((corner & 1) - 0.5F) * scale;
        float y = 0.5F + (((corner >> 1) & 1) - 0.5F) * scale;
        float z = 0.5F + (((corner >> 2) & 1) - 0.5F) * scale;
        vertices.addVertex(pose, x, y, z).setColor(color[0], color[1], color[2], alpha)
            .setUv(sprite.getU(0.5F), sprite.getV(0.5F)).setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, 0, 1, 0);
    }

    private static final class Layers {
        private static final RenderType SURFACE = RenderType.create("anvilcraft:stellar_surface",
            RenderSetup.builder(ModRenderPipelines.STELLAR_SURFACE)
                .withTexture("Sampler0", Sheets.BLOCKS_MAPPER.sheet()).createRenderSetup());
        private static final RenderType CORONA = RenderType.create("anvilcraft:stellar_corona",
            RenderSetup.builder(ModRenderPipelines.STELLAR_CORONA)
                .withTexture("Sampler0", Sheets.BLOCKS_MAPPER.sheet()).bufferSize(8192).createRenderSetup());
    }
}
