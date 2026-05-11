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
    public static final ModelResourceLocation BASE_MODEL = ModelResourceLocation.standalone(
        AnvilCraft.of("block/smart_block_placer_base")
    );
    public static final ModelResourceLocation UPPERARM_MODEL = ModelResourceLocation.standalone(
        AnvilCraft.of("block/smart_block_placer_upperarm")
    );
    public static final ModelResourceLocation FOREARM_MODEL = ModelResourceLocation.standalone(
        AnvilCraft.of("block/smart_block_placer_forearm")
    );
    public static final ModelResourceLocation CLAW_MODEL = ModelResourceLocation.standalone(
        AnvilCraft.of("block/smart_block_placer_claw")
    );

    // 客户端动画状态
    private float clientTicks = 0f;
    private boolean wasPowered = false;
    private boolean wasRedstoneSignal = false;
    private long lastGameTime = 0;
    private IAnimationScheme currentAnimationScheme;

    @SuppressWarnings("unused")
    public SmartBlockPlacerRenderer(BlockEntityRendererProvider.Context context) {
        this.currentAnimationScheme = new SwingBaseAnimationScheme();
    }
    
    /**
     * 动画方案接口 - 支持多种动画策略
     */
    @SuppressWarnings("checkstyle:RightCurly")
    public interface IAnimationScheme {
        default float getBaseSwingAngle(float time, boolean isPowered) {
            return 0f;
        }

        default float getUpperArmAngle(float time, boolean isPowered) {
            return 0f;
        }

        default float getForearmAngle(float time, boolean isPowered) {
            return 0f;
        }

        default float getClawAngle(float time, boolean isPowered) {
            return 0f;
        }
    }
    
    /**
     * 设置动画方案
     *
     * @param scheme 动画方案实现
     */
    @SuppressWarnings("unused")
    public void setAnimationScheme(IAnimationScheme scheme) {
        this.currentAnimationScheme = scheme;
    }
    
    /**
     * 旋转盘摆动动画方案 - 仅底座左右摆动，机械臂跟随
     */
    public static class SwingBaseAnimationScheme implements IAnimationScheme {
        @Override
        public float getBaseSwingAngle(float ticks, boolean isPowered) {
            if (!isPowered) return 0f;
            
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
        
        @Override
        public float getUpperArmAngle(float time, boolean isPowered) {
            return 0f;
        }

        @Override
        public float getForearmAngle(float time, boolean isPowered) {
            return 0f;
        }

        @Override
        public float getClawAngle(float time, boolean isPowered) {
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

        // 计算动画时间
        boolean isCurrentlyPowered = entity.isPowered();
        boolean hasRedstoneSignal = entity.isHasRedstoneSignal();
        float smoothTicks = 0f;
        
        if (isCurrentlyPowered && !hasRedstoneSignal) {
            if (entity.getLevel() == null) {
                smoothTicks = clientTicks + partialTick;
            } else {
                long currentGameTime = entity.getLevel().getGameTime();
                if (!wasPowered || wasRedstoneSignal) {
                    clientTicks = 0f;
                    lastGameTime = currentGameTime;
                }
                long tickDelta = currentGameTime - lastGameTime;
                if (tickDelta > 0) {
                    clientTicks += tickDelta;
                    lastGameTime = currentGameTime;
                }
                smoothTicks = clientTicks + partialTick;
            }
        } else {
            clientTicks = 0f;
            lastGameTime = 0;
        }
        
        wasPowered = isCurrentlyPowered;
        wasRedstoneSignal = hasRedstoneSignal;
        
        // 计算动画角度
        float baseSwingAngle = 0f;
        float upperArmAngle = 0f;
        float forearmAngle = 0f;
        float clawAngle = 0f;
        
        if (isCurrentlyPowered && currentAnimationScheme != null) {
            baseSwingAngle = currentAnimationScheme.getBaseSwingAngle(smoothTicks, isCurrentlyPowered);
            upperArmAngle = currentAnimationScheme.getUpperArmAngle(smoothTicks, isCurrentlyPowered);
            forearmAngle = currentAnimationScheme.getForearmAngle(smoothTicks, isCurrentlyPowered);
            clawAngle = currentAnimationScheme.getClawAngle(smoothTicks, isCurrentlyPowered);
        }
        
        // 渲染底座
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(baseSwingAngle));
        poseStack.translate(-0.5, 0.0, -0.5);
        renderModel(poseStack, buffer, BASE_MODEL, packedLight, packedOverlay);
        poseStack.popPose();
        
        // 渲染大臂（跟随底座旋转）
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(baseSwingAngle));
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
