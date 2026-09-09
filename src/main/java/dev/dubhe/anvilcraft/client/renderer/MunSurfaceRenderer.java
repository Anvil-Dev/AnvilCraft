package dev.dubhe.anvilcraft.client.renderer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.support.MunClientSky;
import dev.dubhe.anvilcraft.integration.iris.IrisState;
import dev.dubhe.anvilcraft.worldgen.MunSkyMath;
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

import java.io.IOException;
import javax.annotation.Nullable;

/** 月面直射光、邻近地形遮挡与克制的受光面炫光。 */
public final class MunSurfaceRenderer {
    private static final MunShadowMap SHADOW_MAP = new MunShadowMap();
    private static @Nullable ShaderInstance terrainShader;
    private static @Nullable ShaderInstance glareShader;
    private static @Nullable RenderTarget glareTarget;
    private static boolean terrainPass;
    private static boolean wroteTerrainMask;

    private MunSurfaceRenderer() {
    }

    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        clear();
        event.registerShader(
            new ShaderInstance(event.getResourceProvider(), AnvilCraft.of("mun_terrain"), DefaultVertexFormat.BLOCK),
            instance -> terrainShader = instance
        );
        event.registerShader(
            new ShaderInstance(event.getResourceProvider(), AnvilCraft.of("mun_glare"), DefaultVertexFormat.POSITION),
            instance -> glareShader = instance
        );
    }

    public static boolean usesTerrainShader() {
        return terrainShader != null && MunClientSky.isMun() && !IrisState.isShaderEnabled();
    }

    public static void setupSodiumUniforms(boolean opaque) {
        int program = GL20C.glGetInteger(GL20C.GL_CURRENT_PROGRAM);
        if (program == 0) return;
        int enabled = GL20C.glGetUniformLocation(program, "MunEnabled");
        if (enabled < 0) return;
        ClientLevel level = Minecraft.getInstance().level;
        boolean active = opaque && level != null && usesTerrainShader() && SHADOW_MAP.textureId() != 0;
        GL20C.glUniform1i(enabled, active ? 1 : 0);
        if (!active) return;
        Vec3 position = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vec3 relativePosition = SHADOW_MAP.relativePosition(position);
        MunSkyMath.Vector sun = MunSkyMath.sunDirection(position.x, position.z, level.getDayTime(), 0);
        GL20C.glUniform3f(GL20C.glGetUniformLocation(program, "SunDirection"), (float) sun.x(), (float) sun.y(), (float) sun.z());
        GL20C.glUniform1f(GL20C.glGetUniformLocation(program, "Sunlight"), MunClientSky.sunlight(level));
        GL20C.glUniform3f(
            GL20C.glGetUniformLocation(program, "CameraPosition"),
            (float) relativePosition.x, (float) relativePosition.y, (float) relativePosition.z
        );
        GL20C.glUniform1i(GL20C.glGetUniformLocation(program, "Sampler3"), 3);
        int oldTexture = GlStateManager._getActiveTexture();
        GlStateManager._activeTexture(GL20C.GL_TEXTURE3);
        GlStateManager._bindTexture(SHADOW_MAP.textureId());
        GlStateManager._activeTexture(oldTexture);
        wroteTerrainMask = true;
    }

    public static @Nullable ShaderInstance terrain(@Nullable ShaderInstance original, float alphaCutoff) {
        ShaderInstance shader = terrainShader;
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (shader == null || level == null || !terrainPass || !usesTerrainShader()) return original;
        if (SHADOW_MAP.textureId() == 0) return original;
        wroteTerrainMask = true;
        Vec3 position = minecraft.gameRenderer.getMainCamera().getPosition();
        Vec3 relativePosition = SHADOW_MAP.relativePosition(position);
        MunSkyMath.Vector sun = MunSkyMath.sunDirection(position.x, position.z, level.getDayTime(), 0);
        shader.safeGetUniform("SunDirection").set((float) sun.x(), (float) sun.y(), (float) sun.z());
        shader.safeGetUniform("Sunlight").set(MunClientSky.sunlight(level));
        // 使用相对遮挡缓存原点的坐标，避免远离世界原点后浮点精度损坏法线和阴影。
        shader.safeGetUniform("CameraPosition").set(
            (float) relativePosition.x, (float) relativePosition.y, (float) relativePosition.z
        );
        shader.safeGetUniform("AlphaCutoff").set(alphaCutoff);
        RenderSystem.setShaderTexture(3, SHADOW_MAP.textureId());
        return shader;
    }

    public static void prepareShadows() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !usesTerrainShader()) {
            SHADOW_MAP.close();
            return;
        }
        int oldActiveTexture = GlStateManager._getActiveTexture();
        int oldBinding = GL11C.glGetInteger(GL11C.GL_TEXTURE_BINDING_2D);
        try {
            SHADOW_MAP.prepare(level, Minecraft.getInstance().gameRenderer.getMainCamera().getPosition());
        } finally {
            GlStateManager._activeTexture(oldActiveTexture);
            GlStateManager._bindTexture(oldBinding);
        }
    }

    public static void onBlockChanged(ClientLevel level, BlockPos pos, BlockState state) {
        SHADOW_MAP.blockChanged(level, pos, state);
    }

    public static void onChunkChanged(ChunkPos pos) {
        SHADOW_MAP.chunkChanged(pos);
    }

    public static void renderGlare() {
        ShaderInstance shader = glareShader;
        if (shader == null || !wroteTerrainMask || !usesTerrainShader()) return;
        wroteTerrainMask = false;
        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget main = minecraft.getMainRenderTarget();
        if (glareTarget == null) glareTarget = new TextureTarget(main.width, main.height, false, Minecraft.ON_OSX);
        if (glareTarget.width != main.width || glareTarget.height != main.height) {
            glareTarget.resize(main.width, main.height, Minecraft.ON_OSX);
        }
        shader.safeGetUniform("TexelSize").set(1.0F / main.width, 1.0F / main.height);
        shader.safeGetUniform("GlareStrength").set(RenderState.isBloomEffectEnabled() ? 0.055F : 0.0F);
        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        try {
            // 地形的 alpha 暂存受光量，合成后恢复；主缓冲深度始终保留供实体继续渲染。
            glareTarget.bindWrite(false);
            RenderSystem.setShaderTexture(0, main.getColorTextureId());
            RenderSystem.setShader(() -> shader);
            MunSkyRenderer.drawScreenQuad();
            main.bindWrite(false);
            shader.safeGetUniform("GlareStrength").set(0.0F);
            RenderSystem.setShaderTexture(0, glareTarget.getColorTextureId());
            MunSkyRenderer.drawScreenQuad();
        } finally {
            main.bindWrite(false);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
        }
    }

    public static void clear() {
        if (glareTarget != null) {
            glareTarget.destroyBuffers();
            glareTarget = null;
        }
        SHADOW_MAP.close();
        terrainPass = false;
        wroteTerrainMask = false;
    }

    public static void beginTerrain() {
        terrainPass = true;
    }

    public static void endTerrain() {
        terrainPass = false;
    }
}
