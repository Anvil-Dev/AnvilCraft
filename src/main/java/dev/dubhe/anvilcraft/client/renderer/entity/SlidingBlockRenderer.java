package dev.dubhe.anvilcraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.api.sliding.SlidingBlockInfo;
import dev.dubhe.anvilcraft.entity.SlidingBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
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
    public void render(SlidingBlockEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffer, int packedLight) {
        for (SlidingBlockInfo info : entity.getSection().blocks()) {
            this.renderSingleBlock(
                info,
                entity.level(),
                entity.getStartPos(),
                entity.blockPosition(),
                pose,
                buffer,
                partialTick,
                packedLight
            );
        }
        super.render(entity, yaw, partialTick, pose, buffer, packedLight);
    }

    private void renderSingleBlock(
        SlidingBlockInfo info,
        Level level,
        BlockPos startPos,
        BlockPos center,
        PoseStack pose,
        MultiBufferSource buffer,
        float partialTick,
        int packedLight
    ) {
        BlockState state = info.state();
        // 这里不能直接用 state == level.getBlockState(center) 来判断中心方块，
        // 因为滑动方块总是位于实体坐标处，而中心方块可能正在被活塞恢复中。
        if (state == level.getBlockState(center)) return;
        boolean movedPiston = false;
        if (state.getRenderShape() != RenderShape.MODEL) {
            // 进程方块（MOVING_PISTON）的真实方块存在 PistonMovingBlockEntity 的 movedState 里，
            // 从中心位置取实体恢复出来继续渲染。
            if (!state.is(Blocks.MOVING_PISTON)) return;
            if (!(level.getBlockEntity(center, BlockEntityType.PISTON).orElse(null) instanceof PistonMovingBlockEntity pbe)) return;
            BlockState moved = pbe.getMovedState();
            if (moved.getRenderShape() != RenderShape.MODEL) return;
            state = moved;
            movedPiston = true;
        }
        pose.pushPose();
        final BlockPos pos = info.getPos(center);
        startPos = info.getPos(startPos);
        pose.translate(-0.5, 0.0, -0.5);
        pose.translate(info.offsetX(), info.offsetY(), info.offsetZ());

        BlockEntity blockEntity = info.blockEntity();
        if (blockEntity != null && !movedPiston) {
            // 通过网络包反序列化出来的方块实体没有世界和位置，
            // 不设置的话方块实体渲染器（例如 WIP 方块）会直接跳过渲染。
            blockEntity.setLevel(level);
            blockEntity.worldPosition = pos;
            BlockEntityRenderer<BlockEntity> renderer = Minecraft.getInstance()
                .getBlockEntityRenderDispatcher()
                .getRenderer(blockEntity);
            if (renderer != null) {
                renderer.render(blockEntity, partialTick, pose, buffer, packedLight, OverlayTexture.NO_OVERLAY);
            }
        }

        var model = this.dispatcher.getBlockModel(state);
        for (var renderType : model.getRenderTypes(state, RandomSource.create(state.getSeed(startPos)), ModelData.EMPTY)) {
            this.dispatcher.getModelRenderer().tesselateBlock(
                level,
                this.dispatcher.getBlockModel(state),
                state,
                pos,
                pose,
                buffer.getBuffer(RenderTypeHelper.getMovingBlockRenderType(renderType)),
                false,
                RandomSource.create(),
                state.getSeed(startPos),
                OverlayTexture.NO_OVERLAY,
                ModelData.EMPTY,
                renderType
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
