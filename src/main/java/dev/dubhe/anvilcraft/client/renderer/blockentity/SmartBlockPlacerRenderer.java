package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.block.entity.SmartBlockPlacerBlockEntity;
import dev.dubhe.anvilcraft.block.power.consumer.SmartBlockPlacerBlock;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.SmartBlockPlacerRenderState;
import dev.dubhe.anvilcraft.client.support.FeatureRendererSupport;
import dev.dubhe.anvilcraft.init.ModSoundEvents;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.TurtleEggBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SmartBlockPlacerRenderer implements BlockEntityRenderer<SmartBlockPlacerBlockEntity, SmartBlockPlacerRenderState> {
    // 位置列表缓存
    private final Map<String, List<BlockPos>> positionCache = new HashMap<>();

    public static final StandaloneModelKey<BlockStateModel> BASE_MODEL = new StandaloneModelKey<>(
        () -> "AnvilCraft: Smart Block Placer Base Model"
    );
    public static final StandaloneModelKey<BlockStateModel> UPPERARM_MODEL = new StandaloneModelKey<>(
        () -> "AnvilCraft: Smart Block Placer Upperarm Model"
    );
    public static final StandaloneModelKey<BlockStateModel> FOREARM_MODEL = new StandaloneModelKey<>(
        () -> "AnvilCraft: Smart Block Placer Forearm Model"
    );
    public static final StandaloneModelKey<BlockStateModel> CLAW_MODEL = new StandaloneModelKey<>(
        () -> "AnvilCraft: Smart Block Placer Claw Model"
    );
    public static final StandaloneModelKey<BlockStateModel> CLAW_OPEN_MODEL = new StandaloneModelKey<>(
        () -> "AnvilCraft: Smart Block Placer Claw Open Model"
    );

    private static final WorkingAnimationScheme WORKING_ANIMATION_SCHEME = new WorkingAnimationScheme();

    private final ItemModelResolver itemModelResolver;

    @SuppressWarnings("unused")
    public SmartBlockPlacerRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public SmartBlockPlacerRenderState createRenderState() {
        return new SmartBlockPlacerRenderState();
    }

    /**
     * 工作动画方案（放置方块时）
     */
    private static class WorkingAnimationScheme {
        // 机械臂参数（单位：Minecraft方块）
        private static final float UPPER_ARM_LENGTH = 2.5f;  // 大臂长度
        private static final float FOREARM_LENGTH = 2.5f;    // 小臂长度
        private static final float BASE_HEIGHT = 0.0f;       // 底座关节高度（相对于底座模型）

        /**
         * 计算机械臂角度
         *
         * @return float[]{baseSwingAngle, upperArmAngle, forearmAngle, clawAngle}
         */
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
                cosForearm = Math.clamp(cosForearm, -1.0f, 1.0f);
                float forearmAngleFromUpper = (float) Math.toDegrees(Math.acos(cosForearm));

                float cosUpperArm = (clampedDist * clampedDist + UPPER_ARM_LENGTH * UPPER_ARM_LENGTH - FOREARM_LENGTH * FOREARM_LENGTH)
                    / (2 * clampedDist * UPPER_ARM_LENGTH);
                cosUpperArm = Math.clamp(cosUpperArm, -1.0f, 1.0f);
                float upperArmAngleFromTarget = (float) Math.toDegrees(Math.acos(cosUpperArm));
                upperArmAngle = -(180f - upperArmAngleFromTarget - elevationAngle) * 0.6f + 20f;
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

    }

    @SuppressWarnings(
        {
            "checkstyle:VariableDeclarationUsageDistance",
            "checkstyle:Indentation"
        }
    )
    @Override
    public void extractRenderState(
        SmartBlockPlacerBlockEntity entity,
        SmartBlockPlacerRenderState state,
        float partialTick,
        Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTick, cameraPosition, breakProgress);

        // 从放置速度获取动画总时长，跟随配置项 smartBlockPlacerInterval 动态变化
        state.setAnimationDurationTicks(SmartBlockPlacerBlockEntity.getPlacementInterval());

        // Initialize models
        state.setBaseModel(FeatureRendererSupport.initialize(BASE_MODEL, entity));
        state.setUpperArmModel(FeatureRendererSupport.initialize(UPPERARM_MODEL, entity));
        state.setForearmModel(FeatureRendererSupport.initialize(FOREARM_MODEL, entity));
        state.setClawModel(FeatureRendererSupport.initialize(CLAW_MODEL, entity));
        state.setClawOpenModel(FeatureRendererSupport.initialize(CLAW_OPEN_MODEL, entity));

        BlockState blockState = entity.getBlockState();
        if (!(blockState.getBlock() instanceof SmartBlockPlacerBlock)) return;

        Direction facing = blockState.getValue(HorizontalDirectionalBlock.FACING);
        boolean upsideDown = blockState.getValue(SmartBlockPlacerBlock.UPSIDE_DOWN);
        state.setFacing(facing);
        state.setUpsideDown(upsideDown);

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
        Level retractLevel = entity.getLevel();
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
                float interruptProgress = Math.min(1.0f, (elapsedTicks + partialTick) / (float) state
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

        Level retractAnimLevel = entity.getLevel();
        if (entity.isClientIsRetracting() && retractAnimLevel != null) {
            long currentTime = retractAnimLevel.getGameTime();
            long elapsedRetractTicks = currentTime - entity.getClientRetractStartTime();

            float startProgress = entity.getClientRetractStartProgress();
            float remainingProgress = 1.0f - startProgress;
            float retractDuration = state.getAnimationDurationTicks() * remainingProgress;

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
                boolean animationCompleted = elapsedTicks >= state.getAnimationDurationTicks() + 5;

                if (animationCompleted) {
                    BlockPos targetPos = this.getNextTargetPosition(entity, facing, upsideDown);
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
                            if (entity.getLevel().getRandom().nextFloat() < 0.6f) {
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
                BlockPos targetPos = this.getNextTargetPosition(entity, facing, upsideDown);
                if (targetPos != null && entity.getLevel() != null) {
                    entity.getLevel().playLocalSound(
                        entity.getBlockPos(),
                        ModSoundEvents.SMART_BLOCK_PLACER_EXTEND.get(),
                        SoundSource.BLOCKS,
                        0.4f,
                        1.3f,
                        false
                    );
                    if (entity.getLevel().getRandom().nextFloat() < 0.6f) {
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

                if (elapsedTicks < state.getAnimationDurationTicks()) {
                    animationProgress = Math.min(
                        1.0f,
                        (elapsedTicks + partialTick) / (float) state.getAnimationDurationTicks()
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

        // 全新逻辑：钳子只在动画的伸出阶段（0-70%）打开，收回阶段（70-100%）闭合
        boolean shouldClawBeOpen = isAnimationPlaying && animationProgress > 0f && animationProgress <= 0.7f;
        state.setClawOpen(shouldClawBeOpen);

        // Store angles in render state
        state.setBaseSwingAngle(baseSwingAngle);
        state.setUpperArmAngle(upperArmAngle);
        state.setForearmAngle(forearmAngle);
        state.setClawAngle(clawAngle);

        // Handle held item
        if (shouldClawBeOpen && entity.getLevel() != null) {
            ItemStack stack = entity.getCurrentHeldBlock();
            if (!stack.isEmpty()) {
                state.setHeldItem(FeatureRendererSupport.initialize(stack, this.itemModelResolver));
                state.setHasHeldItem(true);
            } else {
                state.setHasHeldItem(false);
            }
        } else {
            state.setHasHeldItem(false);
        }
    }

    @Override
    public void submit(
        SmartBlockPlacerRenderState state,
        PoseStack poseStack,
        SubmitNodeCollector collector,
        CameraRenderState camera
    ) {
        boolean upsideDown = state.isUpsideDown();
        final float baseSwingAngle = state.getBaseSwingAngle();
        final float upperArmAngle = state.getUpperArmAngle();
        final float forearmAngle = state.getForearmAngle();
        final float clawAngle = state.getClawAngle();
        final boolean clawOpen = state.isClawOpen();

        final int light = state.lightCoords;
        final int overlay = OverlayTexture.NO_OVERLAY;

        // 应用变换
        poseStack.pushPose();
        poseStack.translate(0.5, 1.5, 0.5);
        if (upsideDown) {
            poseStack.mulPose(Axis.XP.rotationDegrees(180f));
        }
        Direction facing = state.getFacing();
        this.applyHorizontalRotation(poseStack, facing, upsideDown);
        poseStack.translate(0, upsideDown ? 0.5 : -1.5, 0);

        // 渲染底座
        poseStack.pushPose();
        poseStack.mulPose((upsideDown ? Axis.YN : Axis.YP).rotationDegrees(baseSwingAngle));
        poseStack.translate(-0.5, 0.0, -0.5);
        state.getBaseModel().submit(poseStack, collector, light, overlay, 0);
        poseStack.popPose();

        // 渲染大臂
        poseStack.pushPose();
        poseStack.mulPose((upsideDown ? Axis.YN : Axis.YP).rotationDegrees(baseSwingAngle));
        poseStack.translate(0, 0.625, 0);
        poseStack.mulPose(Axis.XP.rotationDegrees(upperArmAngle));
        poseStack.translate(0, -0.625, 0);
        poseStack.translate(-0.5, 0.0, -0.5);
        state.getUpperArmModel().submit(poseStack, collector, light, overlay, 0);

        // 渲染小臂和钳子
        poseStack.pushPose();
        poseStack.translate(0.6875, 1.0625, 0.9375);
        poseStack.mulPose(Axis.XP.rotationDegrees(forearmAngle));
        poseStack.translate(-0.6875, -1.0625, -0.9375);
        state.getForearmModel().submit(poseStack, collector, light, overlay, 0);
        poseStack.pushPose();
        poseStack.translate(0.5, 1.3125, 0.375);
        poseStack.mulPose(Axis.XP.rotationDegrees(clawAngle));
        poseStack.translate(-0.5, -1.3125, -0.375);

        // 切换钳子模型
        if (clawOpen) {
            state.getClawOpenModel().submit(poseStack, collector, light, overlay, 0);
        } else {
            state.getClawModel().submit(poseStack, collector, light, overlay, 0);
        }

        // 渲染钳子中的方块
        if (clawOpen && state.isHasHeldItem()) {
            poseStack.pushPose();
            poseStack.translate(0.50, 0.94, 0.19);
            poseStack.mulPose(Axis.XP.rotationDegrees(-40));
            poseStack.scale(0.9f, 0.9f, 0.9f);
            state.getHeldItem().item.submit(poseStack, collector, light, overlay, 0);
            poseStack.popPose();
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

    private boolean canBeStacked(
        BlockState state,
        @Nullable BlockItem blockItem
    ) {
        if (state.is(Blocks.TURTLE_EGG)) {
            if (state.getValue(TurtleEggBlock.EGGS) < 4) {
                return blockItem == null || state.getBlock() == blockItem.getBlock();
            }
            return false;
        }
        if (state.is(Blocks.SEA_PICKLE)) {
            if (state.getValue(SeaPickleBlock.PICKLES) < 4) {
                return blockItem == null || state.getBlock() == blockItem.getBlock();
            }
            return false;
        }
        if (state.getBlock() instanceof CandleBlock) {
            if (state.getValue(CandleBlock.CANDLES) < 4) {
                return blockItem == null || state.getBlock() == blockItem.getBlock();
            }
            return false;
        }
        if (state.is(Blocks.PINK_PETALS)) {
            if (state.getValue(BlockStateProperties.FLOWER_AMOUNT) < 4) {
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
            return entity.getCurrentTargetPos();
        }

        // 普通模式：使用 layerPositions
        BlockPos basePos = entity.getBlockPos().relative(facing.getOpposite(), -4);

        Map<Integer, Set<Integer>> layerPositions = entity.getLayerPositions();

        List<BlockPos> allPositions = this.buildOrderedPositionsForRenderer(basePos, facing, layerPositions, upsideDown);

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

            BlockState targetState = entity.getLevel().getBlockState(targetPos);

            if (targetState.isAir()) {
                return targetPos;
            }

            if (!targetState.getFluidState().isEmpty()) {
                return targetPos;
            }

            if (!targetState.isAir()) {
                ItemStack heldItem = entity.getCurrentHeldBlock();
                if (!heldItem.isEmpty() && heldItem.getItem() instanceof BlockItem heldBlockItem) {
                    if (this.canBeStacked(targetState, heldBlockItem)) {
                        return targetPos;
                    }
                } else if (heldItem.isEmpty()) {
                    if (this.canBeStacked(targetState, null)) {
                        return targetPos;
                    }
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
}
