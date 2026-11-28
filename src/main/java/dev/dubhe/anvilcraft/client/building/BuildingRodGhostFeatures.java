package dev.dubhe.anvilcraft.client.building;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.anvilcraft.lib.v2.rendering.cachedber.renderer.CachedBlockEntityRenderDispatcher;
import dev.dubhe.anvilcraft.client.support.HammerPreviewEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.pipeline.VertexConsumerWrapper;

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
    void render(
        List<BlockEntity> blockEntities, List<Entity> entities, PoseStack pose, CameraRenderState camera, Vec3 cameraPos, int alpha
    ) {
        var client = Minecraft.getInstance();
        this.buffers.alpha = alpha;
        this.buffers.placed = false;
        for (BlockEntity entity : blockEntities) {
            BlockEntityRenderer renderer = client.getBlockEntityRenderDispatcher().getRenderer(entity);
            pose.pushPose();
            try {
                BuildingRodRenderer.positionCell(pose, entity.getBlockPos());
                if (renderer != null) {
                    BlockEntityRenderState state = this.states.computeIfAbsent(entity, ignored -> renderer.createRenderState());
                    renderer.extractRenderState(entity, state, 0, cameraPos, null);
                    state.lightCoords = LightCoordsUtil.FULL_BRIGHT;
                    client.getBlockEntityRenderDispatcher().submit(state, pose, this.nodes, camera);
                }
                this.submitCached(entity, pose, camera, 0);
            } finally {
                pose.popPose();
            }
        }
        for (Entity entity : entities) {
            var state = client.getEntityRenderDispatcher().extractEntity(entity, 0);
            state.lightCoords = LightCoordsUtil.FULL_BRIGHT;
            client.getEntityRenderDispatcher().submit(state, camera, entity.getX(), entity.getY(), entity.getZ(), pose, this.nodes);
        }
        this.flush();
    }

    void submitPlaced(BlockEntity entity, PoseStack pose, CameraRenderState camera, float partialTick) {
        var client = Minecraft.getInstance();
        var dispatcher = client.getBlockEntityRenderDispatcher();
        var renderer = dispatcher.getRenderer(entity);
        if (renderer != null) {
            var state = renderer.createRenderState();
            renderer.extractRenderState(entity, state, partialTick, camera.pos, null);
            dispatcher.submit(state, pose, this.nodes, camera);
        }
        this.submitCached(entity, pose, camera, partialTick);
    }

    private void submitCached(BlockEntity entity, PoseStack pose, CameraRenderState camera, float partialTick) {
        var cached = HammerPreviewEntity.extractCached(entity, partialTick, Minecraft.getInstance().gameRenderer.getMainCamera());
        if (cached != null) {
            CachedBlockEntityRenderDispatcher.INSTANCE.submit(cached, pose, this.nodes, camera);
        }
    }

    void drawPlaced(int alpha) {
        this.buffers.alpha = alpha;
        this.buffers.placed = true;
        this.flush();
    }

    void close() {
        this.clear();
        this.features.close();
        this.buffers.sharedBuffer.close();
    }

    private void flush() {
        this.features.renderAllFeatures();
        this.buffers.endBatch();
        this.features.endFrame();
        this.nodes.endFrame();
    }

    private static final class GhostBuffers extends MultiBufferSource.BufferSource {
        private int alpha;
        private boolean placed;

        private GhostBuffers() {
            super(new ByteBufferBuilder(256), new LinkedHashMap<>());
        }

        @Override
        public VertexConsumer getBuffer(RenderType type) {
            var target = super.getBuffer(BuildingRodRenderTypes.ghost(type));
            if (this.placed) {
                return new VertexConsumerWrapper(target) {
                    @Override
                    public VertexConsumer setColor(int color) {
                        this.parent.setColor(ARGB.color(ARGB.alpha(color) * GhostBuffers.this.alpha / 255, color));
                        return this;
                    }

                    @Override
                    public VertexConsumer setColor(int red, int green, int blue, int opacity) {
                        this.parent.setColor(red, green, blue, opacity * GhostBuffers.this.alpha / 255);
                        return this;
                    }
                };
            }
            return new BuildingRodRenderer.GhostConsumer(target, null, this.alpha);
        }
    }
}
