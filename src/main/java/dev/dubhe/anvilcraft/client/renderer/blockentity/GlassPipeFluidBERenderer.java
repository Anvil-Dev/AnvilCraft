package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.entity.fluid.GlassPipeBlockEntity;
import dev.dubhe.anvilcraft.block.fluid.PipeBlock;
import dev.dubhe.anvilcraft.block.fluid.PipeCornerBlock;
import dev.dubhe.anvilcraft.block.fluid.PipeNodeBlock;
import dev.dubhe.anvilcraft.block.fluid.PipeStraightBlock;
import dev.dubhe.anvilcraft.client.support.FluidRenderHelper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.EnumSet;
import java.util.Set;

public class GlassPipeFluidBERenderer implements BlockEntityRenderer<GlassPipeBlockEntity> {

    private static final float FLUID_MIN = 4.01f / 16.0f;
    private static final float FLUID_MAX = 11.99f / 16.0f;
    private static final float FLUID_STRAIGHT_ARM_MIN = 0.0f;
    private static final float FLUID_STRAIGHT_ARM_MAX = 1.0f;

    @SuppressWarnings("unused")
    public GlassPipeFluidBERenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
        GlassPipeBlockEntity be,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        int packedOverlay
    ) {
        Level level = be.getLevel();
        if (level == null) {
            return;
        }
        BlockState state = level.getBlockState(be.getBlockPos());
        if (!(state.getBlock() instanceof PipeBlock pipe) || !pipe.isGlassPipe()) {
            return;
        }
        FluidStack fluid = be.getDisplayFluid();
        if (fluid.isEmpty()) {
            return;
        }
        renderDisplayFluid(fluid, state, be.getDisplayDirections(), poseStack, buffer, packedLight);
    }

    private static void renderDisplayFluid(
        FluidStack fluid,
        BlockState state,
        Set<Direction> displayDirections,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight
    ) {
        float[] min = {FLUID_MIN, FLUID_MIN, FLUID_MIN};
        float[] max = {FLUID_MAX, FLUID_MAX, FLUID_MAX};
        if (state.getBlock() instanceof PipeStraightBlock) {
            Direction.Axis axis = state.getValue(PipeBlock.AXIS);
            Direction startDirection = Direction.get(Direction.AxisDirection.NEGATIVE, axis);
            Direction endDirection = Direction.get(Direction.AxisDirection.POSITIVE, axis);
            extendFluidBounds(startDirection, min, max);
            extendFluidBounds(endDirection, min, max);
            renderFluidBox(
                fluid,
                min,
                max,
                poseStack,
                buffer,
                packedLight,
                EnumSet.of(startDirection, endDirection)
            );
            return;
        } else if (state.getBlock() instanceof PipeCornerBlock) {
            PipeBlock.CornerEnded corner = state.getValue(PipeBlock.CORNER_ENDED);
            Direction firstDirection = corner.getFirstDirection();
            Direction secondDirection = corner.getSecondDirection();
            renderFluidBox(
                fluid,
                min,
                max,
                poseStack,
                buffer,
                packedLight,
                EnumSet.of(firstDirection, secondDirection)
            );

            EnumSet<Direction> firstSkippedSides = EnumSet.of(firstDirection.getOpposite(), firstDirection);
            renderFluidArm(fluid, firstDirection, poseStack, buffer, packedLight, firstSkippedSides);

            EnumSet<Direction> secondSkippedSides = EnumSet.of(secondDirection.getOpposite(), secondDirection);
            renderFluidArm(fluid, secondDirection, poseStack, buffer, packedLight, secondSkippedSides);
            return;
        } else if (state.getBlock() instanceof PipeNodeBlock) {
            EnumSet<Direction> renderedDirections = EnumSet.noneOf(Direction.class);
            for (Direction direction : Direction.values()) {
                PipeBlock.NodePipe nodePipe = state.getValue(PipeBlock.getPropertyForDirection(direction));
                if (nodePipe != PipeBlock.NodePipe.NONE && displayDirections.contains(direction)) {
                    renderedDirections.add(direction);
                }
            }
            renderFluidBox(fluid, min, max, poseStack, buffer, packedLight);
            for (Direction direction : renderedDirections) {
                renderFluidArm(
                    fluid,
                    direction,
                    poseStack,
                    buffer,
                    packedLight,
                    EnumSet.of(direction.getOpposite(), direction)
                );
            }
            return;
        }
        renderFluidBox(fluid, min, max, poseStack, buffer, packedLight);
    }

    private static void renderFluidArm(
        FluidStack fluid,
        Direction direction,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        Set<Direction> skippedSides
    ) {
        float[] min = {FLUID_MIN, FLUID_MIN, FLUID_MIN};
        float[] max = {FLUID_MAX, FLUID_MAX, FLUID_MAX};
        extendFluidArmBounds(direction, min, max);
        renderFluidBox(fluid, min, max, poseStack, buffer, packedLight, skippedSides);
    }

    private static void renderFluidBox(
        FluidStack fluid,
        float[] min,
        float[] max,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight
    ) {
        renderFluidBox(fluid, min, max, poseStack, buffer, packedLight, Set.of());
    }

    private static void renderFluidBox(
        FluidStack fluid,
        float[] min,
        float[] max,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        Set<Direction> skippedSides
    ) {
        FluidRenderHelper.INSTANCE.renderFluidBox(
            fluid,
            min[0], min[1], min[2],
            max[0], max[1], max[2],
            buffer, poseStack, packedLight,
            skippedSides, false
        );
    }

    private static void extendFluidBounds(Direction direction, float[] min, float[] max) {
        switch (direction) {
            case DOWN -> min[1] = FLUID_STRAIGHT_ARM_MIN;
            case UP -> max[1] = FLUID_STRAIGHT_ARM_MAX;
            case NORTH -> min[2] = FLUID_STRAIGHT_ARM_MIN;
            case SOUTH -> max[2] = FLUID_STRAIGHT_ARM_MAX;
            case WEST -> min[0] = FLUID_STRAIGHT_ARM_MIN;
            case EAST -> max[0] = FLUID_STRAIGHT_ARM_MAX;
            default -> {
            }
        }
    }

    private static void extendFluidArmBounds(Direction direction, float[] min, float[] max) {
        switch (direction) {
            case DOWN -> {
                min[1] = FLUID_STRAIGHT_ARM_MIN;
                max[1] = FLUID_MIN;
            }
            case UP -> {
                min[1] = FLUID_MAX;
                max[1] = FLUID_STRAIGHT_ARM_MAX;
            }
            case NORTH -> {
                min[2] = FLUID_STRAIGHT_ARM_MIN;
                max[2] = FLUID_MIN;
            }
            case SOUTH -> {
                min[2] = FLUID_MAX;
                max[2] = FLUID_STRAIGHT_ARM_MAX;
            }
            case WEST -> {
                min[0] = FLUID_STRAIGHT_ARM_MIN;
                max[0] = FLUID_MIN;
            }
            case EAST -> {
                min[0] = FLUID_MAX;
                max[0] = FLUID_STRAIGHT_ARM_MAX;
            }
            default -> {
            }
        }
    }
}
