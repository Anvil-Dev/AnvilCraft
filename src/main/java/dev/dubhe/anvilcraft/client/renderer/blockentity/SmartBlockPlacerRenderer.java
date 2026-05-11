package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.SmartBlockPlacerBlock;
import dev.dubhe.anvilcraft.block.entity.SmartBlockPlacerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

public class SmartBlockPlacerRenderer implements BlockEntityRenderer<SmartBlockPlacerBlockEntity> {
    private static final ModelResourceLocation BASE_MODEL = ModelResourceLocation.standalone(
        AnvilCraft.of("block/smart_block_placer_base")
    );
    private static final ModelResourceLocation UPPERARM_MODEL = ModelResourceLocation.standalone(
        AnvilCraft.of("block/smart_block_placer_upperarm")
    );
    private static final ModelResourceLocation FOREARM_MODEL = ModelResourceLocation.standalone(
        AnvilCraft.of("block/smart_block_placer_forearm")
    );
    private static final ModelResourceLocation CLAW_MODEL = ModelResourceLocation.standalone(
        AnvilCraft.of("block/smart_block_placer_claw")
    );

    private static final SwingBaseAnimationScheme ANIMATION_SCHEME = new SwingBaseAnimationScheme();

    public SmartBlockPlacerRenderer(BlockEntityRendererProvider.Context context) {
        // 不需要初始化，使用静态常量
    }
    
    /**
     * 底座摆动动画方案
     */
    private static class SwingBaseAnimationScheme {
        @SuppressWarnings("SameReturnValue")
        public float getBaseSwingAngle(float ticks) {
            
            // 周期: 140tick (7秒), 角速度: 1.5°/tick
            // 0°→30°→停→30°→-30°→停→-30°→0°→停
            float t = ticks % 140.0f;
            if (t < 20.0f) return t * 1.5f;
            if (t < 40.0f) return 30f;
            if (t < 80.0f) return 30f - (t - 40.0f) * 1.5f;
            if (t < 100.0f) return -30f;
            if (t < 120.0f) return -30f + (t - 100.0f) * 1.5f;
            return 0f;
        }
    }

    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    @Override
    public void render(
        SmartBlockPlacerBlockEntity entity,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        int packedOverlay
    ) {
        BlockState state = entity.getBlockState();
        if (!(state.getBlock() instanceof SmartBlockPlacerBlock)) return;
        
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        boolean upsideDown = state.getValue(SmartBlockPlacerBlock.UPSIDE_DOWN);
        
        // 应用变换：居中 -> 倒置 -> 水平旋转 -> 贴地
        poseStack.pushPose();
        poseStack.translate(0.5, 1.5, 0.5);
        if (upsideDown) {
            poseStack.mulPose(Axis.XP.rotationDegrees(180f));
        }
        applyHorizontalRotation(poseStack, facing, upsideDown);
        poseStack.translate(0, upsideDown ? 0.5 : -1.5, 0);

        // 计算动画时间（使用BlockEntity独立的状态）
        boolean isCurrentlyPowered = entity.isPowered();
        boolean hasRedstoneSignal = entity.isHasRedstoneSignal();
        float smoothTicks = 0f;
        
        float currentAnimationTicks = entity.getClientAnimationTicks();
        long currentLastGameTime = entity.getClientLastGameTime();
        boolean currentWasPowered = entity.isClientWasPowered();
        boolean currentWasRedstoneSignal = entity.isClientWasRedstoneSignal();
        
        if (isCurrentlyPowered && !hasRedstoneSignal) {
            if (entity.getLevel() == null) {
                smoothTicks = currentAnimationTicks + partialTick;
            } else {
                long currentGameTime = entity.getLevel().getGameTime();
                if (!currentWasPowered || currentWasRedstoneSignal) {
                    currentAnimationTicks = 0f;
                    currentLastGameTime = currentGameTime;
                }
                long tickDelta = currentGameTime - currentLastGameTime;
                if (tickDelta > 0) {
                    currentAnimationTicks += tickDelta;
                    currentLastGameTime = currentGameTime;
                }
                smoothTicks = currentAnimationTicks + partialTick;
            }
        } else {
            currentAnimationTicks = 0f;
            currentLastGameTime = 0;
        }
        
        // 更新BlockEntity的动画状态
        entity.updateClientAnimationState(currentAnimationTicks, currentLastGameTime, isCurrentlyPowered, hasRedstoneSignal);
        
        // 计算动画角度
        float baseSwingAngle = 0f;
        float upperArmAngle = 0f;
        float forearmAngle = 0f;
        float clawAngle = 0f;
        
        if (isCurrentlyPowered) {
            baseSwingAngle = ANIMATION_SCHEME.getBaseSwingAngle(smoothTicks);
        }
        
        // 渲染底座
        poseStack.pushPose();
        // 倒挂时X轴翻转180度，Y轴方向反转，需要使用Axis.YN来保持正常的水平旋转方向
        poseStack.mulPose((upsideDown ? Axis.YN : Axis.YP).rotationDegrees(baseSwingAngle));
        poseStack.translate(-0.5, 0.0, -0.5);
        renderModel(poseStack, buffer, BASE_MODEL, packedLight, packedOverlay);
        poseStack.popPose();
        
        // 渲染大臂（跟随底座旋转）
        poseStack.pushPose();
        // 倒挂时X轴翻转180度，Y轴方向反转，需要使用Axis.YN来保持正常的水平旋转方向
        poseStack.mulPose((upsideDown ? Axis.YN : Axis.YP).rotationDegrees(baseSwingAngle));
        poseStack.mulPose(Axis.XP.rotationDegrees(upperArmAngle));
        poseStack.translate(-0.5, 0.0, -0.5);
        renderModel(poseStack, buffer, UPPERARM_MODEL, packedLight, packedOverlay);
        
        // 渲染小臂和钳子
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(forearmAngle));
        renderModel(poseStack, buffer, FOREARM_MODEL, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(clawAngle));
        poseStack.translate(0.0, -0.1, 0.0);
        renderModel(poseStack, buffer, CLAW_MODEL, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
        poseStack.popPose();
        poseStack.popPose();
    }
    
    private void applyHorizontalRotation(PoseStack poseStack, Direction facing, boolean upsideDown) {
        float rotation = switch (facing) {
            case NORTH -> 0f;
            case WEST -> 90f;
            case SOUTH -> 180f;
            case EAST -> 270f;
            default -> 0f;
        };
        // 倒挂时，南北朝向需要额外旋转180度来修正模型翻转
        if (upsideDown && (facing == Direction.NORTH || facing == Direction.SOUTH)) {
            rotation = (rotation + 180f) % 360f;
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
    }
    @SuppressWarnings({"checkstyle:EmptyLineSeparator", "deprecation"})
    private void renderModel(
        PoseStack poseStack, MultiBufferSource buffer, ModelResourceLocation model, int packedLight, int packedOverlay) {
        final VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.cutout());
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
            poseStack.last(),
            vertexConsumer,
            null,
            Minecraft.getInstance().getModelManager().getModel(model),
            0,
            0,
            0,
            packedLight,
            packedOverlay
        );
    }
}
