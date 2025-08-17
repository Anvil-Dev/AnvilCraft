package dev.dubhe.anvilcraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.api.sliding.SlidingBlockInfo;
import dev.dubhe.anvilcraft.entity.SlidingBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.model.data.ModelData;

public class SlidingBlockRenderer extends EntityRenderer<SlidingBlockEntity> {
    private final BlockRenderDispatcher dispatcher;

    public SlidingBlockRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
        this.dispatcher = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(SlidingBlockEntity entity, float yaw, float pTicks, PoseStack pose, MultiBufferSource buffer, int packedLight) {
        for (SlidingBlockInfo info : entity.getSection().blocks()) {
            this.renderSingleBlock(info, entity.level(), entity.getStartPos(), entity.blockPosition(), pose, buffer);
        }
        super.render(entity, yaw, pTicks, pose, buffer, packedLight);
    }

    private void renderSingleBlock(
        SlidingBlockInfo info, Level level,
        BlockPos startPos, BlockPos center,
        PoseStack pose, MultiBufferSource buffer
    ) {
        BlockState state = info.state();
        if (state.getRenderShape() != RenderShape.MODEL) return;
        if (state == level.getBlockState(center) || state.getRenderShape() == RenderShape.INVISIBLE) return;
        pose.pushPose();
        BlockPos pos = info.getPos(center);
        startPos = info.getPos(startPos);
        pose.translate(-0.5, 0.0, -0.5);
        pose.translate(info.x(), info.y(), info.z());
        var model = this.dispatcher.getBlockModel(state);
        for (var renderType : model.getRenderTypes(state, RandomSource.create(state.getSeed(startPos)), ModelData.EMPTY)) {
            this.dispatcher
                .getModelRenderer()
                .tesselateBlock(
                    level, this.dispatcher.getBlockModel(state), state, pos,
                    pose, buffer.getBuffer(RenderTypeHelper.getMovingBlockRenderType(renderType)),
                    false,
                    RandomSource.create(), state.getSeed(startPos),
                    OverlayTexture.NO_OVERLAY, ModelData.EMPTY, renderType
                );
        }
        pose.popPose();
    }

    /**
     * Returns the location of an entity's texture.
     */
    @SuppressWarnings("deprecation")
    @Override
    public ResourceLocation getTextureLocation(SlidingBlockEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
