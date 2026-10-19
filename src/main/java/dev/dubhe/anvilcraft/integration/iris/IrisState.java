package dev.dubhe.anvilcraft.integration.iris;

import dev.dubhe.anvilcraft.client.renderer.RenderState;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.pathways.HandRenderer;
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
