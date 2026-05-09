package dev.dubhe.anvilcraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.api.sliding.SlidingBlockInfo;
import dev.dubhe.anvilcraft.client.renderer.entity.state.SlidingBlockRenderState;
import dev.dubhe.anvilcraft.entity.SlidingBlockEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

public class SlidingBlockRenderer extends EntityRenderer<SlidingBlockEntity, SlidingBlockRenderState> {
    public SlidingBlockRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
    }

    @Override
    public SlidingBlockRenderState createRenderState() {
        return new SlidingBlockRenderState();
    }

    @Override
    public void extractRenderState(SlidingBlockEntity entity, SlidingBlockRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        for (SlidingBlockInfo info : entity.getSection().blocks()) {
            MovingBlockRenderState moving = new MovingBlockRenderState();
            moving.randomSeedPos = entity.getStartPos();
            moving.blockPos = entity.blockPosition();
            moving.blockState = info.state();
            if (entity.level() instanceof ClientLevel clientLevel) {
                moving.biome = clientLevel.getBiome(moving.blockPos);
                moving.cardinalLighting = clientLevel.cardinalLighting();
                moving.lightEngine = clientLevel.getLightEngine();
            }
            state.getStates().put(moving, info.offset());
        }
    }

    @Override
    public void submit(
        SlidingBlockRenderState state, PoseStack pose, SubmitNodeCollector collector,
        CameraRenderState camera
    ) {
        for (Map.Entry<MovingBlockRenderState, Vec3i> entry : state.getStates().entrySet()) {
            MovingBlockRenderState moving = entry.getKey();
            BlockState blockState = moving.blockState;
            if (blockState.getRenderShape() != RenderShape.MODEL) return;
            pose.pushPose();
            pose.translate(-0.5, 0.0, -0.5);
            Vec3i offset = entry.getValue();
            pose.translate(offset.getX(), offset.getY(), offset.getZ());
            collector.submitMovingBlock(pose, moving);
            pose.popPose();
            super.submit(state, pose, collector, camera);
        }
        super.submit(state, pose, collector, camera);
    }
}
