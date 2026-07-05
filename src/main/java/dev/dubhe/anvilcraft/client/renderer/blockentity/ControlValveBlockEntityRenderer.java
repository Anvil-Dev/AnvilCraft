package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.fluid.ControlValveBlockEntity;
import dev.dubhe.anvilcraft.block.fluid.ControlValveBlock;
import dev.dubhe.anvilcraft.client.support.FluidRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Vector3f;

/**
 * 控制阀的方块实体渲染器：把手轮模型渲染在控制阀<b>朝向玩家一侧</b>的中心，
 * 并随流速设置旋转。并渲染内部过滤的流体。在另外三面渲染内部过滤的流体
 */
public class ControlValveBlockEntityRenderer implements BlockEntityRenderer<ControlValveBlockEntity> {

    private static final ModelResourceLocation HANDWHEEL =
        ModelResourceLocation.standalone(AnvilCraft.of("block/control_valve_handwheel"));

    /** 基准角偏移（度）：对齐手轮北标记初始朝向（玩家视角右侧）。 */
    private static final float BASE_ANGLE_DEG = 0.0f;

    /** 流体指示器小方块半边长 */
    private static final float INDICATOR_HALF = 0.0625f;
    /** 流体指示器中心距表面距离 */
    private static final float INDICATOR_DEPTH = 0.295f;

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

        // --- 渲染手轮 ---
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        applyUpToFacing(poseStack, facing);
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

        // --- 在另外三面渲染内部过滤的流体 ---
        FluidStack filterFluid = be.getFilter(0);
        if (!filterFluid.isEmpty()) {
            renderFluidIndicators(be, filterFluid, poseStack, buffer, packedLight);
        }

        // --- 红石激活时在手轮位置渲染红石粒子 ---
        if (be.isLocked()) {
            renderRedstoneIndicator(be, poseStack, buffer, packedLight, packedOverlay);
        }
    }

    /**
     * 在阀门侧面的非手轮面上渲染流指示器小方块，使用 {@link FluidRenderHelper} 复用鱼缸的液面渲染。
     */
    private void renderFluidIndicators(
        ControlValveBlockEntity be,
        FluidStack filterFluid,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight
    ) {
        Direction facing = be.getFacing();
        Direction.Axis axis = be.getBlockState().getValue(ControlValveBlock.AXIS);
        float h = INDICATOR_HALF;
        float y = INDICATOR_DEPTH;

        for (Direction side : Direction.values()) {
            if (side.getAxis() == axis || side == facing) {
                continue;
            }

            poseStack.pushPose();
            poseStack.translate(0.5, 0.5, 0.5);
            applyUpToFacing(poseStack, side);
            // 旋转后局部 +Y 指向 side 方向，小方块范围为 (-h..h, y-h..y+h, -h..h)
            FluidRenderHelper.INSTANCE.renderFluidBox(
                filterFluid,
                -h, y - h, -h,
                h, y + h, h,
                buffer, poseStack, packedLight,
                true, false
            );
            poseStack.popPose();
        }
    }

    /**
     * 红石激活时在手轮位置生成红石粒子效果。
     */
    private void renderRedstoneIndicator(
        ControlValveBlockEntity be,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        int packedOverlay
    ) {
        if (be.getLevel() == null) return;
        Direction facing = be.getFacing();
        var pos = be.getBlockPos();
        var level = be.getLevel();

        // 手轮面中心的世界坐标
        double x = pos.getX() + 0.5 + facing.getStepX() * 0.425;
        double y = pos.getY() + 0.5 + facing.getStepY() * 0.425;
        double z = pos.getZ() + 0.5 + facing.getStepZ() * 0.425;

        var redstoneColor = new Vector3f(1.0f, 0.1f, 0.1f);
        var particle = new DustParticleOptions(redstoneColor, 1.0f);

        if (level.isClientSide() && level.getGameTime() % 20 == 0 && level.random.nextFloat() < 0.3f) {
            level.addParticle(particle,
                x + (level.random.nextDouble() - 0.5) * 0.15,
                y + (level.random.nextDouble() - 0.5) * 0.15,
                z + (level.random.nextDouble() - 0.5) * 0.15,
                0, 0.01, 0);
        }
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
