package dev.dubhe.anvilcraft.client.renderer.mun;

import com.mojang.blaze3d.opengl.GlStateManager;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.Objects;

public final class MunShadowCasterProbe {
    private static final MunShadowProgram OPAQUE = new MunShadowProgram(false);
    private static final MunShadowProgram TRANSLUCENT = new MunShadowProgram(true);
    private static int runs;

    public static void verify() {
        OPAQUE.validate();
        TRANSLUCENT.validate();
        int sampler = GL30C.glGetIntegeri(GL33C.GL_SAMPLER_BINDING, 0);
        try (var outer = new MunShadowGlScope()) {
            GlStateManager._activeTexture(GL13C.GL_TEXTURE0);
            try (var ignored = new MunShadowGlScope()) {
                int texture = GlStateManager._genTexture();
                try (var target = new MunShadowTarget(32, true)) {
                    GlStateManager._bindTexture(texture);
                    GL11C.glTexImage2D(GL11C.GL_TEXTURE_2D, 0, GL11C.GL_RGBA8, 1, 1, 0,
                        GL11C.GL_RGBA, GL11C.GL_UNSIGNED_BYTE, (ByteBuffer) null);
                    GlStateManager._texParameter(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MIN_FILTER, GL11C.GL_NEAREST);
                    GlStateManager._texParameter(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MAG_FILTER, GL11C.GL_NEAREST);
                    GL33C.glBindSampler(0, 0);
                    sample(target, texture, false, false, 0xFFFFFFFF, 0x00FF0000, true, -1);
                    sample(target, texture, false, false, 0x7FFFFFFF, 0xFF000000, false, -1);
                    sample(target, texture, false, false, 0x80FFFFFF, 0xFF000000, true, -1);
                    sample(target, texture, false, false, 0xFFFFFFFF, 0x7F000000, false, -1);
                    sample(target, texture, false, false, 0xFFFFFFFF, 0x80000000, true, -1);
                    sample(target, texture, true, false, 0x004080C0, 0xFFFFFFFF, false, -1);
                    sample(target, texture, true, false, 0x804080C0, 0xFFFFFFFF, true, transmission(0x804080C0, 0xFFFFFFFF));
                    sample(target, texture, true, true, 0x804080C0, 0xFFFFFFFF, false, -1);
                    sample(target, texture, true, false, 0xFF4080C0, 0xFFFFFFFF, false, -1);
                    sample(target, texture, true, true, 0xFF4080C0, 0xFFFFFFFF, true, 0);
                    sample(target, texture, true, false, 0x804080C0, 0x80CC8040, true, transmission(0x804080C0, 0x80CC8040));
                    sample(target, texture, false, false, 0xFFFFFFFF, 0xFFFF0000, true, -1, 192);
                    sample(target, texture, false, false, 0xFFFFFFFF, 0xFFFF0000, true, -1, -192);
                } finally {
                    GlStateManager._deleteTexture(texture);
                    GL33C.glBindSampler(0, sampler);
                }
            }
        }
        AnvilCraft.LOGGER.info("PORT_MUN_SHADOW_CASTER_PASSED: run={}, thirteen alpha/color/depth cases", ++runs);
    }

    private static void sample(MunShadowTarget target, int texture, boolean translucent, boolean opaque,
                               int texel, int color, boolean written, int packedColor) {
        sample(target, texture, translucent, opaque, texel, color, written, packedColor, 0);
    }

    private static void sample(MunShadowTarget target, int texture, boolean translucent, boolean opaque,
                               int texel, int color, boolean written, int packedColor, float height) {
        target.bind();
        GlStateManager._enableDepthTest();
        GlStateManager._depthFunc(GL11C.GL_LEQUAL);
        GlStateManager._disableCull();
        GlStateManager._disableBlend();
        GlStateManager._colorMask(15);
        GlStateManager._bindTexture(texture);
        try (var stack = MemoryStack.stackPush()) {
            var pixel = stack.malloc(4);
            pixel.put(0, (byte) (texel >> 16)).put(1, (byte) (texel >> 8)).put(2, (byte) texel).put(3, (byte) (texel >> 24));
            GL11C.glTexSubImage2D(GL11C.GL_TEXTURE_2D, 0, 0, 0, 1, 1, GL11C.GL_RGBA, GL11C.GL_UNSIGNED_BYTE, pixel);
        }
        var solar = new MunSolarLighting();
        solar.update(0, 0, new Vec3(0, 64, 0));
        var program = translucent ? TRANSLUCENT : OPAQUE;
        program.apply(solar, 16, 32, opaque);
        var projection = new MunShadowProjection();
        projection.update(solar.project(new Vec3(0, 64 + height, 0)), new Vec3(0, 64, 0), 16, 32);
        program.origin(new Vec3(0, 64, 0), projection.offset(new Vec3(0, 64, 0)));
        try (var mesh = mesh(color, height)) {
            mesh.draw();
        }
        try (var stack = MemoryStack.stackPush()) {
            var depth = stack.mallocFloat(1);
            GL11C.glReadPixels(16, 16, 1, 1, GL11C.GL_DEPTH_COMPONENT, GL11C.GL_FLOAT, depth);
            check(Math.abs(depth.get(0) - (written ? 0.5 - height / MunShadowProjection.DEPTH : 1)) < 0.000001,
                "Caster depth/alpha changed: " + depth.get(0) + ", texel=" + Integer.toHexString(texel)
                    + ", color=" + Integer.toHexString(color) + ", translucent=" + translucent + ", opaque=" + opaque);
            if (translucent && written) {
                GL11C.glReadBuffer(GL30C.GL_COLOR_ATTACHMENT0);
                var rg = stack.mallocInt(2);
                GL11C.glReadPixels(16, 16, 1, 1, GL30C.GL_RG_INTEGER, GL11C.GL_UNSIGNED_INT, rg);
                check(rg.get(0) == packedColor, "Transmission packing changed: " + rg.get(0) + " != " + packedColor);
                check(Math.abs(rg.get(1) - 8388607) <= 1, "Transmission lost 24-bit depth");
            }
        }
    }

    private static MunShadowMesh mesh(int color, float height) {
        try (var builder = new MunShadowMesh.Builder(6)) {
            var vertex = MunShadowMesh.Builder.class.getDeclaredMethod("coloredVertex",
                float.class, float.class, float.class, float.class, float.class, int.class);
            vertex.setAccessible(true);
            for (int index : new int[]{0, 1, 2, 0, 2, 3}) {
                vertex.invoke(builder, index == 1 || index == 2 ? 16F : -16F, height, index >= 2 ? 16F : -16F, 0.5F, 0.5F, color);
            }
            return Objects.requireNonNull(builder.finish());
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static int transmission(int texel, int color) {
        double alpha = (texel >>> 24) / 255.0 * (color >>> 24) / 255.0;
        int result = 0;
        for (int channel = 0; channel < 3; channel++) {
            int shift = 16 - channel * 8;
            double rgb = (texel >> shift & 255) / 255.0 * (color >> shift & 255) / 255.0;
            int value = (int) Math.round((1 - alpha) * (1 - alpha + alpha * rgb) * 255);
            result |= value << (channel * 8);
        }
        return result;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
