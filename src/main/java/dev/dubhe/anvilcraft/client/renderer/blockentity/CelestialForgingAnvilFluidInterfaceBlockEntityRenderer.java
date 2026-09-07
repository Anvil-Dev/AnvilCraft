package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.block.cfa.interfaces.CelestialForgingAnvilInterfaceBlock;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilFluidInterfaceBlockEntity;
import dev.dubhe.anvilcraft.client.support.FluidRenderHelper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class CelestialForgingAnvilFluidInterfaceBlockEntityRenderer
    implements BlockEntityRenderer<CelestialForgingAnvilFluidInterfaceBlockEntity> {
    private static final int DISPLAY_CAPACITY = 80_000;
    // Inset from the coincident tank / tank inverted model faces to avoid Z-fighting.
    private static final float MIN_X = 5 / 16f + 0.001f;
    private static final float MIN_Y = 17 / 16f + 0.001f;
    private static final float MIN_Z = 7 / 16f + 0.001f;
    private static final float MAX_X = 11 / 16f - 0.001f;
    private static final float MAX_Y = 21 / 16f - 0.001f;
    private static final float MAX_Z = 13 / 16f - 0.001f;

    public CelestialForgingAnvilFluidInterfaceBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public AABB getRenderBoundingBox(CelestialForgingAnvilFluidInterfaceBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).expandTowards(0, 5 / 16d, 0);
    }

    @Override
    public void render(
        CelestialForgingAnvilFluidInterfaceBlockEntity blockEntity,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int light,
        int overlay
    ) {
        List<FluidStack> fluids = Arrays.stream(blockEntity.getTanks())
            .map(FluidTank::getFluid)
            .filter(fluid -> !fluid.isEmpty())
            .sorted(Comparator
                .comparingInt(FluidStack::getAmount)
                .reversed()
                .thenComparing(fluid -> BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString()))
            .toList();
        if (fluids.isEmpty()) return;

        Direction facing = blockEntity.getBlockState().getValue(CelestialForgingAnvilInterfaceBlock.FACING);
        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(180 - facing.toYRot()));
        poseStack.translate(-0.5, 0, -0.5);

        long totalAmount = fluids.stream().mapToLong(FluidStack::getAmount).sum();
        long renderAmount = Math.max(totalAmount, DISPLAY_CAPACITY);
        double layerBottom = 0;
        for (FluidStack fluid : fluids) {
            if (layerBottom >= 1) break;
            double layerTop = Math.min(1, layerBottom + (double) fluid.getAmount() / renderAmount);
            drawFluid(poseStack, buffer, light, fluid, layerBottom, layerTop);
            layerBottom = layerTop;
        }
        poseStack.popPose();
    }

    private static void drawFluid(
        PoseStack poseStack,
        MultiBufferSource buffer,
        int light,
        FluidStack fluid,
        double layerBottom,
        double layerTop
    ) {
        if (fluid.getFluidType().isLighterThanAir()) {
            FluidRenderHelper.INSTANCE.renderFluidBox(
                fluid,
                MIN_X, MIN_Y, MIN_Z,
                MAX_X, MAX_Y, MAX_Z,
                buffer, poseStack, light,
                true, (float) (layerTop - layerBottom)
            );
            return;
        }

        float height = MAX_Y - MIN_Y;
        float minY = (float) (MIN_Y + layerBottom * height);
        float maxY = (float) (MIN_Y + layerTop * height);
        FluidRenderHelper.INSTANCE.renderFluidBox(
            fluid,
            MIN_X, minY, MIN_Z,
            MAX_X, maxY, MAX_Z,
            buffer, poseStack, light,
            true, false
        );
    }
}
