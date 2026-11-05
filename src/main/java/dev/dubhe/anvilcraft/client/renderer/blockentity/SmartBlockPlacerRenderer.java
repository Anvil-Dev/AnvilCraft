package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Either;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.SmartBlockPlacerBlock;
import dev.dubhe.anvilcraft.block.entity.SmartBlockPlacerBlockEntity;
import dev.dubhe.anvilcraft.client.selection.ModelSelectionRenderer;
import dev.dubhe.anvilcraft.init.ModSoundEvents;
import dev.dubhe.anvilcraft.util.AabbUtil;
import dev.dubhe.anvilcraft.util.BlockPlacementUtil;
import dev.dubhe.anvilcraft.util.BlockStateAndEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
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
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.TurtleEggBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Vector3f;

import java.util.List;
import javax.annotation.Nullable;

public class SmartBlockPlacerRenderer implements BlockEntityRenderer<SmartBlockPlacerBlockEntity>,
    ModelSelectionRenderer<SmartBlockPlacerBlockEntity> {
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

    private static final PlacementAnimation PLACEMENT_ANIMATION = new PlacementAnimation();
    private static final ItemDisplayContext HELD_ITEM_CONTEXT = ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;

    private final BlockRenderDispatcher blockRenderer;
    private final BlockEntityRenderDispatcher blockEntityRenderer;
    private final ItemRenderer itemRenderer;
    private final ModelManager modelManager;
    private final BlockColors blockColors;
    private final RandomSource random = RandomSource.create();

    public SmartBlockPlacerRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
        this.blockEntityRenderer = context.getBlockEntityRenderDispatcher();
        this.itemRenderer = context.getItemRenderer();
        this.modelManager = Minecraft.getInstance().getModelManager();
        this.blockColors = Minecraft.getInstance().getBlockColors();
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

    private record ArmRenderState(
        float baseSwingAngle,
        float upperArmAngle,
        float forearmAngle,
        float clawAngle,
        float animationProgress,
        boolean animationPlaying
    ) {
    }

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
        
        final Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        final boolean upsideDown = state.getValue(SmartBlockPlacerBlock.UPSIDE_DOWN);
        final @Nullable Level level = entity.getLevel();
        
        // 应用变换
        poseStack.pushPose();
        this.applyBaseTransform(poseStack, facing, upsideDown);

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
        
        ArmRenderState armState = new ArmRenderState(
            baseSwingAngle,
            upperArmAngle,
            forearmAngle,
            clawAngle,
            animationProgress,
            isAnimationPlaying
        );
        this.renderArm(entity, level, poseStack, buffer, partialTick, packedLight, packedOverlay, armState);
        poseStack.popPose();
    }

    public void renderDancingArm(
        double renderTick, PoseStack pose, MultiBufferSource buffer, int packedLight, int packedOverlay
    ) {
        float lowerArmAngle = Mth.lerp((Mth.sin((float) (renderTick / 4 % (2 * Math.PI))) + 1) / 2, -45, 15);
        float upperArmAngle = Mth.lerp((Mth.sin((float) (renderTick / 8 % (2 * Math.PI))) + 1) / 4, -45, 95);
        // Create's dancing angles, adjusted for the baked 22.5-degree forearm and claw rest poses.
        ArmRenderState state = new ArmRenderState(
            (float) (renderTick * 10 % 360), lowerArmAngle, upperArmAngle + 22.5f, -lowerArmAngle + 22.5f, 0, false
        );
        pose.pushPose();
        this.applyBaseTransform(pose, Direction.NORTH, false);
        this.visitArmModels(
            pose, state,
            (model, modelPose) -> this.renderModel(modelPose, buffer, model, packedLight, packedOverlay),
            () -> {}
        );
        pose.popPose();
    }

    private void renderArm(
        SmartBlockPlacerBlockEntity entity,
        @Nullable Level level,
        PoseStack poseStack,
        MultiBufferSource buffer,
        float partialTick,
        int packedLight,
        int packedOverlay,
        ArmRenderState state
    ) {
        this.visitArmModels(
            poseStack,
            state,
            (model, pose) -> this.renderWorldModel(pose, buffer, model, packedLight, packedOverlay, level),
            () -> this.renderHeldContent(poseStack, buffer, entity.getCurrentHeldBlock(), level, partialTick, packedLight, packedOverlay)
        );
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
        ModelResourceLocation currentClawModel = shouldClawBeOpen ? CLAW_OPEN_MODEL : CLAW_MODEL;
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
    
    private void renderWorldModel(
        PoseStack poseStack, MultiBufferSource buffer, ModelResourceLocation model,
        int packedLight, int packedOverlay, @Nullable Level level
    ) {
        if (level == null) {
            this.renderModel(poseStack, buffer, model, packedLight, packedOverlay);
            return;
        }
        VertexConsumer vertices = buffer.getBuffer(RenderType.cutout());
        BakedModel bakedModel = this.modelManager.getModel(model);
        for (Direction cull : Direction.values()) {
            this.random.setSeed(42L);
            this.renderWorldQuads(
                poseStack.last(), vertices, bakedModel.getQuads(null, cull, this.random), packedLight, packedOverlay, level
            );
        }
        this.random.setSeed(42L);
        this.renderWorldQuads(
            poseStack.last(), vertices, bakedModel.getQuads(null, null, this.random), packedLight, packedOverlay, level
        );
    }

    private void renderWorldQuads(
        PoseStack.Pose pose, VertexConsumer consumer, List<BakedQuad> quads,
        int packedLight, int packedOverlay, Level level
    ) {
        Vector3f normal = new Vector3f();
        for (BakedQuad quad : quads) {
            Direction local = quad.getDirection();
            normal.set(local.getStepX(), local.getStepY(), local.getStepZ()).mul(pose.normal());
            Direction worldDirection = Direction.getNearest(normal.x(), normal.y(), normal.z());
            float shade = level.getShade(worldDirection, quad.isShade());
            consumer.putBulkData(pose, quad, shade, shade, shade, 1.0f, packedLight, packedOverlay);
        }
    }

    @SuppressWarnings("deprecation")
    private void renderModel(
        PoseStack poseStack, MultiBufferSource buffer, ModelResourceLocation model, int packedLight, int packedOverlay) {
        final VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.cutout());
        this.blockRenderer.getModelRenderer().renderModel(
            poseStack.last(),
            vertexConsumer,
            null,
            this.modelManager.getModel(model),
            0,
            0,
            0,
            packedLight,
            packedOverlay
        );
    }
    
    private void renderHeldContent(
        PoseStack poseStack,
        MultiBufferSource buffer,
        @Nullable Either<ItemStack, BlockStateAndEntity> heldBlock,
        @Nullable Level level,
        float partialTick,
        int packedLight,
        int packedOverlay
    ) {
        if (heldBlock == null) {
            return;
        }
        
        poseStack.pushPose();
        poseStack.translate(0.425 + 11F / 2048, 1.0625, 0.1125);
        poseStack.mulPose(Axis.XN.rotationDegrees(130));
        poseStack.mulPose(Axis.YN.rotationDegrees(10.625F));
        poseStack.mulPose(Axis.ZN.rotationDegrees(44.25F));
        poseStack.scale(0.65f, 0.65f, 0.65f);
        
        heldBlock
            .ifLeft(stack -> this.renderHeldItem(stack, poseStack, buffer, level, packedLight, packedOverlay))
            .ifRight(held -> {
                if (held.be() != null && !held.be().hasLevel() && level != null) {
                    held.be().setLevel(level);
                }

                BlockState state = held.state();
                boolean applied = false;
                if (state.getBlock().asItem() instanceof BlockItem item) {
                    this.itemRenderer.getModel(item.getDefaultInstance(), level, null, 0)
                        .applyTransform(HELD_ITEM_CONTEXT, poseStack, false);
                    applied = true;
                }
                BakedModel model = this.blockRenderer.getBlockModel(state);
                if (!applied) {
                    model.applyTransform(HELD_ITEM_CONTEXT, poseStack, false);
                }
                this.renderHeldBlockState(held, model, poseStack, buffer, partialTick, packedLight, packedOverlay);
            });
        
        poseStack.popPose();
    }

    private void renderHeldItem(
        ItemStack stack,
        PoseStack poseStack,
        MultiBufferSource buffer,
        @Nullable Level level,
        int packedLight,
        int packedOverlay
    ) {
        this.itemRenderer.renderStatic(
            stack,
            HELD_ITEM_CONTEXT,
            packedLight,
            packedOverlay,
            poseStack,
            buffer,
            level,
            0
        );
    }

    private void renderHeldBlockState(
        BlockStateAndEntity bsae,
        BakedModel model,
        PoseStack poseStack,
        MultiBufferSource buffer,
        float partialTick,
        int packedLight,
        int packedOverlay
    ) {
        poseStack.translate(-0.5F, -0.5F, -0.5F);

        BlockState state = bsae.state();
        BlockEntity entity = bsae.be();
        switch (state.getRenderShape()) {
            case MODEL -> {
                int color = this.blockColors.getColor(state, null, null, 0);
                float red = (float) (color >> 16 & 0xFF) / 255.0F;
                float green = (float) (color >> 8 & 0xFF) / 255.0F;
                float blue = (float) (color & 0xFF) / 255.0F;
                ModelData modelData = ModelData.EMPTY;
                boolean cull = !(state.getBlock() instanceof HalfTransparentBlock) && !(state.getBlock() instanceof StainedGlassPaneBlock);

                for (RenderType blockRenderType : model.getRenderTypes(state, RandomSource.create(42L), modelData)) {
                    VertexConsumer vertexConsumer = buffer.getBuffer(
                        RenderTypeHelper.getEntityRenderType(blockRenderType, cull)
                    );
                    this.blockRenderer.getModelRenderer().renderModel(
                        poseStack.last(),
                        vertexConsumer,
                        state,
                        model,
                        red,
                        green,
                        blue,
                        packedLight,
                        packedOverlay,
                        modelData,
                        blockRenderType
                    );
                }

                if (entity == null) return;
                BlockEntityRenderer<BlockEntity> renderer = this.blockEntityRenderer.getRenderer(entity);
                if (renderer == null) return;
                renderer.render(entity, partialTick, poseStack, buffer, packedLight, packedOverlay);
            }
            case ENTITYBLOCK_ANIMATED -> {
                ItemStack stack = new ItemStack(state.getBlock());
                IClientItemExtensions.of(stack).getCustomRenderer().renderByItem(
                    stack,
                    ItemDisplayContext.NONE,
                    poseStack,
                    buffer,
                    packedLight,
                    packedOverlay
                );
            }
            default -> {}
        }
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
        return AabbUtil.createInclusive(pos.north().west(), pos.above().south().east());
    }
}
