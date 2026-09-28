package dev.dubhe.anvilcraft.client.support;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.anvilcraft.lib.v2.util.client.Line;
import dev.dubhe.anvilcraft.api.power.SimplePowerGrid;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import dev.dubhe.anvilcraft.client.renderer.RenderState;
import dev.dubhe.anvilcraft.constant.Constant;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PowerGridSupport {
    private static final Map<Integer, SimplePowerGrid> GRID_MAP = Collections.synchronizedMap(new HashMap<>());

    public static Map<Integer, SimplePowerGrid> getGridMap() {
        return PowerGridSupport.GRID_MAP;
    }

    /// 渲染
    public static void submitPowerGridBounds(PoseStack poseStack, SubmitNodeCollector nodeCollector, Vec3 camera) {
        if (Minecraft.getInstance().level == null) return;
        String level = Minecraft.getInstance().level.dimension().identifier().toString();
        List<SimplePowerGrid> gridsToRender = new ArrayList<>();
        for (SimplePowerGrid grid : PowerGridSupport.GRID_MAP.values()) {
            if (!grid.shouldRender(camera)) continue;
            if (!grid.getLevel().equals(level)) continue;
            grid.requestGridOutline();
            if (grid.getPowerGridBoundLines().isEmpty()) continue;
            gridsToRender.add(grid);
        }
        if (gridsToRender.isEmpty()) return;
        float width = lineWidth();
        nodeCollector.submitCustomGeometry(
            poseStack, RenderTypes.lines(), (pose, buffer) -> {
                for (SimplePowerGrid grid : gridsToRender) {
                    for (Line line : grid.getPowerGridBoundLines()) {
                        renderLine(line, pose, buffer, camera, grid.getColor(), width);
                    }
                }
            }
        );
    }

    public static void submitEnhancedTransmitterLine(PoseStack poseStack, SubmitNodeCollector collector, Vec3 camera) {
        if (!RenderState.isEnhancedRenderingAvailable() || !RenderState.isBloomEffectEnabled()) return;
        submitTransmitterLines(poseStack, collector, camera, ModRenderTypes.LINE_BLOOM);
    }

    public static void submitTransmitterLine(PoseStack poseStack, SubmitNodeCollector collector, Vec3 camera) {
        if (RenderState.isEnhancedRenderingAvailable() && RenderState.isBloomEffectEnabled()) return;
        submitTransmitterLines(poseStack, collector, camera, RenderTypes.lines());
    }

    private static void submitTransmitterLines(
        PoseStack poseStack, SubmitNodeCollector collector, Vec3 camera,
        net.minecraft.client.renderer.rendertype.RenderType renderType
    ) {
        if (!AnvilCraftClient.CONFIG.renderPowerTransmitterLines || Minecraft.getInstance().level == null) return;
        String level = Minecraft.getInstance().level.dimension().identifier().toString();
        List<Line> lines = new ArrayList<>();
        synchronized (GRID_MAP) {
            for (SimplePowerGrid grid : GRID_MAP.values()) {
                if (grid.shouldRender(camera) && grid.getLevel().equals(level)) lines.addAll(grid.getPowerTransmitterLines());
            }
        }
        if (lines.isEmpty()) return;
        float width = lineWidth();
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            for (Line line : lines) renderLine(line, pose, buffer, camera, Constant.TRANSMITTER_LINE_COLOR, width);
        });
    }

    private static float lineWidth() {
        return Math.max(2.5F, Minecraft.getInstance().getWindow().getWidth() / 1920.0F * 2.5F);
    }

    private static void renderLine(Line line, PoseStack.Pose pose, VertexConsumer buffer, Vec3 camera, int color, float width) {
        float dx = (float) (line.start().x - line.end().x) / line.length();
        float dy = (float) (line.start().y - line.end().y) / line.length();
        float dz = (float) (line.start().z - line.end().z) / line.length();
        buffer.addVertex(pose.pose(), (float) (line.start().x - camera.x), (float) (line.start().y - camera.y),
            (float) (line.start().z - camera.z)).setColor(color).setLineWidth(width).setNormal(pose, dx, dy, dz);
        buffer.addVertex(pose.pose(), (float) (line.end().x - camera.x), (float) (line.end().y - camera.y),
            (float) (line.end().z - camera.z)).setColor(color).setLineWidth(width).setNormal(pose, dx, dy, dz);
    }

    public static void clearAllGrid() {
        SimplePowerGrid.recreateExecutorLimitedParallelism();
        for (SimplePowerGrid value : PowerGridSupport.GRID_MAP.values()) {
            value.destroy();
        }
        PowerGridSupport.GRID_MAP.clear();
    }
}
