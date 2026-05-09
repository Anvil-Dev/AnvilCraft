package dev.dubhe.anvilcraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.entity.AnimateAscendingBlockEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.FallingBlockRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

public class AscendingBlockRenderer extends EntityRenderer<AnimateAscendingBlockEntity, FallingBlockRenderState> {
    /**
     * 上升方块渲染器
     */
    public AscendingBlockRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
    }

    public FallingBlockRenderState createRenderState() {
        return new FallingBlockRenderState();
    }

    public void extractRenderState(AnimateAscendingBlockEntity entity, FallingBlockRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        BlockPos pos = BlockPos.containing(entity.getX(), entity.getBoundingBox().maxY, entity.getZ());
        state.movingBlockRenderState.randomSeedPos = entity.getStartPos();
        state.movingBlockRenderState.blockPos = pos;
        state.movingBlockRenderState.blockState = entity.getBlockState();
        if (entity.level() instanceof ClientLevel clientLevel) {
            state.movingBlockRenderState.biome = clientLevel.getBiome(pos);
            state.movingBlockRenderState.cardinalLighting = clientLevel.cardinalLighting();
            state.movingBlockRenderState.lightEngine = clientLevel.getLightEngine();
        }
    }

    public void submit(FallingBlockRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        BlockState blockState = state.movingBlockRenderState.blockState;
        if (blockState.getRenderShape() != RenderShape.MODEL) return;
        pose.pushPose();
        pose.translate(-0.5, 0.0, -0.5);
        collector.submitMovingBlock(pose, state.movingBlockRenderState);
        pose.popPose();
        super.submit(state, pose, collector, camera);
    }
}
