package dev.dubhe.anvilcraft.client.renderer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL30C;

import java.nio.ByteBuffer;

/** 只保存深度，避免为每个阴影像素分配和写入浮点颜色附件。 */
final class MunShadowTarget implements AutoCloseable {
    private final int texture;
    private final int framebuffer;
    private final int size;

    MunShadowTarget(int size) {
        RenderSystem.assertOnRenderThread();
        this.size = Math.min(size, RenderSystem.maxSupportedTextureSize());
        this.texture = TextureUtil.generateTextureId();
        this.framebuffer = GlStateManager.glGenFramebuffers();
        GlStateManager._bindTexture(this.texture);
        GL30C.glTexImage2D(GL30C.GL_TEXTURE_2D, 0, GL30C.GL_DEPTH_COMPONENT24, this.size, this.size, 0,
            GL30C.GL_DEPTH_COMPONENT, GL30C.GL_UNSIGNED_INT, (ByteBuffer) null);
        GlStateManager._texParameter(GL30C.GL_TEXTURE_2D, GL30C.GL_TEXTURE_MIN_FILTER, GL30C.GL_NEAREST);
        GlStateManager._texParameter(GL30C.GL_TEXTURE_2D, GL30C.GL_TEXTURE_MAG_FILTER, GL30C.GL_NEAREST);
        GlStateManager._texParameter(GL30C.GL_TEXTURE_2D, GL30C.GL_TEXTURE_WRAP_S, GL30C.GL_CLAMP_TO_EDGE);
        GlStateManager._texParameter(GL30C.GL_TEXTURE_2D, GL30C.GL_TEXTURE_WRAP_T, GL30C.GL_CLAMP_TO_EDGE);
        GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, this.framebuffer);
        GlStateManager._glFramebufferTexture2D(GL30C.GL_FRAMEBUFFER, GL30C.GL_DEPTH_ATTACHMENT, GL30C.GL_TEXTURE_2D, this.texture, 0);
        GL30C.glDrawBuffer(GL30C.GL_NONE);
        GL30C.glReadBuffer(GL30C.GL_NONE);
        if (GL30C.glCheckFramebufferStatus(GL30C.GL_FRAMEBUFFER) != GL30C.GL_FRAMEBUFFER_COMPLETE) {
            this.close();
            throw new IllegalStateException("Incomplete lunar shadow framebuffer");
        }
    }

    void bind() {
        GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, this.framebuffer);
        RenderSystem.viewport(0, 0, this.size, this.size);
        GL30C.glClearDepth(1);
        RenderSystem.clear(GL30C.GL_DEPTH_BUFFER_BIT, false);
    }

    void copyDepthFrom(MunShadowTarget source) {
        GlStateManager._glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, source.framebuffer);
        GlStateManager._glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, this.framebuffer);
        RenderSystem.viewport(0, 0, this.size, this.size);
        GlStateManager._glBlitFrameBuffer(0, 0, source.size, source.size, 0, 0, this.size, this.size,
            GL30C.GL_DEPTH_BUFFER_BIT, GL30C.GL_NEAREST);
    }

    int size() {
        return this.size;
    }

    int texture() {
        return this.texture;
    }

    @Override
    public void close() {
        TextureUtil.releaseTextureId(this.texture);
        GlStateManager._glDeleteFramebuffers(this.framebuffer);
    }
}
