package dev.dubhe.anvilcraft.client.renderer.mun;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.neoforge.client.GlStateBackup;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL14C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import javax.annotation.Nullable;

/** 用原版接口恢复绘制状态；原版没有查询接口的外部状态集中在这里读取。 */
final class MunRenderScope implements AutoCloseable {
    private static final int BLEND = 1;
    private static final int BLEND_FACTORS = 2;
    private static final int DEPTH = 4;
    private static final int CULL = 8;
    private static final int COLOR_MASK = 16;
    private static final int SCISSOR = 32;
    private static final int FRAMEBUFFERS = 64;
    private static final int VIEWPORT = 128;
    private static final int PROGRAM = 256;
    private static final int TEXTURE = 512;
    private static final int SHADER = 1024;
    private static final int SECOND_SAMPLER = 2048;
    private static final int DEPTH_RANGE = 4096;
    private static final int THIRD_SAMPLER = 8192;
    private final int flags;
    private final GlStateBackup state = new GlStateBackup();
    private final @Nullable ShaderInstance shader;
    private final int texture0;
    private final int texture1;
    private final int texture2;
    private int drawFramebuffer;
    private int readFramebuffer;
    private int viewportX;
    private int viewportY;
    private int viewportWidth;
    private int viewportHeight;
    private int program;
    private int activeTexture;
    private int textureBinding;
    private double nearDepth;
    private double farDepth;

    private MunRenderScope(int flags) {
        this.flags = flags;
        RenderSystem.backupGlState(this.state);
        this.shader = RenderSystem.getShader();
        this.texture0 = RenderSystem.getShaderTexture(0);
        this.texture1 = this.has(SECOND_SAMPLER) ? RenderSystem.getShaderTexture(1) : 0;
        this.texture2 = this.has(THIRD_SAMPLER) ? RenderSystem.getShaderTexture(2) : 0;
        // 第三方渲染器可能直接修改 GL，不能用原版缓存替代进入边界时的真实状态。
        if (this.has(BLEND)) this.state.blendEnabled = GL11C.glIsEnabled(GL11C.GL_BLEND);
        if (this.has(BLEND_FACTORS)) {
            this.state.blendSrcRgb = GL11C.glGetInteger(GL14C.GL_BLEND_SRC_RGB);
            this.state.blendDestRgb = GL11C.glGetInteger(GL14C.GL_BLEND_DST_RGB);
            this.state.blendSrcAlpha = GL11C.glGetInteger(GL14C.GL_BLEND_SRC_ALPHA);
            this.state.blendDestAlpha = GL11C.glGetInteger(GL14C.GL_BLEND_DST_ALPHA);
        }
        if (this.has(DEPTH)) {
            this.state.depthEnabled = GL11C.glIsEnabled(GL11C.GL_DEPTH_TEST);
            this.state.depthMask = GL11C.glGetBoolean(GL11C.GL_DEPTH_WRITEMASK);
            this.state.depthFunc = GL11C.glGetInteger(GL11C.GL_DEPTH_FUNC);
        }
        if (this.has(CULL)) this.state.cullEnabled = GL11C.glIsEnabled(GL11C.GL_CULL_FACE);
        if (this.has(SCISSOR)) this.state.scissorEnabled = GL11C.glIsEnabled(GL11C.GL_SCISSOR_TEST);
        if (this.has(FRAMEBUFFERS)) {
            this.drawFramebuffer = GL11C.glGetInteger(GL30C.GL_DRAW_FRAMEBUFFER_BINDING);
            this.readFramebuffer = GL11C.glGetInteger(GL30C.GL_READ_FRAMEBUFFER_BINDING);
        }
        if (this.has(PROGRAM)) this.program = GL11C.glGetInteger(GL20C.GL_CURRENT_PROGRAM);
        if (this.has(TEXTURE)) {
            this.activeTexture = GlStateManager._getActiveTexture();
            this.textureBinding = GL11C.glGetInteger(GL11C.GL_TEXTURE_BINDING_2D);
        }
        if (this.has(COLOR_MASK | VIEWPORT | DEPTH_RANGE)) this.captureVectors();
    }

    private void captureVectors() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            if (this.has(COLOR_MASK)) {
                ByteBuffer mask = stack.malloc(4);
                GL11C.glGetBooleanv(GL11C.GL_COLOR_WRITEMASK, mask);
                this.state.colorMaskRed = mask.get(0) != 0;
                this.state.colorMaskGreen = mask.get(1) != 0;
                this.state.colorMaskBlue = mask.get(2) != 0;
                this.state.colorMaskAlpha = mask.get(3) != 0;
            }
            if (this.has(VIEWPORT)) {
                IntBuffer viewport = stack.mallocInt(4);
                GL11C.glGetIntegerv(GL11C.GL_VIEWPORT, viewport);
                this.viewportX = viewport.get(0);
                this.viewportY = viewport.get(1);
                this.viewportWidth = viewport.get(2);
                this.viewportHeight = viewport.get(3);
            }
            if (this.has(DEPTH_RANGE)) {
                var range = stack.mallocDouble(2);
                GL11C.glGetDoublev(GL11C.GL_DEPTH_RANGE, range);
                this.nearDepth = range.get(0);
                this.farDepth = range.get(1);
            }
        }
    }

    static MunRenderScope sky() {
        return new MunRenderScope(BLEND | DEPTH | SHADER | SECOND_SAMPLER);
    }

    static MunRenderScope celestialSky() {
        return new MunRenderScope(BLEND | BLEND_FACTORS | DEPTH | CULL | SHADER | SECOND_SAMPLER | THIRD_SAMPLER);
    }

    static MunRenderScope vanillaSky() {
        return new MunRenderScope(BLEND | BLEND_FACTORS | DEPTH | CULL | SHADER | DEPTH_RANGE);
    }

    static MunRenderScope shadows() {
        return new MunRenderScope(BLEND | DEPTH | CULL | COLOR_MASK | SCISSOR | FRAMEBUFFERS | VIEWPORT | SHADER);
    }

    static MunRenderScope postProcess() {
        return new MunRenderScope(BLEND | BLEND_FACTORS | DEPTH | CULL | COLOR_MASK | SCISSOR
            | FRAMEBUFFERS | VIEWPORT | PROGRAM | TEXTURE | SHADER | SECOND_SAMPLER);
    }

    static MunRenderScope resources() {
        return new MunRenderScope(PROGRAM | TEXTURE);
    }

    static MunRenderScope targetAllocation() {
        return new MunRenderScope(FRAMEBUFFERS | TEXTURE);
    }

    void useFarDepth() {
        GL11C.glDepthRange(1, 1);
    }

    private boolean has(int flag) {
        return (this.flags & flag) != 0;
    }

    @Override
    public void close() {
        if (this.has(SHADER)) {
            RenderSystem.setShader(() -> this.shader);
            RenderSystem.setShaderTexture(0, this.texture0);
            if (this.has(SECOND_SAMPLER)) RenderSystem.setShaderTexture(1, this.texture1);
            if (this.has(THIRD_SAMPLER)) RenderSystem.setShaderTexture(2, this.texture2);
        }
        if (this.has(PROGRAM)) GlStateManager._glUseProgram(this.program);
        if (this.has(TEXTURE)) {
            GlStateManager._activeTexture(this.activeTexture);
            GlStateManager._bindTexture(this.textureBinding);
        }
        if (this.has(FRAMEBUFFERS)) {
            GlStateManager._glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, this.drawFramebuffer);
            GlStateManager._glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, this.readFramebuffer);
        }
        if (this.has(VIEWPORT)) RenderSystem.viewport(this.viewportX, this.viewportY, this.viewportWidth, this.viewportHeight);
        RenderSystem.restoreGlState(this.state);
        if (this.has(DEPTH_RANGE)) GL11C.glDepthRange(this.nearDepth, this.farDepth);
    }
}
