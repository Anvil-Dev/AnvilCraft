package dev.dubhe.anvilcraft.client.support;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import dev.dubhe.anvilcraft.mixin.accessor.RenderSystemAccessor;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;

/** 隔离 GUI 离屏绘制对帧缓冲、矩阵和混合状态的修改。 */
final class PreviewRenderState implements AutoCloseable {
    private final int drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
    private final int readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
    private final int[] viewport = new int[4];
    private final int[] scissorBox = new int[4];
    private final int[] colorMask = new int[4];
    private final float[] clearColor = new float[4];
    private final boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
    private final boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
    private final boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
    private final boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
    private final boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
    private final int sourceRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
    private final int destinationRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
    private final int sourceAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
    private final int destinationAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
    private final float fogEnd = RenderSystem.getShaderFogEnd();
    private final FogShape fogShape = RenderSystem.getShaderFogShape();
    private final float[] fogColor = RenderSystem.getShaderFogColor().clone();
    private final int[] shaderTextures = new int[] {
        RenderSystem.getShaderTexture(0), RenderSystem.getShaderTexture(1), RenderSystem.getShaderTexture(2)
    };
    private final int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
    private final int[] boundTextures = new int[3];
    private final int boundTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
    private final Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
    private final VertexSorting sorting = RenderSystem.getVertexSorting();
    private final float[] color = RenderSystem.getShaderColor().clone();
    private final float fogStart = RenderSystem.getShaderFogStart();
    private final @Nullable ShaderInstance shader = RenderSystem.getShader();
    private final Vector3f light0;
    private final Vector3f light1;

    PreviewRenderState() {
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, this.viewport);
        GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, this.scissorBox);
        GL11.glGetIntegerv(GL11.GL_COLOR_WRITEMASK, this.colorMask);
        GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE, this.clearColor);
        Vector3f[] lights = RenderSystemAccessor.anvilcraft$getShaderLightDirections();
        this.light0 = new Vector3f(lights[0]);
        this.light1 = new Vector3f(lights[1]);
        for (int index = 0; index < this.boundTextures.length; index++) {
            GlStateManager._activeTexture(GL13.GL_TEXTURE0 + index);
            this.boundTextures[index] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        }
        GlStateManager._activeTexture(this.activeTexture);
        RenderSystem.getModelViewStack().pushMatrix();
    }

    @Override
    public void close() {
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, this.drawFramebuffer);
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, this.readFramebuffer);
        RenderSystem.viewport(this.viewport[0], this.viewport[1], this.viewport[2], this.viewport[3]);
        if (this.scissor) {
            RenderSystem.enableScissor(this.scissorBox[0], this.scissorBox[1], this.scissorBox[2], this.scissorBox[3]);
        } else {
            RenderSystem.disableScissor();
        }
        if (this.depth) {
            RenderSystem.enableDepthTest();
        } else {
            RenderSystem.disableDepthTest();
        }
        RenderSystem.depthMask(this.depthMask);
        RenderSystem.colorMask(this.colorMask[0] != 0, this.colorMask[1] != 0, this.colorMask[2] != 0, this.colorMask[3] != 0);
        GlStateManager._clearColor(this.clearColor[0], this.clearColor[1], this.clearColor[2], this.clearColor[3]);
        RenderSystem.setProjectionMatrix(this.projection, this.sorting);
        RenderSystem.getModelViewStack().popMatrix();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setShaderLights(this.light0, this.light1);
        RenderSystem.setShaderColor(this.color[0], this.color[1], this.color[2], this.color[3]);
        RenderSystem.setShaderFogStart(this.fogStart);
        RenderSystem.setShader(() -> this.shader);
        RenderSystem.setShaderFogEnd(this.fogEnd);
        RenderSystem.setShaderFogShape(this.fogShape);
        RenderSystem.setShaderFogColor(this.fogColor[0], this.fogColor[1], this.fogColor[2], this.fogColor[3]);
        RenderSystem.blendFuncSeparate(this.sourceRgb, this.destinationRgb, this.sourceAlpha, this.destinationAlpha);
        if (this.blend) {
            RenderSystem.enableBlend();
        } else {
            RenderSystem.disableBlend();
        }
        if (this.cull) {
            RenderSystem.enableCull();
        } else {
            RenderSystem.disableCull();
        }
        for (int index = 0; index < this.shaderTextures.length; index++) {
            RenderSystem.setShaderTexture(index, this.shaderTextures[index]);
            GlStateManager._activeTexture(GL13.GL_TEXTURE0 + index);
            GlStateManager._bindTexture(this.boundTextures[index]);
        }
        GlStateManager._activeTexture(this.activeTexture);
        GlStateManager._bindTexture(this.boundTexture);
    }
}
