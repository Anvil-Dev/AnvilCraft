package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.entity.WipBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;

public class WipBlockEntityRenderer implements BlockEntityRenderer<WipBlockEntity> {

    public WipBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
        WipBlockEntity wipBlockEntity,
        float v,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        int packedOverlay
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = wipBlockEntity.getLevel();
        if (level == null) return;
        poseStack.pushPose();
        BlockRenderDispatcher blockRenderDispatcher = minecraft.getBlockRenderer();
        BlockState state = wipBlockEntity.getInitialBlock();
        BakedModel bakedModel = blockRenderDispatcher.getBlockModel(state);
        RandomSource rand = RandomSource.create(state.getSeed(wipBlockEntity.getBlockPos()));
        ChunkRenderTypeSet types = bakedModel.getRenderTypes(
            state,
            rand,
            ModelData.EMPTY
        );
        for (RenderType renderType : types.asList()) {
            blockRenderDispatcher.getModelRenderer().renderModel(
                poseStack.last(),
                buffer.getBuffer(renderType),
                state,
                bakedModel,
                0,
                0,
                0,
                packedLight,
                packedOverlay
            );
        }
        poseStack.popPose();
    }
}
