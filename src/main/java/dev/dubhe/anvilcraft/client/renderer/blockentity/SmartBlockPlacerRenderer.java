package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.SmartBlockPlacerBlock;
import dev.dubhe.anvilcraft.block.entity.SmartBlockPlacerBlockEntity;
import dev.dubhe.anvilcraft.init.ModSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SmartBlockPlacerRenderer implements BlockEntityRenderer<SmartBlockPlacerBlockEntity> {
    // 位置列表缓存
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
         * 计算机械臂角度
         *
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
            float[] targetAngles = this.calculateTargetAngles(targetPos, placerPos, facing, upsideDown);
        
            float baseAngle;
            float upperArmAngle;
            float forearmAngle;
            float clawAngle;
        
            if (animationProgress <= 0.2f) {
                // 阶段1：底盘旋转 + 小臂补偿
                float phase1Progress = animationProgress / 0.2f;
        
                baseAngle = targetAngles[0] * phase1Progress;
                upperArmAngle = 0f;
        
                float compensationAngle = targetAngles[1] + targetAngles[2];
                forearmAngle = compensationAngle * phase1Progress;
                clawAngle = targetAngles[3] * phase1Progress;
        
            } else if (animationProgress <= 0.3f) {
                // 阶段2：停顿
                baseAngle = targetAngles[0];
                upperArmAngle = 0f;
                forearmAngle = targetAngles[1] + targetAngles[2];
                clawAngle = targetAngles[3];
        
            } else if (animationProgress <= 0.7f) {
                // 阶段3：大臂推出
                float phase3Progress = (animationProgress - 0.3f) / 0.4f;
        
                baseAngle = targetAngles[0];
                upperArmAngle = targetAngles[1] * phase3Progress;
        
                float startForearmAngle = targetAngles[1] + targetAngles[2];
                float endForearmAngle = targetAngles[2];
                forearmAngle = startForearmAngle + (endForearmAngle - startForearmAngle) * phase3Progress;
        
                clawAngle = targetAngles[3];
        
            } else {
                // 阶段4：收回
                float phase4Progress = (animationProgress - 0.7f) / 0.3f;
                        
                baseAngle = targetAngles[0] * (1f - phase4Progress);
                upperArmAngle = targetAngles[1] * (1f - phase4Progress);
                forearmAngle = targetAngles[2] * (1f - phase4Progress);
                clawAngle = targetAngles[3] * (1f - phase4Progress);
            }
        
            return new float[]{baseAngle, upperArmAngle, forearmAngle, clawAngle};
        }
        
        @SuppressWarnings(
            {
            "checkstyle:OneStatementPerLine",
            "checkstyle:LineLength"
            }
        )
        private float[] calculateTargetAngles(
            BlockPos targetPos,
            BlockPos placerPos,
            Direction facing,
            boolean upsideDown
        ) {
            double dx = targetPos.getX() - placerPos.getX();
            double dy = targetPos.getY() - placerPos.getY();
            double dz = targetPos.getZ() - placerPos.getZ();

            Direction right = facing.getCounterClockWise();

            double forwardDist = dx * facing.getStepX() + dz * facing.getStepZ();
            double rightDist = dx * right.getStepX() + dz * right.getStepZ();

            final float baseAngle = (float) Math.toDegrees(Math.atan2(rightDist, forwardDist));
            final float horizontalDist = (float) Math.sqrt(forwardDist * forwardDist + rightDist * rightDist);

            float targetHeight = (float) dy - BASE_HEIGHT;
            if (upsideDown) {
                targetHeight = -(float) dy - BASE_HEIGHT;
            }

            final float elevationAngle = (float) Math.toDegrees(Math.atan2(targetHeight, horizontalDist));
            final float distToTarget = (float) Math.sqrt(horizontalDist * horizontalDist + targetHeight * targetHeight);
            final boolean isOverRange = distToTarget >= UPPER_ARM_LENGTH + FOREARM_LENGTH;

            float upperArmAngle;
            float forearmAngle;
            if (isOverRange) {
                upperArmAngle = elevationAngle - 74f;
                forearmAngle = 85f;
            } else {
                float clampedDist = Math.max(0.01f, distToTarget);

                float cosForearm = (UPPER_ARM_LENGTH * UPPER_ARM_LENGTH + FOREARM_LENGTH * FOREARM_LENGTH - clampedDist * clampedDist)
                    / (2 * UPPER_ARM_LENGTH * FOREARM_LENGTH);
                cosForearm = Math.max(-1.0f, Math.min(1.0f, cosForearm));
                float forearmAngleFromUpper = (float) Math.toDegrees(Math.acos(cosForearm));

                float cosUpperArm = (clampedDist * clampedDist + UPPER_ARM_LENGTH * UPPER_ARM_LENGTH - FOREARM_LENGTH * FOREARM_LENGTH)
                    / (2 * clampedDist * UPPER_ARM_LENGTH);
                cosUpperArm = Math.max(-1.0f, Math.min(1.0f, cosUpperArm));
                float upperArmAngleFromTarget = (float) Math.toDegrees(Math.acos(cosUpperArm));                upperArmAngle = -(180f - upperArmAngleFromTarget - elevationAngle) * 0.6f + 20f;
                forearmAngle = forearmAngleFromUpper * 0.8f - 10f;
            }

            upperArmAngle += horizontalDist <= 2.0f ? -10f :
                           (horizontalDist >= 4.0f ? -50f :
                           -10f + (-35f) * (horizontalDist - 2.0f) / 2.0f);

            // 在2-4格距离范围内，根据仰角增加小臂角度的动态修正（温和版）
            float forearmHeightCorrection = 0f;
            if (horizontalDist > 2.0f && horizontalDist < 4.0f) {
                // 3格距离附近，高度变化对小臂角度的影响更明显
                float distFactor = 1.0f - Math.abs(horizontalDist - 3.0f); // 在3格时最大
                forearmHeightCorrection = elevationAngle * 0.2f * distFactor;
            }
            forearmAngle += forearmHeightCorrection;
            
            forearmAngle += horizontalDist >= 4.0f ? 40f : 0f;

            // 蟹钳角度增强：在3格距离附近适度增加对高度变化的敏感度
            float clawHeightSensitivity = -0.4f;
            if (horizontalDist > 2.0f && horizontalDist < 4.0f) {
                // 在3格距离时，敏感度从-0.4增加到-0.7（微调版）
                float distFactor = 1.0f - Math.abs(horizontalDist - 3.0f);
                clawHeightSensitivity = -0.4f + (-0.3f) * distFactor;
            }
            float clawAngle = 45f - elevationAngle * clawHeightSensitivity + (isOverRange ? -10f : 0f);

            return new float[]{baseAngle, upperArmAngle, forearmAngle, clawAngle};
        }

        /**
         * 获取动画持续时间
         */
        public int getAnimationDurationTicks() {
            return ANIMATION_DURATION_TICKS;
        }
    }

    @SuppressWarnings(
        {
            "checkstyle:VariableDeclarationUsageDistance",
            "checkstyle:Indentation"
        }
    )
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
        
        // 应用变换
        poseStack.pushPose();
        poseStack.translate(0.5, 1.5, 0.5);
        if (upsideDown) {
            poseStack.mulPose(Axis.XP.rotationDegrees(180f));
        }
        applyHorizontalRotation(poseStack, facing, upsideDown);
        poseStack.translate(0, upsideDown ? 0.5 : -1.5, 0);

        boolean isCurrentlyPowered = entity.isPowered();
        boolean hasRedstoneSignal = entity.isHasRedstoneSignal();
        
        entity.updateClientAnimationState(isCurrentlyPowered, hasRedstoneSignal);
        
        // 初始化动画变量
        float baseSwingAngle = 0f;
        float upperArmAngle = 0f;
        float forearmAngle = 0f;
        float clawAngle = 0f;
        float animationProgress = 0f;
        boolean isAnimationPlaying = false;
        
        boolean isWorking = entity.getPlaceCooldown() > 0;
        
        // 检测是否需要开始收回动画
        boolean wasWorkingLastFrame = entity.getClientAnimationStartTime() != 0;
        boolean shouldStartRetract = wasWorkingLastFrame && !isWorking && !entity.isClientIsRetracting();
        net.minecraft.world.level.Level retractLevel = entity.getLevel();
        if (shouldStartRetract && retractLevel != null) {
            long animStartTime = entity.getClientAnimationStartTime();
            BlockPos animTargetPos = entity.getClientLastTargetPos();
            
            if (animStartTime != 0 && animTargetPos != null) {
                if (!entity.isRetractSoundPlayed()) {
                    retractLevel.playLocalSound(
                        entity.getBlockPos(),
                        ModSoundEvents.SMART_BLOCK_PLACER_RETRACT.get(),
                        SoundSource.BLOCKS,
                        0.4f,
                        1.3f,
                        false
                    );
                }
                entity.setClientIsRetracting(true);
                entity.setClientRetractStartTime(retractLevel.getGameTime());
                
                long elapsedTicks = retractLevel.getGameTime() - animStartTime;
                float interruptProgress = Math.min(1.0f, (elapsedTicks + partialTick) / (float) WORKING_ANIMATION_SCHEME
                    .getAnimationDurationTicks());
                float[] angles = WORKING_ANIMATION_SCHEME.calculateArmAngles(
                    animTargetPos, entity.getBlockPos(), facing, upsideDown, interruptProgress
                );
                entity.setClientRetractStartAngles(angles);
                entity.setClientRetractStartProgress(interruptProgress);
            }
        }
        
        // 重新开始工作时取消收回状态
        if (isCurrentlyPowered && !hasRedstoneSignal && isWorking) {
            entity.setClientIsRetracting(false);
        }
        
        net.minecraft.world.level.Level retractAnimLevel = entity.getLevel();
        if (entity.isClientIsRetracting() && retractAnimLevel != null) {
            long currentTime = retractAnimLevel.getGameTime();
            long elapsedRetractTicks = currentTime - entity.getClientRetractStartTime();
            
            float startProgress = entity.getClientRetractStartProgress();
            float remainingProgress = 1.0f - startProgress;
            float retractDuration = WORKING_ANIMATION_SCHEME.getAnimationDurationTicks() * remainingProgress;
            
            if (retractDuration <= 0) {
                entity.setClientIsRetracting(false);
                entity.setClientAnimationStartTime(0);
                entity.setClientLastTargetPos(null);
            } else {
                float retractProgress = Math.min(
                    1.0f,
                    (elapsedRetractTicks + partialTick) / retractDuration
                );
                
                float[] startAngles = entity.getClientRetractStartAngles();
                baseSwingAngle = startAngles[0] * (1f - retractProgress);
                upperArmAngle = startAngles[1] * (1f - retractProgress);
                forearmAngle = startAngles[2] * (1f - retractProgress);
                clawAngle = startAngles[3] * (1f - retractProgress);
                
                if (retractProgress >= 1.0f) {
                    entity.setClientIsRetracting(false);
                    entity.setClientAnimationStartTime(0);
                    entity.setClientLastTargetPos(null);
                }
            }
        } else if (isCurrentlyPowered && !hasRedstoneSignal && isWorking && retractAnimLevel != null) {
            long currentTime = retractAnimLevel.getGameTime();
            long animStartTime = entity.getClientAnimationStartTime();
            BlockPos animTargetPos = entity.getClientLastTargetPos();
            
            boolean hasValidWorkItem = !entity.getCurrentHeldBlock().isEmpty() || animStartTime != 0;
            
            // 如果动画已播放完成，检查工作条件
            if (animStartTime != 0 && animTargetPos != null) {
                long elapsedTicks = currentTime - animStartTime;
                boolean animationCompleted = elapsedTicks >= WORKING_ANIMATION_SCHEME.getAnimationDurationTicks() + 5;
                
                if (animationCompleted) {
                    BlockPos targetPos = getNextTargetPosition(entity, facing, upsideDown);
                    if (targetPos == null || targetPos.equals(animTargetPos)) {
                        if (!entity.isClientIsRetracting()) {
                            if (!entity.isRetractSoundPlayed()) {
                                entity.getLevel().playLocalSound(
                                    entity.getBlockPos(),
                                    ModSoundEvents.SMART_BLOCK_PLACER_RETRACT.get(),
                                    SoundSource.BLOCKS,
                                    0.4f,
                                    1.3f,
                                    false
                                );
                            }
                            entity.setClientIsRetracting(true);
                            entity.setClientRetractStartTime(currentTime);
                            
                            float[] endAngles = WORKING_ANIMATION_SCHEME.calculateArmAngles(
                                animTargetPos, entity.getBlockPos(), facing, upsideDown, 1.0f
                            );
                            entity.setClientRetractStartAngles(endAngles);
                            entity.setClientRetractStartProgress(1.0f);
                            
                            entity.setClientAnimationStartTime(0);
                            entity.setClientLastTargetPos(null);
                        }
                    } else {
                        if (entity.getLevel() != null) {
                            entity.getLevel().playLocalSound(
                                entity.getBlockPos(),
                                ModSoundEvents.SMART_BLOCK_PLACER_EXTEND.get(),
                                SoundSource.BLOCKS,
                                0.4f,
                                1.3f,
                                false
                            );
                            if (entity.getLevel().random.nextFloat() < 0.6f) {
                                entity.getLevel().playLocalSound(
                                    entity.getBlockPos(),
                                    ModSoundEvents.SMART_BLOCK_PLACER_SHULKER_OPEN.get(),
                                    SoundSource.BLOCKS,
                                    0.4f,
                                    1.5f,
                                    false
                                );
                            }
                        }
                        entity.setClientAnimationStartTime(currentTime);
                        entity.setClientLastTargetPos(targetPos);
                        entity.setRetractSoundPlayed(false);
                        animStartTime = currentTime;
                        animTargetPos = targetPos;
                    }
                }
            }
            
            if (animStartTime == 0 && hasValidWorkItem) {
                BlockPos targetPos = getNextTargetPosition(entity, facing, upsideDown);
                if (targetPos != null && entity.getLevel() != null) {
                    entity.getLevel().playLocalSound(
                        entity.getBlockPos(),
                        ModSoundEvents.SMART_BLOCK_PLACER_EXTEND.get(),
                        SoundSource.BLOCKS,
                        0.4f,
                        1.3f,
                        false
                    );
                    if (entity.getLevel().random.nextFloat() < 0.6f) {
                        entity.getLevel().playLocalSound(
                            entity.getBlockPos(),
                            ModSoundEvents.SMART_BLOCK_PLACER_SHULKER_OPEN.get(),
                            SoundSource.BLOCKS,
                            0.4f,
                            1.5f,
                            false
                        );
                    }
                    entity.setClientAnimationStartTime(currentTime);
                    entity.setClientLastTargetPos(targetPos);
                    entity.setRetractSoundPlayed(false);
                    animStartTime = currentTime;
                    animTargetPos = targetPos;
                }
            }
            
            // 播放动画
            if (animStartTime != 0 && animTargetPos != null) {
                isAnimationPlaying = true;
                long elapsedTicks = currentTime - animStartTime;

                if (elapsedTicks < WORKING_ANIMATION_SCHEME.getAnimationDurationTicks()) {
                    animationProgress = Math.min(
                        1.0f,
                        (elapsedTicks + partialTick) / (float) WORKING_ANIMATION_SCHEME.getAnimationDurationTicks()
                    );
                } else {
                    animationProgress = 1.0f;
                }

                // 进入阶段4（收回阶段）时播放收回音效
                if (!entity.isRetractSoundPlayed() && animationProgress >= 0.7f && entity.getLevel() != null) {
                    entity.getLevel().playLocalSound(
                        entity.getBlockPos(),
                        ModSoundEvents.SMART_BLOCK_PLACER_RETRACT.get(),
                        SoundSource.BLOCKS,
                        0.8f,
                        1.3f,
                        false
                    );
                    entity.setRetractSoundPlayed(true);
                }

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
        poseStack.mulPose((upsideDown ? Axis.YN : Axis.YP).rotationDegrees(baseSwingAngle));
        poseStack.translate(-0.5, 0.0, -0.5);
        renderModel(poseStack, buffer, BASE_MODEL, packedLight, packedOverlay);
        poseStack.popPose();
        
        // 渲染大臂
        poseStack.pushPose();
        poseStack.mulPose((upsideDown ? Axis.YN : Axis.YP).rotationDegrees(baseSwingAngle));
        poseStack.translate(0, 0.625, 0);
        poseStack.mulPose(Axis.XP.rotationDegrees(upperArmAngle));
        poseStack.translate(0, -0.625, 0);
        poseStack.translate(-0.5, 0.0, -0.5);
        renderModel(poseStack, buffer, UPPERARM_MODEL, packedLight, packedOverlay);
        
        // 渲染小臂和钳子
        poseStack.pushPose();
        poseStack.translate(0.6875, 1.0625, 0.9375);
        poseStack.mulPose(Axis.XP.rotationDegrees(forearmAngle));
        poseStack.translate(-0.6875, -1.0625, -0.9375);
        renderModel(poseStack, buffer, FOREARM_MODEL, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.5, 1.3125, 0.375);
        poseStack.mulPose(Axis.XP.rotationDegrees(clawAngle));
        poseStack.translate(-0.5, -1.3125, -0.375);
        
        // 切换钳子模型
        // 全新逻辑：钳子只在动画的伸出阶段（0-70%）打开，收回阶段（70-100%）闭合
        boolean shouldClawBeOpen = isAnimationPlaying && animationProgress > 0f && animationProgress <= 0.7f;

        // 正在播放工作动画且处于伸出阶段（0-70%）：钳子打开

        ModelResourceLocation currentClawModel = shouldClawBeOpen ? CLAW_OPEN_MODEL : CLAW_MODEL;
        renderModel(poseStack, buffer, currentClawModel, packedLight, packedOverlay);
        
        // 渲染钳子中的方块
        if (shouldClawBeOpen && entity.getLevel() != null) {
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
        if (upsideDown && (facing == Direction.NORTH || facing == Direction.SOUTH)) {
            rotation = (rotation + 180f) % 360f;
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
    }
    
    private boolean canBeStacked(net.minecraft.world.level.block.state.BlockState state,
        @Nullable net.minecraft.world.item.BlockItem blockItem) {
        if (state.is(net.minecraft.world.level.block.Blocks.TURTLE_EGG)) {
            if (state.getValue(net.minecraft.world.level.block.TurtleEggBlock.EGGS) < 4) {
                return blockItem == null || state.getBlock() == blockItem.getBlock();
            }
            return false;
        }
        if (state.is(net.minecraft.world.level.block.Blocks.SEA_PICKLE)) {
            if (state.getValue(net.minecraft.world.level.block.SeaPickleBlock.PICKLES) < 4) {
                return blockItem == null || state.getBlock() == blockItem.getBlock();
            }
            return false;
        }
        if (state.getBlock() instanceof net.minecraft.world.level.block.CandleBlock) {
            if (state.getValue(net.minecraft.world.level.block.CandleBlock.CANDLES) < 4) {
                return blockItem == null || state.getBlock() == blockItem.getBlock();
            }
            return false;
        }
        if (state.is(net.minecraft.world.level.block.Blocks.PINK_PETALS)) {
            if (state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FLOWER_AMOUNT) < 4) {
                return blockItem == null || state.getBlock() == blockItem.getBlock();
            }
            return false;
        }
        return false;
    }
    
    /**
     * 获取下一个放置目标位置
     */
    @Nullable
    private BlockPos getNextTargetPosition(SmartBlockPlacerBlockEntity entity, Direction facing, boolean upsideDown) {
        // 蓝图模式：使用结构数据计算目标位置
        var loadedStructure = entity.getLoadedStructure();
        if (loadedStructure != null && !loadedStructure.isEmpty()) {
            // 先旋转结构数据，再计算目标位置
            var rotatedStructure = SmartBlockPlacerBlockEntity.rotateStructureDataStatic(
                loadedStructure);
            if (!rotatedStructure.isEmpty()) {
                return getBlueprintTargetPosition(entity, facing, upsideDown, rotatedStructure);
            }
            return null;
        }
        
        // 普通模式：使用 layerPositions
        BlockPos basePos = entity.getBlockPos().relative(facing.getOpposite(), -4);
        
        Map<Integer, Set<Integer>> layerPositions = entity.getLayerPositions();
        
        List<BlockPos> allPositions = buildOrderedPositionsForRenderer(basePos, facing, layerPositions, upsideDown);
        
        if (allPositions.isEmpty()) {
            return null;
        }
        
        int currentIndex = entity.getCurrentPlacementIndex();
        if (currentIndex >= allPositions.size()) {
            currentIndex = 0;
        }
        
        // 查找第一个空位或可放置位置
        for (int i = 0; i < allPositions.size(); i++) {
            int index = (currentIndex + i) % allPositions.size();
            BlockPos targetPos = allPositions.get(index);
            
            if (entity.getLevel() == null) {
                return null;
            }
            
            net.minecraft.world.level.block.state.BlockState targetState = entity.getLevel().getBlockState(targetPos);
            
            if (targetState.isAir()) {
                return targetPos;
            }
            
            if (!targetState.getFluidState().isEmpty()) {
                return targetPos;
            }
            
            if (!targetState.isAir()) {
                net.minecraft.world.item.ItemStack heldItem = entity.getCurrentHeldBlock();
                if (!heldItem.isEmpty() && heldItem.getItem() instanceof net.minecraft.world.item.BlockItem heldBlockItem) {
                    if (canBeStacked(targetState, heldBlockItem)) {
                        return targetPos;
                    }
                } else if (heldItem.isEmpty()) {
                    if (canBeStacked(targetState, null)) {
                        return targetPos;
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * 获取蓝图模式的目标位置
     */
    @Nullable
    private BlockPos getBlueprintTargetPosition(SmartBlockPlacerBlockEntity entity, Direction facing, boolean upsideDown, 
        dev.dubhe.anvilcraft.util.StructureLoadUtil.StructureData structure) {
        
        // 直接使用 BlockEntity 提供的方法获取当前目标位置
        // 这个方法会正确处理 currentPlacementIndex 到实际索引的转换
        BlockPos currentTarget = entity.getCurrentBlueprintTargetPosition();
        
        if (currentTarget == null) {
            return null;
        }
        
        // 获取当前钳子中的方块类型
        net.minecraft.world.item.ItemStack heldItem = entity.getCurrentHeldBlock();
        net.minecraft.world.level.block.Block heldBlock = null;
        if (!heldItem.isEmpty() && heldItem.getItem() instanceof net.minecraft.world.item.BlockItem heldBlockItem) {
            heldBlock = heldBlockItem.getBlock();
        }
        
        // 如果有 heldBlock，检查当前位置是否匹配
        if (heldBlock != null) {
            if (entity.getLevel() == null) {
                return null;
            }
            
            net.minecraft.world.level.block.state.BlockState targetState = entity.getLevel().getBlockState(currentTarget);
            
            // 检查位置是否可以放置
            boolean canPlace = false;
            if (targetState.isAir()) {
                canPlace = true;
            } else if (!targetState.getFluidState().isEmpty()) {
                canPlace = true;
            } else if (canBeStacked(
                                targetState, heldItem.getItem()
                                                 instanceof net.minecraft.world.item.BlockItem
                                             ? (net.minecraft.world.item.BlockItem) heldItem.getItem() : null)) {
                canPlace = true;
            } else if (canBeStacked(targetState, null)) {
                canPlace = true;
            }
            
            if (canPlace) {
                // 检查这个位置在蓝图中需要的方块是否与 heldBlock 匹配
                int currentIndex = entity.getCurrentPlacementIndex();
                // 使用传入的 structure 参数（已经是旋转后的数据）
                List<Integer> orderedIndices = SmartBlockPlacerBlockEntity.buildOrderedBlueprintIndices(structure, upsideDown);
                
                if (currentIndex < orderedIndices.size()) {
                    int actualIndex = orderedIndices.get(currentIndex);
                    if (actualIndex < structure.blocks.size()) {
                        net.minecraft.world.level.block.Block requiredBlock = structure.blocks.get(actualIndex).state().getBlock();
                        if (requiredBlock == heldBlock) {
                            return currentTarget;
                        }
                    }
                }
            }
            
            // 当前位置不匹配，查找下一个匹配的位置
            List<BlockPos> allPositions = SmartBlockPlacerBlockEntity.buildBlueprintPositions(
                entity.getBlockPos(), facing, upsideDown, structure);
            List<Integer> orderedIndices = SmartBlockPlacerBlockEntity.buildOrderedBlueprintIndices(structure, upsideDown);
            
            int currentOrderIndex = entity.getCurrentPlacementIndex();
            for (int i = 1; i < orderedIndices.size(); i++) {
                int orderIndex = (currentOrderIndex + i) % orderedIndices.size();
                int actualIndex = orderedIndices.get(orderIndex);
                BlockPos targetPos = allPositions.get(actualIndex);
                
                if (entity.getLevel() == null) {
                    return null;
                }
                
                net.minecraft.world.level.block.state.BlockState loopState = entity.getLevel().getBlockState(targetPos);
                
                boolean loopCanPlace = loopState.isAir() || !loopState.getFluidState().isEmpty()
                    || canBeStacked(loopState, heldItem.getItem() instanceof net.minecraft.world.item.BlockItem
                        ? (net.minecraft.world.item.BlockItem) heldItem.getItem() : null)
                    || canBeStacked(loopState, null);
                
                if (!loopCanPlace) {
                    continue;
                }
                
                if (actualIndex < structure.blocks.size()) {
                    net.minecraft.world.level.block.Block requiredBlock = structure.blocks.get(actualIndex).state().getBlock();
                    if (requiredBlock == heldBlock) {
                        return targetPos;
                    }
                }
            }
        } else {
            // 没有 heldBlock，返回当前目标位置（如果可以放置）
            if (entity.getLevel() == null) {
                return null;
            }
            
            net.minecraft.world.level.block.state.BlockState targetState = entity.getLevel().getBlockState(currentTarget);
            
            if (targetState.isAir() || !targetState.getFluidState().isEmpty()) {
                return currentTarget;
            }
            
            // 当前位置不可放置，查找下一个空位
            List<BlockPos> allPositions = SmartBlockPlacerBlockEntity.buildBlueprintPositions(
                entity.getBlockPos(), facing, upsideDown, structure);
            List<Integer> orderedIndices = SmartBlockPlacerBlockEntity.buildOrderedBlueprintIndices(structure, upsideDown);
            
            int currentOrderIndex = entity.getCurrentPlacementIndex();
            for (int i = 1; i < orderedIndices.size(); i++) {
                int orderIndex = (currentOrderIndex + i) % orderedIndices.size();
                int actualIndex = orderedIndices.get(orderIndex);
                BlockPos targetPos = allPositions.get(actualIndex);
                
                if (entity.getLevel() == null) {
                    return null;
                }
                
                net.minecraft.world.level.block.state.BlockState state = entity.getLevel().getBlockState(targetPos);
                
                if (state.isAir() || !state.getFluidState().isEmpty()) {
                    return targetPos;
                }
            }
        }
        
        return null;
    }
    
    /**
     * 构建有序的放置位置列表
     */
    private List<BlockPos> buildOrderedPositionsForRenderer(
        BlockPos basePos, Direction facing, Map<Integer, Set<Integer>> layerPositions, boolean upsideDown) {
        String cacheKey = basePos.toShortString() + "_" + facing.getName() + "_" + upsideDown + "_" + layerPositions.hashCode();
        
        if (this.positionCache.containsKey(cacheKey)) {
            return this.positionCache.get(cacheKey);
        }
        
        List<BlockPos> positions = SmartBlockPlacerBlockEntity.buildOrderedPositions(basePos, facing, layerPositions, upsideDown);
        
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
    
    private void renderHeldBlock(
        PoseStack poseStack, MultiBufferSource buffer, SmartBlockPlacerBlockEntity entity, int packedLight, int packedOverlay) {
        ItemStack stack = entity.getCurrentHeldBlock();
        
        if (stack.isEmpty()) {
            return;
        }
        
        poseStack.pushPose();
        poseStack.translate(0.5, 0.96, 0.1);
        poseStack.mulPose(Axis.XP.rotationDegrees(-40));
        poseStack.scale(0.65f, 0.65f, 0.65f);
        
        net.minecraft.client.renderer.entity.ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        itemRenderer.renderStatic(
            stack,
            net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
            packedLight,
            packedOverlay,
            poseStack,
            buffer,
            entity.getLevel(),
            0
        );
        
        poseStack.popPose();
    }
}
