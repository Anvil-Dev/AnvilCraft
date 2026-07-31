package dev.dubhe.anvilcraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.api.sliding.SlidingBlockInfo;
import dev.dubhe.anvilcraft.client.renderer.entity.state.SlidingBlockRenderState;
import dev.dubhe.anvilcraft.entity.SlidingBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
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

            BlockEntityRenderState beState = null;
            BlockEntity be = info.blockEntity();
            if (be != null) {
                Minecraft minecraft = Minecraft.getInstance();
                BlockEntityRenderer<BlockEntity, BlockEntityRenderState> renderer = minecraft
                    .getBlockEntityRenderDispatcher()
                    .getRenderer(be);
                if (renderer == null) return;
                beState = renderer.createRenderState();
                renderer.extractRenderState(
                    be,
                    beState,
                    partialTicks,
                    minecraft.gameRenderer.getMainCamera().position(),
                    null
                );
            }

            state.getPairs().put(info.offset(), new SlidingBlockRenderState.RenderPair(moving, beState));
        }
    }

    @Override
    public void submit(
        SlidingBlockRenderState state, PoseStack pose, SubmitNodeCollector collector,
        CameraRenderState camera
    ) {
        for (Map.Entry<Vec3i, SlidingBlockRenderState.RenderPair> entry : state.getPairs().entrySet()) {
            SlidingBlockRenderState.RenderPair pair = entry.getValue();
            MovingBlockRenderState moving = pair.block();
            BlockState blockState = moving.blockState;
            if (blockState.getRenderShape() == RenderShape.INVISIBLE) return;
            pose.pushPose();
            pose.translate(-0.5, 0.0, -0.5);
            Vec3i offset = entry.getKey();
            pose.translate(offset.getX(), offset.getY(), offset.getZ());
            collector.submitMovingBlock(pose, moving);
            if (pair.entity() != null) {
                BlockEntityRenderer<BlockEntity, BlockEntityRenderState> renderer = Minecraft.getInstance()
                    .getBlockEntityRenderDispatcher()
                    .getRenderer(pair.entity());
                if (renderer != null) {
                    renderer.submit(pair.entity(), pose, collector, camera);
                }
            }
            pose.popPose();
        }
        super.submit(state, pose, collector, camera);
    }
}
