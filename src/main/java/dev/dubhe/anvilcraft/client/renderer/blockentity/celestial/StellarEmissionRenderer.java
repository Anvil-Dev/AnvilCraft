package dev.dubhe.anvilcraft.client.renderer.blockentity.celestial;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyClass;
import dev.dubhe.anvilcraft.block.entity.celestial.StarData;
import dev.dubhe.anvilcraft.block.entity.celestial.StellarEventProfile;
import dev.dubhe.anvilcraft.block.entity.celestial.StellarVisualState;
import dev.dubhe.anvilcraft.integration.iris.IrisState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.pipeline.VertexConsumerWrapper;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL20C;

import java.io.IOException;
import javax.annotation.Nullable;

import static net.minecraft.client.renderer.RenderStateShard.COLOR_DEPTH_WRITE;
import static net.minecraft.client.renderer.RenderStateShard.COLOR_WRITE;
import static net.minecraft.client.renderer.RenderStateShard.CULL;
import static net.minecraft.client.renderer.RenderStateShard.EQUAL_DEPTH_TEST;
import static net.minecraft.client.renderer.RenderStateShard.NO_CULL;
import static net.minecraft.client.renderer.RenderStateShard.RENDERTYPE_EYES_SHADER;
import static net.minecraft.client.renderer.RenderStateShard.TRANSLUCENT_TRANSPARENCY;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class StellarEmissionRenderer {
    private static final int HALO_BANDS = 6;
    private static final CelestialRenderState STATE = new CelestialRenderState();
    @Nullable
    private static ShaderInstance surfaceShader;

    private static final RenderType SURFACE = RenderType.create(
        "anvilcraft:stellar_surface", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, false,
        RenderType.CompositeState.builder()
            .setShaderState(new RenderStateShard.ShaderStateShard(StellarEmissionRenderer::surfaceShader))
            .setTextureState(new RenderStateShard.TextureStateShard(TextureAtlas.LOCATION_BLOCKS, false, false))
            .setWriteMaskState(COLOR_DEPTH_WRITE)
            .createCompositeState(false)
    );
    private static final RenderType FALLBACK_SURFACE = RenderType.create(
        "anvilcraft:stellar_surface_emissive", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, false,
        RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_EYES_SHADER)
            .setTextureState(new RenderStateShard.TextureStateShard(TextureAtlas.LOCATION_BLOCKS, false, false))
            .setWriteMaskState(COLOR_DEPTH_WRITE)
            .setCullState(CULL)
            .createCompositeState(false)
    );
    private static final RenderType FALLBACK_GLOW = RenderType.create(
        "anvilcraft:stellar_surface_glow", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, false,
        RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_EYES_SHADER)
            .setTextureState(new RenderStateShard.TextureStateShard(TextureAtlas.LOCATION_BLOCKS, false, false))
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setDepthTestState(EQUAL_DEPTH_TEST)
            .setWriteMaskState(COLOR_WRITE)
            .setCullState(CULL)
            .createCompositeState(false)
    );
    private static final RenderType CORONA = RenderType.create(
        "anvilcraft:stellar_corona", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 8192, false, false,
        RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_EYES_SHADER)
            .setTextureState(new RenderStateShard.TextureStateShard(TextureAtlas.LOCATION_BLOCKS, false, false))
            .setTransparencyState(new RenderStateShard.TransparencyStateShard(
                "stellar_additive",
                () -> {
                    RenderSystem.enableBlend();
                    RenderSystem.blendFuncSeparate(
                        GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                        GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE
                    );
                },
                () -> {
                    RenderSystem.disableBlend();
                    RenderSystem.defaultBlendFunc();
                }
            ))
            .setWriteMaskState(COLOR_WRITE)
            .setCullState(NO_CULL)
            .createCompositeState(false)
    );

    private StellarEmissionRenderer() {
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) {
        STATE.beginReload();
        surfaceShader = null;
        try {
            ShaderInstance shader = new ShaderInstance(event.getResourceProvider(), AnvilCraft.of("stellar_surface"),
                DefaultVertexFormat.NEW_ENTITY);
            if (GL20C.glGetProgrami(shader.getId(), GL20C.GL_LINK_STATUS) == GL20C.GL_FALSE) {
                shader.close();
                STATE.fail();
                return;
            }
            event.registerShader(shader, loaded -> {
                surfaceShader = loaded;
                STATE.completeReload();
            });
        } catch (IOException | RuntimeException exception) {
            STATE.fail();
            AnvilCraft.LOGGER.warn("Stellar surface shader unavailable; using vanilla celestial rendering.", exception);
        }
    }

    private static ShaderInstance surfaceShader() {
        ShaderInstance shader = surfaceShader;
        return shader != null ? shader : GameRenderer.getRendertypeEyesShader();
    }

    public static void render(
        StarData star, BakedModel model, PoseStack poseStack, MultiBufferSource buffers, int overlay,
        @Nullable StellarVisualState visual, @Nullable StellarEventProfile event, float eventProgress
    ) {
        if (!STATE.standard(AnvilCraft.CLIENT_CONFIG.stellarRenderingMode, IrisState.isShaderEnabled())) {
            VanillaCelestialRenderer.star(star, model, poseStack, buffers, overlay, visual);
            return;
        }
        try {
            renderStandard(star, model, poseStack, buffers, overlay, visual, event, eventProgress);
        } catch (RuntimeException exception) {
            if (STATE.fail()) AnvilCraft.LOGGER.warn("Stellar emission failed; using vanilla celestial rendering.", exception);
            VanillaCelestialRenderer.star(star, model, poseStack, buffers, overlay, visual);
        }
    }

    private static void renderStandard(
        StarData star, BakedModel model, PoseStack poseStack, MultiBufferSource buffers, int overlay,
        @Nullable StellarVisualState visual, @Nullable StellarEventProfile event, float eventProgress
    ) {
        float temperature = visual == null
            ? StellarVisualState.temperatureForSurfaceClass(star.bodyClass(), star.energy()) : visual.temperature();
        float luminosity = visual == null ? Math.max(0.01f, star.energy() / 32.0f) : visual.luminosity();
        float emission = visual == null ? 1.0f : visual.emission();
        float exposure = StellarRadiance.exposure(temperature, luminosity, emission);
        if (event != null) exposure = StellarRadiance.eventExposure(exposure, event.emission(eventProgress));
        float[] color = visual == null ? CelestialBodyTextureBakery.starColor(star) : visual.surfaceColorComponents();
        if (color[0] + color[1] + color[2] == 0.0f) {
            int rgb = StellarVisualState.colorForTemperature(temperature);
            color = new float[] {((rgb >> 16) & 255) / 255.0f, ((rgb >> 8) & 255) / 255.0f, (rgb & 255) / 255.0f};
        }
        StellarRadiance.normalizeColor(color);
        float[] coreColor = StellarRadiance.coreColor(color, temperature, exposure);
        boolean custom = surfaceShader != null && !IrisState.isShaderEnabled();
        float brightness = custom ? 1.0f : StellarRadiance.toneMap(1.0f, exposure);
        brightness *= StellarRadiance.surfaceGain(temperature);
        float red = coreColor[0] * brightness;
        float green = coreColor[1] * brightness;
        float blue = coreColor[2] * brightness;
        // Vertex alpha carries per-star exposure, so deferred batches never share mutable uniforms.
        float alpha = custom ? StellarRadiance.encodeExposure(exposure) : 1.0f;
        drawModel(model, poseStack, buffers.getBuffer(custom ? SURFACE : FALLBACK_SURFACE), red, green, blue, alpha, overlay);
        if (!custom) {
            // Blend toward the same stellar color instead of clipping individual channels with additive exposure.
            TextureAtlasSprite sprite = Minecraft.getInstance().getBlockRenderer()
                .getBlockModel(Blocks.WHITE_CONCRETE.defaultBlockState()).getParticleIcon(ModelData.EMPTY);
            VertexConsumer consumer = new VertexConsumerWrapper(buffers.getBuffer(FALLBACK_GLOW)) {
                @Override
                public VertexConsumer setUv(float u, float v) {
                    this.parent.setUv(sprite.getU(0.5f), sprite.getV(0.5f));
                    return this;
                }
            };
            float glow = Math.min(0.95f, 0.8f + 0.01f * exposure);
            drawModel(model, poseStack, consumer, red, green, blue, glow, overlay);
        }
        poseStack.pushPose();
        try {
            if (star.bodyClass() == CelestialBodyClass.NEUTRON_STAR) {
                // The compact star model occupies only the central 6/16 of its model space.
                poseStack.translate(0.5, 0.5, 0.5);
                poseStack.scale(0.375f, 0.375f, 0.375f);
                poseStack.translate(-0.5, -0.5, -0.5);
            }
            renderCorona(poseStack, buffers, color, StellarRadiance.haloScale(exposure) - 1.0f,
                StellarRadiance.haloStrength(exposure) * 0.45f, StellarRadiance.rimBoost(temperature, exposure), overlay);
        } finally {
            poseStack.popPose();
        }
    }

    public static void renderBrownDwarfGlow(PoseStack poseStack, MultiBufferSource buffers, int overlay) {
        if (!STATE.standard(AnvilCraft.CLIENT_CONFIG.stellarRenderingMode, IrisState.isShaderEnabled())) {
            VanillaCelestialRenderer.brownDwarf(poseStack, buffers, overlay);
            return;
        }
        try {
            renderBrownDwarfStandard(poseStack, buffers, overlay);
        } catch (RuntimeException exception) {
            if (STATE.fail()) AnvilCraft.LOGGER.warn("Brown dwarf emission failed; using vanilla celestial rendering.", exception);
            VanillaCelestialRenderer.brownDwarf(poseStack, buffers, overlay);
        }
    }

    private static void renderBrownDwarfStandard(PoseStack poseStack, MultiBufferSource buffers, int overlay) {
        BakedModel cube = Minecraft.getInstance().getBlockRenderer().getBlockModel(Blocks.WHITE_CONCRETE.defaultBlockState());
        float[] color = {1.0f, 0.3f, 0.1f};
        drawModel(cube, poseStack, buffers.getBuffer(CORONA), color[0], color[1], color[2],
            StellarRadiance.BROWN_DWARF_SURFACE_GLOW, overlay);
        renderCorona(poseStack, buffers, color, StellarRadiance.BROWN_DWARF_HALO_SCALE - 1.0f,
            StellarRadiance.BROWN_DWARF_HALO_ALPHA, 0.0f, overlay);
    }

    private static void renderCorona(
        PoseStack poseStack, MultiBufferSource buffers, float[] color, float reach, float edgeAlpha, float rimBoost, int overlay
    ) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getBlockRenderer()
            .getBlockModel(Blocks.WHITE_CONCRETE.defaultBlockState()).getParticleIcon(ModelData.EMPTY);
        Vector3f camera = new Matrix4f(poseStack.last().pose()).invert().transformPosition(new Vector3f());
        VertexConsumer consumer = buffers.getBuffer(CORONA);
        float red = color[0];
        float green = color[1];
        float blue = color[2];
        for (int corner = 0; corner < 8; corner++) {
            for (int axis = 0; axis < 3; axis++) {
                if ((corner & (1 << axis)) != 0 || !StellarRadiance.silhouetteEdge(camera, corner, axis)) continue;
                int end = corner | (1 << axis);
                for (int band = 0; band < HALO_BANDS; band++) {
                    float inner = band / (float) HALO_BANDS;
                    float outer = (band + 1) / (float) HALO_BANDS;
                    inner *= inner;
                    outer *= outer;
                    float innerScale = 1.0f + reach * inner;
                    float outerScale = 1.0f + reach * outer;
                    float innerAlpha = StellarRadiance.haloAlpha(edgeAlpha, rimBoost, inner);
                    float outerAlpha = StellarRadiance.haloAlpha(edgeAlpha, rimBoost, outer);
                    haloVertex(consumer, poseStack, sprite, corner, innerScale, red, green, blue, innerAlpha, overlay);
                    haloVertex(consumer, poseStack, sprite, end, innerScale, red, green, blue, innerAlpha, overlay);
                    haloVertex(consumer, poseStack, sprite, end, outerScale, red, green, blue, outerAlpha, overlay);
                    haloVertex(consumer, poseStack, sprite, corner, outerScale, red, green, blue, outerAlpha, overlay);
                }
            }
        }
    }

    private static void haloVertex(
        VertexConsumer consumer, PoseStack poseStack, TextureAtlasSprite sprite, int corner, float scale,
        float red, float green, float blue, float alpha, int overlay
    ) {
        float x = 0.5f + ((corner & 1) - 0.5f) * scale;
        float y = 0.5f + (((corner >> 1) & 1) - 0.5f) * scale;
        float z = 0.5f + (((corner >> 2) & 1) - 0.5f) * scale;
        consumer.addVertex(poseStack.last(), x, y, z).setColor(red, green, blue, alpha)
            .setUv(sprite.getU(0.5f), sprite.getV(0.5f)).setOverlay(overlay).setLight(LightTexture.FULL_BRIGHT)
            .setNormal(poseStack.last(), 0, 1, 0);
    }

    private static void drawModel(
        BakedModel model, PoseStack poseStack, VertexConsumer consumer, float red, float green, float blue, float alpha, int overlay
    ) {
        RandomSource random = RandomSource.create(42L);
        for (Direction direction : Direction.values()) {
            random.setSeed(42L);
            for (BakedQuad quad : model.getQuads(null, direction, random, ModelData.EMPTY, null)) {
                consumer.putBulkData(poseStack.last(), quad, red, green, blue, alpha, LightTexture.FULL_BRIGHT, overlay);
            }
        }
        random.setSeed(42L);
        for (BakedQuad quad : model.getQuads(null, null, random, ModelData.EMPTY, null)) {
            consumer.putBulkData(poseStack.last(), quad, red, green, blue, alpha, LightTexture.FULL_BRIGHT, overlay);
        }
    }
}
