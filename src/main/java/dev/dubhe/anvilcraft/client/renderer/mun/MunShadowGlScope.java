package dev.dubhe.anvilcraft.client.renderer.mun;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL14C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.system.MemoryStack;

/** Keeps unsupported integer/depth shadow resources isolated from native render passes. */
final class MunShadowGlScope implements AutoCloseable {
    private final int drawFramebuffer;
    private final int readFramebuffer;
    private final int activeTexture;
    private final int texture;
    private final int program;
    private final int vertexArray;
    private final int arrayBuffer;
    private final int elementBuffer;
    private final int[] viewport = new int[4];
    private final int[] scissorBox = new int[4];
    private final boolean depth;
    private final boolean blend;
    private final boolean cull;
    private final boolean scissor;
    private final boolean depthMask;
    private final int depthFunction;
    private final double clearDepth;
    private final double[] depthRange = new double[2];
    private final int colorMask;
    private final int sourceRgb;
    private final int destinationRgb;
    private final int sourceAlpha;
    private final int destinationAlpha;

    static boolean supported() {
        return "OpenGL".equalsIgnoreCase(RenderSystem.getDevice().getBackendName());
    }

    MunShadowGlScope() {
        RenderSystem.assertOnRenderThread();
        if (!supported()) throw new IllegalStateException("Mun integer shadow resources require the OpenGL backend");
        this.drawFramebuffer = GL11C.glGetInteger(GL30C.GL_DRAW_FRAMEBUFFER_BINDING);
        this.readFramebuffer = GL11C.glGetInteger(GL30C.GL_READ_FRAMEBUFFER_BINDING);
        this.activeTexture = GL11C.glGetInteger(GL13C.GL_ACTIVE_TEXTURE);
        this.texture = GL11C.glGetInteger(GL11C.GL_TEXTURE_BINDING_2D);
        this.program = GL11C.glGetInteger(GL20C.GL_CURRENT_PROGRAM);
        this.vertexArray = GL11C.glGetInteger(GL30C.GL_VERTEX_ARRAY_BINDING);
        this.arrayBuffer = GL11C.glGetInteger(GL30C.GL_ARRAY_BUFFER_BINDING);
        this.elementBuffer = GL11C.glGetInteger(GL30C.GL_ELEMENT_ARRAY_BUFFER_BINDING);
        this.depth = GL11C.glIsEnabled(GL11C.GL_DEPTH_TEST);
        this.blend = GL11C.glIsEnabled(GL11C.GL_BLEND);
        this.cull = GL11C.glIsEnabled(GL11C.GL_CULL_FACE);
        this.scissor = GL11C.glIsEnabled(GL11C.GL_SCISSOR_TEST);
        this.depthMask = GL11C.glGetBoolean(GL11C.GL_DEPTH_WRITEMASK);
        this.depthFunction = GL11C.glGetInteger(GL11C.GL_DEPTH_FUNC);
        this.clearDepth = GL11C.glGetDouble(GL11C.GL_DEPTH_CLEAR_VALUE);
        this.sourceRgb = GL11C.glGetInteger(GL14C.GL_BLEND_SRC_RGB);
        this.destinationRgb = GL11C.glGetInteger(GL14C.GL_BLEND_DST_RGB);
        this.sourceAlpha = GL11C.glGetInteger(GL14C.GL_BLEND_SRC_ALPHA);
        this.destinationAlpha = GL11C.glGetInteger(GL14C.GL_BLEND_DST_ALPHA);
        GL11C.glGetIntegerv(GL11C.GL_VIEWPORT, this.viewport);
        GL11C.glGetIntegerv(GL11C.GL_SCISSOR_BOX, this.scissorBox);
        GL11C.glGetDoublev(GL11C.GL_DEPTH_RANGE, this.depthRange);
        try (var stack = MemoryStack.stackPush()) {
            var mask = stack.malloc(4);
            GL11C.glGetBooleanv(GL11C.GL_COLOR_WRITEMASK, mask);
            this.colorMask = (mask.get(0) != 0 ? 1 : 0) | (mask.get(1) != 0 ? 2 : 0)
                | (mask.get(2) != 0 ? 4 : 0) | (mask.get(3) != 0 ? 8 : 0);
        }
    }

    @Override
    public void close() {
        GlStateManager._glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, this.drawFramebuffer);
        GlStateManager._glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, this.readFramebuffer);
        GlStateManager._viewport(this.viewport[0], this.viewport[1], this.viewport[2], this.viewport[3]);
        GL11C.glScissor(this.scissorBox[0], this.scissorBox[1], this.scissorBox[2], this.scissorBox[3]);
        if (this.depth) GlStateManager._enableDepthTest();
        else GlStateManager._disableDepthTest();
        if (this.blend) GlStateManager._enableBlend();
        else GlStateManager._disableBlend();
        if (this.cull) GlStateManager._enableCull();
        else GlStateManager._disableCull();
        if (this.scissor) GlStateManager._enableScissorTest();
        else GlStateManager._disableScissorTest();
        GlStateManager._depthMask(this.depthMask);
        GlStateManager._depthFunc(this.depthFunction);
        GL11C.glClearDepth(this.clearDepth);
        GL11C.glDepthRange(this.depthRange[0], this.depthRange[1]);
        GlStateManager._colorMask(this.colorMask);
        GlStateManager.glBlendFuncSeparate(this.sourceRgb, this.destinationRgb, this.sourceAlpha, this.destinationAlpha);
        GlStateManager._activeTexture(this.activeTexture);
        GlStateManager._bindTexture(this.texture);
        GlStateManager._glUseProgram(this.program);
        GlStateManager._glBindVertexArray(this.vertexArray);
        GlStateManager._glBindBuffer(GL30C.GL_ARRAY_BUFFER, this.arrayBuffer);
        if (this.vertexArray != 0) GlStateManager._glBindBuffer(GL30C.GL_ELEMENT_ARRAY_BUFFER, this.elementBuffer);
    }
}
