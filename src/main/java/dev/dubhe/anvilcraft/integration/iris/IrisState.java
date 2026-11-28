package dev.dubhe.anvilcraft.integration.iris;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.anvilcraft.lib.v2.integration.Integration;
import dev.anvilcraft.lib.v2.integration.IntegrationType;
import dev.dubhe.anvilcraft.client.init.ModRenderPipelines;
import dev.dubhe.anvilcraft.client.renderer.RenderState;
import dev.dubhe.anvilcraft.mixin.compat.IrisHandRendererAccessor;
import lombok.extern.slf4j.Slf4j;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.pathways.HandRenderer;
import net.irisshaders.iris.pipeline.IrisPipelines;
import net.irisshaders.iris.pipeline.programs.ShaderKey;
import net.irisshaders.iris.vertices.IrisVertexFormats;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeStorage;

@Integration(value = "iris", type = IntegrationType.CLIENT)
@Slf4j
public class IrisState {
    private static final boolean HAS_IRIS_API = IrisState.class.getClassLoader()
        .getResource("net/irisshaders/iris/Iris.class") != null;

    public void applyClient() {
        if (!hasIrisApi()) {
            return;
        }
        copyPipeline(RenderPipelines.TRANSLUCENT_BLOCK, ModRenderPipelines.BUILDING_ROD_GHOST);
        copyPipeline(RenderPipelines.TRANSLUCENT_BLOCK, ModRenderPipelines.BUILDING_ROD_PLACED);
        copyPipeline(RenderPipelines.TRANSLUCENT_BLOCK, ModRenderPipelines.PLACEMENT_GHOST);
        copyPipeline(RenderPipelines.TRANSLUCENT_BLOCK, ModRenderPipelines.COLORED_OVERLAY);
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

    public static void copyPipeline(RenderPipeline source, RenderPipeline target) {
        if (hasIrisApi()) {
            copyPipelineInternal(source, target);
        }
    }

    private static void copyPipelineInternal(RenderPipeline source, RenderPipeline target) {
        IrisPipelines.copyPipeline(source, target);
    }

    public static boolean isBlockFormat(VertexFormat format) {
        return format == DefaultVertexFormat.BLOCK || hasIrisApi() && isBlockFormatInternal(format);
    }

    private static boolean isBlockFormatInternal(VertexFormat format) {
        return format == IrisVertexFormats.TERRAIN;
    }

    public static boolean isRenderingShadows() {
        return hasIrisApi() && isRenderingShadowsInternal();
    }

    private static boolean isRenderingShadowsInternal() {
        return IrisApi.getInstance().isRenderingShadowPass();
    }

    public static int shaderGeneration() {
        return hasIrisApi() ? shaderGenerationInternal() : 0;
    }

    private static int shaderGenerationInternal() {
        return Iris.getPipelineManager().getVersionCounterForSodiumShaderReload();
    }

    public static boolean isShaderEnabled() {
        if (hasIrisApi()) {
            return IrisState.isShaderEnabledInternal();
        }
        return false;
    }

    private static boolean isShaderEnabledInternal() {
        return IrisApi.getInstance().isShaderPackInUse();
    }

    public static SubmitNodeStorage getHandSubmitNodeStorage(SubmitNodeStorage fallback) {
        return hasIrisApi() ? getHandSubmitNodeStorageInternal(fallback) : fallback;
    }

    private static SubmitNodeStorage getHandSubmitNodeStorageInternal(SubmitNodeStorage fallback) {
        HandRenderer renderer = HandRenderer.INSTANCE;
        return renderer.isActive() ? ((IrisHandRendererAccessor) renderer).anvilcraft$getSubmitNodeCollector() : fallback;
    }

    private static boolean hasIrisApi() {
        return HAS_IRIS_API && RenderState.isIrisPresent();
    }
}
