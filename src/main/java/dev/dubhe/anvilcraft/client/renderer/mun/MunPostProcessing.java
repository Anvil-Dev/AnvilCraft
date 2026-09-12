package dev.dubhe.anvilcraft.client.renderer.mun;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11C;

import java.io.IOException;
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
        try (MunRenderScope ignored = MunRenderScope.postProcess()) {
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
            RenderSystem.disableScissor();
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
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.SRC_ALPHA,
                GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
            MunSkyRenderer.drawScreenQuad();
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
