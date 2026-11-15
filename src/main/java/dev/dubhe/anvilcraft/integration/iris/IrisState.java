package dev.dubhe.anvilcraft.integration.iris;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.dubhe.anvilcraft.client.renderer.RenderState;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.pathways.HandRenderer;
import net.irisshaders.iris.vertices.ImmediateState;
import net.minecraft.client.renderer.MultiBufferSource;

public class IrisState {
    public static boolean isShaderEnabled() {
        if (RenderState.isIrisPresent()) {
            return isShaderEnabledInternal();
        }
        return false;
    }

    private static boolean isShaderEnabledInternal() {
        return IrisApi.getInstance().isShaderPackInUse();
    }

    /** 缓存管线按原版顶点布局上传，创建缓冲时禁止光影扩展该布局。 */
    public static BufferBuilder createUnextendedBufferBuilder(
        ByteBufferBuilder buffer, VertexFormat.Mode mode, VertexFormat format
    ) {
        if (!RenderState.isIrisPresent()) {
            return new BufferBuilder(buffer, mode, format);
        }
        return createUnextendedBufferBuilderInternal(buffer, mode, format);
    }

    private static BufferBuilder createUnextendedBufferBuilderInternal(
        ByteBufferBuilder buffer, VertexFormat.Mode mode, VertexFormat format
    ) {
        boolean previous = ImmediateState.skipExtension.get();
        ImmediateState.skipExtension.set(true);
        try {
            return new BufferBuilder(buffer, mode, format);
        } finally {
            ImmediateState.skipExtension.set(previous);
        }
    }

    public static MultiBufferSource getHandBufferSource(MultiBufferSource fallback) {
        if (!RenderState.isIrisPresent()) return fallback;
        try {
            return getHandBufferSourceInternal(fallback);
        } catch (LinkageError ignored) {
            return fallback;
        }
    }

    private static MultiBufferSource getHandBufferSourceInternal(MultiBufferSource fallback) {
        return HandRenderer.INSTANCE.isActive() ? HandRenderer.INSTANCE.getBufferSource() : fallback;
    }

}
