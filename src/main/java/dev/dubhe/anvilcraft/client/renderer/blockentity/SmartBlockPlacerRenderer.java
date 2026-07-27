package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Either;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.SmartBlockPlacerBlock;
import dev.dubhe.anvilcraft.block.entity.SmartBlockPlacerBlockEntity;
import dev.dubhe.anvilcraft.init.ModSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.List;
import javax.annotation.Nullable;

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
    private static final ModelResourceLocation CLAW_OPEN_MODEL = ModelResourceLocation.standalone(
        AnvilCraft.of("block/smart_block_placer_claw_open")
    );

    private static final PlacementAnimation PLACEMENT_ANIMATION = new PlacementAnimation();
    private static final ItemDisplayContext HELD_ITEM_CONTEXT = ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;

    private final BlockRenderDispatcher blockRenderer;
    private final ItemRenderer itemRenderer;
    private final ModelManager modelManager;
    private final BlockColors blockColors;

    public SmartBlockPlacerRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
        this.itemRenderer = context.getItemRenderer();
        this.modelManager = Minecraft.getInstance().getModelManager();
        this.blockColors = Minecraft.getInstance().getBlockColors();
    }

    /**
     * 工作动画方案（放置方块时）
     */
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
        poseStack.translate(0.5, 1.5, 0.5);
        if (upsideDown) {
            poseStack.mulPose(Axis.XP.rotationDegrees(180f));
        }
        applyHorizontalRotation(poseStack, facing, upsideDown);
        poseStack.translate(0, upsideDown ? 0.5 : -1.5, 0);

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
                animationTargetPos = getNextTargetPosition(entity, level, facing, upsideDown);
                if (animationTargetPos != null) {
                    level.playLocalSound(
                        entity.getBlockPos(),
                        ModSoundEvents.SMART_BLOCK_PLACER_EXTEND.get(),
                        SoundSource.BLOCKS,
                        0.4f,
                        1.3f,
                        false
                    );
                    if (level.random.nextFloat() < 0.6f) {
                        level.playLocalSound(
                            entity.getBlockPos(),
                            ModSoundEvents.SMART_BLOCK_PLACER_SHULKER_OPEN.get(),
                            SoundSource.BLOCKS,
                            0.4f,
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
                        0.8f,
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
        
        ArmRenderState armState = new ArmRenderState(
            baseSwingAngle,
            upperArmAngle,
            forearmAngle,
            clawAngle,
            animationProgress,
            isAnimationPlaying
        );
        renderArm(entity, level, poseStack, buffer, packedLight, packedOverlay, upsideDown, armState);
        poseStack.popPose();
    }

    private void renderArm(
        SmartBlockPlacerBlockEntity entity,
        @Nullable Level level,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        int packedOverlay,
        boolean upsideDown,
        ArmRenderState state
    ) {
        poseStack.pushPose();
        poseStack.mulPose((upsideDown ? Axis.YN : Axis.YP).rotationDegrees(state.baseSwingAngle()));
        poseStack.translate(-0.5, 0.0, -0.5);
        renderModel(poseStack, buffer, BASE_MODEL, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.mulPose((upsideDown ? Axis.YN : Axis.YP).rotationDegrees(state.baseSwingAngle()));
        poseStack.translate(0, 0.625, 0);
        poseStack.mulPose(Axis.XP.rotationDegrees(state.upperArmAngle()));
        poseStack.translate(0, -0.625, 0);
        poseStack.translate(-0.5, 0.0, -0.5);
        renderModel(poseStack, buffer, UPPERARM_MODEL, packedLight, packedOverlay);

        poseStack.pushPose();
        poseStack.translate(0.6875, 1.0625, 0.9375);
        poseStack.mulPose(Axis.XP.rotationDegrees(state.forearmAngle()));
        poseStack.translate(-0.6875, -1.0625, -0.9375);
        renderModel(poseStack, buffer, FOREARM_MODEL, packedLight, packedOverlay);

        poseStack.pushPose();
        poseStack.translate(0.5, 1.3125, 0.375);
        poseStack.mulPose(Axis.XP.rotationDegrees(state.clawAngle()));
        poseStack.translate(-0.5, -1.3125, -0.375);

        boolean shouldClawBeOpen = state.animationPlaying()
            && state.animationProgress() > 0f
            && state.animationProgress() <= 0.7f;
        ModelResourceLocation currentClawModel = shouldClawBeOpen ? CLAW_OPEN_MODEL : CLAW_MODEL;
        renderModel(poseStack, buffer, currentClawModel, packedLight, packedOverlay);

        if (shouldClawBeOpen && level != null) {
            renderHeldContent(poseStack, buffer, entity.getCurrentHeldBlock(), level, packedLight, packedOverlay);
        }

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
            if (!SmartBlockPlacerBlockEntity.isTargetUnobstructed(level, targetPos)) {
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
                if (canBeStacked(targetState, heldBlock)) {
                    return targetPos;
                }
            }
        }
        
        return null;
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
        @Nullable Either<ItemStack, BlockState> heldBlock,
        Level level,
        int packedLight,
        int packedOverlay
    ) {
        if (heldBlock == null) {
            return;
        }
        
        poseStack.pushPose();
        poseStack.translate(0.5, 0.96, 0.1);
        poseStack.mulPose(Axis.XP.rotationDegrees(-40));
        poseStack.scale(0.65f, 0.65f, 0.65f);
        
        heldBlock
            .ifLeft(stack -> this.renderHeldItem(stack, poseStack, buffer, level, packedLight, packedOverlay))
            .ifRight(state -> this.renderHeldBlockState(state, poseStack, buffer, packedLight, packedOverlay));
        
        poseStack.popPose();
    }

    private void renderHeldItem(
        ItemStack stack,
        PoseStack poseStack,
        MultiBufferSource buffer,
        Level level,
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
        BlockState state, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay
    ) {
        BakedModel model = this.blockRenderer.getBlockModel(state)
            .applyTransform(HELD_ITEM_CONTEXT, poseStack, false);
        poseStack.translate(-0.5F, -0.5F, -0.5F);

        if (state.getRenderShape() != RenderShape.MODEL) {
            this.blockRenderer.renderSingleBlock(
                state,
                poseStack,
                buffer,
                packedLight,
                packedOverlay,
                ModelData.EMPTY,
                RenderType.cutout()
            );
            return;
        }

        int color = this.blockColors.getColor(state, null, null, 0);
        float red = (float) (color >> 16 & 0xFF) / 255.0F;
        float green = (float) (color >> 8 & 0xFF) / 255.0F;
        float blue = (float) (color & 0xFF) / 255.0F;
        ModelData modelData = ModelData.EMPTY;
        boolean cull = !(state.getBlock() instanceof HalfTransparentBlock)
            && !(state.getBlock() instanceof StainedGlassPaneBlock);

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
    }

    private static @Nullable Block getDisplayedBlock(@Nullable Either<ItemStack, BlockState> displayedBlock) {
        if (displayedBlock == null) {
            return null;
        }
        return displayedBlock.map(
            stack -> stack.getItem() instanceof BlockItem blockItem ? blockItem.getBlock() : null,
            BlockState::getBlock
        );
    }
}
