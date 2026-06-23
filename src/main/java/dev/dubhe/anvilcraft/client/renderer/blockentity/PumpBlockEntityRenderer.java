package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.block.entity.fluid.PumpBlockEntity;
import dev.dubhe.anvilcraft.block.fluid.PumpBlock;
import dev.dubhe.anvilcraft.block.state.Orientation;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.PumpRenderState;
import dev.dubhe.anvilcraft.client.support.FeatureRendererSupport;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
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
public class PumpBlockEntityRenderer implements BlockEntityRenderer<PumpBlockEntity, PumpRenderState> {

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
        if (!be.isWorking()) return;

        net.minecraft.world.level.Level level = be.getLevel();
        if (level == null) return;

        Orientation orientation = blockState.getValue(PumpBlock.ORIENTATION);
        state.setOrientation(orientation);

        float speed = 1.0f;
        long gameTime = level.getGameTime();
        float cycle = ((gameTime + partialTicks) * speed) % 20.0f / 20.0f;

        float angle = cycle * 2.0f * (float) Math.PI;
        state.setPiston1Offset((float) Math.sin(angle) * MAX_PISTON_OFFSET);
        state.setPiston2Offset((float) Math.cos(angle) * MAX_PISTON_OFFSET);

        state.setPiston1(FeatureRendererSupport.initialize(PUMP_PISTON_1, be));
        state.setPiston2(FeatureRendererSupport.initialize(PUMP_PISTON_2, be));
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

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(-orientation.getYRotation()));
        poseStack.mulPose(Axis.XP.rotationDegrees(orientation.getXRotation()));
        poseStack.translate(-0.5, -0.5, -0.5);
        poseStack.translate(0, -2.0f / 16.0f, 0);

        BlockModelRenderState piston1 = state.getPiston1();
        float piston1Offset = state.getPiston1Offset();
        poseStack.pushPose();
        poseStack.translate(0, piston1Offset, 0);
        piston1.submit(poseStack, submitNodeCollector, state.lightCoords, 655360, 0);
        poseStack.popPose();

        BlockModelRenderState piston2 = state.getPiston2();
        float piston2Offset = state.getPiston2Offset();
        poseStack.pushPose();
        poseStack.translate(0, piston2Offset, 0);
        piston2.submit(poseStack, submitNodeCollector, state.lightCoords, 655360, 0);
        poseStack.popPose();

        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(PumpBlockEntity blockEntity) {
        return AABB.ofSize(blockEntity.getBlockPos().getCenter(), 2, 2, 2);
    }
}
