package dev.dubhe.anvilcraft.integration.iris;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.irisshaders.iris.pathways.HandRenderer;
import net.irisshaders.iris.shadows.ShadowRenderingState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/** Keeps celestial emission and atmosphere out of the shader pack's color processing. */
@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class CelestialIrisRenderer {
    private static final int MAX_DRAWS = 8192;
    private static final List<Draw> DRAWS = new ArrayList<>();
    private static final List<Draw> ATMOSPHERES = new ArrayList<>();
    private static boolean collecting;
    private static boolean failed;

    private CelestialIrisRenderer() {
    }

    public static void beginFrame() {
        DRAWS.clear();
        ATMOSPHERES.clear();
        collecting = !failed;
    }

    public static void submit(
        PoseStack pose, OrderedSubmitNodeCollector collector, RenderType type,
        SubmitNodeCollector.CustomGeometryRenderer renderer, boolean atmosphere
    ) {
        if (!IrisState.isShaderEnabled()) {
            collector.submitCustomGeometry(pose, type, renderer);
            return;
        }
        collector.submitCustomGeometry(pose, type, (matrix, vertices) -> {
            if (!defer(matrix, type, renderer, atmosphere)) renderer.render(matrix, vertices);
        });
    }

    private static boolean defer(
        PoseStack.Pose pose, RenderType type, SubmitNodeCollector.CustomGeometryRenderer renderer, boolean atmosphere
    ) {
        if (!collecting || HandRenderer.INSTANCE.isActive()) return false;
        if (ShadowRenderingState.areShadowsCurrentlyBeingRendered()) return true;
        var projection = RenderSystem.getProjectionMatrixBuffer();
        var fog = RenderSystem.getShaderFog();
        if (projection == null || fog == null || DRAWS.size() + ATMOSPHERES.size() >= MAX_DRAWS) return false;
        var target = atmosphere ? ATMOSPHERES : DRAWS;
        target.add(new Draw(pose.copy(), type, renderer, new Matrix4f(RenderSystem.getModelViewMatrix()),
            projection, RenderSystem.getProjectionType(), fog));
        return true;
    }

    public static void render() {
        collecting = false;
        if (DRAWS.isEmpty() && ATMOSPHERES.isEmpty()) return;
        var projection = RenderSystem.getProjectionMatrixBuffer();
        var projectionType = RenderSystem.getProjectionType();
        var fog = RenderSystem.getShaderFog();
        var color = RenderSystem.outputColorTextureOverride;
        var depth = RenderSystem.outputDepthTextureOverride;
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        try {
            var target = Minecraft.getInstance().getMainRenderTarget();
            RenderSystem.outputColorTextureOverride = target.getColorTextureView();
            RenderSystem.outputDepthTextureOverride = target.getDepthTextureView();
            for (Draw draw : DRAWS) draw.render();
            for (Draw draw : ATMOSPHERES) draw.render();
        } catch (RuntimeException exception) {
            failed = true;
            AnvilCraft.LOGGER.warn("Celestial Iris composition unavailable; using native rendering", exception);
        } finally {
            modelView.popMatrix();
            if (projection != null) RenderSystem.setProjectionMatrix(projection, projectionType);
            if (fog != null) RenderSystem.setShaderFog(fog);
            RenderSystem.outputColorTextureOverride = color;
            RenderSystem.outputDepthTextureOverride = depth;
            DRAWS.clear();
            ATMOSPHERES.clear();
        }
    }

    @SubscribeEvent
    public static void reload(ModelEvent.BakingCompleted event) {
        reset();
    }

    public static void reset() {
        DRAWS.clear();
        ATMOSPHERES.clear();
        collecting = false;
        failed = false;
    }

    private record Draw(PoseStack.Pose pose, RenderType type, SubmitNodeCollector.CustomGeometryRenderer geometry,
                        Matrix4f modelView, GpuBufferSlice projection, ProjectionType projectionType, GpuBufferSlice fog) {
        private void render() {
            RenderSystem.getModelViewStack().set(this.modelView);
            RenderSystem.setProjectionMatrix(this.projection, this.projectionType);
            RenderSystem.setShaderFog(this.fog);
            var vertices = Tesselator.getInstance().begin(this.type.mode(), this.type.format());
            this.geometry.render(this.pose, vertices);
            var mesh = vertices.build();
            if (mesh != null) this.type.draw(mesh);
        }
    }
}
