package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.PumpBlock;
import dev.dubhe.anvilcraft.block.entity.PumpBlockEntity;
import dev.dubhe.anvilcraft.block.state.Orientation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 泵的方块实体渲染器。
 * 在工作状态时渲染两个活塞模型（pump_piston_1, pump_piston_2），
 * 交替上下运动，运动速度与流体传输量正相关。
 *
 * <p>基础模型（pump_base/pump_off/pump_overload）由 blockstate 系统渲染，
 * 本渲染器仅负责动画活塞部分。
 */
public class PumpBlockEntityRenderer implements BlockEntityRenderer<PumpBlockEntity> {

    private static final ModelResourceLocation PUMP_PISTON_1 =
        ModelResourceLocation.standalone(AnvilCraft.of("block/pump_piston_1"));
    private static final ModelResourceLocation PUMP_PISTON_2 =
        ModelResourceLocation.standalone(AnvilCraft.of("block/pump_piston_2"));

    /** 活塞最大位移（单位：方块，2/16 = 2 像素） */
    private static final float MAX_PISTON_OFFSET = 2.0f / 16.0f;

    /** 最大传输速率（mB/tick），用于归一化动画速度 */
    private static final float MAX_TRANSFER_RATE = 500.0f;

    @SuppressWarnings("unused")
    public PumpBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
        PumpBlockEntity blockEntity,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        int packedOverlay
    ) {
        if (!blockEntity.isWorking()) return;

        BlockState state = blockEntity.getBlockState();
        if (!(state.getBlock() instanceof PumpBlock)) return;

        // 动画速度与传输量正相关
        float speed = blockEntity.getLastTransferAmount() / MAX_TRANSFER_RATE;
        if (speed < 0.05f) speed = 1.0f;

        long gameTime = blockEntity.getLevel().getGameTime();
        float cycle = ((gameTime + partialTick) * speed) % 20.0f / 20.0f;

        Orientation orientation = state.getValue(PumpBlock.ORIENTATION);
        // 应用与 blockstate 相同的旋转，使活塞坐标系与模型对齐
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        // blockstate Y+ 方向与 JOML 相反，需取反
        // 顺序：先 X 后 Y（mulPose 后乘，先 Y 则 X 对向量先生效）
        poseStack.mulPose(Axis.YP.rotationDegrees(-orientation.getYRotation()));
        poseStack.mulPose(Axis.XP.rotationDegrees(-orientation.getXRotation()));
        poseStack.translate(-0.5, -0.5, -0.5);

        poseStack.translate(0, -2.0f / 16.0f, 0);

        // 活塞模型沿 +Z 方向运动（模型空间中输出方向 = +Z）
        BakedModel piston1 = Minecraft.getInstance().getModelManager().getModel(PUMP_PISTON_1);
        // 活塞 1：峰值在 cycle=0.25（上顶点）
        float piston1Offset = sinPulse(cycle, 0.25f) * MAX_PISTON_OFFSET;
        poseStack.pushPose();
        poseStack.translate(0, piston1Offset, 0);
        renderPistonModel(poseStack, buffer, piston1, packedLight, packedOverlay);
        poseStack.popPose();

        BakedModel piston2 = Minecraft.getInstance().getModelManager().getModel(PUMP_PISTON_2);
        // 活塞 2：峰值在 cycle=0.75（上顶点），与活塞 1 错开半周期
        float piston2Offset = sinPulse(cycle, 0.75f) * MAX_PISTON_OFFSET;
        poseStack.pushPose();
        poseStack.translate(0, piston2Offset, 0);
        renderPistonModel(poseStack, buffer, piston2, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.popPose();
    }

    /**
     * 正弦脉冲：在 phase 处达到最大值 1，phase±0.25 处为 0。
     */
    private static float sinPulse(float cycle, float phase) {
        float dist = Math.abs(cycle - phase);
        if (dist > 0.5f) dist = 1.0f - dist;
        if (dist > 0.25f) return 0f;
        return (float) Math.sin(dist / 0.25f * Math.PI);
    }

    private void renderPistonModel(
        PoseStack poseStack,
        MultiBufferSource buffer,
        BakedModel model,
        int packedLight,
        int packedOverlay
    ) {
        Minecraft.getInstance()
            .getBlockRenderer()
            .getModelRenderer()
            .renderModel(
                poseStack.last(),
                buffer.getBuffer(RenderType.cutout()),
                null,
                model,
                1.0f, 1.0f, 1.0f,
                packedLight,
                packedOverlay
            );
    }
}
