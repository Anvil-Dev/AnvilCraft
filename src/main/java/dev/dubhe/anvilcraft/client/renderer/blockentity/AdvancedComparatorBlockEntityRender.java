package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.AdvancedComparatorBlock;
import dev.dubhe.anvilcraft.block.entity.AdvancedComparatorBlockEntity;
import dev.dubhe.anvilcraft.client.selection.ModelSelectionRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.neoforge.client.model.data.ModelData;

public class AdvancedComparatorBlockEntityRender
    implements BlockEntityRenderer<AdvancedComparatorBlockEntity>, ModelSelectionRenderer<AdvancedComparatorBlockEntity> {
    private static final ModelResourceLocation INDICATOR = ModelResourceLocation.standalone(
        AnvilCraft.of("block/advanced_comparator_indicator")
    );

    @SuppressWarnings("unused")
    public AdvancedComparatorBlockEntityRender(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
        AdvancedComparatorBlockEntity blockEntity,
        float tickDelta,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int light,
        int overlay
    ) {
        // noinspection DataFlowIssue
        this.collectSelectionModels(blockEntity, tickDelta, poseStack, (model, pose) -> Minecraft.getInstance()
            .getBlockRenderer()
            .getModelRenderer()
            .renderModel(
                pose.last(),
                bufferSource.getBuffer(RenderType.cutout()),
                null,
                Minecraft.getInstance().getModelManager().getModel(model),
                0, 0, 0,
                light,
                overlay,
                ModelData.EMPTY,
                null
        ));
    }

    @Override
    public void collectSelectionModels(
        AdvancedComparatorBlockEntity blockEntity, float partialTick, PoseStack poseStack, ModelConsumer consumer
    ) {
        poseStack.pushPose();
        MountedRedstonePose.apply(poseStack, blockEntity.getBlockState());
        poseStack.translate(0, blockEntity.getBlockState().getValue(AdvancedComparatorBlock.POWER) / 48f, 0);
        consumer.accept(INDICATOR, poseStack);
        poseStack.popPose();
    }
}
