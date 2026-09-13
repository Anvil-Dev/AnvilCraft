package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.block.entity.fluid.PumpBlockEntity;
import dev.dubhe.anvilcraft.block.fluid.PumpBlock;
import dev.dubhe.anvilcraft.block.state.Orientation;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.PumpRenderState;
import dev.dubhe.anvilcraft.client.selection.ModelSelectionRenderer;
import dev.dubhe.anvilcraft.client.support.FeatureRendererSupport;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.jspecify.annotations.Nullable;

/**
 * 泵的方块实体渲染器。
 * 在工作状态时渲染两个活塞模型（pump_piston_1, pump_piston_2），
 * 交替上下运动。
 */
public class PumpBlockEntityRenderer
    implements BlockEntityRenderer<PumpBlockEntity, PumpRenderState>, ModelSelectionRenderer<PumpBlockEntity> {

    public static final StandaloneModelKey<BlockStateModel> PUMP_PISTON_1 =
        new StandaloneModelKey<>(() -> "AnvilCraft: Pump Piston 1 Model");
    public static final StandaloneModelKey<BlockStateModel> PUMP_PISTON_2 =
        new StandaloneModelKey<>(() -> "AnvilCraft: Pump Piston 2 Model");

    private static final float MAX_PISTON_OFFSET = 1.5f / 16.0f;

    @SuppressWarnings("unused")
    public PumpBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public PumpRenderState createRenderState() {
        return new PumpRenderState();
    }

    @Override
    public void extractRenderState(
        PumpBlockEntity be,
        PumpRenderState state,
        float partialTicks,
        Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        BlockState blockState = be.getBlockState();
        if (!(blockState.getBlock() instanceof PumpBlock)) return;
        state.setOrientation(null);
        if (!be.isWorking()) return;

        Level level = be.getLevel();
        if (level == null) return;

        Orientation orientation = blockState.getValue(PumpBlock.ORIENTATION);
        state.setOrientation(orientation);

        float speed = 1.0f;
        long gameTime = level.getGameTime();
        float cycle = ((gameTime + partialTicks) * speed) % 20.0f / 20.0f;

        float angle = cycle * 2.0f * (float) Math.PI;
        state.setPiston1Offset((float) Math.sin(angle) * PumpBlockEntityRenderer.MAX_PISTON_OFFSET);
        state.setPiston2Offset((float) Math.cos(angle) * PumpBlockEntityRenderer.MAX_PISTON_OFFSET);

        state.setPiston1(FeatureRendererSupport.initialize(PumpBlockEntityRenderer.PUMP_PISTON_1, be));
        state.setPiston2(FeatureRendererSupport.initialize(PumpBlockEntityRenderer.PUMP_PISTON_2, be));
    }

    @Override
    public void submit(
        PumpRenderState state,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        CameraRenderState camera
    ) {
        Orientation orientation = state.getOrientation();
        if (orientation == null) return;

        emitPistons(orientation, state.getPiston1Offset(), state.getPiston2Offset(), poseStack, (model, pose) -> {
            var piston = model.equals(dev.dubhe.anvilcraft.client.selection.SelectionModel.standalone(PUMP_PISTON_1))
                ? state.getPiston1() : state.getPiston2();
            piston.submit(pose, submitNodeCollector, state.lightCoords, 655360, 0);
        });
    }

    @Override
    public void collectSelectionModels(PumpBlockEntity be, float partialTick, PoseStack pose, ModelConsumer consumer) {
        if (!be.isWorking() || be.getLevel() == null) return;
        float cycle = ((be.getLevel().getGameTime() + partialTick) % 20.0F) / 20.0F;
        collectPistons(be.getBlockState(), cycle, pose, consumer);
    }

    @Override
    public void collectPreviewModels(PumpBlockEntity be, float partialTick, PoseStack pose, ModelConsumer consumer) {
        collectPistons(be.getBlockState(), 0, pose, consumer);
    }

    private static void collectPistons(BlockState state, float cycle, PoseStack pose, ModelConsumer consumer) {
        float angle = cycle * 2.0F * (float) Math.PI;
        emitPistons(state.getValue(PumpBlock.ORIENTATION), (float) Math.sin(angle) * MAX_PISTON_OFFSET,
            (float) Math.cos(angle) * MAX_PISTON_OFFSET, pose, consumer);
    }

    private static void emitPistons(Orientation orientation, float first, float second, PoseStack pose, ModelConsumer consumer) {
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-orientation.getYRotation()));
        pose.mulPose(Axis.XP.rotationDegrees(orientation.getXRotation()));
        pose.translate(-0.5, -0.5, -0.5);
        pose.translate(0, -2.0F / 16.0F, 0);
        pose.pushPose();
        pose.translate(0, first, 0);
        consumer.accept(PUMP_PISTON_1, pose);
        pose.popPose();
        pose.pushPose();
        pose.translate(0, second, 0);
        consumer.accept(PUMP_PISTON_2, pose);
        pose.popPose();
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(PumpBlockEntity blockEntity) {
        return AABB.ofSize(blockEntity.getBlockPos().getCenter(), 2, 2, 2);
    }
}
