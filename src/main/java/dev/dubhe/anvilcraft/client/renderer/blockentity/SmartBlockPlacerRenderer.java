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
import net.minecraft.util.Mth;
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

    private record ArmAngles(float baseSwing, float upperArm, float forearm, float claw) {
    }

    /**
     * 放置动画：底座转向目标，机械臂按模型实际臂长做二连杆逆解；目标超出臂展时完全伸展指向目标，爪部始终对准目标方块。
     */
    private static class PlacementAnimation {
        // 关节偏移（单位：方块），与 visitArmModels 中的平移一致，z 增大表示向后
        private static final float ELBOW_HEIGHT = 0.625f;
        private static final float FOREARM_JOINT_Y = 1.0625f;
        private static final float FOREARM_JOINT_Z = 0.4375f;
        private static final float CLAW_JOINT_Y = 0.25f;
        private static final float CLAW_JOINT_Z = -0.5625f;
        private static final float GRIP_Y = -0.25f;
        private static final float GRIP_Z = -0.2625f;

        // 各段在动画角为 0 时的仰角（水平朝前为 0，抬头为正），由关节偏移换算
        private static final float UPPER_ARM_REST_ELEVATION = elevationOf(FOREARM_JOINT_Y, FOREARM_JOINT_Z);
        private static final float FOREARM_REST_ELEVATION = elevationOf(CLAW_JOINT_Y, CLAW_JOINT_Z);
        private static final float CLAW_REST_ELEVATION = elevationOf(GRIP_Y, GRIP_Z);
        private static final float FOREARM_JOINT_LENGTH = (float) Math.hypot(FOREARM_JOINT_Y, FOREARM_JOINT_Z);
        private static final float CLAW_JOINT_LENGTH = (float) Math.hypot(CLAW_JOINT_Y, CLAW_JOINT_Z);

        // 实际臂长（单位：方块）：肘 → 前臂关节、前臂关节 → 抓取点
        private static final float UPPER_ARM_LENGTH = FOREARM_JOINT_LENGTH;
        private static final float FOREARM_LENGTH = CLAW_JOINT_LENGTH + (float) Math.hypot(GRIP_Y, GRIP_Z);

        // 动画时间轴，与 ExecutionPhase 的 PREPARE / EXTEND / RESET 对齐
        private static final float SWING_END = 0.2f;
        private static final float HOLD_END = 0.3f;
        static final float EXTEND_END = 0.7f;

        /**
         * 计算动画进度对应的机械臂角度
         */
        ArmAngles calculateArmAngles(
            BlockPos targetPos,
            BlockPos placerPos,
            Direction facing,
            boolean upsideDown,
            float animationProgress
        ) {
            ArmAngles target = this.calculateTargetAngles(targetPos, placerPos, facing, upsideDown);

            if (animationProgress <= SWING_END) {
                // 阶段1：底座转向，同时把肘部预摆到目标仰角
                float progress = animationProgress / SWING_END;
                return new ArmAngles(
                    target.baseSwing() * progress,
                    0f,
                    (target.upperArm() + target.forearm()) * progress,
                    target.claw() * progress
                );
            }
            if (animationProgress <= HOLD_END) {
                // 阶段2：停顿
                return new ArmAngles(target.baseSwing(), 0f, target.upperArm() + target.forearm(), target.claw());
            }
            if (animationProgress <= EXTEND_END) {
                // 阶段3：大臂推出，小臂保持目标仰角
                float progress = (animationProgress - HOLD_END) / (EXTEND_END - HOLD_END);
                float startForearm = target.upperArm() + target.forearm();
                return new ArmAngles(
                    target.baseSwing(),
                    target.upperArm() * progress,
                    startForearm + (target.forearm() - startForearm) * progress,
                    target.claw()
                );
            }
            // 阶段4：收回
            float progress = (animationProgress - EXTEND_END) / (1f - EXTEND_END);
            return new ArmAngles(
                target.baseSwing() * (1f - progress),
                target.upperArm() * (1f - progress),
                target.forearm() * (1f - progress),
                target.claw() * (1f - progress)
            );
        }

        private ArmAngles calculateTargetAngles(
            BlockPos targetPos,
            BlockPos placerPos,
            Direction facing,
            boolean upsideDown
        ) {
            // 底座关节位于方块底面中心，倒置时位于顶面中心
            Vec3 joint = Vec3.atBottomCenterOf(placerPos).add(0, upsideDown ? 1 : 0, 0);
            Vec3 delta = Vec3.atCenterOf(targetPos).subtract(joint);

            Direction right = facing.getCounterClockWise();
            double forwardDist = delta.x * facing.getStepX() + delta.z * facing.getStepZ();
            double lateralDist = delta.x * right.getStepX() + delta.z * right.getStepZ();
            // 倒置时底座绕 X 轴翻转，局部 X 轴方向相反
            if (upsideDown) lateralDist = -lateralDist;
            double verticalDist = upsideDown ? -delta.y : delta.y;

            float baseSwing = (float) Math.toDegrees(Math.atan2(lateralDist, forwardDist));
            float horizontalDist = (float) Math.hypot(lateralDist, forwardDist);

            // 逆解以肘关节为原点；距离超出臂展时 reach 被夹住，肘部自然完全伸展
            double elbowHeight = verticalDist - ELBOW_HEIGHT;
            float elevation = (float) Math.toDegrees(Math.atan2(elbowHeight, horizontalDist));
            float distance = (float) Math.hypot(horizontalDist, elbowHeight);
            float reach = Math.clamp(distance, 0.01f, UPPER_ARM_LENGTH + FOREARM_LENGTH);
            float elbowBend = 180f - acosDegrees(
                (UPPER_ARM_LENGTH * UPPER_ARM_LENGTH + FOREARM_LENGTH * FOREARM_LENGTH - reach * reach)
                    / (2f * UPPER_ARM_LENGTH * FOREARM_LENGTH)
            );
            float shoulderElevation = elevation + acosDegrees(
                (reach * reach + UPPER_ARM_LENGTH * UPPER_ARM_LENGTH - FOREARM_LENGTH * FOREARM_LENGTH)
                    / (2f * reach * UPPER_ARM_LENGTH)
            );
            float forearmElevation = shoulderElevation - elbowBend;

            float upperArm = shoulderElevation - UPPER_ARM_REST_ELEVATION;
            float forearm = forearmElevation - FOREARM_REST_ELEVATION - upperArm;

            // 爪部由自身关节指向目标
            double upperArmRad = Math.toRadians(UPPER_ARM_REST_ELEVATION + upperArm);
            double forearmRad = Math.toRadians(FOREARM_REST_ELEVATION + upperArm + forearm);
            double clawJointHeight = ELBOW_HEIGHT
                + FOREARM_JOINT_LENGTH * Math.sin(upperArmRad)
                + CLAW_JOINT_LENGTH * Math.sin(forearmRad);
            double clawJointDepth = -FOREARM_JOINT_LENGTH * Math.cos(upperArmRad)
                - CLAW_JOINT_LENGTH * Math.cos(forearmRad);
            float aimElevation = (float) Math.toDegrees(
                Math.atan2(verticalDist - clawJointHeight, horizontalDist + clawJointDepth)
            );

            float claw = aimElevation - CLAW_REST_ELEVATION - upperArm - forearm;
            return new ArmAngles(baseSwing, upperArm, forearm, claw);
        }

        private static float acosDegrees(float cosine) {
            return (float) Math.toDegrees(Math.acos(Math.clamp(cosine, -1f, 1f)));
        }

        /**
         * 由关节偏移求该段在动画角为 0 时的仰角
         */
        private static float elevationOf(float offsetY, float offsetZ) {
            return (float) Math.toDegrees(Math.atan2(offsetY, -offsetZ));
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
                ArmAngles angles = PLACEMENT_ANIMATION.calculateArmAngles(
                    animationTargetPos, entity.getBlockPos(), facing, upsideDown, animationProgress
                );
                baseSwingAngle = angles.baseSwing();
                upperArmAngle = angles.upperArm();
                forearmAngle = angles.forearm();
                clawAngle = angles.claw();
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
        this.visitArmModels(pose, state.arm,
            (key, modelPose) -> state.models.get(key).submitModel(
                ModRenderTypes.CUTOUT_BLOCK, modelPose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0),
            () -> this.submitHeldContent(state, pose, collector, camera));
        pose.popPose();
    }

    public void collectDancingModels(double renderTick, PoseStack pose, ModelConsumer consumer) {
        float lowerArmAngle = Mth.lerp((Mth.sin((float) (renderTick / 4 % (2 * Math.PI))) + 1) / 2, -45, 15);
        float upperArmAngle = Mth.lerp((Mth.sin((float) (renderTick / 8 % (2 * Math.PI))) + 1) / 4, -45, 95);
        // Create's dancing angles, adjusted for the baked 22.5-degree forearm and claw rest poses.
        ArmRenderState state = new ArmRenderState(
            (float) (renderTick * 10 % 360), lowerArmAngle, upperArmAngle + 22.5f, -lowerArmAngle + 22.5f, 0, false
        );
        pose.pushPose();
        this.applyBaseTransform(pose, Direction.NORTH, false);
        this.visitArmModels(pose, state, consumer, () -> {});
        pose.popPose();
    }

    private void visitArmModels(
        PoseStack poseStack, ArmRenderState state, ModelConsumer consumer, Runnable heldContent
    ) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(state.baseSwingAngle()));
        poseStack.translate(-0.5, 0.0, -0.5);
        consumer.accept(BASE_MODEL, poseStack);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(state.baseSwingAngle()));
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
            && state.animationProgress() <= PlacementAnimation.EXTEND_END;
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
        ArmAngles angles = target == null ? new ArmAngles(0f, 0f, 0f, 0f) : PLACEMENT_ANIMATION.calculateArmAngles(
            target, entity.getBlockPos(), facing, upsideDown, progress
        );
        ArmRenderState arm = new ArmRenderState(
            angles.baseSwing(), angles.upperArm(), angles.forearm(), angles.claw(), progress, target != null
        );
        pose.pushPose();
        this.applyBaseTransform(pose, facing, upsideDown);
        this.visitArmModels(pose, arm, consumer, () -> {});
        pose.popPose();
    }

    private void applyBaseTransform(PoseStack poseStack, Direction facing, boolean upsideDown) {
        poseStack.translate(0.5, 1.5, 0.5);
        if (upsideDown) poseStack.mulPose(Axis.XP.rotationDegrees(180f));
        this.applyHorizontalRotation(poseStack, facing, upsideDown);
        poseStack.translate(0, upsideDown ? 0.5 : -1.5, 0);
    }

    private void applyHorizontalRotation(PoseStack poseStack, Direction facing, boolean upsideDown) {
        // 局部 -Z 为机械臂正前方；倒置时底座绕 X 轴翻转会让 Z 轴反向，旋转角随之取补角
        float rotation = (float) Math.toDegrees(
            Math.atan2(-facing.getStepX(), upsideDown ? facing.getStepZ() : -facing.getStepZ())
        );
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
