package dev.dubhe.anvilcraft.client.renderer.mun;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL42C;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;

/** 双缓冲世界受影格历史；冲突时只放弃历史，不把别处的明暗套到当前表面。 */
final class MunShadowHistory implements AutoCloseable {
    private static final int SIZE = 1024;
    private static final int FRAME_LIMIT = 1 << 18;
    private final int[] textures = new int[2];
    private final int[][] savedImages = new int[2][6];
    private int claims;
    private int framebuffer;
    private int frame;
    private int previousFrame;
    private int read;
    private boolean active;
    private boolean invalidated = true;

    void invalidate() {
        this.invalidated = true;
    }

    void begin() {
        RenderSystem.assertOnRenderThread();
        if (!GL.getCapabilities().GL_ARB_shader_image_load_store) return;
        if (this.active) this.end();
        if (this.frame == FRAME_LIMIT - 1) this.close();
        int binding = GL30C.glGetInteger(GL30C.GL_TEXTURE_BINDING_2D);
        int drawFramebuffer = GL30C.glGetInteger(GL30C.GL_DRAW_FRAMEBUFFER_BINDING);
        boolean scissor = GL30C.glIsEnabled(GL30C.GL_SCISSOR_TEST);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer mask = stack.malloc(4);
            GL30C.glGetBooleanv(GL30C.GL_COLOR_WRITEMASK, mask);
            RenderSystem.colorMask(true, true, true, true);
            try {
                GlStateManager._disableScissorTest();
                if (this.framebuffer == 0) {
                    this.framebuffer = GlStateManager.glGenFramebuffers();
                    this.textures[0] = this.createTexture(false);
                    this.textures[1] = this.createTexture(false);
                    this.claims = this.createTexture(true);
                    GlStateManager._glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, this.framebuffer);
                    GL30C.glDrawBuffer(GL30C.GL_COLOR_ATTACHMENT0);
                    for (int texture : this.textures) {
                        GlStateManager._glFramebufferTexture2D(GL30C.GL_DRAW_FRAMEBUFFER, GL30C.GL_COLOR_ATTACHMENT0,
                            GL30C.GL_TEXTURE_2D, texture, 0);
                        this.checkFramebuffer();
                        GL30C.glClearBufferiv(GL30C.GL_COLOR, 0, new int[4]);
                    }
                    GlStateManager._glFramebufferTexture2D(GL30C.GL_DRAW_FRAMEBUFFER, GL30C.GL_COLOR_ATTACHMENT0,
                        GL30C.GL_TEXTURE_2D, this.claims, 0);
                    GL30C.glDrawBuffer(GL30C.GL_COLOR_ATTACHMENT0);
                    this.checkFramebuffer();
                }
                GlStateManager._disableScissorTest();
                GlStateManager._glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, this.framebuffer);
                GL30C.glClearBufferuiv(GL30C.GL_COLOR, 0, new int[4]);
                for (int unit = 0; unit < 2; unit++) {
                    int[] saved = this.savedImages[unit];
                    saved[0] = GL30C.glGetIntegeri(GL42C.GL_IMAGE_BINDING_NAME, unit);
                    saved[1] = GL30C.glGetIntegeri(GL42C.GL_IMAGE_BINDING_LEVEL, unit);
                    saved[2] = GL30C.glGetIntegeri(GL42C.GL_IMAGE_BINDING_LAYERED, unit);
                    saved[3] = GL30C.glGetIntegeri(GL42C.GL_IMAGE_BINDING_LAYER, unit);
                    saved[4] = GL30C.glGetIntegeri(GL42C.GL_IMAGE_BINDING_ACCESS, unit);
                    saved[5] = GL30C.glGetIntegeri(GL42C.GL_IMAGE_BINDING_FORMAT, unit);
                }
                GL42C.glBindImageTexture(0, this.textures[1 - this.read], 0, false, 0, GL42C.GL_WRITE_ONLY, GL30C.GL_RGBA32I);
                GL42C.glBindImageTexture(1, this.claims, 0, false, 0, GL42C.GL_READ_WRITE, GL30C.GL_R32UI);
                this.previousFrame = this.invalidated ? -1 : this.frame;
                this.frame++;
                this.invalidated = false;
                this.active = true;
            } finally {
                RenderSystem.colorMask(mask.get(0) != 0, mask.get(1) != 0, mask.get(2) != 0, mask.get(3) != 0);
            }
        } finally {
            GlStateManager._glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
            GlStateManager._bindTexture(binding);
            if (scissor) GlStateManager._enableScissorTest();
        }
    }

    private int createTexture(boolean claim) {
        int texture = TextureUtil.generateTextureId();
        GlStateManager._bindTexture(texture);
        GL30C.glTexImage2D(GL30C.GL_TEXTURE_2D, 0, claim ? GL30C.GL_R32UI : GL30C.GL_RGBA32I, SIZE, SIZE, 0,
            claim ? GL30C.GL_RED_INTEGER : GL30C.GL_RGBA_INTEGER, claim ? GL30C.GL_UNSIGNED_INT : GL30C.GL_INT,
            (ByteBuffer) null);
        GlStateManager._texParameter(GL30C.GL_TEXTURE_2D, GL30C.GL_TEXTURE_MIN_FILTER, GL30C.GL_NEAREST);
        GlStateManager._texParameter(GL30C.GL_TEXTURE_2D, GL30C.GL_TEXTURE_MAG_FILTER, GL30C.GL_NEAREST);
        return texture;
    }

    private void checkFramebuffer() {
        if (GL30C.glCheckFramebufferStatus(GL30C.GL_DRAW_FRAMEBUFFER) != GL30C.GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("Incomplete Mun shadow history framebuffer");
        }
    }

    void apply(ShaderInstance shader, BlockPos origin) {
        shader.safeGetUniform("ShadowHistoryFrame").set(this.active ? this.frame : 0);
        shader.safeGetUniform("ShadowHistoryPrevious").set(this.previousFrame);
        shader.safeGetUniform("ShadowHistoryOrigin").set(origin.getX(), origin.getY(), origin.getZ());
        shader.safeGetUniform("ShadowHistoryOutput").set(0);
        shader.safeGetUniform("ShadowHistoryClaims").set(1);
        shader.setSampler("ShadowHistory", this.textures[this.read]);
    }

    void apply(int program, BlockPos origin) {
        GL30C.glUniform1i(GL30C.glGetUniformLocation(program, "ShadowHistoryFrame"), this.active ? this.frame : 0);
        GL30C.glUniform1i(GL30C.glGetUniformLocation(program, "ShadowHistoryPrevious"), this.previousFrame);
        GL30C.glUniform3i(GL30C.glGetUniformLocation(program, "ShadowHistoryOrigin"), origin.getX(), origin.getY(), origin.getZ());
        GL30C.glUniform1i(GL30C.glGetUniformLocation(program, "ShadowHistoryOutput"), 0);
        GL30C.glUniform1i(GL30C.glGetUniformLocation(program, "ShadowHistoryClaims"), 1);
        GL30C.glUniform1i(GL30C.glGetUniformLocation(program, "ShadowHistory"), 6);
        int old = GlStateManager._getActiveTexture();
        GlStateManager._activeTexture(GL30C.GL_TEXTURE6);
        GlStateManager._bindTexture(this.textures[this.read]);
        GlStateManager._activeTexture(old);
    }

    void end() {
        if (!this.active) return;
        GL42C.glMemoryBarrier(GL42C.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT | GL42C.GL_TEXTURE_FETCH_BARRIER_BIT
            | GL42C.GL_FRAMEBUFFER_BARRIER_BIT);
        for (int unit = 0; unit < 2; unit++) {
            int[] saved = this.savedImages[unit];
            GL42C.glBindImageTexture(unit, saved[0], saved[1], saved[2] != 0, saved[3], saved[4], saved[5]);
        }
        this.read = 1 - this.read;
        this.active = false;
    }

    @Override
    public void close() {
        this.end();
        for (int index = 0; index < 2; index++) {
            if (this.textures[index] != 0) TextureUtil.releaseTextureId(this.textures[index]);
            this.textures[index] = 0;
        }
        if (this.claims != 0) TextureUtil.releaseTextureId(this.claims);
        if (this.framebuffer != 0) GlStateManager._glDeleteFramebuffers(this.framebuffer);
        this.claims = 0;
        this.framebuffer = 0;
        this.frame = 0;
        this.previousFrame = -1;
        this.read = 0;
        this.invalidated = true;
    }
}
