package dev.dubhe.anvilcraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.entity.RailgunAnvilEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;

public class RailgunAnvilRenderer extends EntityRenderer<RailgunAnvilEntity> {
    private final BlockRenderDispatcher dispatcher;
    private final ItemRenderer itemRenderer;

    public RailgunAnvilRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
        this.dispatcher = context.getBlockRenderDispatcher();
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(RailgunAnvilEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        if (entity.isReturning()) {
            pose.pushPose();
            pose.translate(0.0, 0.25, 0.0);
            pose.mulPose(Axis.YP.rotationDegrees((entity.tickCount + partialTick) * 18.0F));
            this.itemRenderer.renderStatic(
                entity.getReturnedItem(), ItemDisplayContext.GROUND, light, OverlayTexture.NO_OVERLAY,
                pose, buffers, entity.level(), entity.getId());
            pose.popPose();
            super.render(entity, yaw, partialTick, pose, buffers, light);
            return;
        }
        BlockState state = entity.getBlockState();
        pose.pushPose();
        pose.translate(-0.5, 0.0, -0.5);
        this.dispatcher.getModelRenderer().tesselateBlock(
            entity.level(), dispatcher.getBlockModel(state), state, BlockPos.containing(entity.position()), pose,
            buffers.getBuffer(RenderType.TRANSLUCENT), false, RandomSource.create(), state.getSeed(entity.blockPosition()),
            OverlayTexture.NO_OVERLAY);
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(RailgunAnvilEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
