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

    // 客户端本地的 tick 计数器
    private float clientTicks = 0f;
    private boolean wasPowered = false; // 记录上一次的通电状态
    private long lastGameTime = 0; // 记录上次游戏时间
    
    // 动画方案接口
    private IAnimationScheme currentAnimationScheme = null;

    public SmartBlockPlacerRenderer(BlockEntityRendererProvider.Context context) {
        // 默认使用旋转盘摆动动画方案
        this.currentAnimationScheme = new SwingBaseAnimationScheme();
    }
    
    /**
     * 动画方案接口
     * 后续可以实现多种动画方案,如:默认动画、快速动画、平滑动画等
     */
    public interface IAnimationScheme {
        /**
         * 获取底座摆动角度
         *
         * @param time 客户端时间
         * @param isPowered 是否通电
         * @return 旋转角度(度)
         */
        default float getBaseSwingAngle(float time, boolean isPowered) {
            return 0f;
        }
        
        /**
         * 获取大臂旋转角度
         *
         * @param time 客户端时间
         * @param isPowered 是否通电
         * @return 旋转角度(度)
         */
        float getUpperArmAngle(float time, boolean isPowered);
        
        /**
         * 获取小臂旋转角度
         *
         * @param time 客户端时间
         * @param isPowered 是否通电
         * @return 旋转角度(度)
         */
        float getForearmAngle(float time, boolean isPowered);
        
        /**
         * 获取钳子开合角度
         *
         * @param time 客户端时间
         * @param isPowered 是否通电
         * @return 旋转角度(度)
         */
        float getClawAngle(float time, boolean isPowered);
    }
    
    /**
     * 设置动画方案
     *
     * @param scheme 动画方案实现
     */
    public void setAnimationScheme(IAnimationScheme scheme) {
        this.currentAnimationScheme = scheme;
    }
    
    /**
     * 旋转盘摆动动画方案
     * 仅让旋转盘左右摆动，机械臂不动但跟随转动
     */
    public static class SwingBaseAnimationScheme implements IAnimationScheme {
        @Override
        public float getBaseSwingAngle(float ticks, boolean isPowered) {
            if (!isPowered) return 0f;
            
            // 动画周期: 0°→30°→停顿→30°→-30°→停顿→-30°→0°→停顿
            // 总周期: 140 tick (7秒), 角速度: 1.5°/tick
            float t = ticks % 140.0f;
            
            if (t < 20.0f) return t * 1.5f;                    // 0-1秒: 0° → 30°
            if (t < 40.0f) return 30f;                          // 1-2秒: 停在 30°
            if (t < 80.0f) return 30f - (t - 40.0f) * 1.5f;    // 2-4秒: 30° → -30°
            if (t < 100.0f) return -30f;                        // 4-5秒: 停在 -30°
            if (t < 120.0f) return -30f + (t - 100.0f) * 1.5f; // 5-6秒: -30° → 0°
            return 0f;                                          // 6-7秒: 停在 0°
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
        if (!(state.getBlock() instanceof SmartBlockPlacerBlock)) {
            return;
        }
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        poseStack.pushPose();
        
        // 先将整个机械臂平移到方块中心(Y=0.0 让底座贴地)
        poseStack.translate(0.5, 0.0, 0.5);
        
        // 计算底座朝向旋转角度
        float baseRotation = switch (facing) {
            case NORTH -> 0f;
            case WEST -> 90f;
            case SOUTH -> 180f;
            case EAST -> 270f;
            default -> 0f;
        };

        // 计算平滑动画时间
        boolean isCurrentlyPowered = entity.isPowered();
        float smoothTicks = 0f;
        
        if (isCurrentlyPowered) {
            long currentGameTime = entity.getLevel().getGameTime();
            
            // 新通电时重置计数器
            if (!wasPowered) {
                clientTicks = 0f;
                lastGameTime = currentGameTime;
            }
            
            // 累加经过的 tick
            long tickDelta = currentGameTime - lastGameTime;
            if (tickDelta > 0) {
                clientTicks += tickDelta;
                lastGameTime = currentGameTime;
            }
            
            smoothTicks = clientTicks + partialTick;
        } else {
            clientTicks = 0f;
            lastGameTime = 0;
        }
        
        wasPowered = isCurrentlyPowered;
        
        // 计算各部件旋转角度
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
        poseStack.mulPose(Axis.YP.rotationDegrees(baseRotation + baseSwingAngle));
        poseStack.translate(-0.5, 0.0, -0.5);
        renderModel(poseStack, buffer, BASE_MODEL, packedLight, packedOverlay);
        poseStack.popPose();
        
        // 渲染大臂（跟随底座旋转）
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(baseRotation + baseSwingAngle));
        poseStack.mulPose(Axis.XP.rotationDegrees(upperArmAngle));
        poseStack.translate(-0.5, 0.0, -0.5);
        renderModel(poseStack, buffer, UPPERARM_MODEL, packedLight, packedOverlay);
        
        // 渲染小臂
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(forearmAngle));
        renderModel(poseStack, buffer, FOREARM_MODEL, packedLight, packedOverlay);
        poseStack.popPose();
        
        // 渲染钳子
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(clawAngle));
        poseStack.translate(0.0, -0.1, 0.0);
        renderModel(poseStack, buffer, CLAW_MODEL, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
        poseStack.popPose();
    }
    @SuppressWarnings("checkstyle:EmptyLineSeparator")
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
