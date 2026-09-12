package dev.dubhe.anvilcraft.client.renderer.mun;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.config.AnvilCraftClientConfig.MunLightingQuality;
import dev.dubhe.anvilcraft.integration.iris.IrisState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import javax.annotation.Nullable;

/** 世界位置相关的面向光照和阴影；保留原版 AO、透明度以及光影包接管。 */
public final class MunSurfaceRenderer {
    private static final MunShadowMap SHADOW_MAP = new MunShadowMap();
    private static final MunSolarLighting SOLAR = new MunSolarLighting();
    private static final MunSolarLighting SHADOW_SOLAR = new MunSolarLighting();
    private static final MunShadowClock SHADOW_CLOCK = new MunShadowClock();
    private static final MunShadowHistory SHADOW_HISTORY = new MunShadowHistory();
    private static long historyGeometryRevision;
    private static @Nullable ShaderInstance entityShader;
    private static boolean shadowCapture;
    private static long frame;
    private static long terrainUniformFrame = -1;
    private static long entityUniformFrame = -1;
    private static @Nullable ShaderInstance terrainShader;
    private static @Nullable ShaderInstance shadowShader;
    private static @Nullable ShaderInstance translucentShadowShader;
    private static boolean terrainPass;
    private static boolean worldPass;
    private static boolean postProcessed;
    private static MunLightingQuality quality = MunLightingQuality.STANDARD;
    private static MunLightingProfile profile = MunLightingProfile.of(quality);
    private static Vec3 renderOrigin = Vec3.ZERO;
    private static Vec3 anchor = Vec3.ZERO;

    private MunSurfaceRenderer() {
    }

    static void registerShaders(MunShaderRegistration shaders) throws IOException {
        shaders.add("mun/mun_terrain", DefaultVertexFormat.BLOCK, instance -> terrainShader = instance);
        shaders.add("mun/mun_shadow", DefaultVertexFormat.POSITION_TEX_COLOR, instance -> shadowShader = instance);
        shaders.add("mun/mun_translucent_shadow", DefaultVertexFormat.POSITION_TEX_COLOR, instance -> translucentShadowShader = instance);
        shaders.add("mun/mun_entity", DefaultVertexFormat.NEW_ENTITY, instance -> entityShader = instance);
        MunPostProcessing.registerShaders(shaders);
    }

    static void resetShaders() {
        terrainShader = null;
        shadowShader = null;
        translucentShadowShader = null;
        entityShader = null;
        MunPostProcessing.resetShader();
        clear();
    }

    public static boolean isLightingEnabled() {
        return MunRenderPipeline.enabled();
    }

    public static boolean usesTerrainShader() {
        return isLightingEnabled() && terrainShader != null && shadowShader != null && translucentShadowShader != null
            && MunClientSky.isMun() && !IrisState.isShaderEnabled();
    }

    public static void setupSodiumUniforms() {
        if (!MunRenderPipeline.requested()) return;
        try {
            MunSodiumShaderBindings bindings = MunSodiumShaderBindings.begin(usesTerrainShader());
            if (bindings != null) {
                bindings.apply(SOLAR, SHADOW_SOLAR, SHADOW_HISTORY, SHADOW_MAP, profile, relativeCamera(), renderOrigin, anchor);
            }
        } catch (RuntimeException exception) {
            MunRenderPipeline.fail();
            MunSodiumShaderBindings.disable();
        }
    }

    public static @Nullable ShaderInstance terrain(@Nullable ShaderInstance original, float alphaCutoff) {
        ShaderInstance shader = terrainShader;
        if (shader == null || !worldPass || shadowCapture || !usesTerrainShader()) return original;
        try {
            if (!terrainPass) shader.safeGetUniform("ChunkOffset").set(0.0F, 0.0F, 0.0F);
            if (terrainUniformFrame != frame) {
                applySurfaceUniforms(shader);
                terrainUniformFrame = frame;
            }
            shader.safeGetUniform("AlphaCutoff").set(alphaCutoff);
            return shader;
        } catch (RuntimeException exception) {
            MunRenderPipeline.fail();
            return original;
        }
    }

    public static @Nullable ShaderInstance entity(@Nullable ShaderInstance original, float alphaCutoff, int overlayMode) {
        ShaderInstance shader = entityShader;
        if (shader == null || !worldPass || shadowCapture || !usesTerrainShader()) return original;
        try {
            if (entityUniformFrame != frame) {
                applySurfaceUniforms(shader);
                entityUniformFrame = frame;
            }
            shader.safeGetUniform("AlphaCutoff").set(alphaCutoff);
            shader.safeGetUniform("OverlayMode").set(overlayMode);
            return shader;
        } catch (RuntimeException exception) {
            MunRenderPipeline.fail();
            return original;
        }
    }

    private static void applySurfaceUniforms(ShaderInstance shader) {
        SOLAR.apply(shader);
        SHADOW_SOLAR.applyShadow(shader);
        SHADOW_HISTORY.apply(shader, BlockPos.containing(renderOrigin));
        Vec3 position = relativeCamera();
        shader.safeGetUniform("CameraPosition").set((float) position.x, (float) position.y, (float) position.z);
        shader.safeGetUniform("ShadowCount").set(shadowCount());
        shader.safeGetUniform("TranslucentShadows").set(profile.translucentShadows() ? 1 : 0);
        for (int index = 0; index < 3; index++) shader.setSampler("TranslucentShadowMap" + index, SHADOW_MAP.translucentTextureId(index));
        shader.safeGetUniform("AmbientFloor").set(profile.ambientFloor());
        Vec3 localAnchor = anchor.subtract(renderOrigin);
        shader.safeGetUniform("ShadowAnchor").set((float) localAnchor.x, (float) localAnchor.y, (float) localAnchor.z);
        for (int index = 0; index < 3; index++) {
            shader.safeGetUniform("ShadowMatrix" + index).set(SHADOW_MAP.matrix(index));
            shader.setSampler("ShadowMap" + index, SHADOW_MAP.textureId(index));
            shader.setSampler("ShadowStaticMap" + index, SHADOW_MAP.staticTextureId(index));
            shader.safeGetUniform("ShadowInfo" + index).set(
                SHADOW_MAP.span(index) / 2, SHADOW_MAP.span(index) / MunShadowProjection.DEPTH, 0, 0
            );
        }
    }

    static void beginShadowCapture() {
        shadowCapture = true;
    }

    static void endShadowCapture() {
        shadowCapture = false;
    }

    private static Vec3 relativeCamera() {
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        return camera.subtract(renderOrigin);
    }

    public static void prepareShadows() {
        if (!isLightingEnabled()) return;
        try {
            prepareShadowResources();
        } catch (RuntimeException exception) {
            MunRenderPipeline.fail();
            MunRenderPipeline.release(MunSurfaceRenderer::clear);
        }
    }

    private static void prepareShadowResources() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        ShaderInstance shader = shadowShader;
        ShaderInstance translucentShader = translucentShadowShader;
        if (level == null || shader == null || translucentShader == null || !usesTerrainShader()) {
            SHADOW_MAP.close();
            SHADOW_HISTORY.close();
            SHADOW_CLOCK.clear();
            MunPostProcessing.clear();
            return;
        }
        var camera = minecraft.gameRenderer.getMainCamera();
        float tick = minecraft.getTimer().getGameTimeDeltaPartialTick(true);
        var entity = camera.getEntity();
        anchor = entity == null ? camera.getPosition() : entity.getPosition(tick).add(0, 1.6, 0);
        // 以附近的整格为原点，避免大坐标相减损失阴影精度。
        Vec3 nextOrigin = Vec3.atLowerCornerOf(BlockPos.containing(anchor));
        if (nextOrigin.distanceToSqr(renderOrigin) > 64 * 64) SHADOW_HISTORY.invalidate();
        renderOrigin = nextOrigin;
        double time = MunClientSky.partialDayTime(level, tick);
        SOLAR.update(level.getDayTime(), time, renderOrigin);
        if (profile.cascades() == 0) return;
        if (SHADOW_CLOCK.update(level.getDayTime(), time, SOLAR.direction(anchor.x, anchor.z).y())) SHADOW_HISTORY.invalidate();
        SHADOW_SOLAR.update(SHADOW_CLOCK.dayTime(), SHADOW_CLOCK.partialTick(), renderOrigin);
        try (MunRenderScope ignored = MunRenderScope.resources()) {
            SHADOW_MAP.prepare(level, anchor, renderOrigin, SHADOW_SOLAR, tick, shader, translucentShader, profile);
            if (historyGeometryRevision != SHADOW_MAP.geometryRevision()) {
                historyGeometryRevision = SHADOW_MAP.geometryRevision();
                SHADOW_HISTORY.invalidate();
            }
            SHADOW_HISTORY.begin();
        }
    }

    private static int shadowCount() {
        return profile.cascades() > 0 ? SHADOW_MAP.count() : 0;
    }

    public static void postProcess(org.joml.Matrix4f projection) {
        if (!postProcessed && usesTerrainShader()) {
            postProcessed = true;
            try {
                MunPostProcessing.render(projection, profile, 1);
            } catch (RuntimeException exception) {
                MunRenderPipeline.fail();
                MunRenderPipeline.release(MunSurfaceRenderer::clear);
            }
        }
    }

    public static void onBlockChanged(ClientLevel level, BlockPos pos, BlockState previous, BlockState state) {
        if (!isLightingEnabled()) return;
        SHADOW_MAP.blockChanged(level, pos, previous, state);
    }

    public static void onModelDataChanged(BlockEntity entity) {
        if (!isLightingEnabled()) return;
        if (entity.getLevel() != Minecraft.getInstance().level || !MunClientSky.isMun()) return;
        BlockPos pos = entity.getBlockPos();
        for (int z = (pos.getZ() - 1) >> 4; z <= (pos.getZ() + 1) >> 4; z++) {
            for (int x = (pos.getX() - 1) >> 4; x <= (pos.getX() + 1) >> 4; x++) {
                SHADOW_MAP.chunkChanged(new ChunkPos(x, z));
            }
        }
    }

    public static void onChunkChanged(ChunkPos pos) {
        if (!isLightingEnabled()) return;
        SHADOW_MAP.chunkChanged(pos);
    }

    public static void clear() {
        MunRenderPipeline.release(SHADOW_HISTORY::close);
        SHADOW_CLOCK.clear();
        MunRenderPipeline.release(SHADOW_MAP::close);
        MunRenderPipeline.release(MunPostProcessing::clear);
        terrainPass = false;
        worldPass = false;
        shadowCapture = false;
        terrainUniformFrame = -1;
        entityUniformFrame = -1;
    }

    public static void beginWorld() {
        updateQuality();
        if (!isLightingEnabled()) return;
        frame++;
        worldPass = true;
        postProcessed = false;
    }

    private static void updateQuality() {
        MunLightingQuality configured = AnvilCraft.CLIENT_CONFIG.munLightingQuality;
        if (configured == quality) return;
        quality = configured;
        profile = MunLightingProfile.of(configured);
        clear();
    }

    public static void endWorld() {
        if (!worldPass) return;
        try {
            SHADOW_HISTORY.end();
        } catch (RuntimeException exception) {
            MunRenderPipeline.fail();
        }
        worldPass = false;
        terrainPass = false;
    }

    public static void beginTerrain() {
        terrainPass = isLightingEnabled();
    }

    public static void endTerrain() {
        terrainPass = false;
    }
}
