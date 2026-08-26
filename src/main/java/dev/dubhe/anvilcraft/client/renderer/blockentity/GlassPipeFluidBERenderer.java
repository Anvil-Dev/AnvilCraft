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
    private static final float FLUID_NODE_MIN = 3.01f / 16.0f;
    private static final float FLUID_NODE_MAX = 12.99f / 16.0f;
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
        float alphaFill = be.isShowingGas() ? be.getGasAlpha() : 1.0f;
        renderDisplayFluid(fluid, state, be.getDisplayDirections(), alphaFill, poseStack, buffer, packedLight);
    }

    private static void renderDisplayFluid(
        FluidStack fluid,
        BlockState state,
        Set<Direction> displayDirections,
        float alphaFill,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight
    ) {
        if (displayDirections.isEmpty()) {
            return;
        }
        float[] min = {FLUID_MIN, FLUID_MIN, FLUID_MIN};
        float[] max = {FLUID_MAX, FLUID_MAX, FLUID_MAX};
        if (state.getBlock() instanceof PipeStraightBlock) {
            Direction.Axis axis = state.getValue(PipeBlock.AXIS);
            Direction startDirection = Direction.get(Direction.AxisDirection.NEGATIVE, axis);
            Direction endDirection = Direction.get(Direction.AxisDirection.POSITIVE, axis);
            EnumSet<Direction> renderedDirections = visibleDirections(
                displayDirections, startDirection, endDirection);
            if (renderedDirections.isEmpty()) {
                return;
            }
            if (renderedDirections.size() < 2) {
                renderFluidBox(fluid, min, max, poseStack, buffer, packedLight, renderedDirections, alphaFill);
                for (Direction direction : renderedDirections) {
                    renderFluidArm(
                        fluid,
                        direction,
                        poseStack,
                        buffer,
                        packedLight,
                        EnumSet.of(direction.getOpposite(), direction),
                        alphaFill
                    );
                }
                return;
            }
            extendFluidBounds(startDirection, min, max);
            extendFluidBounds(endDirection, min, max);
            renderFluidBox(
                fluid,
                min,
                max,
                poseStack,
                buffer,
                packedLight,
                EnumSet.of(startDirection, endDirection),
                alphaFill
            );
            return;
        } else if (state.getBlock() instanceof PipeCornerBlock) {
            PipeBlock.CornerEnded corner = state.getValue(PipeBlock.CORNER_ENDED);
            Direction firstDirection = corner.getFirstDirection();
            Direction secondDirection = corner.getSecondDirection();
            EnumSet<Direction> renderedDirections = visibleDirections(
                displayDirections, firstDirection, secondDirection);
            if (renderedDirections.isEmpty()) {
                return;
            }
            renderFluidBox(
                fluid,
                min,
                max,
                poseStack,
                buffer,
                packedLight,
                renderedDirections,
                alphaFill
            );

            for (Direction direction : renderedDirections) {
                renderFluidArm(
                    fluid,
                    direction,
                    poseStack,
                    buffer,
                    packedLight,
                    EnumSet.of(direction.getOpposite(), direction),
                    alphaFill
                );
            }
            return;
        } else if (state.getBlock() instanceof PipeNodeBlock) {
            EnumSet<Direction> renderedDirections = EnumSet.noneOf(Direction.class);
            for (Direction direction : Direction.values()) {
                PipeBlock.NodePipe nodePipe = state.getValue(PipeBlock.getPropertyForDirection(direction));
                if (nodePipe != PipeBlock.NodePipe.NONE && displayDirections.contains(direction)) {
                    renderedDirections.add(direction);
                }
            }
            float[] nodeMin = {FLUID_NODE_MIN, FLUID_NODE_MIN, FLUID_NODE_MIN};
            float[] nodeMax = {FLUID_NODE_MAX, FLUID_NODE_MAX, FLUID_NODE_MAX};
            renderFluidBox(fluid, nodeMin, nodeMax, poseStack, buffer, packedLight, Set.of(), alphaFill);
            for (Direction direction : renderedDirections) {
                renderFluidArm(
                    fluid,
                    direction,
                    poseStack,
                    buffer,
                    packedLight,
                    EnumSet.of(direction.getOpposite(), direction),
                    alphaFill
                );
            }
            return;
        }
        renderFluidBox(fluid, min, max, poseStack, buffer, packedLight, Set.of(), alphaFill);
    }

    private static EnumSet<Direction> visibleDirections(
        Set<Direction> displayDirections, Direction firstDirection, Direction secondDirection
    ) {
        EnumSet<Direction> renderedDirections = EnumSet.noneOf(Direction.class);
        if (displayDirections.contains(firstDirection)) {
            renderedDirections.add(firstDirection);
        }
        if (displayDirections.contains(secondDirection)) {
            renderedDirections.add(secondDirection);
        }
        return renderedDirections;
    }

    private static void renderFluidArm(
        FluidStack fluid,
        Direction direction,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        Set<Direction> skippedSides,
        float alphaFill
    ) {
        float[] min = {FLUID_MIN, FLUID_MIN, FLUID_MIN};
        float[] max = {FLUID_MAX, FLUID_MAX, FLUID_MAX};
        extendFluidArmBounds(direction, min, max);
        renderFluidBox(fluid, min, max, poseStack, buffer, packedLight, skippedSides, alphaFill);
    }

    private static void renderFluidBox(
        FluidStack fluid,
        float[] min,
        float[] max,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight
    ) {
        renderFluidBox(fluid, min, max, poseStack, buffer, packedLight, Set.of(), 1.0f);
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
        renderFluidBox(fluid, min, max, poseStack, buffer, packedLight, skippedSides, 1.0f);
    }

    private static void renderFluidBox(
        FluidStack fluid,
        float[] min,
        float[] max,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        Set<Direction> skippedSides,
        float alphaFill
    ) {
        FluidRenderHelper.INSTANCE.renderFluidBox(
            fluid,
            min[0], min[1], min[2],
            max[0], max[1], max[2],
            buffer, poseStack, packedLight,
            skippedSides, alphaFill
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
