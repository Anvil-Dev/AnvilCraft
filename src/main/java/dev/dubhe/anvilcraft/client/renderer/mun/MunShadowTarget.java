package dev.dubhe.anvilcraft.client.renderer.mun;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL30C;

/** 原版渲染目标的深度附件适配；透光投影额外保存压缩颜色与 24 位深度。 */
final class MunShadowTarget extends RenderTarget implements AutoCloseable {
    private final boolean translucent;

    MunShadowTarget(int size) {
        this(size, false);
    }

    MunShadowTarget(int size, boolean translucent) {
        super(true);
        RenderSystem.assertOnRenderThread();
        this.translucent = translucent;
        int resolution = Math.min(size, RenderSystem.maxSupportedTextureSize());
        try (MunRenderScope ignored = MunRenderScope.targetAllocation()) {
            try {
                this.createBuffers(resolution, resolution, false);
            } catch (RuntimeException exception) {
                this.close();
                throw exception;
            }
        }
    }

    @Override
    public void createBuffers(int width, int height, boolean clearError) {
        RenderSystem.assertOnRenderThreadOrInit();
        if (width <= 0 || height <= 0) throw new IllegalStateException("Invalid Mun shadow framebuffer size");
        this.width = width;
        this.height = height;
        this.viewWidth = width;
        this.viewHeight = height;
        this.depthBufferId = TextureUtil.generateTextureId();
        this.frameBufferId = GlStateManager.glGenFramebuffers();
        GlStateManager._bindTexture(this.depthBufferId);
        // 原版默认目标会额外分配 RGBA8；这里保留纯深度布局及原有精度，避免增加级联显存。
        GlStateManager._texImage2D(GL30C.GL_TEXTURE_2D, 0, GL30C.GL_DEPTH_COMPONENT24, width, height, 0,
            GL30C.GL_DEPTH_COMPONENT, GL30C.GL_UNSIGNED_INT, null);
        this.configureTexture();
        this.bindWrite(false);
        GlStateManager._glFramebufferTexture2D(GL30C.GL_FRAMEBUFFER, GL30C.GL_DEPTH_ATTACHMENT,
            GL30C.GL_TEXTURE_2D, this.depthBufferId, 0);
        GL30C.glDrawBuffer(GL30C.GL_NONE);
        GL30C.glReadBuffer(GL30C.GL_NONE);
        if (this.translucent) {
            this.colorTextureId = TextureUtil.generateTextureId();
            GlStateManager._bindTexture(this.colorTextureId);
            GlStateManager._texImage2D(GL30C.GL_TEXTURE_2D, 0, GL30C.GL_RG32UI, width, height, 0,
                GL30C.GL_RG_INTEGER, GL30C.GL_UNSIGNED_INT, null);
            this.configureTexture();
            GlStateManager._glFramebufferTexture2D(GL30C.GL_FRAMEBUFFER, GL30C.GL_COLOR_ATTACHMENT0,
                GL30C.GL_TEXTURE_2D, this.colorTextureId, 0);
            GL30C.glDrawBuffer(GL30C.GL_COLOR_ATTACHMENT0);
        }
        this.checkStatus();
    }

    private void configureTexture() {
        GlStateManager._texParameter(GL30C.GL_TEXTURE_2D, GL30C.GL_TEXTURE_MIN_FILTER, GL30C.GL_NEAREST);
        GlStateManager._texParameter(GL30C.GL_TEXTURE_2D, GL30C.GL_TEXTURE_MAG_FILTER, GL30C.GL_NEAREST);
        GlStateManager._texParameter(GL30C.GL_TEXTURE_2D, GL30C.GL_TEXTURE_WRAP_S, GL30C.GL_CLAMP_TO_EDGE);
        GlStateManager._texParameter(GL30C.GL_TEXTURE_2D, GL30C.GL_TEXTURE_WRAP_T, GL30C.GL_CLAMP_TO_EDGE);
    }

    void bind() {
        this.bindWrite(true);
        RenderSystem.clearDepth(1);
        RenderSystem.clear(GL30C.GL_DEPTH_BUFFER_BIT, false);
        if (this.translucent) GL30C.glClearBufferuiv(GL30C.GL_COLOR, 0, new int[]{0xFFFFFF, 0xFFFFFF, 0, 0});
    }

    @Override
    public void clear(boolean clearError) {
        this.bind();
        this.unbindWrite();
    }

    @Override
    public void copyDepthFrom(RenderTarget source) {
        super.copyDepthFrom(source);
        // 原版复制会解绑目标；动态投影随后继续绘制到本目标，不再次清除已复制的深度。
        this.bindWrite(true);
    }

    int size() {
        return this.width;
    }

    int texture() {
        return this.getDepthTextureId();
    }

    int transmission() {
        return this.translucent ? this.getColorTextureId() : 0;
    }

    @Override
    public void close() {
        this.destroyBuffers();
    }
}
