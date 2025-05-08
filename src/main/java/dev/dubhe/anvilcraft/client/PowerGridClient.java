package dev.dubhe.anvilcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.SimplePowerGrid;
import dev.dubhe.anvilcraft.client.init.ModRenderTargets;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import dev.dubhe.anvilcraft.client.renderer.Line;
import dev.dubhe.anvilcraft.client.renderer.RenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PowerGridClient {
    private static final Map<Integer, SimplePowerGrid> GRID_MAP = Collections.synchronizedMap(new HashMap<>());

    public static Map<Integer, SimplePowerGrid> getGridMap() {
        return PowerGridClient.GRID_MAP;
    }

    /**
     * 渲染
     */
    public static void render(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, Vec3 camera) {
        if (Minecraft.getInstance().level == null) return;
        String level = Minecraft.getInstance().level.dimension().location().toString();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
        for (SimplePowerGrid grid : PowerGridClient.GRID_MAP.values()) {
            if (!grid.shouldRender(camera)) continue;
            if (!grid.getLevel().equals(level)) continue;
            for (Line line : grid.getPowerGridBoundLines()) {
                line.render(poseStack, consumer, camera, grid.getColor());
            }
        }
    }

    public static void renderEnhancedTransmitterLine(
        PoseStack poseStack,
        MultiBufferSource.BufferSource bufferSource,
        Vec3 camera
    ) {
        if (!RenderState.isEnhancedRenderingAvailable() || !RenderState.isBloomEffectEnabled()) return;
        if (!AnvilCraft.config.renderPowerTransmitterLines) return;
        if (Minecraft.getInstance().level == null) return;
        if (ModRenderTargets.getBloomTarget() != null) {
            ModRenderTargets.getBloomTarget().setClearColor(0, 0, 0, 0);
            ModRenderTargets.getBloomTarget().clear(Minecraft.ON_OSX);
            ModRenderTargets.getBloomTarget().copyDepthFrom(Minecraft.getInstance().getMainRenderTarget());
        }
        String level = Minecraft.getInstance().level.dimension().location().toString();

        VertexConsumer consumer1 = bufferSource.getBuffer(ModRenderTypes.LINE_BLOOM);
        for (SimplePowerGrid grid : PowerGridClient.GRID_MAP.values()) {
            if (!grid.shouldRender(camera)) continue;
            if (!grid.getLevel().equals(level)) continue;
            grid.getPowerTransmitterLines().forEach(it -> it.render(poseStack, consumer1, camera, 0x9966ccff));
        }
        bufferSource.endBatch();
    }

    public static void renderTransmitterLine(
        PoseStack poseStack,
        MultiBufferSource.BufferSource bufferSource,
        Vec3 camera
    ) {
        if (RenderState.isEnhancedRenderingAvailable() && RenderState.isBloomEffectEnabled()) return;
        if (!AnvilCraft.config.renderPowerTransmitterLines) return;
        if (Minecraft.getInstance().level == null) return;
        String level = Minecraft.getInstance().level.dimension().location().toString();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.LINES);
        for (SimplePowerGrid grid : PowerGridClient.GRID_MAP.values()) {
            if (!grid.shouldRender(camera)) continue;
            if (!grid.getLevel().equals(level)) continue;
            grid.getPowerTransmitterLines().forEach(it -> it.render(poseStack, consumer, camera, 0x9966ccff));
        }
    }

    public static void clearAllGrid() {
        SimplePowerGrid.recreateExecutor();
        for (SimplePowerGrid value : GRID_MAP.values()) {
            value.destroy();
        }
        GRID_MAP.clear();
    }
}
