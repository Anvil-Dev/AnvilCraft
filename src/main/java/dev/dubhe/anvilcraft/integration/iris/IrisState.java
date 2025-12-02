package dev.dubhe.anvilcraft.integration.iris;

import dev.dubhe.anvilcraft.client.renderer.RenderState;
import net.irisshaders.iris.api.v0.IrisApi;

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
}
