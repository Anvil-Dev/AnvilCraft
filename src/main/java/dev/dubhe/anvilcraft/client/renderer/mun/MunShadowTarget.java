package dev.dubhe.anvilcraft.client.renderer.mun;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL30C;

import java.nio.ByteBuffer;

/** Exact source depth/transmission formats which are absent from the native texture-format enum. */
final class MunShadowTarget implements AutoCloseable {
    private final int size;
    private int framebuffer;
    private int depth;
    private int transmission;

    MunShadowTarget(int size) {
        this(size, false);
    }

    MunShadowTarget(int size, boolean translucent) {
        RenderSystem.assertOnRenderThread();
        if (size <= 0 || size > RenderSystem.getDevice().getMaxTextureSize()) {
            throw new IllegalArgumentException("Invalid Mun shadow target size: " + size);
        }
        this.size = size;
        try (var ignored = new MunShadowGlScope()) {
            this.framebuffer = GlStateManager.glGenFramebuffers();
            this.depth = this.createTexture(GL30C.GL_DEPTH_COMPONENT24, GL30C.GL_DEPTH_COMPONENT);
            GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, this.framebuffer);
            GlStateManager._glFramebufferTexture2D(GL30C.GL_FRAMEBUFFER, GL30C.GL_DEPTH_ATTACHMENT,
                GL30C.GL_TEXTURE_2D, this.depth, 0);
            GL30C.glDrawBuffer(GL30C.GL_NONE);
            GL30C.glReadBuffer(GL30C.GL_NONE);
            if (translucent) {
                this.transmission = this.createTexture(GL30C.GL_RG32UI, GL30C.GL_RG_INTEGER);
                GlStateManager._glFramebufferTexture2D(GL30C.GL_FRAMEBUFFER, GL30C.GL_COLOR_ATTACHMENT0,
                    GL30C.GL_TEXTURE_2D, this.transmission, 0);
                GL30C.glDrawBuffer(GL30C.GL_COLOR_ATTACHMENT0);
            }
            if (GL30C.glCheckFramebufferStatus(GL30C.GL_FRAMEBUFFER) != GL30C.GL_FRAMEBUFFER_COMPLETE) {
                throw new IllegalStateException("Incomplete Mun shadow framebuffer");
            }
        } catch (RuntimeException exception) {
            this.close();
            throw exception;
        }
    }

    private int createTexture(int internalFormat, int format) {
        int id = GlStateManager._genTexture();
        GlStateManager._bindTexture(id);
        GL11C.glTexImage2D(GL11C.GL_TEXTURE_2D, 0, internalFormat, this.size, this.size, 0,
            format, GL11C.GL_UNSIGNED_INT, (ByteBuffer) null);
        GlStateManager._texParameter(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MIN_FILTER, GL11C.GL_NEAREST);
        GlStateManager._texParameter(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MAG_FILTER, GL11C.GL_NEAREST);
        GlStateManager._texParameter(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_S, GL30C.GL_CLAMP_TO_EDGE);
        GlStateManager._texParameter(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_T, GL30C.GL_CLAMP_TO_EDGE);
        return id;
    }

    void bind() {
        if (this.framebuffer == 0) throw new IllegalStateException("Mun shadow target is closed");
        GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, this.framebuffer);
        GlStateManager._viewport(0, 0, this.size, this.size);
        GlStateManager._disableScissorTest();
        GlStateManager._depthMask(true);
        GL11C.glClearDepth(1);
        GL11C.glClear(GL11C.GL_DEPTH_BUFFER_BIT);
        if (this.transmission != 0) {
            GlStateManager._colorMask(15);
            GL30C.glClearBufferuiv(GL11C.GL_COLOR, 0, new int[]{0xFFFFFF, 0xFFFFFF, 0, 0});
        }
    }

    void copyDepthFrom(MunShadowTarget source) {
        if (source.framebuffer == 0 || this.framebuffer == 0) throw new IllegalStateException("Closed shadow depth copy");
        GlStateManager._glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, source.framebuffer);
        GlStateManager._glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, this.framebuffer);
        GlStateManager._glBlitFrameBuffer(0, 0, source.size, source.size, 0, 0, this.size, this.size,
            GL11C.GL_DEPTH_BUFFER_BIT, GL11C.GL_NEAREST);
        GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, this.framebuffer);
        GlStateManager._viewport(0, 0, this.size, this.size);
    }

    int size() {
        return this.size;
    }

    int texture() {
        return this.depth;
    }

    int transmission() {
        return this.transmission;
    }

    @Override
    public void close() {
        if (this.depth != 0) GlStateManager._deleteTexture(this.depth);
        if (this.transmission != 0) GlStateManager._deleteTexture(this.transmission);
        if (this.framebuffer != 0) GlStateManager._glDeleteFramebuffers(this.framebuffer);
        this.depth = 0;
        this.transmission = 0;
        this.framebuffer = 0;
    }
}
