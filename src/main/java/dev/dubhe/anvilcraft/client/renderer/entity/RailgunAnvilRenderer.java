package dev.dubhe.anvilcraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.client.renderer.entity.state.RailgunAnvilRenderState;
import dev.dubhe.anvilcraft.entity.RailgunAnvilEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

public class RailgunAnvilRenderer extends EntityRenderer<RailgunAnvilEntity, RailgunAnvilRenderState> {
    private final ItemModelResolver itemModelResolver;

    public RailgunAnvilRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
        this.itemModelResolver = context.getItemModelResolver();
    }

    @Override
    public RailgunAnvilRenderState createRenderState() {
        return new RailgunAnvilRenderState();
    }

    @Override
    public void extractRenderState(
        RailgunAnvilEntity entity,
        RailgunAnvilRenderState state,
        float partialTicks
    ) {
        super.extractRenderState(entity, state, partialTicks);
        state.returning = entity.isReturning();
        this.itemModelResolver.updateForNonLiving(
            state.returnedItem,
            entity.getReturnedItem(),
            ItemDisplayContext.GROUND,
            entity
        );

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

    @Override
    public void submit(
        RailgunAnvilRenderState state,
        PoseStack pose,
        SubmitNodeCollector collector,
        CameraRenderState camera
    ) {
        if (state.returning) {
            pose.pushPose();
            pose.translate(0.0, 0.25, 0.0);
            pose.mulPose(Axis.YP.rotationDegrees(state.ageInTicks * 18.0F));
            state.returnedItem.submit(
                pose,
                collector,
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                state.outlineColor
            );
            pose.popPose();
            super.submit(state, pose, collector, camera);
            return;
        }

        BlockState blockState = state.movingBlockRenderState.blockState;
        if (blockState.getRenderShape() != RenderShape.MODEL) return;
        pose.pushPose();
        pose.translate(-0.5, 0.0, -0.5);
        collector.submitMovingBlock(pose, state.movingBlockRenderState);
        pose.popPose();
        super.submit(state, pose, collector, camera);
    }
}
