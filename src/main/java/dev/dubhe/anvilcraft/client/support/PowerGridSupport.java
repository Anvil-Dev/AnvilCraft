package dev.dubhe.anvilcraft.client.support;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.api.power.PowerComponentInfo;
import dev.dubhe.anvilcraft.api.power.SimplePowerGrid;
import dev.dubhe.anvilcraft.network.PowerGridSyncChunkPacket;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.client.init.ModRenderTargets;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import dev.dubhe.anvilcraft.client.renderer.Line;
import dev.dubhe.anvilcraft.client.renderer.RenderState;
import dev.dubhe.anvilcraft.constant.Constant;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PowerGridSupport {
    private static final Map<Integer, SimplePowerGrid> GRID_MAP = Collections.synchronizedMap(new HashMap<>());
    private static final Map<Integer, PendingGridSync> PENDING = Collections.synchronizedMap(new HashMap<>());

    public static Map<Integer, SimplePowerGrid> getGridMap() {
        return PowerGridSupport.GRID_MAP;
    }

    /**
     * 渲染
     */
    public static void render(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, Vec3 camera) {
        if (Minecraft.getInstance().level == null) return;
        String level = Minecraft.getInstance().level.dimension().location().toString();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
        for (SimplePowerGrid grid : PowerGridSupport.GRID_MAP.values()) {
            if (!grid.shouldRender(camera)) continue;
            if (!grid.getLevel().equals(level)) continue;
            grid.requestGridOutline();
            for (Line line : grid.getPowerGridBoundLines()) {
                line.render(poseStack, consumer, camera, grid.getColor());
            }
        }
    }

    public static void renderEnhancedTransmitterLine(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, Vec3 camera) {
        if (!RenderState.isEnhancedRenderingAvailable() || !RenderState.isBloomEffectEnabled()) return;
        if (!AnvilCraftClient.CONFIG.renderPowerTransmitterLines) return;
        if (Minecraft.getInstance().level == null) return;
        if (ModRenderTargets.getBloomTarget() != null) {
            ModRenderTargets.getBloomTarget().setClearColor(0, 0, 0, 0);
            ModRenderTargets.getBloomTarget().clear(Minecraft.ON_OSX);
            ModRenderTargets.getBloomTarget().copyDepthFrom(Minecraft.getInstance().getMainRenderTarget());
        }
        String level = Minecraft.getInstance().level.dimension().location().toString();

        VertexConsumer consumer1 = bufferSource.getBuffer(ModRenderTypes.LINE_BLOOM);
        for (SimplePowerGrid grid : PowerGridSupport.GRID_MAP.values()) {
            if (!grid.shouldRender(camera)) continue;
            if (!grid.getLevel().equals(level)) continue;
            grid.getPowerTransmitterLines().forEach(it -> it.render(poseStack, consumer1, camera, Constant.TRANSMITTER_LINE_COLOR));
        }
        bufferSource.endBatch();
    }

    public static void renderTransmitterLine(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, Vec3 camera) {
        if (RenderState.isEnhancedRenderingAvailable() && RenderState.isBloomEffectEnabled()) return;
        if (!AnvilCraftClient.CONFIG.renderPowerTransmitterLines) return;
        if (Minecraft.getInstance().level == null) return;
        String level = Minecraft.getInstance().level.dimension().location().toString();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.LINES);
        for (SimplePowerGrid grid : PowerGridSupport.GRID_MAP.values()) {
            if (!grid.shouldRender(camera)) continue;
            if (!grid.getLevel().equals(level)) continue;
            grid.getPowerTransmitterLines().forEach(it -> it.render(poseStack, consumer, camera, Constant.TRANSMITTER_LINE_COLOR));
        }
    }

    public static void mergeSyncChunk(PowerGridSyncChunkPacket packet) {
        int key = packet.gridId();
        int totalChunks = packet.totalChunks();
        int chunkIndex = packet.chunkIndex();
        if (totalChunks <= 0 || chunkIndex < 0 || chunkIndex >= totalChunks) {
            return;
        }
        PENDING.compute(key, (id, pending) -> {
            if (pending == null || pending.totalChunks != packet.totalChunks()) {
                pending = new PendingGridSync(packet.totalChunks());
            }
            pending.chunks[packet.chunkIndex()] = List.copyOf(packet.components());
            pending.generate = packet.generate();
            pending.consume = packet.consume();
            pending.infinitePower = packet.infinitePower();
            if (pending.isComplete()) {
                List<PowerComponentInfo> all = new ArrayList<>();
                for (List<PowerComponentInfo> chunk : pending.chunks) {
                    if (chunk != null) {
                        all.addAll(chunk);
                    }
                }
                SimplePowerGrid grid = new SimplePowerGrid(
                    packet.gridId(),
                    packet.level(),
                    packet.pos(),
                    all,
                    pending.generate,
                    pending.consume,
                    pending.infinitePower
                );
                PowerGridSupport.GRID_MAP.compute(packet.gridId(), (gridId, previous) -> {
                    grid.rebuildTransmitterVisualLines(previous);
                    if (previous != null) previous.destroy();
                    return grid;
                });
                return null;
            }
            return pending;
        });
    }

    public static void removeGrid(int gridId) {
        SimplePowerGrid powerGrid = GRID_MAP.remove(gridId);
        if (powerGrid != null) {
            powerGrid.destroy();
        }
        PENDING.remove(gridId);
    }

    public static void clearAllGrid() {
        SimplePowerGrid.recreateExecutorLimitedParallelism();
        for (SimplePowerGrid value : GRID_MAP.values()) {
            value.destroy();
        }
        GRID_MAP.clear();
        PENDING.clear();
    }

    private static final class PendingGridSync {
        private final int totalChunks;
        private final List<PowerComponentInfo>[] chunks;
        private int generate;
        private int consume;
        private boolean infinitePower;

        @SuppressWarnings("unchecked")
        private PendingGridSync(int totalChunks) {
            this.totalChunks = totalChunks;
            this.chunks = new List[totalChunks];
        }

        private boolean isComplete() {
            for (List<PowerComponentInfo> chunk : chunks) {
                if (chunk == null) return false;
            }
            return true;
        }
    }
}
