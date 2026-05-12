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
import net.minecraft.core.BlockPos;
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
    private static final WorkingAnimationScheme WORKING_ANIMATION_SCHEME = new WorkingAnimationScheme();

    public SmartBlockPlacerRenderer(BlockEntityRendererProvider.Context context) {
        // 不需要初始化，使用静态常量
    }
    
    /**
     * 底座摆动动画方案（待机状态）
     */
    private static class SwingBaseAnimationScheme {
        @SuppressWarnings("SameReturnValue")
        public float getBaseSwingAngle(float swingProgress, boolean isSwinging, float swingDirection) {
            // 如果正在摆动，计算角度
            if (isSwinging) {
                // 摆动动画：
                // 0-40tick（0-2秒）：从0°缓慢转到目标角度
                // 40-60tick（2-3秒）：在目标角度停留1秒
                // 60-100tick（3-5秒）：从目标角度缓慢回到0°
                float targetAngle = 25f * swingDirection; // 目标角度±25°
                
                if (swingProgress <= 40f) {
                    // 第一阶段：从0°缓慢转到目标角度（2秒）
                    float t = swingProgress / 40f;
                    // 使用缓动函数让运动更平滑
                    return targetAngle * (float) Math.sin(t * Math.PI / 2);
                } else if (swingProgress <= 60f) {
                    // 第二阶段：在目标角度停留1秒
                    return targetAngle;
                } else if (swingProgress <= 100f) {
                    // 第三阶段：从目标角度缓慢回到0°（2秒）
                    float t = (swingProgress - 60f) / 40f;
                    // 使用缓动函数让运动更平滑
                    return targetAngle * (1f - (float) Math.sin(t * Math.PI / 2));
                }
            }
            
            return 0f;
        }
    }
    
    /**
     * 工作动画方案（放置方块时）
     */
    private static class WorkingAnimationScheme {
        // 机械臂参数（单位：Minecraft方块）
        private static final float UPPER_ARM_LENGTH = 2.5f;  // 大臂长度
        private static final float FOREARM_LENGTH = 2.5f;    // 小臂长度
        private static final float BASE_HEIGHT = 0.0f;       // 底座关节高度（相对于底座模型）
        private static final float CLAW_OFFSET = 0.1f;       // 钳子偏移
        
        /**
         * 计算机械臂角度以指向目标位置
         * 
         * @param targetPos 目标位置（世界坐标）
         * @param placerPos 放置器位置（世界坐标）
         * @param facing 放置器朝向
         * @param upsideDown 是否倒挂
         * @param animationProgress 动画进度（0-1）
         * @return float[]{baseSwingAngle, upperArmAngle, forearmAngle, clawAngle}
         */
        @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
        public float[] calculateArmAngles(
            net.minecraft.core.BlockPos targetPos,
            net.minecraft.core.BlockPos placerPos,
            Direction facing,
            boolean upsideDown,
            float animationProgress
        ) {
            // 1. 计算目标位置相对于放置器的偏移
            double dx = targetPos.getX() - placerPos.getX();
            double dy = targetPos.getY() - placerPos.getY();
            double dz = targetPos.getZ() - placerPos.getZ();
            
            // 2. 根据朝向转换到局部坐标系
            // facing就是放置器的前方（5×5网格在facing方向上）
            Direction forward = facing;
            Direction right = facing.getCounterClockWise(); // 逆时针方向
            
            // 计算在局部坐标系中的位置（只考虑XZ平面）
            double forwardDist = dx * forward.getStepX() + dz * forward.getStepZ();
            double rightDist = dx * right.getStepX() + dz * right.getStepZ();
            // 垂直距离：直接使用dy，不需要取反
            double verticalDist = dy;
            
            // 3. 计算底座旋转角度（水平面内）
            // 计算的是相对于放置器前方的偏移角度
            float baseAngle = (float) Math.toDegrees(Math.atan2(rightDist, forwardDist));
            
            // 4. 计算水平距离
            float horizontalDist = (float) Math.sqrt(forwardDist * forwardDist + rightDist * rightDist);
            
            // 5. 计算垂直距离（考虑底座高度）
            float targetHeight = (float) verticalDist - BASE_HEIGHT;
            
            // 6. 计算仰角（从水平面到目标的角度）
            float elevationAngle = (float) Math.toDegrees(Math.atan2(targetHeight, horizontalDist));
            
            // 7. 直接使用逆运动学计算关节角度
            // 计算实际需要的总长度（3D距离）
            float distToTarget = (float) Math.sqrt(horizontalDist * horizontalDist + targetHeight * targetHeight);
            
            float upperArmAngle;
            float forearmAngle;
            
            // 如果目标超出机械臂最大长度，完全伸直指向目标
            if (distToTarget >= UPPER_ARM_LENGTH + FOREARM_LENGTH) {
                // 完全伸直：大臂指向目标方向，小臂伸直
                upperArmAngle = -30f - horizontalDist * 14f + targetHeight * 10f;
                forearmAngle = 90f; // 小臂伸直（180° * 0.5 = 90°）
            } else {
                // 限制距离在合理范围内，避免除零和无效计算
                float clampedDist = Math.max(0.01f, distToTarget);
                
                // 使用余弦定理计算关节角度（基于3D距离）
                float cosForearm = (UPPER_ARM_LENGTH * UPPER_ARM_LENGTH + FOREARM_LENGTH * FOREARM_LENGTH - clampedDist * clampedDist) 
                    / (2 * UPPER_ARM_LENGTH * FOREARM_LENGTH);
                float cosUpperArm = (clampedDist * clampedDist + UPPER_ARM_LENGTH * UPPER_ARM_LENGTH - FOREARM_LENGTH * FOREARM_LENGTH) 
                    / (2 * clampedDist * UPPER_ARM_LENGTH);
                
                // 限制cos值在[-1, 1]范围内
                cosForearm = Math.max(-1.0f, Math.min(1.0f, cosForearm));
                cosUpperArm = Math.max(-1.0f, Math.min(1.0f, cosUpperArm));
                
                // 计算角度
                // forearmAngleFromUpper 是肘关节的内角（0°-180°）
                // 完全伸直时内角=0°，完全弯曲时内角=180°
                float forearmAngleFromUpper = (float) Math.toDegrees(Math.acos(cosForearm));
                float upperArmAngleFromTarget = (float) Math.toDegrees(Math.acos(cosUpperArm));
                
                // 大臂角度：水平距离 + 高度共同影响
                // 基础：近距离→-30°, 远距离→-100°
                // 高度修正：目标在上方(targetHeight>0) → 角度增大（更垂直）
                //          目标在下方(targetHeight<0) → 角度减小（更水平）
                // 公式：θ = -30 - horizontalDist * 14 + targetHeight * 10
                upperArmAngle = -30f - horizontalDist * 14f + targetHeight * 10f;
                
                // 小臂角度：相对于大臂的弯曲角度
                // 内角 = forearmAngleFromUpper
                // 小臂需要旋转的角度 = 180° - 内角（让大臂和小臂形成内角）
                // 偏转减半让动作更柔和
                forearmAngle = (180f - forearmAngleFromUpper) * 0.5f;
            }
            
            // 钳子角度：根据仰角调整，让钳子指向目标
            // 钳子模型有-45°的初始X轴旋转，需要补偿这个角度
            // 钳子应该垂直向下（相对于小臂末端）指向目标
            // 当仰角为0（水平）时，钳子应该保持-45°（模型初始角度）
            // 当仰角为负（目标在下方）时，钳子需要向上旋转（减少负角度）
            // 当仰角为正（目标在上方）时，钳子需要向下旋转（增加负角度）
            float clawAngle = 45f - elevationAngle * 0.8f; // 初始-45° - 仰角补偿
            
            // 应用动画进度插值（从待机位置平滑过渡到目标位置）
            float easedProgress = easeInOutCubic(animationProgress);
            baseAngle *= easedProgress;
            upperArmAngle *= easedProgress;
            forearmAngle *= easedProgress;
            clawAngle *= easedProgress;
            
            return new float[]{baseAngle, upperArmAngle, forearmAngle, clawAngle};
        }
        
        /**
         * 缓动函数：让动画更平滑
         */
        private float easeInOutCubic(float t) {
            return t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f;
        }
    }

    /**
     * 缓动函数：让动画更平滑
     */
    private float easeInOutCubic(float t) {
        return t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f;
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
                // 检测是否是刚通电（之前未通电或之前有红石信号）
                boolean justPowered = !currentWasPowered || currentWasRedstoneSignal;
                
                if (justPowered) {
                    // 刚通电，初始化动画偏移量
                    entity.initAnimationOffset();
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
            
            // 更新BlockEntity的待机动画状态
            entity.updateIdleAnimationState(smoothTicks);
        } else {
            currentAnimationTicks = 0f;
            currentLastGameTime = 0;
            entity.resetIdleAnimationState();
        }
        
        // 更新BlockEntity的动画状态
        entity.updateClientAnimationState(currentAnimationTicks, currentLastGameTime, isCurrentlyPowered, hasRedstoneSignal);
        
        // 计算动画角度
        float baseSwingAngle = 0f;
        float upperArmAngle = 0f;
        float forearmAngle = 0f;
        float clawAngle = 0f;
        
        // 判断是否处于工作状态（正在放置方块）
        boolean isWorking = entity.isWaitingForPlacement() || entity.getPlaceCooldown() > 0;
        
        // 通电且不在工作状态时执行待机动画（随机摆动）
        if (isCurrentlyPowered && !hasRedstoneSignal && !isWorking) {
            baseSwingAngle = ANIMATION_SCHEME.getBaseSwingAngle(
                entity.getClientIdleSwingProgress(),
                entity.isClientIdleSwinging(),
                entity.getClientIdleSwingDirection()
            );
        } else if (isWorking && entity.getLevel() != null) {
            // 工作状态下计算机械臂指向目标位置的动画
            // 获取下一个要放置的位置
            BlockPos nextTargetPos = getNextTargetPosition(entity, facing, upsideDown);
            if (nextTargetPos != null) {
                // 计算动画进度（基于placeCooldown）
                float animationProgress = 1.0f;
                if (entity.isWaitingForPlacement()) {
                    // 等待阶段：从0到1，加入partialTick实现平滑
                    animationProgress = 1.0f - (float) (entity.getPlaceCooldown() - partialTick) / 40f;
                    animationProgress = Math.max(0.0f, Math.min(1.0f, animationProgress));
                } else if (entity.getPlaceCooldown() > 0) {
                    // 冷却阶段：保持到1
                    animationProgress = 1.0f;
                }
                
                // 计算目标角度（传入animationProgress进行插值）
                float[] targetAngles = WORKING_ANIMATION_SCHEME.calculateArmAngles(
                    nextTargetPos,
                    entity.getBlockPos(),
                    facing,
                    upsideDown,
                    animationProgress
                );
                
                // 获取前一个目标位置和过渡进度
                BlockPos previousTargetPos = entity.getClientPreviousTargetPos();
                float transitionProgress = entity.getClientTransitionProgress();
                
                // 统一使用过渡动画，无论是切换目标还是初次移动
                if (previousTargetPos != null && transitionProgress < 1.0f) {
                    // 更新过渡进度（20帧完成过渡，约1秒）
                    // 加入partialTick实现帧间平滑插值
                    float frameDelta = (1.0f / 20.0f); // 每tick增加 5%
                    transitionProgress += frameDelta;
                    if (transitionProgress > 1.0f) transitionProgress = 1.0f;
                    entity.updateTransitionProgress(transitionProgress);
                    
                    // 计算前一个目标的角度
                    float[] previousAngles = WORKING_ANIMATION_SCHEME.calculateArmAngles(
                        previousTargetPos,
                        entity.getBlockPos(),
                        facing,
                        upsideDown,
                        1.0f // 前一个目标使用完成状态
                    );
                    
                    // 在前后角度之间插值，加入partialTick平滑
                    float easedTransition = easeInOutCubic(Math.min(1.0f, transitionProgress));
                    baseSwingAngle = previousAngles[0] + (targetAngles[0] - previousAngles[0]) * easedTransition;
                    upperArmAngle = previousAngles[1] + (targetAngles[1] - previousAngles[1]) * easedTransition;
                    forearmAngle = previousAngles[2] + (targetAngles[2] - previousAngles[2]) * easedTransition;
                    clawAngle = previousAngles[3] + (targetAngles[3] - previousAngles[3]) * easedTransition;
                } else if (transitionProgress < 1.0f) {
                    // 初次移动：从待机位置平滑过渡到目标位置
                    float frameDelta = (1.0f / 20.0f); // 每tick增加 5%
                    transitionProgress += frameDelta;
                    if (transitionProgress > 1.0f) transitionProgress = 1.0f;
                    entity.updateTransitionProgress(transitionProgress);
                    
                    // 待机位置角度（全0）
                    float easedTransition = easeInOutCubic(Math.min(1.0f, transitionProgress));
                    baseSwingAngle = targetAngles[0] * easedTransition;
                    upperArmAngle = targetAngles[1] * easedTransition;
                    forearmAngle = targetAngles[2] * easedTransition;
                    clawAngle = targetAngles[3] * easedTransition;
                } else {
                    // 过渡完成，直接使用目标角度（已经包含animationProgress的平滑）
                    baseSwingAngle = targetAngles[0];
                    upperArmAngle = targetAngles[1];
                    forearmAngle = targetAngles[2];
                    clawAngle = targetAngles[3];
                }
                
                // 更新BlockEntity的工作状态
                entity.updateWorkingTarget(nextTargetPos, animationProgress);
            }
        } else {
            // 不在工作状态，重置
            entity.resetWorkingState();
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
        // 大臂的旋转中心Y轴在 10/16=0.625
        poseStack.translate(0, 0.625, 0);
        poseStack.mulPose(Axis.XP.rotationDegrees(upperArmAngle));
        poseStack.translate(0, -0.625, 0);
        poseStack.translate(-0.5, 0.0, -0.5);
        renderModel(poseStack, buffer, UPPERARM_MODEL, packedLight, packedOverlay);
        
        // 渲染小臂和钳子
        poseStack.pushPose();
        // 小臂的旋转中心在 (0.6875, 1.0625, 0.9375) 即 (11/16, 17/16, 15/16)
        poseStack.translate(0.6875, 1.0625, 0.9375);
        poseStack.mulPose(Axis.XP.rotationDegrees(forearmAngle));
        poseStack.translate(-0.6875, -1.0625, -0.9375);
        renderModel(poseStack, buffer, FOREARM_MODEL, packedLight, packedOverlay);
        poseStack.pushPose();
        // 钳子的旋转中心在 (0.5, 1.3125, 0.375) 即 (8/16, 21/16, 6/16)
        poseStack.translate(0.5, 1.3125, 0.375);
        poseStack.mulPose(Axis.XP.rotationDegrees(clawAngle));
        poseStack.translate(-0.5, -1.3125, -0.375);
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
    
    /**
     * 获取下一个要放置的目标位置
     */
    private BlockPos getNextTargetPosition(SmartBlockPlacerBlockEntity entity, Direction facing, boolean upsideDown) {
        // 计算基准位置（放置器前方4格，水平方向）
        BlockPos basePos = entity.getBlockPos().relative(facing.getOpposite(), -4);
        
        // 获取所有配置的位置
        java.util.Map<Integer, java.util.Set<Integer>> layerPositions = entity.getLayerPositions();
        
        // 构建有序的放置位置列表（与BlockEntity保持一致）
        java.util.List<BlockPos> allPositions = buildOrderedPositionsForRenderer(basePos, facing, layerPositions);
        
        // 如果没有配置任何位置，返回null
        if (allPositions.isEmpty()) {
            return null;
        }
        
        // 获取当前放置索引
        int currentIndex = entity.getCurrentPlacementIndex();
        if (currentIndex >= allPositions.size()) {
            currentIndex = 0;
        }
        
        // 从当前索引开始查找第一个空位
        for (int i = 0; i < allPositions.size(); i++) {
            int index = (currentIndex + i) % allPositions.size();
            BlockPos targetPos = allPositions.get(index);
            
            // 如果目标位置为空，返回该位置
            if (entity.getLevel() != null && entity.getLevel().isEmptyBlock(targetPos)) {
                return targetPos;
            }
        }
        
        // 所有位置都已有方块，返回null
        return null;
    }
    
    /**
     * 构建有序的放置位置列表（渲染器使用）
     * 顺序：从最下面一层开始，每一层从最远离放置器的位置开始，从左到右，然后逐渐向下
     */
    private java.util.List<BlockPos> buildOrderedPositionsForRenderer(
        BlockPos basePos, Direction facing, java.util.Map<Integer, java.util.Set<Integer>> layerPositions) {
        java.util.List<BlockPos> positions = new java.util.ArrayList<>();
        
        // 获取所有layer，按layer升序排序（从最下面开始）
        // 注意：必须使用排序后的List，确保顺序稳定
        java.util.List<Integer> sortedLayers = new java.util.ArrayList<>(layerPositions.keySet());
        sortedLayers.sort(Integer::compareTo);
        
        for (int layer : sortedLayers) {
            java.util.Set<Integer> layerPositionsSet = layerPositions.get(layer);
            if (layerPositionsSet == null || layerPositionsSet.isEmpty()) {
                continue;
            }
            
            // 将该层的所有位置收集起来
            java.util.List<int[]> rowColList = new java.util.ArrayList<>();
            for (int position : layerPositionsSet) {
                int row = position / 5;
                int col = position % 5;
                rowColList.add(new int[]{row, col, position}); // 保存原始position用于调试
            }
            
            // 排序：先按row升序（从远到近，0→4），再按col升序（从左到右，0→4）
            rowColList.sort((a, b) -> {
                if (a[0] != b[0]) {
                    return Integer.compare(a[0], b[0]); // row升序（0→4，远→近）
                }
                return Integer.compare(a[1], b[1]); // col升序（0→4，左→右）
            });
            
            // 按排序后的顺序添加位置
            for (int[] rowCol : rowColList) {
                int row = rowCol[0];
                int col = rowCol[1];
                BlockPos targetPos = calculateTargetPosition(basePos, facing, row, col, layer);
                positions.add(targetPos);
            }
        }
        
        return positions;
    }
    
    /**
     * 计算目标位置（复制自BlockEntity的逻辑）
     */
    private BlockPos calculateTargetPosition(BlockPos basePos, Direction facing, int row, int col, int layer) {
        // 根据朝向计算水平偏移方向
        Direction right = facing.getClockWise();
        
        BlockPos pos = basePos;
        pos = pos.above(layer); // 层偏移（垂直向上）
        pos = pos.relative(right, col - 2); // 列偏移（-2到+2）
        pos = pos.relative(facing.getClockWise().getClockWise(), row - 2); // 行偏移（-2到+2）
        
        return pos;
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
