package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.AdvancedComparatorBlock;
import dev.dubhe.anvilcraft.block.entity.AdvancedComparatorBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;

public class AdvancedComparatorBlockEntityRender implements BlockEntityRenderer<AdvancedComparatorBlockEntity> {
    private static final ModelResourceLocation INDICATOR = ModelResourceLocation.standalone(
        AnvilCraft.of("block/advanced_comparator_indicator")
    );

    @SuppressWarnings("unused")
    public AdvancedComparatorBlockEntityRender(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
        @NotNull AdvancedComparatorBlockEntity blockEntity,
        float tickDelta,
        @NotNull PoseStack poseStack,
        @NotNull MultiBufferSource bufferSource,
        int light,
        int overlay
    ) {
        poseStack.pushPose();
        float height = getHeight(blockEntity);
        poseStack.translate(0, height, 0);
        // noinspection DataFlowIssue
        Minecraft.getInstance()
            .getBlockRenderer()
            .getModelRenderer()
            .renderModel(
                poseStack.last(),
                bufferSource.getBuffer(RenderType.cutout()),
                null,
                Minecraft.getInstance().getModelManager().getModel(INDICATOR),
                0, 0, 0,
                light,
                overlay,
                ModelData.EMPTY,
                null
        );
        poseStack.popPose();
    }

    private float getHeight(AdvancedComparatorBlockEntity blockEntity) {
        Level level = blockEntity.getLevel();
        int inputtingSignal = 0;
        if (level != null && level.getBlockState(blockEntity.getBlockPos()).getBlock() == ModBlocks.ADVANCED_COMPARATOR.get()) {
            inputtingSignal = level.getBlockState(blockEntity.getBlockPos()).getValue(AdvancedComparatorBlock.POWER);
        }
        return (inputtingSignal / 3f * .0625f);
    }
}
