package dev.dubhe.anvilcraft.client.renderer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.support.MunClientSky;
import dev.dubhe.anvilcraft.config.AnvilCraftClientConfig.MunLightingQuality;
import dev.dubhe.anvilcraft.integration.iris.IrisState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import javax.annotation.Nullable;

/** 世界位置相关的面向光照和阴影；保留原版 AO、透明度以及光影包接管。 */
public final class MunSurfaceRenderer {
    private static final int[] TRANSLUCENT_TEXTURE_UNITS = {7, 11, 2};
    private static final MunShadowMap SHADOW_MAP = new MunShadowMap();
    private static final MunSolarLighting SOLAR = new MunSolarLighting();
    private static final MunSolarLighting SHADOW_SOLAR = new MunSolarLighting();
    private static final MunShadowClock SHADOW_CLOCK = new MunShadowClock();
    private static final MunShadowHistory SHADOW_HISTORY = new MunShadowHistory();
    private static long historyGeometryRevision;
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

    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        clear();
        event.registerShader(
            new ShaderInstance(event.getResourceProvider(), AnvilCraft.of("mun_terrain"), DefaultVertexFormat.BLOCK),
            instance -> terrainShader = instance
        );
        event.registerShader(
            new ShaderInstance(event.getResourceProvider(), AnvilCraft.of("mun_shadow"), DefaultVertexFormat.POSITION_TEX_COLOR),
            instance -> shadowShader = instance
        );
        event.registerShader(
            new ShaderInstance(event.getResourceProvider(), AnvilCraft.of("mun_translucent_shadow"),
                DefaultVertexFormat.POSITION_TEX_COLOR),
            instance -> translucentShadowShader = instance
        );
        MunPostProcessing.registerShaders(event);
    }

    public static boolean isLightingEnabled() {
        return AnvilCraft.CLIENT_CONFIG.munLightingQuality != MunLightingQuality.OFF;
    }

    public static boolean usesTerrainShader() {
        return isLightingEnabled() && terrainShader != null && shadowShader != null && translucentShadowShader != null
            && MunClientSky.isMun() && !IrisState.isShaderEnabled();
    }

    public static void setupSodiumUniforms() {
        int program = GL20C.glGetInteger(GL20C.GL_CURRENT_PROGRAM);
        if (program == 0) return;
        int enabled = GL20C.glGetUniformLocation(program, "MunEnabled");
        if (enabled < 0) return;
        boolean active = usesTerrainShader();
        GL20C.glUniform1i(enabled, active ? 1 : 0);
        // 关闭月球分支时，整数采样器仍不能与原版的浮点采样器共用纹理单元。
        GL20C.glUniform1i(GL20C.glGetUniformLocation(program, "ShadowHistory"), 6);
        for (int index = 0; index < 3; index++) {
            GL20C.glUniform1i(GL20C.glGetUniformLocation(program, "TranslucentShadowMap" + index), TRANSLUCENT_TEXTURE_UNITS[index]);
        }
        if (!active) return;
        Vec3 position = relativeCamera();
        GL20C.glUniform3f(GL20C.glGetUniformLocation(program, "CameraPosition"),
            (float) position.x, (float) position.y, (float) position.z);
        SOLAR.apply(program);
        SHADOW_SOLAR.applyShadow(program);
        SHADOW_HISTORY.apply(program, BlockPos.containing(renderOrigin));
        GL20C.glUniform1i(GL20C.glGetUniformLocation(program, "ShadowCount"), shadowCount());
        GL20C.glUniform1i(GL20C.glGetUniformLocation(program, "TranslucentShadows"), profile.translucentShadows() ? 1 : 0);
        GL20C.glUniform1f(GL20C.glGetUniformLocation(program, "AmbientFloor"), profile.ambientFloor());
        Vec3 localAnchor = anchor.subtract(renderOrigin);
        GL20C.glUniform3f(GL20C.glGetUniformLocation(program, "ShadowAnchor"),
            (float) localAnchor.x, (float) localAnchor.y, (float) localAnchor.z);
        int oldTexture = GlStateManager._getActiveTexture();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            for (int index = 0; index < 3; index++) {
                GL20C.glUniformMatrix4fv(GL20C.glGetUniformLocation(program, "ShadowMatrix" + index), false,
                    SHADOW_MAP.matrix(index).get(stack.mallocFloat(16)));
                GL20C.glUniform4f(GL20C.glGetUniformLocation(program, "ShadowInfo" + index),
                    SHADOW_MAP.span(index) / 2, SHADOW_MAP.span(index) / MunShadowProjection.DEPTH, 0, 0);
                GL20C.glUniform1i(GL20C.glGetUniformLocation(program, "ShadowMap" + index), index + 8);
                GlStateManager._activeTexture(GL20C.GL_TEXTURE8 + index);
                GlStateManager._bindTexture(SHADOW_MAP.textureId(index));
                GL20C.glUniform1i(GL20C.glGetUniformLocation(program, "ShadowStaticMap" + index), index + 3);
                GlStateManager._activeTexture(GL20C.GL_TEXTURE3 + index);
                GlStateManager._bindTexture(SHADOW_MAP.staticTextureId(index));
            }
            for (int index = 0; index < 3; index++) {
                int unit = TRANSLUCENT_TEXTURE_UNITS[index];
                GlStateManager._activeTexture(GL20C.GL_TEXTURE0 + unit);
                GlStateManager._bindTexture(SHADOW_MAP.translucentTextureId(index));
            }
        } finally {
            GlStateManager._activeTexture(oldTexture);
        }
    }

    public static @Nullable ShaderInstance terrain(@Nullable ShaderInstance original, float alphaCutoff) {
        ShaderInstance shader = terrainShader;
        if (shader == null || !worldPass || !usesTerrainShader()) return original;
        if (!terrainPass) shader.safeGetUniform("ChunkOffset").set(0.0F, 0.0F, 0.0F);
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
        shader.safeGetUniform("AlphaCutoff").set(alphaCutoff);
        return shader;
    }

    private static Vec3 relativeCamera() {
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        return camera.subtract(renderOrigin);
    }

    public static void prepareShadows() {
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
        int oldActiveTexture = GlStateManager._getActiveTexture();
        int oldBinding = GL11C.glGetInteger(GL11C.GL_TEXTURE_BINDING_2D);
        int oldProgram = GL11C.glGetInteger(GL20C.GL_CURRENT_PROGRAM);
        try {
            SHADOW_MAP.prepare(level, anchor, renderOrigin, SHADOW_SOLAR, tick, shader, translucentShader, profile);
            if (historyGeometryRevision != SHADOW_MAP.geometryRevision()) {
                historyGeometryRevision = SHADOW_MAP.geometryRevision();
                SHADOW_HISTORY.invalidate();
            }
            SHADOW_HISTORY.begin();
        } finally {
            GlStateManager._glUseProgram(oldProgram);
            GlStateManager._activeTexture(oldActiveTexture);
            GlStateManager._bindTexture(oldBinding);
        }
    }

    private static int shadowCount() {
        return profile.cascades() > 0 ? SHADOW_MAP.count() : 0;
    }

    public static void postProcess(org.joml.Matrix4f projection) {
        if (!postProcessed && usesTerrainShader()) {
            postProcessed = true;
            MunPostProcessing.render(projection, profile, 1);
        }
    }

    public static void onBlockChanged(ClientLevel level, BlockPos pos, BlockState previous, BlockState state) {
        SHADOW_MAP.blockChanged(level, pos, previous, state);
    }

    public static void onChunkChanged(ChunkPos pos) {
        SHADOW_MAP.chunkChanged(pos);
    }

    public static void clear() {
        SHADOW_HISTORY.close();
        SHADOW_CLOCK.clear();
        SHADOW_MAP.close();
        MunPostProcessing.clear();
        terrainPass = false;
        worldPass = false;
    }

    public static void beginWorld() {
        updateQuality();
        worldPass = true;
        postProcessed = false;
    }

    private static void updateQuality() {
        MunLightingQuality configured = AnvilCraft.CLIENT_CONFIG.munLightingQuality;
        if (configured == quality) return;
        final boolean rebuild = (configured == MunLightingQuality.OFF) != (quality == MunLightingQuality.OFF);
        quality = configured;
        profile = MunLightingProfile.of(configured);
        SHADOW_MAP.close();
        SHADOW_HISTORY.close();
        SHADOW_CLOCK.clear();
        MunPostProcessing.clear();
        if (rebuild && MunClientSky.isMun()) {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.levelRenderer.allChanged();
            minecraft.gameRenderer.lightTexture().tick();
        }
    }

    public static void endWorld() {
        SHADOW_HISTORY.end();
        worldPass = false;
        terrainPass = false;
    }

    public static void beginTerrain() {
        terrainPass = true;
    }

    public static void endTerrain() {
        terrainPass = false;
    }
}
