package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.fluid.PipeCheckValveBlockEntity;
import dev.dubhe.anvilcraft.block.fluid.PipeBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;

/**
 * 管道面止逆阀渲染器：为每个装阀的面渲染一段单臂阀体模型
 * （{@code block/check_valve_arm}，安装在模型空间 +Y 臂、开口朝 +Y 表示"沿臂向外放行"）。
 *
 * <p>对每个面 {@code face}：先把模型 +Y 旋到 {@code face}，若该面当前允许流出方向为
 * {@code face}（向外）则保持，否则（向内）绕垂直轴翻转 180° 使阀体开口反向，直观显示放行方向。
 *
 * <p><b>光照</b>：不能用无着色的 {@code renderModel} 重载（六面等亮 → 又平又白），
 * 而是逐面按其<b>世界方向</b>取 {@link Level#getShade} 做方向漫反射（上亮下暗），与管道本体一致。
 */
public class PipeCheckValveBERenderer implements BlockEntityRenderer<PipeCheckValveBlockEntity> {

    private static final ModelResourceLocation ARM =
        ModelResourceLocation.standalone(AnvilCraft.of("block/check_valve_arm"));
    private static final RandomSource RANDOM = RandomSource.create();

    @SuppressWarnings("unused")
    public PipeCheckValveBERenderer(BlockEntityRendererProvider.Context context) {
    }

    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    @Override
    public void render(
        PipeCheckValveBlockEntity be,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        int packedOverlay
    ) {
        Map<Direction, Direction> flows = be.effectiveFlows();
        if (flows.isEmpty()) {
            return;
        }
        Level level = be.getLevel();
        if (level == null) {
            return;
        }
        BlockState state = level.getBlockState(be.getBlockPos());
        if (!state.hasProperty(PipeBlock.HAS_CHECK_VALVE)
            || !state.getValue(PipeBlock.HAS_CHECK_VALVE)) {
            return;
        }
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(ARM);
        VertexConsumer consumer = buffer.getBuffer(RenderType.cutout());
        for (Map.Entry<Direction, Direction> entry : flows.entrySet()) {
            Direction face = entry.getKey();
            Direction flowOut = entry.getValue();
            poseStack.pushPose();
            poseStack.translate(0.5, 0.5, 0.5);
            // 1) 把模型 +Y（臂安装方向）旋到 face
            applyUpToFacing(poseStack, face);
            // 2) 若允许流出方向为向内（= face 反侧）则翻转 180°，使阀体开口指向流出侧
            if (flowOut == face.getOpposite()) {
                poseStack.mulPose(Axis.XP.rotationDegrees(180));
            }
            poseStack.translate(-0.5, -0.5, -0.5);
            renderShaded(poseStack.last(), consumer, model, packedLight, packedOverlay, level);
            poseStack.popPose();
        }
    }

    /**
     * 逐面按世界方向做漫反射着色渲染模型（等价于区块渲染器对静态方块的着色，无 AO）。
     */
    private static void renderShaded(
        PoseStack.Pose pose,
        VertexConsumer consumer,
        BakedModel model,
        int packedLight,
        int packedOverlay,
        Level level
    ) {
        for (Direction cull : Direction.values()) {
            RANDOM.setSeed(42L);
            renderQuads(pose, consumer, model.getQuads(null, cull, RANDOM), packedLight, packedOverlay, level);
        }
        RANDOM.setSeed(42L);
        renderQuads(pose, consumer, model.getQuads(null, null, RANDOM), packedLight, packedOverlay, level);
    }

    private static void renderQuads(
        PoseStack.Pose pose,
        VertexConsumer consumer,
        List<BakedQuad> quads,
        int packedLight,
        int packedOverlay,
        Level level
    ) {
        for (BakedQuad quad : quads) {
            // 面法线经姿态法线矩阵变换到世界方向，取最近的六向，据此取漫反射系数
            Direction local = quad.getDirection();
            Vector3f n = new Vector3f(local.getStepX(), local.getStepY(), local.getStepZ());
            n.mul(pose.normal());
            Direction worldDir = Direction.getNearest(n.x(), n.y(), n.z());
            float shade = level.getShade(worldDir, true);
            consumer.putBulkData(pose, quad, shade, shade, shade, 1.0f, packedLight, packedOverlay);
        }
    }

    /** 把模型空间的 +Y（臂安装方向）旋转到 {@code facing}。 */
    private static void applyUpToFacing(PoseStack poseStack, Direction facing) {
        switch (facing) {
            case UP -> poseStack.mulPose(Axis.XP.rotationDegrees(0));
            case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(180));
            case NORTH -> poseStack.mulPose(Axis.XP.rotationDegrees(-90));
            case SOUTH -> poseStack.mulPose(Axis.XP.rotationDegrees(90));
            case WEST -> poseStack.mulPose(Axis.ZP.rotationDegrees(90));
            case EAST -> poseStack.mulPose(Axis.ZP.rotationDegrees(-90));
            default -> {
            }
        }
    }
}
