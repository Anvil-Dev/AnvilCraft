package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.fluid.ControlValveBlockEntity;
import dev.dubhe.anvilcraft.block.fluid.ControlValveBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;

/**
 * 控制阀的方块实体渲染器：把手轮模型渲染在控制阀<b>朝向玩家一侧</b>（{@link ControlValveBlockEntity#getFacing()}）的中心，
 * 并随流速设置旋转。
 *
 * <h3>旋转映射</h3>
 * 手轮转角 {@code angle = 90° × (MAX_RATE − rate) / MAX_RATE}，<b>逆时针</b>：
 * rate=2000 → 基准角（0°）；rate=0 → +90°。
 *
 * <h3>朝向</h3>
 * 手轮模型原本安装在模型空间 +Y（顶面）、绕 Y 轴旋转。渲染时先把 +Y 旋到 {@code facing}，
 * 再绕 {@code facing} 法线施加转角。{@link #BASE_ANGLE_DEG} 用于对齐标记位置"，
 * 视觉不符时调此常量即可。
 */
public class ControlValveBlockEntityRenderer implements BlockEntityRenderer<ControlValveBlockEntity> {

    private static final ModelResourceLocation HANDWHEEL =
        ModelResourceLocation.standalone(AnvilCraft.of("block/control_valve_handwheel"));

    /** 基准角偏移（度）：对齐手轮北标记初始朝向（玩家视角右侧）。 */
    private static final float BASE_ANGLE_DEG = 0.0f;

    @SuppressWarnings("unused")
    public ControlValveBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    @Override
    public void render(
        ControlValveBlockEntity be,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        int packedOverlay
    ) {
        Direction facing = be.getFacing();
        // rate=2000 → 0°，rate=0 → -90°，顺时针是关，逆时针是开
        float ratio = (ControlValveBlockEntity.MAX_RATE - be.getMaxRate()) / (float) ControlValveBlockEntity.MAX_RATE;
        float spinDeg = BASE_ANGLE_DEG - 90.0f * ratio;
        // axis 为 Z 轴的控制阀或 axis 为 Y 轴且手轮朝向 Z+/Z- 的控制阀手轮标记初始朝向补正 90°
        Direction.Axis axis = be.getBlockState().getValue(ControlValveBlock.AXIS);
        if (axis == Direction.Axis.Z || (axis == Direction.Axis.Y && facing.getAxis() == Direction.Axis.Z)) {
            spinDeg += 90.0f;
        }

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        // 1) 把模型 +Y（安装面）旋到 facing
        applyUpToFacing(poseStack, facing);
        // 2) 绕安装面法线（现为模型局部 +Y）逆时针旋转；逆时针 = 绕 +Y 正角
        poseStack.mulPose(Axis.YP.rotationDegrees(spinDeg));
        poseStack.translate(-0.5, -0.5, -0.5);

        BakedModel model = Minecraft.getInstance().getModelManager().getModel(HANDWHEEL);
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
        poseStack.popPose();
    }

    /**
     * 把模型空间的 +Y（顶面，手轮安装面）旋转到 {@code facing} 方向。
     * 旋转后模型局部 +Y 指向 {@code facing}，圆盘平面贴合该面。
     */
    private static void applyUpToFacing(PoseStack poseStack, Direction facing) {
        switch (facing) {
            case UP -> poseStack.mulPose(Axis.XP.rotationDegrees(0));
            case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(180));
            case NORTH -> poseStack.mulPose(Axis.XP.rotationDegrees(-90));
            case SOUTH -> poseStack.mulPose(Axis.XP.rotationDegrees(90));
            case WEST -> poseStack.mulPose(Axis.ZP.rotationDegrees(90));
            case EAST -> poseStack.mulPose(Axis.ZP.rotationDegrees(-90));
            default -> {}
        }
    }
}
