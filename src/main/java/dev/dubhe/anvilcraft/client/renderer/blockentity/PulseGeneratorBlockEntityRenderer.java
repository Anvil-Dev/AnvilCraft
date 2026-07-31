package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.block.entity.PulseGeneratorBlockEntity;
import dev.dubhe.anvilcraft.block.utility.redstone.PulseGeneratorBlock;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.PulseGeneratorRenderState;
import dev.dubhe.anvilcraft.client.support.FeatureRendererSupport;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.jspecify.annotations.Nullable;

public class PulseGeneratorBlockEntityRenderer
    implements BlockEntityRenderer<PulseGeneratorBlockEntity, PulseGeneratorRenderState> {
    public static final StandaloneModelKey<BlockStateModel> INDICATOR = new StandaloneModelKey<>(
        () -> "AnvilCraft: Pulse Generator Indicator"
    );
    public static final StandaloneModelKey<BlockStateModel> INDICATOR_OVERSPEED = new StandaloneModelKey<>(
        () -> "AnvilCraft: Pulse Generator Overspeed Indicator"
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

    public PulseGeneratorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public PulseGeneratorRenderState createRenderState() {
        return new PulseGeneratorRenderState();
    }

    @Override
    public void extractRenderState(
        PulseGeneratorBlockEntity blockEntity,
        PulseGeneratorRenderState state,
        float partialTick,
        Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(
            blockEntity, state, partialTick, cameraPosition, breakProgress);
        state.setFacing(blockEntity.getBlockState().getValue(PulseGeneratorBlock.FACING));
        state.setOutputting(blockEntity.getState() == PulseGeneratorBlockEntity.State.OUTPUTTING);
        state.setPhaseProgress(blockEntity.getPhaseProgress(partialTick));
        boolean overspeed = blockEntity.isProcessing()
                            && blockEntity.getWaitingTime() + blockEntity.getSignalDuration() <= 3;
        state.setIndicator(FeatureRendererSupport.initialize(
            overspeed ? PulseGeneratorBlockEntityRenderer.INDICATOR_OVERSPEED : PulseGeneratorBlockEntityRenderer.INDICATOR,
            blockEntity
        ));
    }

    @Override
    public void submit(
        PulseGeneratorRenderState state,
        PoseStack pose,
        SubmitNodeCollector collector,
        CameraRenderState camera
    ) {
        pose.pushPose();
        pose.translate(0.5f, 0.0f, 0.5f);
        pose.mulPose(Axis.YP.rotationDegrees(-state.getFacing().toYRot()));
        pose.translate(-0.5f, 0.0f, -0.5f);
        float phaseStartAngle =
            state.isOutputting() ? PulseGeneratorBlockEntityRenderer.END_ANGLE : PulseGeneratorBlockEntityRenderer.START_ANGLE;
        PulseGeneratorBlockEntityRenderer.translateOnTable(pose);
        PulseGeneratorBlockEntityRenderer.rotateOnTable(
            pose, phaseStartAngle + (PulseGeneratorBlockEntityRenderer.END_ANGLE - PulseGeneratorBlockEntityRenderer.START_ANGLE)
                                    * state.getPhaseProgress()
        );
        state.getIndicator().submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }

    private static void translateOnTable(PoseStack pose) {
        pose.translate(
            PulseGeneratorBlockEntityRenderer.TABLE_ORIGIN_X, PulseGeneratorBlockEntityRenderer.TABLE_ORIGIN_Y,
            PulseGeneratorBlockEntityRenderer.TABLE_ORIGIN_Z
        );
        pose.mulPose(Axis.XP.rotationDegrees(PulseGeneratorBlockEntityRenderer.TABLE_ANGLE));
        pose.translate(0.0f, 0.0f, PulseGeneratorBlockEntityRenderer.INDICATOR_OFFSET_Z);
        pose.mulPose(Axis.XP.rotationDegrees(-PulseGeneratorBlockEntityRenderer.TABLE_ANGLE));
        pose.translate(
            -PulseGeneratorBlockEntityRenderer.TABLE_ORIGIN_X, -PulseGeneratorBlockEntityRenderer.TABLE_ORIGIN_Y,
            -PulseGeneratorBlockEntityRenderer.TABLE_ORIGIN_Z
        );
    }

    private static void rotateOnTable(PoseStack pose, float angle) {
        pose.translate(
            PulseGeneratorBlockEntityRenderer.TABLE_ORIGIN_X, PulseGeneratorBlockEntityRenderer.TABLE_ORIGIN_Y,
            PulseGeneratorBlockEntityRenderer.TABLE_ORIGIN_Z
        );
        pose.mulPose(Axis.XP.rotationDegrees(PulseGeneratorBlockEntityRenderer.TABLE_ANGLE));
        pose.translate(
            PulseGeneratorBlockEntityRenderer.INDICATOR_PIVOT_X - PulseGeneratorBlockEntityRenderer.TABLE_ORIGIN_X,
            PulseGeneratorBlockEntityRenderer.INDICATOR_PIVOT_Y - PulseGeneratorBlockEntityRenderer.TABLE_ORIGIN_Y,
            PulseGeneratorBlockEntityRenderer.INDICATOR_PIVOT_Z - PulseGeneratorBlockEntityRenderer.TABLE_ORIGIN_Z
        );
        pose.mulPose(Axis.YP.rotationDegrees(angle));
        pose.translate(
            PulseGeneratorBlockEntityRenderer.TABLE_ORIGIN_X - PulseGeneratorBlockEntityRenderer.INDICATOR_PIVOT_X,
            PulseGeneratorBlockEntityRenderer.TABLE_ORIGIN_Y - PulseGeneratorBlockEntityRenderer.INDICATOR_PIVOT_Y,
            PulseGeneratorBlockEntityRenderer.TABLE_ORIGIN_Z - PulseGeneratorBlockEntityRenderer.INDICATOR_PIVOT_Z
        );
        pose.mulPose(Axis.XP.rotationDegrees(-PulseGeneratorBlockEntityRenderer.TABLE_ANGLE));
        pose.translate(
            -PulseGeneratorBlockEntityRenderer.TABLE_ORIGIN_X, -PulseGeneratorBlockEntityRenderer.TABLE_ORIGIN_Y,
            -PulseGeneratorBlockEntityRenderer.TABLE_ORIGIN_Z
        );
    }
}
