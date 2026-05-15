package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.SmartBlockPlacerBlock;
import dev.dubhe.anvilcraft.block.entity.SmartBlockPlacerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SmartBlockPlacerRenderer implements BlockEntityRenderer<SmartBlockPlacerBlockEntity> {
    // 位置列表缓存，避免每帧重新分配
    private final Map<String, List<BlockPos>> positionCache = new HashMap<>();
    
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
    private static final ModelResourceLocation CLAW_OPEN_MODEL = ModelResourceLocation.standalone(
        AnvilCraft.of("block/smart_block_placer_claw_open")
    );

    private static final WorkingAnimationScheme WORKING_ANIMATION_SCHEME = new WorkingAnimationScheme();

    @SuppressWarnings("unused")
    public SmartBlockPlacerRenderer(BlockEntityRendererProvider.Context context) {
        // 不需要初始化，使用静态常量
    }

    /**
     * 工作动画方案（放置方块时）
     */
    private static class WorkingAnimationScheme {
        // 机械臂参数（单位：Minecraft方块）
        private static final float UPPER_ARM_LENGTH = 2.5f;  // 大臂长度
        private static final float FOREARM_LENGTH = 2.5f;    // 小臂长度
        private static final float BASE_HEIGHT = 0.0f;       // 底座关节高度（相对于底座模型）
        private static final int ANIMATION_DURATION_TICKS = 20; // 动画总持续时间：20tick = 1秒
        
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
            BlockPos targetPos,
            BlockPos placerPos,
            Direction facing,
            boolean upsideDown,
            float animationProgress
        ) {
            // 计算目标角度
            float[] targetAngles = this.calculateTargetAngles(targetPos, placerPos, facing, upsideDown);
        
            // 根据动画进度计算当前角度
            float baseAngle;
            float upperArmAngle;
            float forearmAngle;
            float clawAngle;
        
            if (animationProgress <= 0.2f) {
                // 阶段1：底盘旋转 + 小臂和钳子指向目标
                // 大臂不动(0°)，小臂需要补偿角度以指向目标
                float phase1Progress = animationProgress / 0.2f;
        
                baseAngle = targetAngles[0] * phase1Progress;
                upperArmAngle = 0f; // 大臂不动
        
                // 计算补偿角度：当大臂为0时，小臂需要多少度才能让钳子指向目标
                // 使用简化补偿：小臂角度 = 目标大臂角度 + 目标小臂角度
                float compensationAngle = targetAngles[1] + targetAngles[2];
                forearmAngle = compensationAngle * phase1Progress;
                clawAngle = targetAngles[3] * phase1Progress;
        
            } else if (animationProgress <= 0.3f) {
                // 阶段2：停顿，保持指向目标
                baseAngle = targetAngles[0];
                upperArmAngle = 0f;
                forearmAngle = targetAngles[1] + targetAngles[2]; // 补偿角度
                clawAngle = targetAngles[3];
        
            } else if (animationProgress <= 0.7f) {
                // 阶段3：大臂推出（延长），小臂持续补偿
                float phase3Progress = (animationProgress - 0.3f) / 0.4f;
        
                baseAngle = targetAngles[0];
                upperArmAngle = targetAngles[1] * phase3Progress;
        
                // 小臂补偿：从"补偿角度"渐变到"目标角度"
                // 当大臂到位时，小臂也应该是目标角度
                float startForearmAngle = targetAngles[1] + targetAngles[2]; // 起始补偿角度
                float endForearmAngle = targetAngles[2]; // 结束目标角度
                forearmAngle = startForearmAngle + (endForearmAngle - startForearmAngle) * phase3Progress;
        
                clawAngle = targetAngles[3];
        
            } else {
                // 阶段4：收回动画 - 合并为一个平滑的过程
                // 底盘、大臂、小臂同时归零
                float phase4Progress = (animationProgress - 0.7f) / 0.3f;
                        
                baseAngle = targetAngles[0] * (1f - phase4Progress); // 底盘归零
                upperArmAngle = targetAngles[1] * (1f - phase4Progress); // 大臂归零
                forearmAngle = targetAngles[2] * (1f - phase4Progress); // 小臂直接归零
                clawAngle = targetAngles[3] * (1f - phase4Progress); // 钳子归零
            }
        
            return new float[]{baseAngle, upperArmAngle, forearmAngle, clawAngle};
        }
        
        /**
         * 计算目标角度（不考虑动画进度）
         */
        private float[] calculateTargetAngles(
            BlockPos targetPos,
            BlockPos placerPos,
            Direction facing,
            boolean upsideDown
        ) {
            // 1. 计算目标位置相对于放置器的偏移
            double dx = targetPos.getX() - placerPos.getX();
            double dy = targetPos.getY() - placerPos.getY();
            double dz = targetPos.getZ() - placerPos.getZ();

            // 2. 根据朝向转换到局部坐标系
            Direction right = facing.getCounterClockWise();

            // 计算在局部坐标系中的位置
            double forwardDist = dx * facing.getStepX() + dz * facing.getStepZ();
            double rightDist = dx * right.getStepX() + dz * right.getStepZ();

            // 3. 计算底座旋转角度（水平面内）
            final float baseAngle = (float) Math.toDegrees(Math.atan2(rightDist, forwardDist));

            // 4. 计算水平距离
            final float horizontalDist = (float) Math.sqrt(forwardDist * forwardDist + rightDist * rightDist);

            // 5. 计算垂直距离（倒挂时需要翻转）
            float targetHeight = (float) dy - BASE_HEIGHT;
            if (upsideDown) {
                targetHeight = -(float) dy - BASE_HEIGHT;
            }

            // 6. 计算仰角
            final float elevationAngle = (float) Math.toDegrees(Math.atan2(targetHeight, horizontalDist));

            // 7. 计算机械臂关节角度（逆运动学）
            final float distToTarget = (float) Math.sqrt(horizontalDist * horizontalDist + targetHeight * targetHeight);
            final boolean isOverRange = distToTarget >= UPPER_ARM_LENGTH + FOREARM_LENGTH;

            float upperArmAngle;
            float forearmAngle;
            if (isOverRange) {
                // 超距情况：机械臂完全伸直指向目标
                upperArmAngle = elevationAngle - 74f;
                forearmAngle = 85f;
            } else {
                // 正常情况：使用余弦定理计算关节角度
                float clampedDist = Math.max(0.01f, distToTarget);

                float cosForearm = (UPPER_ARM_LENGTH * UPPER_ARM_LENGTH + FOREARM_LENGTH * FOREARM_LENGTH - clampedDist * clampedDist)
                    / (2 * UPPER_ARM_LENGTH * FOREARM_LENGTH);
                cosForearm = Math.max(-1.0f, Math.min(1.0f, cosForearm));
                float forearmAngleFromUpper = (float) Math.toDegrees(Math.acos(cosForearm));

                float cosUpperArm = (clampedDist * clampedDist + UPPER_ARM_LENGTH * UPPER_ARM_LENGTH - FOREARM_LENGTH * FOREARM_LENGTH)
                    / (2 * clampedDist * UPPER_ARM_LENGTH);
                cosUpperArm = Math.max(-1.0f, Math.min(1.0f, cosUpperArm));
                float upperArmAngleFromTarget = (float) Math.toDegrees(Math.acos(cosUpperArm));

                upperArmAngle = -(180f - upperArmAngleFromTarget - elevationAngle) * 0.6f + 20f;
                forearmAngle = forearmAngleFromUpper * 0.8f - 10f;
            }

            // 应用距离修正
            upperArmAngle += horizontalDist <= 2.0f ? -10f :
                           (horizontalDist >= 4.0f ? -66f :
                           -10f + (-50f) * (horizontalDist - 2.0f) / 2.0f);

            forearmAngle += horizontalDist >= 4.0f ? 40f : 0f;

            // 钳子角度（随高度变化，超距时额外修正）
            float clawAngle = 45f - elevationAngle * -0.4f + (isOverRange ? -10f : 0f);

            return new float[]{baseAngle, upperArmAngle, forearmAngle, clawAngle};
        }

        /**
         * 获取动画持续时间（tick）
         */
        public int getAnimationDurationTicks() {
            return ANIMATION_DURATION_TICKS;
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
        
        // 更新BlockEntity的动画状态
        entity.updateClientAnimationState(isCurrentlyPowered, hasRedstoneSignal);
        
        // 计算动画角度
        float baseSwingAngle = 0f;
        float upperArmAngle = 0f;
        float forearmAngle = 0f;
        float clawAngle = 0f;
        
        // 判断是否处于工作状态（正在放置方块）
        boolean isWorking = entity.getPlaceCooldown() > 0;
        
        if (isCurrentlyPowered && !hasRedstoneSignal && isWorking && entity.getLevel() != null) {
            // 获取动画进度
            long currentTime = entity.getLevel().getGameTime();
            long animStartTime = entity.getClientAnimationStartTime();
            BlockPos animTargetPos = entity.getClientLastTargetPos();
            
            // 如果动画未开始，查找目标位置并初始化
            if (animStartTime == 0) {
                BlockPos targetPos = getNextTargetPosition(entity, facing, upsideDown);
                if (targetPos != null) {
                    entity.setClientAnimationStartTime(currentTime);
                    entity.setClientLastTargetPos(targetPos);
                    animStartTime = currentTime;
                    animTargetPos = targetPos;
                }
            }
            
            // 播放动画（animStartTime != 0 说明 animTargetPos 已经被设置）
            if (animStartTime != 0) {
                // 使用缓存的目标位置播放动画
                // 在整个动画周期内锁定目标位置，确保动画流畅
                long elapsedTicks = currentTime - animStartTime;

                // 只有当动画完成后，才查找新的目标位置
                if (elapsedTicks >= WORKING_ANIMATION_SCHEME.getAnimationDurationTicks()) {
                    BlockPos targetPos = getNextTargetPosition(entity, facing, upsideDown);
                    // 如果找到新的目标位置且与当前不同，开始新动画
                    if (targetPos != null && !targetPos.equals(animTargetPos)) {
                        // 修复：使用当前时间作为新动画的起点，避免累积偏移
                        entity.setClientAnimationStartTime(currentTime);
                        entity.setClientLastTargetPos(targetPos);
                        animTargetPos = targetPos;
                        elapsedTicks = 0;
                    } else {
                        // 没有新目标或目标未改变，保持动画完成状态
                        elapsedTicks = WORKING_ANIMATION_SCHEME.getAnimationDurationTicks();
                    }
                }
                
                float animationProgress = Math.min(
                    1.0f,
                    (elapsedTicks + partialTick) / (float) WORKING_ANIMATION_SCHEME.getAnimationDurationTicks()
                );

                // 计算当前角度
                float[] angles = WORKING_ANIMATION_SCHEME.calculateArmAngles(
                    animTargetPos, entity.getBlockPos(), facing, upsideDown, animationProgress
                );
                baseSwingAngle = angles[0];
                upperArmAngle = angles[1];
                forearmAngle = angles[2];
                clawAngle = angles[3];
            }
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
        
        // 根据方块放置状态切换钳子模型
        // placeCooldown > 0 表示正在放置方块，使用打开的钳子模型
        // placeCooldown == 0 表示放置完成，使用闭合的钳子模型
        ModelResourceLocation currentClawModel = isWorking ? CLAW_OPEN_MODEL : CLAW_MODEL;
        renderModel(poseStack, buffer, currentClawModel, packedLight, packedOverlay);
        
        // 如果正在工作，在钳子开口位置渲染要放置的方块
        if (isWorking && entity.getLevel() != null) {
            renderHeldBlock(poseStack, buffer, entity, packedLight, packedOverlay);
        }
        
        poseStack.popPose();
        poseStack.popPose();
        poseStack.popPose();
        poseStack.popPose();
    }
    
    private void applyHorizontalRotation(PoseStack poseStack, Direction facing, boolean upsideDown) {
        float rotation = switch (facing) {
            case WEST -> 90f;
            case SOUTH -> 180f;
            case EAST -> 270f;
            default -> 0f; // NORTH
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
    @Nullable
    private BlockPos getNextTargetPosition(SmartBlockPlacerBlockEntity entity, Direction facing, boolean upsideDown) {
        // 计算基准位置（放置器前方4格，水平方向）
        BlockPos basePos = entity.getBlockPos().relative(facing.getOpposite(), -4);
        
        // 获取所有配置的位置
        Map<Integer, Set<Integer>> layerPositions = entity.getLayerPositions();
        
        // 构建有序的放置位置列表（与BlockEntity保持一致）
        List<BlockPos> allPositions = buildOrderedPositionsForRenderer(basePos, facing, layerPositions, upsideDown);
        
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
    private List<BlockPos> buildOrderedPositionsForRenderer(
        BlockPos basePos, Direction facing, Map<Integer, Set<Integer>> layerPositions, boolean upsideDown) {
        // 使用缓存的 key：放置器位置 + 朝向 + 倒挂状态 + layerPositions 的哈希
        String cacheKey = basePos.toShortString() + "_" + facing.getName() + "_" + upsideDown + "_" + layerPositions.hashCode();
        
        // 检查缓存
        if (this.positionCache.containsKey(cacheKey)) {
            return this.positionCache.get(cacheKey);
        }
        
        // 调用 BlockEntity 的静态方法计算位置列表
        List<BlockPos> positions = SmartBlockPlacerBlockEntity.buildOrderedPositions(basePos, facing, layerPositions, upsideDown);
        
        // 更新缓存
        this.positionCache.put(cacheKey, positions);
        
        return positions;
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
    
    /**
     * 渲染钳子中持有的方块
     */
    @SuppressWarnings({"checkstyle:EmptyLineSeparator", "deprecation"})
    private void renderHeldBlock(
        PoseStack poseStack, MultiBufferSource buffer, SmartBlockPlacerBlockEntity entity, int packedLight, int packedOverlay) {
        // 使用 currentHeldBlock 字段（已同步到客户端）来获取要渲染的方块
        ItemStack stack = entity.getCurrentHeldBlock();
        
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) {
            return;
        }
        
        // 获取方块的BlockState
        Block block = blockItem.getBlock();
        BlockState blockState = block.defaultBlockState();
        
        // 渲染方块模型
        poseStack.pushPose();
        // 方块在钳子开口位置（相对于钳子旋转中心）
        poseStack.translate(0.375, 0.9, -0.1);
        poseStack.scale(0.25f, 0.25f, 0.25f);
        
        BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
        BakedModel bakedModel = blockRenderer.getBlockModel(blockState);
        
        // 根据方块状态选择合适的渲染类型
        RenderType renderType = ItemBlockRenderTypes
            .getRenderType(blockState, false);
        
        // 使用renderModel方法渲染方块
        blockRenderer.getModelRenderer().renderModel(
            poseStack.last(),
            buffer.getBuffer(renderType),
            blockState,
            bakedModel,
            1f, 1f, 1f,
            packedLight,
            packedOverlay
        );
        
        poseStack.popPose();
    }
}
