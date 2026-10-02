package dev.dubhe.anvilcraft.client.building;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class BuildingRodGhostFeatures {
    private final SubmitNodeStorage nodes = new SubmitNodeStorage();
    private final GhostBuffers buffers = new GhostBuffers();
    private final FeatureRenderDispatcher features;
    private final Map<BlockEntity, BlockEntityRenderState> states = new IdentityHashMap<>();

    BuildingRodGhostFeatures() {
        var client = Minecraft.getInstance();
        var renderBuffers = client.renderBuffers();
        this.features = new FeatureRenderDispatcher(this.nodes, client.getModelManager(), this.buffers, client.getAtlasManager(),
            renderBuffers.outlineBufferSource(), renderBuffers.crumblingBufferSource(),
            client.font, client.gameRenderer.getGameRenderState());
    }

    void clear() {
        this.states.clear();
        this.nodes.clear();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    void render(List<BlockEntity> blockEntities, List<Entity> entities, PoseStack pose, CameraRenderState camera, int alpha) {
        var client = Minecraft.getInstance();
        this.buffers.alpha = alpha;
        for (BlockEntity entity : blockEntities) {
            BlockEntityRenderer renderer = client.getBlockEntityRenderDispatcher().getRenderer(entity);
            if (renderer == null) continue;
            BlockEntityRenderState state = this.states.computeIfAbsent(entity, ignored -> renderer.createRenderState());
            renderer.extractRenderState(entity, state, 0, Vec3.ZERO, null);
            state.lightCoords = LightCoordsUtil.FULL_BRIGHT;
            pose.pushPose();
            var pos = entity.getBlockPos();
            pose.translate(pos.getX(), pos.getY(), pos.getZ());
            renderer.submit(state, pose, this.nodes, camera);
            pose.popPose();
        }
        for (Entity entity : entities) {
            var state = client.getEntityRenderDispatcher().extractEntity(entity, 0);
            state.lightCoords = LightCoordsUtil.FULL_BRIGHT;
            client.getEntityRenderDispatcher().submit(state, camera, entity.getX(), entity.getY(), entity.getZ(), pose, this.nodes);
        }
        this.features.renderAllFeatures();
        this.buffers.endBatch();
        this.features.endFrame();
        this.nodes.endFrame();
    }

    private static final class GhostBuffers extends MultiBufferSource.BufferSource {
        private int alpha;

        private GhostBuffers() {
            super(new ByteBufferBuilder(256), new LinkedHashMap<>());
        }

        @Override
        public VertexConsumer getBuffer(RenderType type) {
            return new BuildingRodRenderer.GhostConsumer(
                Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(BuildingRodRenderTypes.ghost(type)),
                BlockPos.ZERO, this.alpha);
        }

        @Override
        public void endBatch() {
            Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
        }

        @Override
        public void endBatch(RenderType type) {
            Minecraft.getInstance().renderBuffers().bufferSource().endBatch(BuildingRodRenderTypes.ghost(type));
        }
    }
}
