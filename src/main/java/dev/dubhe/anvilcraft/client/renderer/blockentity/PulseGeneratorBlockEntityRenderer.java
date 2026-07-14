package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.PulseGeneratorBlock;
import dev.dubhe.anvilcraft.block.entity.PulseGeneratorBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;

public class PulseGeneratorBlockEntityRenderer implements BlockEntityRenderer<PulseGeneratorBlockEntity> {
    private static final ModelResourceLocation INDICATOR = ModelResourceLocation.standalone(
        AnvilCraft.of("block/pulse_generator_indicator")
    );
    private static final ModelResourceLocation INDICATOR_OVERSPEED = ModelResourceLocation.standalone(
        AnvilCraft.of("block/pulse_generator_indicator_overspeed")
    );
    private static final float TABLE_ANGLE = 22.5f;
    private static final float TABLE_ORIGIN_X = 8.0f / 16.0f;
    private static final float TABLE_ORIGIN_Y = 4.0f / 16.0f;
    private static final float TABLE_ORIGIN_Z = 5.0f / 16.0f;
    private static final float INDICATOR_PIVOT_X = 8.0f / 16.0f;
    private static final float INDICATOR_PIVOT_Y = 8.0f / 16.0f;
    private static final float INDICATOR_PIVOT_Z = 9.5f / 16.0f;
    private static final float INDICATOR_OFFSET_Z = -0.5f / 16.0f;
    private static final float START_ANGLE = 0.0f;
    private static final float END_ANGLE = -180.0f;

    @SuppressWarnings("unused")
    public PulseGeneratorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
        PulseGeneratorBlockEntity blockEntity,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedLight,
        int packedOverlay
    ) {
        poseStack.pushPose();
        poseStack.translate(0.5f, 0.0f, 0.5f);
        poseStack.mulPose(Axis.YP.rotationDegrees(-blockEntity.getBlockState().getValue(PulseGeneratorBlock.FACING).toYRot()));
        poseStack.translate(-0.5f, 0.0f, -0.5f);
        boolean outputting = blockEntity.getState() == PulseGeneratorBlockEntity.State.OUTPUTTING;
        boolean overspeed = blockEntity.isProcessing() && blockEntity.getWaitingTime() + blockEntity.getSignalDuration() <= 3;
        float phaseProgress = blockEntity.getPhaseProgress(partialTick);
        float phaseStartAngle = outputting ? END_ANGLE : START_ANGLE;
        translateOnTable(poseStack, INDICATOR_OFFSET_Z);
        rotateOnTable(poseStack, phaseStartAngle + (END_ANGLE - START_ANGLE) * phaseProgress);
        Minecraft.getInstance()
            .getBlockRenderer()
            .getModelRenderer()
            .renderModel(
                poseStack.last(),
                bufferSource.getBuffer(RenderType.cutout()),
                null,
                Minecraft.getInstance().getModelManager().getModel(overspeed ? INDICATOR_OVERSPEED : INDICATOR),
                1.0f,
                1.0f,
                1.0f,
                packedLight,
                packedOverlay
        );
        poseStack.popPose();
    }

    private static void translateOnTable(PoseStack poseStack, float offsetZ) {
        poseStack.translate(TABLE_ORIGIN_X, TABLE_ORIGIN_Y, TABLE_ORIGIN_Z);
        poseStack.mulPose(Axis.XP.rotationDegrees(TABLE_ANGLE));
        poseStack.translate(0.0f, 0.0f, offsetZ);
        poseStack.mulPose(Axis.XP.rotationDegrees(-TABLE_ANGLE));
        poseStack.translate(-TABLE_ORIGIN_X, -TABLE_ORIGIN_Y, -TABLE_ORIGIN_Z);
    }

    private static void rotateOnTable(PoseStack poseStack, float angle) {
        poseStack.translate(TABLE_ORIGIN_X, TABLE_ORIGIN_Y, TABLE_ORIGIN_Z);
        poseStack.mulPose(Axis.XP.rotationDegrees(TABLE_ANGLE));
        poseStack.translate(
            INDICATOR_PIVOT_X - TABLE_ORIGIN_X,
            INDICATOR_PIVOT_Y - TABLE_ORIGIN_Y,
            INDICATOR_PIVOT_Z - TABLE_ORIGIN_Z
        );
        poseStack.mulPose(Axis.YP.rotationDegrees(angle));
        poseStack.translate(
            TABLE_ORIGIN_X - INDICATOR_PIVOT_X,
            TABLE_ORIGIN_Y - INDICATOR_PIVOT_Y,
            TABLE_ORIGIN_Z - INDICATOR_PIVOT_Z
        );
        poseStack.mulPose(Axis.XP.rotationDegrees(-TABLE_ANGLE));
        poseStack.translate(-TABLE_ORIGIN_X, -TABLE_ORIGIN_Y, -TABLE_ORIGIN_Z);
    }
}
