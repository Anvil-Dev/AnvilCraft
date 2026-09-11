package dev.dubhe.anvilcraft.client.renderer.mun;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL14C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import javax.annotation.Nullable;

/** 在不透明物体之后合成 AO 和炫光，保留主缓冲深度及 alpha，透明物体随后正常绘制。 */
final class MunPostProcessing {
    private static @Nullable ShaderInstance shader;
    private static @Nullable RenderTarget first;
    private static @Nullable RenderTarget second;

    private MunPostProcessing() {
    }

    static void registerShaders(MunShaderRegistration shaders) throws IOException {
        shaders.add("mun/mun_post", DefaultVertexFormat.POSITION, instance -> shader = instance);
    }

    static void resetShader() {
        shader = null;
    }

    static void render(Matrix4f projection, MunLightingProfile profile, float sunlight) {
        ShaderInstance effect = shader;
        if (effect == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget main = minecraft.getMainRenderTarget();
        int width = Math.max(1, main.width / profile.effectDownsample());
        int height = Math.max(1, main.height / profile.effectDownsample());
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer viewport = stack.mallocInt(4);
            ByteBuffer mask = stack.malloc(4);
            GL11C.glGetIntegerv(GL11C.GL_VIEWPORT, viewport);
            GL11C.glGetBooleanv(GL11C.GL_COLOR_WRITEMASK, mask);
            int framebuffer = GL11C.glGetInteger(GL30C.GL_DRAW_FRAMEBUFFER_BINDING);
            int readFramebuffer = GL11C.glGetInteger(GL30C.GL_READ_FRAMEBUFFER_BINDING);
            int oldProgram = GL11C.glGetInteger(GL20C.GL_CURRENT_PROGRAM);
            int oldActive = GlStateManager._getActiveTexture();
            int oldBinding = GL11C.glGetInteger(GL11C.GL_TEXTURE_BINDING_2D);
            int texture0 = RenderSystem.getShaderTexture(0);
            int texture1 = RenderSystem.getShaderTexture(1);
            int sourceRgb = GL11C.glGetInteger(GL14C.GL_BLEND_SRC_RGB);
            int destinationRgb = GL11C.glGetInteger(GL14C.GL_BLEND_DST_RGB);
            int sourceAlpha = GL11C.glGetInteger(GL14C.GL_BLEND_SRC_ALPHA);
            int destinationAlpha = GL11C.glGetInteger(GL14C.GL_BLEND_DST_ALPHA);
            boolean blend = GL11C.glIsEnabled(GL11C.GL_BLEND);
            boolean depth = GL11C.glIsEnabled(GL11C.GL_DEPTH_TEST);
            boolean depthMask = GL11C.glGetBoolean(GL11C.GL_DEPTH_WRITEMASK);
            boolean cull = GL11C.glIsEnabled(GL11C.GL_CULL_FACE);
            boolean scissor = GL11C.glIsEnabled(GL11C.GL_SCISSOR_TEST);
            ShaderInstance previous = RenderSystem.getShader();
            try {
                if (first == null || second == null || first.width != width || first.height != height) {
                    clear();
                    first = new EffectTarget(width, height);
                    second = new EffectTarget(width, height);
                    first.setFilterMode(GL11C.GL_LINEAR);
                    second.setFilterMode(GL11C.GL_LINEAR);
                }
                RenderSystem.disableDepthTest();
                RenderSystem.depthMask(false);
                RenderSystem.disableBlend();
                RenderSystem.disableCull();
                GlStateManager._disableScissorTest();
                RenderSystem.colorMask(true, true, true, true);
                effect.safeGetUniform("InverseProjection").set(new Matrix4f(projection).invert());
                effect.safeGetUniform("ProjectionScale").set(projection.m00(), projection.m11());
                effect.safeGetUniform("SceneTexelSize").set(1.0F / main.width, 1.0F / main.height);
                effect.safeGetUniform("TexelSize").set(1.0F / width, 1.0F / height);
                effect.safeGetUniform("SampleCount").set(Minecraft.useAmbientOcclusion() ? profile.aoSamples() : 0);
                effect.safeGetUniform("Radius").set(profile.aoRadius());
                effect.safeGetUniform("AoStrength").set(profile.aoStrength());
                effect.safeGetUniform("GlareStrength").set(profile.glareStrength());
                effect.safeGetUniform("Daylight").set(sunlight);
                RenderSystem.setShaderTexture(1, main.getDepthTextureId());
                draw(effect, main.getColorTextureId(), first, 0);
                effect.safeGetUniform("BlurDirection").set(1.0F, 0.0F);
                draw(effect, first.getColorTextureId(), second, 1);
                effect.safeGetUniform("BlurDirection").set(0.0F, 1.0F);
                draw(effect, second.getColorTextureId(), first, 1);
                main.bindWrite(true);
                effect.safeGetUniform("Mode").set(2);
                RenderSystem.setShaderTexture(0, first.getColorTextureId());
                RenderSystem.setShader(() -> effect);
                effect.apply();
                RenderSystem.enableBlend();
                // dst.rgb * AO + glare；不复制全分辨率场景，也不把阴影塞进材质 alpha。
                RenderSystem.blendFuncSeparate(GL11C.GL_ONE, GL11C.GL_SRC_ALPHA, GL11C.GL_ZERO, GL11C.GL_ONE);
                MunSkyRenderer.drawScreenQuad();
            } finally {
                RenderSystem.setShaderTexture(0, texture0);
                RenderSystem.setShaderTexture(1, texture1);
                if (previous != null) RenderSystem.setShader(() -> previous);
                GlStateManager._glUseProgram(oldProgram);
                GlStateManager._activeTexture(oldActive);
                GlStateManager._bindTexture(oldBinding);
                GlStateManager._glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, framebuffer);
                GlStateManager._glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, readFramebuffer);
                RenderSystem.viewport(viewport.get(0), viewport.get(1), viewport.get(2), viewport.get(3));
                RenderSystem.colorMask(mask.get(0) != 0, mask.get(1) != 0, mask.get(2) != 0, mask.get(3) != 0);
                RenderSystem.blendFuncSeparate(sourceRgb, destinationRgb, sourceAlpha, destinationAlpha);
                if (blend) RenderSystem.enableBlend();
                else RenderSystem.disableBlend();
                if (depth) RenderSystem.enableDepthTest();
                else RenderSystem.disableDepthTest();
                RenderSystem.depthMask(depthMask);
                if (cull) RenderSystem.enableCull();
                else RenderSystem.disableCull();
                if (scissor) GlStateManager._enableScissorTest();
            }
        }
    }

    private static void draw(ShaderInstance shader, int source, RenderTarget target, int mode) {
        target.bindWrite(true);
        shader.safeGetUniform("Mode").set(mode);
        RenderSystem.setShaderTexture(0, source);
        RenderSystem.setShader(() -> shader);
        MunSkyRenderer.drawScreenQuad();
    }

    static void clear() {
        if (first != null) first.destroyBuffers();
        if (second != null) second.destroyBuffers();
        first = null;
        second = null;
    }

    private static final class EffectTarget extends RenderTarget {
        private EffectTarget(int width, int height) {
            super(false);
            try {
                this.resize(width, height, Minecraft.ON_OSX);
            } catch (RuntimeException exception) {
                this.destroyBuffers();
                throw exception;
            }
        }
    }
}
