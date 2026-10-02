package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.block.entity.fluid.AbstractPipeBlockEntity;
import dev.dubhe.anvilcraft.block.fluid.PipeBlock;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.PipeCheckValveRenderState;
import dev.dubhe.anvilcraft.client.selection.ModelSelectionRenderer;
import dev.dubhe.anvilcraft.client.support.FeatureRendererSupport;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public class PipeCheckValveBERenderer<T extends AbstractPipeBlockEntity>
    implements BlockEntityRenderer<T, PipeCheckValveRenderState>, ModelSelectionRenderer<T> {
    public static final StandaloneModelKey<BlockStateModel> ARM = new StandaloneModelKey<>(
        () -> "AnvilCraft: Check Valve Arm Model"
    );

    @SuppressWarnings("unused")
    public PipeCheckValveBERenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public PipeCheckValveRenderState createRenderState() {
        return new PipeCheckValveRenderState();
    }

    @Override
    public void extractRenderState(
        T be,
        PipeCheckValveRenderState state,
        float partialTicks,
        Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        Map<Direction, Direction> flows = be.effectiveFlows();
        state.setFlows(flows.isEmpty() ? new EnumMap<>(Direction.class) : new EnumMap<>(flows));
        state.setArm(FeatureRendererSupport.initialize(PipeCheckValveBERenderer.ARM, be));
    }

    @Override
    public void submit(
        PipeCheckValveRenderState state,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        CameraRenderState camera
    ) {
        if (state.getFlows().isEmpty() || state.getArm() == null) {
            return;
        }

        emitArms(state.getFlows(), poseStack, (model, pose) ->
            state.getArm().submit(pose, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0));
    }

    @Override
    public void collectSelectionModels(T be, float partialTick, PoseStack pose, ModelConsumer consumer) {
        if (be.getLevel() == null || !be.getBlockState().getValue(PipeBlock.HAS_CHECK_VALVE)) return;
        emitArms(be.effectiveFlows(), pose, consumer);
    }

    private static void emitArms(Map<Direction, Direction> flows, PoseStack poseStack, ModelConsumer consumer) {
        for (Map.Entry<Direction, Direction> entry : flows.entrySet()) {
            Direction face = entry.getKey();

            poseStack.pushPose();
            poseStack.translate(0.5, 0.5, 0.5);
            PipeCheckValveBERenderer.applyUpToFacing(poseStack, face);
            Direction flowOut = entry.getValue();
            if (flowOut == face.getOpposite()) {
                poseStack.mulPose(Axis.XP.rotationDegrees(180));
            }
            poseStack.translate(-0.5, -0.5, -0.5);
            consumer.accept(ARM, poseStack);
            poseStack.popPose();
        }
    }

    @Override
    public AABB getRenderBoundingBox(T blockEntity) {
        return AABB.ofSize(blockEntity.getBlockPos().getCenter(), 2, 2, 2);
    }

    private static void applyUpToFacing(PoseStack poseStack, Direction facing) {
        switch (facing) {
            case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(180));
            case NORTH -> poseStack.mulPose(Axis.XP.rotationDegrees(-90));
            case SOUTH -> poseStack.mulPose(Axis.XP.rotationDegrees(90));
            case WEST -> poseStack.mulPose(Axis.ZP.rotationDegrees(90));
            case EAST -> poseStack.mulPose(Axis.ZP.rotationDegrees(-90));
            default -> poseStack.mulPose(Axis.XP.rotationDegrees(0));
        }
    }
}
