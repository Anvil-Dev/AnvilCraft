package dev.dubhe.anvilcraft.client.renderer.mun;

import com.mojang.blaze3d.opengl.GlStateManager;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.opengl.GL42C;
import org.lwjgl.system.MemoryStack;

import java.util.Arrays;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class MunShadowResourceProbe {
    private static boolean complete;

    @SubscribeEvent
    public static void reload(net.neoforged.neoforge.client.event.ModelEvent.BakingCompleted event) {
        complete = false;
    }

    @SubscribeEvent
    public static void frame(RenderFrameEvent.Post event) {
        if (!Boolean.getBoolean("anvilcraft.portMunShadowResources") || complete || Minecraft.getInstance().level == null) return;
        complete = true;
        MunShadowMathChecks.verify();
        try (var ignored = new MunShadowGlScope()) {
            targets();
            if (!GL.getCapabilities().GL_ARB_shader_image_load_store) {
                throw new IllegalStateException("This validation requires shader image load/store");
            }
            history();
            MunShadowMeshProbe.verify();
            MunShadowCasterProbe.verify();
        }
        AnvilCraft.LOGGER.info("PORT_MUN_SHADOW_RESOURCES_PASSED: exact formats, depth copy, integer history and state restoration");
    }

    private static void targets() {
        GlStateManager._viewport(5, 7, 11, 13);
        GlStateManager._enableScissorTest();
        GL11C.glScissor(1, 2, 3, 4);
        GlStateManager._depthMask(false);
        GlStateManager._depthFunc(GL11C.GL_GREATER);
        GlStateManager._colorMask(5);
        int[] before = state();
        int depth;
        int color;
        try (var first = new MunShadowTarget(32); var second = new MunShadowTarget(32);
             var translucent = new MunShadowTarget(32, true)) {
            check(Arrays.equals(before, state()), "Allocation changed native GL state");
            depth = first.texture();
            color = translucent.transmission();
            check(format(depth) == GL30C.GL_DEPTH_COMPONENT24, "Opaque shadow lost 24-bit depth precision");
            check(format(translucent.texture()) == GL30C.GL_DEPTH_COMPONENT24, "Transmission depth format changed");
            check(format(color) == GL30C.GL_RG32UI, "Transmission was converted to a non-integer texture");
            try (var ignored = new MunShadowGlScope()) {
                first.bind();
                check(Math.abs(depth(0, 0) - 1) < 0.000001, "Depth clear failed under inherited scissor/mask");
                GlStateManager._enableScissorTest();
                GL11C.glScissor(8, 8, 8, 8);
                GL11C.glClearDepth(0.25);
                GL11C.glClear(GL11C.GL_DEPTH_BUFFER_BIT);
                second.bind();
                second.copyDepthFrom(first);
                check(Math.abs(depth(10, 10) - 0.25) < 0.000001 && Math.abs(depth(0, 0) - 1) < 0.000001,
                    "Depth copy cleared or changed the static shadow");
                translucent.bind();
                GL11C.glReadBuffer(GL30C.GL_COLOR_ATTACHMENT0);
                check(Arrays.equals(rg(), new int[]{0xFFFFFF, 0xFFFFFF}), "Transmission clear values changed");
                GL30C.glClearBufferuiv(GL11C.GL_COLOR, 0, new int[]{0x123456, 0xABCDEF, 0, 0});
                check(Arrays.equals(rg(), new int[]{0x123456, 0xABCDEF}), "Integer transmission values lost bits");
            }
        }
        check(!GL11C.glIsTexture(depth) && !GL11C.glIsTexture(color), "Shadow resources were not released");
    }

    private static int[] state() {
        int[] viewport = new int[4];
        GL11C.glGetIntegerv(GL11C.GL_VIEWPORT, viewport);
        return new int[]{GL11C.glGetInteger(GL30C.GL_DRAW_FRAMEBUFFER_BINDING),
            GL11C.glGetInteger(GL30C.GL_READ_FRAMEBUFFER_BINDING), GL11C.glGetInteger(GL11C.GL_TEXTURE_BINDING_2D),
            viewport[0], viewport[1], viewport[2], viewport[3], GL11C.glGetBoolean(GL11C.GL_DEPTH_WRITEMASK) ? 1 : 0,
            GL11C.glGetInteger(GL11C.GL_DEPTH_FUNC), GL11C.glIsEnabled(GL11C.GL_SCISSOR_TEST) ? 1 : 0};
    }

    private static int format(int texture) {
        try (var ignored = new MunShadowGlScope()) {
            GlStateManager._bindTexture(texture);
            return GL11C.glGetTexLevelParameteri(GL11C.GL_TEXTURE_2D, 0, GL11C.GL_TEXTURE_INTERNAL_FORMAT);
        }
    }

    private static float depth(int x, int y) {
        try (var stack = MemoryStack.stackPush()) {
            var value = stack.mallocFloat(1);
            GL11C.glReadPixels(x, y, 1, 1, GL11C.GL_DEPTH_COMPONENT, GL11C.GL_FLOAT, value);
            return value.get(0);
        }
    }

    private static int[] rg() {
        try (var stack = MemoryStack.stackPush()) {
            var value = stack.mallocInt(2);
            GL11C.glReadPixels(0, 0, 1, 1, GL30C.GL_RG_INTEGER, GL11C.GL_UNSIGNED_INT, value);
            return new int[]{value.get(0), value.get(1)};
        }
    }

    private static void history() {
        final int program = program();
        final int vao = GlStateManager._glGenVertexArrays();
        final int readFramebuffer = GlStateManager.glGenFramebuffers();
        int sentinelSampler = GL33C.glGenSamplers();
        GL33C.glSamplerParameteri(sentinelSampler, GL11C.GL_TEXTURE_MIN_FILTER, GL11C.GL_LINEAR);
        GL33C.glSamplerParameteri(sentinelSampler, GL11C.GL_TEXTURE_MAG_FILTER, GL11C.GL_LINEAR);
        int originalHistoryTexture = textureBinding(6);
        int originalSampler = GL30C.glGetIntegeri(GL33C.GL_SAMPLER_BINDING, 6);
        GL33C.glBindSampler(6, sentinelSampler);
        int[] image0 = imageBinding(0);
        int[] image1 = imageBinding(1);
        try (var ignored = new MunShadowGlScope(); var target = new MunShadowTarget(4); var history = new MunShadowHistory()) {
            GlStateManager._glUseProgram(program);
            GlStateManager._glBindVertexArray(vao);
            GlStateManager._disableDepthTest();
            GlStateManager._disableCull();
            history.begin();
            int written = GL30C.glGetIntegeri(GL42C.GL_IMAGE_BINDING_NAME, 0);
            int claims = GL30C.glGetIntegeri(GL42C.GL_IMAGE_BINDING_NAME, 1);
            check(format(written) == GL30C.GL_RGBA32I && format(claims) == GL30C.GL_R32UI, "History formats changed");
            target.bind();
            history.apply(program, new BlockPos(7, 8, 9));
            check(GL30C.glGetIntegeri(GL33C.GL_SAMPLER_BINDING, 6) == 0, "Integer history inherited a linear sampler");
            GL11C.glDrawArrays(GL11C.GL_TRIANGLES, 0, 3);
            GL42C.glMemoryBarrier(GL42C.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
            history.apply(program, new BlockPos(70, 80, 90));
            GL11C.glDrawArrays(GL11C.GL_TRIANGLES, 0, 3);
            history.end();
            check(GL30C.glGetIntegeri(GL33C.GL_SAMPLER_BINDING, 6) == sentinelSampler, "History sampler binding leaked");
            check(textureBinding(6) == originalHistoryTexture, "History texture binding leaked");
            check(Arrays.equals(image0, imageBinding(0)) && Arrays.equals(image1, imageBinding(1)), "Image bindings leaked");
            check(Arrays.equals(readHistory(readFramebuffer, written), new int[]{7, 8, 9, 1}), "History claim ownership failed");
            history.begin();
            int next = GL30C.glGetIntegeri(GL42C.GL_IMAGE_BINDING_NAME, 0);
            check(next != written, "History did not swap textures");
            target.bind();
            history.apply(program, new BlockPos(4, 5, 6));
            GL11C.glDrawArrays(GL11C.GL_TRIANGLES, 0, 3);
            history.end();
            check(Arrays.equals(readHistory(readFramebuffer, next), new int[]{7, 8, 9, 2}), "Previous-frame history was not readable");
            history.invalidate();
            history.begin();
            written = GL30C.glGetIntegeri(GL42C.GL_IMAGE_BINDING_NAME, 0);
            target.bind();
            history.apply(program, new BlockPos(4, 5, 6));
            GL11C.glDrawArrays(GL11C.GL_TRIANGLES, 0, 3);
            history.end();
            check(Arrays.equals(readHistory(readFramebuffer, written), new int[]{4, 5, 6, 3}), "Invalidation reused stale history");
            history.close();
            history.apply(program, BlockPos.ZERO);
            check(GL30C.glGetIntegeri(GL33C.GL_SAMPLER_BINDING, 6) == sentinelSampler, "Inactive history changed texture sampling");
        } finally {
            GL33C.glBindSampler(6, originalSampler);
            GL33C.glDeleteSamplers(sentinelSampler);
            GlStateManager._glDeleteFramebuffers(readFramebuffer);
            GL30C.glDeleteVertexArrays(vao);
            GL20C.glDeleteProgram(program);
        }
    }

    private static int textureBinding(int unit) {
        int previous = GL11C.glGetInteger(GL30C.GL_ACTIVE_TEXTURE);
        GlStateManager._activeTexture(GL30C.GL_TEXTURE0 + unit);
        int texture = GL11C.glGetInteger(GL11C.GL_TEXTURE_BINDING_2D);
        GlStateManager._activeTexture(previous);
        return texture;
    }

    private static int[] imageBinding(int unit) {
        return new int[]{GL30C.glGetIntegeri(GL42C.GL_IMAGE_BINDING_NAME, unit),
            GL30C.glGetIntegeri(GL42C.GL_IMAGE_BINDING_LEVEL, unit), GL30C.glGetIntegeri(GL42C.GL_IMAGE_BINDING_LAYERED, unit),
            GL30C.glGetIntegeri(GL42C.GL_IMAGE_BINDING_LAYER, unit), GL30C.glGetIntegeri(GL42C.GL_IMAGE_BINDING_ACCESS, unit),
            GL30C.glGetIntegeri(GL42C.GL_IMAGE_BINDING_FORMAT, unit)};
    }

    private static int[] readHistory(int framebuffer, int texture) {
        GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, framebuffer);
        GlStateManager._glFramebufferTexture2D(GL30C.GL_FRAMEBUFFER, GL30C.GL_COLOR_ATTACHMENT0, GL11C.GL_TEXTURE_2D, texture, 0);
        GL11C.glReadBuffer(GL30C.GL_COLOR_ATTACHMENT0);
        try (var stack = MemoryStack.stackPush()) {
            var data = stack.mallocInt(4);
            GL11C.glReadPixels(0, 0, 1, 1, GL30C.GL_RGBA_INTEGER, GL11C.GL_INT, data);
            return new int[]{data.get(0), data.get(1), data.get(2), data.get(3)};
        }
    }

    private static int program() {
        int vertex = shader(GL20C.GL_VERTEX_SHADER, """
            #version 150
            void main() {
                vec2 p = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);
                gl_Position = vec4(p * 2.0 - 1.0, 0.0, 1.0);
            }
            """);
        int fragment = shader(GL20C.GL_FRAGMENT_SHADER, """
            #version 150
            #extension GL_ARB_shader_image_load_store : require
            layout(rgba32i) uniform writeonly iimage2D ShadowHistoryOutput;
            layout(r32ui) uniform uimage2D ShadowHistoryClaims;
            uniform isampler2D ShadowHistory;
            uniform int ShadowHistoryFrame;
            uniform int ShadowHistoryPrevious;
            uniform ivec3 ShadowHistoryOrigin;
            void main() {
                ivec2 p = ivec2(gl_FragCoord.xy);
                if (imageAtomicCompSwap(ShadowHistoryClaims, p, 0u, uint(ShadowHistoryOrigin.x)) == 0u) {
                    ivec4 value = ShadowHistoryPrevious > 0 ? texelFetch(ShadowHistory, p, 0) + ivec4(0, 0, 0, 1)
                        : ivec4(ShadowHistoryOrigin, ShadowHistoryFrame);
                    imageStore(ShadowHistoryOutput, p, value);
                }
            }
            """);
        int program = GL20C.glCreateProgram();
        GL20C.glAttachShader(program, vertex);
        GL20C.glAttachShader(program, fragment);
        GL20C.glLinkProgram(program);
        GL20C.glDeleteShader(vertex);
        GL20C.glDeleteShader(fragment);
        check(GL20C.glGetProgrami(program, GL20C.GL_LINK_STATUS) != 0, GL20C.glGetProgramInfoLog(program));
        return program;
    }

    private static int shader(int type, String source) {
        int shader = GL20C.glCreateShader(type);
        GL20C.glShaderSource(shader, source);
        GL20C.glCompileShader(shader);
        check(GL20C.glGetShaderi(shader, GL20C.GL_COMPILE_STATUS) != 0, GL20C.glGetShaderInfoLog(shader));
        return shader;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
