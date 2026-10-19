package dev.dubhe.anvilcraft.integration.iris;

import dev.anvilcraft.lib.v2.integration.Integration;
import dev.anvilcraft.lib.v2.integration.IntegrationType;
import dev.dubhe.anvilcraft.client.init.ModRenderPipelines;
import dev.dubhe.anvilcraft.client.renderer.RenderState;
import dev.dubhe.anvilcraft.mixin.compat.IrisHandRendererAccessor;
import lombok.extern.slf4j.Slf4j;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.pathways.HandRenderer;
import net.irisshaders.iris.pipeline.IrisPipelines;
import net.irisshaders.iris.pipeline.programs.ShaderKey;
import net.minecraft.client.renderer.SubmitNodeStorage;

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

    public static SubmitNodeStorage getHandSubmitNodeStorage(SubmitNodeStorage fallback) {
        return RenderState.isIrisPresent() ? getHandSubmitNodeStorageInternal(fallback) : fallback;
    }

    private static SubmitNodeStorage getHandSubmitNodeStorageInternal(SubmitNodeStorage fallback) {
        HandRenderer renderer = HandRenderer.INSTANCE;
        return renderer.isActive() ? ((IrisHandRendererAccessor) renderer).anvilcraft$getSubmitNodeCollector() : fallback;
    }

}
