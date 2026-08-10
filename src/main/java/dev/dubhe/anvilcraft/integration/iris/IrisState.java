package dev.dubhe.anvilcraft.integration.iris;

import dev.anvilcraft.lib.v2.integration.Integration;
import dev.anvilcraft.lib.v2.integration.IntegrationType;
import dev.dubhe.anvilcraft.client.init.ModRenderPipelines;
import dev.dubhe.anvilcraft.client.renderer.RenderState;
import lombok.extern.slf4j.Slf4j;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.pipeline.IrisPipelines;
import net.irisshaders.iris.pipeline.programs.ShaderKey;

@Integration(value = "iris", type = IntegrationType.CLIENT)
@Slf4j
public class IrisState {

    public void applyClient() {
        IrisState.log.info("Iris integration loaded xwx");
        IrisPipelines.assignPipeline(
            ModRenderPipelines.LIGHTNING,
            ShaderKey.TEXTURED_COLOR
        );
        IrisPipelines.assignPipeline(
            ModRenderPipelines.LASER_TRANSLUCENT,
            ShaderKey.BEACON
        );
    }

    public static boolean isShaderEnabled() {
        if (RenderState.isIrisPresent()) {
            return IrisState.isShaderEnabledInternal();
        }
        return false;
    }

    private static boolean isShaderEnabledInternal() {
        return IrisApi.getInstance().isShaderPackInUse();
    }
}
