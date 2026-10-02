package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Either;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.block.entity.SmartBlockPlacerBlockEntity;
import dev.dubhe.anvilcraft.block.power.consumer.SmartBlockPlacerBlock;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.SmartBlockPlacerRenderState;
import dev.dubhe.anvilcraft.client.selection.ModelSelectionRenderer;
import dev.dubhe.anvilcraft.client.selection.SelectionModel;
import dev.dubhe.anvilcraft.client.support.FeatureRendererSupport;
import dev.dubhe.anvilcraft.init.ModSoundEvents;
import dev.dubhe.anvilcraft.mixin.client.ItemLayerTransformInvoker;
import dev.dubhe.anvilcraft.mixin.client.ItemStackRenderStateAccessor;
import dev.dubhe.anvilcraft.util.BlockPlacementUtil;
import dev.dubhe.anvilcraft.util.BlockStateAndEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.TurtleEggBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class SmartBlockPlacerRenderer implements BlockEntityRenderer<SmartBlockPlacerBlockEntity, SmartBlockPlacerRenderState>,
    ModelSelectionRenderer<SmartBlockPlacerBlockEntity> {
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

    private static final PlacementAnimation PLACEMENT_ANIMATION = new PlacementAnimation();
    private static final ItemDisplayContext HELD_ITEM_CONTEXT = ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
    private final ItemModelResolver itemModelResolver;

    public SmartBlockPlacerRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public SmartBlockPlacerRenderState createRenderState() {
        return new SmartBlockPlacerRenderState();
    }

    private static class PlacementAnimation {
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

    public record ArmRenderState(
        float baseSwingAngle,
        float upperArmAngle,
        float forearmAngle,
        float clawAngle,
        float animationProgress,
        boolean animationPlaying
    ) {
    }

    @Override
    public void extractRenderState(
        SmartBlockPlacerBlockEntity entity, SmartBlockPlacerRenderState state, float partialTick,
        Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTick, cameraPosition, breakProgress);
        Direction facing = entity.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        boolean upsideDown = entity.getBlockState().getValue(SmartBlockPlacerBlock.UPSIDE_DOWN);
        Level level = entity.getLevel();
        // 初始化动画变量
        float baseSwingAngle = 0f;
        float upperArmAngle = 0f;
        float forearmAngle = 0f;
        float clawAngle = 0f;
        float animationProgress = 0f;
        boolean isAnimationPlaying = false;

        boolean animationActive = entity.isAnimationActive();
        BlockPos animationTargetPos = entity.getClientAnimationTargetPos();
        if (!animationActive || level == null) {
            entity.setClientAnimationTargetPos(null);
            entity.setClientRetractSoundPlayed(false);
        } else {
            if (animationTargetPos == null) {
                animationTargetPos = this.getNextTargetPosition(entity, level, facing, upsideDown);
                if (animationTargetPos != null) {
                    level.playLocalSound(
                        entity.getBlockPos(),
                        ModSoundEvents.SMART_BLOCK_PLACER_EXTEND.get(),
                        SoundSource.BLOCKS,
                        0.6f,
                        1.3f,
                        false
                    );
                    if (level.getRandom().nextFloat() < 0.6f) {
                        level.playLocalSound(
                            entity.getBlockPos(),
                            ModSoundEvents.SMART_BLOCK_PLACER_SHULKER_OPEN.get(),
                            SoundSource.BLOCKS,
                            0.6f,
                            1.5f,
                            false
                        );
                    }
                    entity.setClientAnimationTargetPos(animationTargetPos);
                }
            }
            if (animationTargetPos != null) {
                isAnimationPlaying = true;
                animationProgress = entity.getAnimationProgress(partialTick);
                if (!entity.isClientRetractSoundPlayed() && animationProgress >= 0.7f) {
                    level.playLocalSound(
                        entity.getBlockPos(),
                        ModSoundEvents.SMART_BLOCK_PLACER_RETRACT.get(),
                        SoundSource.BLOCKS,
                        0.6f,
                        1.3f,
                        false
                    );
                    entity.setClientRetractSoundPlayed(true);
                }
                float[] angles = PLACEMENT_ANIMATION.calculateArmAngles(
                    animationTargetPos, entity.getBlockPos(), facing, upsideDown, animationProgress
                );
                baseSwingAngle = angles[0];
                upperArmAngle = angles[1];
                forearmAngle = angles[2];
                clawAngle = angles[3];
            }
        }

        state.arm = new ArmRenderState(baseSwingAngle, upperArmAngle, forearmAngle, clawAngle, animationProgress, isAnimationPlaying);
        state.facing = facing;
        state.upsideDown = upsideDown;
        state.models.clear();
        for (StandaloneModelKey<BlockStateModel> key : List.of(BASE_MODEL, UPPERARM_MODEL, FOREARM_MODEL, CLAW_MODEL, CLAW_OPEN_MODEL)) {
            state.models.put(SelectionModel.standalone(key), FeatureRendererSupport.initialize(key, entity));
        }
        this.extractHeldContent(entity.getCurrentHeldBlock(), state, level, partialTick, cameraPosition);
    }

    @Override
    public void submit(SmartBlockPlacerRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        this.applyBaseTransform(pose, state.facing, state.upsideDown);
        this.visitArmModels(pose, state.upsideDown, state.arm,
            (key, modelPose) -> state.models.get(key).submitModel(
                ModRenderTypes.CUTOUT_BLOCK, modelPose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0),
            () -> this.submitHeldContent(state, pose, collector, camera));
        pose.popPose();
    }

    private void visitArmModels(
        PoseStack poseStack, boolean upsideDown, ArmRenderState state, ModelConsumer consumer, Runnable heldContent
    ) {
        poseStack.pushPose();
        poseStack.mulPose((upsideDown ? Axis.YN : Axis.YP).rotationDegrees(state.baseSwingAngle()));
        poseStack.translate(-0.5, 0.0, -0.5);
        consumer.accept(BASE_MODEL, poseStack);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.mulPose((upsideDown ? Axis.YN : Axis.YP).rotationDegrees(state.baseSwingAngle()));
        poseStack.translate(0, 0.625, 0);
        poseStack.mulPose(Axis.XP.rotationDegrees(state.upperArmAngle()));
        poseStack.translate(0, -0.625, 0);
        poseStack.translate(-0.5, 0.0, -0.5);
        consumer.accept(UPPERARM_MODEL, poseStack);

        poseStack.pushPose();
        poseStack.translate(0.6875, 1.0625, 0.9375);
        poseStack.mulPose(Axis.XP.rotationDegrees(state.forearmAngle()));
        poseStack.translate(-0.6875, -1.0625, -0.9375);
        consumer.accept(FOREARM_MODEL, poseStack);

        poseStack.pushPose();
        poseStack.translate(0.5, 1.3125, 0.375);
        poseStack.mulPose(Axis.XP.rotationDegrees(state.clawAngle()));
        poseStack.translate(-0.5, -1.3125, -0.375);

        boolean shouldClawBeOpen = state.animationPlaying()
            && state.animationProgress() > 0f
            && state.animationProgress() <= 0.7f;
        StandaloneModelKey<BlockStateModel> currentClawModel = shouldClawBeOpen ? CLAW_OPEN_MODEL : CLAW_MODEL;
        consumer.accept(currentClawModel, poseStack);

        if (shouldClawBeOpen) heldContent.run();

        poseStack.popPose();
        poseStack.popPose();
        poseStack.popPose();
    }

    @Override
    public void collectSelectionModels(
        SmartBlockPlacerBlockEntity entity, float partialTick, PoseStack pose, ModelConsumer consumer
    ) {
        BlockState state = entity.getBlockState();
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        boolean upsideDown = state.getValue(SmartBlockPlacerBlock.UPSIDE_DOWN);
        Level level = entity.getLevel();
        BlockPos target = entity.isAnimationActive() ? entity.getClientAnimationTargetPos() : null;
        if (entity.isAnimationActive() && target == null && level != null) {
            target = this.getNextTargetPosition(entity, level, facing, upsideDown);
        }
        float progress = target == null ? 0 : entity.getAnimationProgress(partialTick);
        float[] angles = target == null ? new float[4] : PLACEMENT_ANIMATION.calculateArmAngles(
            target, entity.getBlockPos(), facing, upsideDown, progress
        );
        ArmRenderState arm = new ArmRenderState(angles[0], angles[1], angles[2], angles[3], progress, target != null);
        pose.pushPose();
        this.applyBaseTransform(pose, facing, upsideDown);
        this.visitArmModels(pose, upsideDown, arm, consumer, () -> {});
        pose.popPose();
    }

    private void applyBaseTransform(PoseStack poseStack, Direction facing, boolean upsideDown) {
        poseStack.translate(0.5, 1.5, 0.5);
        if (upsideDown) poseStack.mulPose(Axis.XP.rotationDegrees(180f));
        this.applyHorizontalRotation(poseStack, facing, upsideDown);
        poseStack.translate(0, upsideDown ? 0.5 : -1.5, 0);
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

    private boolean canBeStacked(BlockState state, @Nullable Block heldBlock) {
        if (state.is(Blocks.TURTLE_EGG)) {
            if (state.getValue(TurtleEggBlock.EGGS) < 4) {
                return heldBlock == null || state.is(heldBlock);
            }
            return false;
        }
        if (state.is(Blocks.SEA_PICKLE)) {
            if (state.getValue(SeaPickleBlock.PICKLES) < 4) {
                return heldBlock == null || state.is(heldBlock);
            }
            return false;
        }
        if (state.getBlock() instanceof CandleBlock) {
            if (state.getValue(CandleBlock.CANDLES) < 4) {
                return heldBlock == null || state.is(heldBlock);
            }
            return false;
        }
        if (state.is(Blocks.PINK_PETALS)) {
            if (state.getValue(BlockStateProperties.FLOWER_AMOUNT) < 4) {
                return heldBlock == null || state.is(heldBlock);
            }
            return false;
        }
        return false;
    }

    /**
     * 获取下一个放置目标位置
     */
    @Nullable
    private BlockPos getNextTargetPosition(
        SmartBlockPlacerBlockEntity entity, Level level, Direction facing, boolean upsideDown
    ) {
        if (entity.hasBlueprint()) {
            return entity.getCurrentBlueprintTargetPosition();
        }

        // 普通模式：使用 layerPositions
        BlockPos basePos = entity.getBlockPos().relative(facing.getOpposite(), -4);

        boolean[] layerPositions = entity.getLayerPositions();

        List<BlockPos> allPositions = SmartBlockPlacerBlockEntity.buildOrderedPositions(
            basePos,
            facing,
            layerPositions,
            upsideDown
        );

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
            if (!BlockPlacementUtil.isTargetUnobstructed(level, targetPos)) {
                continue;
            }
            BlockState targetState = level.getBlockState(targetPos);

            if (targetState.isAir()) {
                return targetPos;
            }

            if (!targetState.getFluidState().isEmpty()) {
                return targetPos;
            }

            if (!targetState.isAir()) {
                Block heldBlock = getDisplayedBlock(entity.getCurrentHeldBlock());
                if (this.canBeStacked(targetState, heldBlock)) {
                    return targetPos;
                }
            }
        }

        return null;
    }

    private void extractHeldContent(
        @Nullable Either<ItemStack, BlockStateAndEntity> held, SmartBlockPlacerRenderState state,
        @Nullable Level level, float partialTick, Vec3 cameraPosition
    ) {
        state.item.clear();
        state.specialItem.clear();
        state.blockModel = null;
        state.blockEntityState = null;
        state.blockEntityRenderer = null;
        state.itemTransform.identity();
        if (held == null) return;
        held.ifLeft(stack -> this.itemModelResolver.updateForTopItem(state.item, stack, HELD_ITEM_CONTEXT, level, null, 0));
        held.ifRight(block -> {
            ItemStack item = new ItemStack(block.state().getBlock());
            ItemStackRenderState display = new ItemStackRenderState();
            this.itemModelResolver.updateForTopItem(display, item, HELD_ITEM_CONTEXT, level, null, 0);
            PoseStack transform = new PoseStack();
            if (!display.isEmpty()) {
                var layer = ((ItemStackRenderStateAccessor) display).anvilcraft$getLayers()[0];
                ((ItemLayerTransformInvoker) layer).anvilcraft$applyTransform(transform.last());
            } else {
                transform.translate(-0.5, -0.5, -0.5);
            }
            state.itemTransform.set(transform.last().pose());
            if (block.state().getRenderShape() != RenderShape.MODEL) {
                this.itemModelResolver.updateForTopItem(state.specialItem, item, ItemDisplayContext.NONE, level, null, 0);
                return;
            }
            Minecraft client = Minecraft.getInstance();
            BlockModelRenderState model = new BlockModelRenderState();
            boolean translucent = block.state().getBlock() instanceof HalfTransparentBlock
                || block.state().getBlock() instanceof StainedGlassPaneBlock;
            client.getModelManager().getBlockStateModelSet().get(block.state()).collectParts(
                RandomSource.create(42), model.setupModel(new Matrix4f(), translucent));
            var tint = client.getBlockColors().getTintSource(block.state(), 0);
            model.tintLayers().add(tint == null ? -1 : tint.color(block.state()));
            state.blockModel = model;
            BlockEntity entity = block.be();
            if (entity != null) {
                if (!entity.hasLevel() && level != null) entity.setLevel(level);
                this.extractHeldBlockEntity(entity, state, partialTick, cameraPosition);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void extractHeldBlockEntity(BlockEntity entity, SmartBlockPlacerRenderState state, float partialTick, Vec3 cameraPosition) {
        var renderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(entity);
        if (renderer == null) return;
        state.blockEntityRenderer = (BlockEntityRenderer<BlockEntity, BlockEntityRenderState>) renderer;
        state.blockEntityState = state.blockEntityRenderer.createRenderState();
        state.blockEntityRenderer.extractRenderState(entity, state.blockEntityState, partialTick, cameraPosition, null);
    }

    private void submitHeldContent(
        SmartBlockPlacerRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera
    ) {
        pose.pushPose();
        pose.translate(0.425 + 11F / 2048, 1.0625, 0.1125);
        pose.mulPose(Axis.XN.rotationDegrees(130));
        pose.mulPose(Axis.YN.rotationDegrees(10.625F));
        pose.mulPose(Axis.ZN.rotationDegrees(44.25F));
        pose.scale(0.65F, 0.65F, 0.65F);
        state.item.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        pose.mulPose(state.itemTransform);
        if (state.blockModel != null) state.blockModel.submitMultiLayer(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        if (state.blockEntityRenderer != null && state.blockEntityState != null) {
            state.blockEntityRenderer.submit(state.blockEntityState, pose, collector, camera);
        }
        if (!state.specialItem.isEmpty()) {
            pose.translate(0.5, 0.5, 0.5);
            state.specialItem.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        }
        pose.popPose();
    }

    private static @Nullable Block getDisplayedBlock(@Nullable Either<ItemStack, BlockStateAndEntity> displayedBlock) {
        if (displayedBlock == null) {
            return null;
        }
        return displayedBlock.map(
            stack -> stack.getItem() instanceof BlockItem blockItem ? blockItem.getBlock() : null,
            held -> held.state().getBlock()
        );
    }

    @Override
    public AABB getRenderBoundingBox(SmartBlockPlacerBlockEntity be) {
        BlockPos pos = be.getBlockPos();
        return new AABB(pos.getX() - 1, pos.getY(), pos.getZ() - 1, pos.getX() + 2, pos.getY() + 2, pos.getZ() + 2);
    }
}
