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

import java.util.ArrayList;
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

        List<FluidStack> liquids = new ArrayList<>();
        List<FluidStack> gases = new ArrayList<>();
        for (FluidStack fluid : fluids) {
            if (fluid.getFluidType().isLighterThanAir()) {
                gases.add(fluid);
            } else {
                liquids.add(fluid);
            }
        }

        double liquidTop = 0;
        for (FluidStack liquid : liquids) {
            if (liquidTop >= 1) break;
            double layerTop = Math.min(1, liquidTop + (double) liquid.getAmount() / renderAmount);
            drawFluid(poseStack, buffer, light, liquid, liquidTop, layerTop);
            liquidTop = layerTop;
        }

        // 气体浮于液面之上：均分液面到罐顶的空隙，各自独立成带，储量仍由透明度表达。
        if (!gases.isEmpty() && liquidTop < 1) {
            double bandHeight = (1 - liquidTop) / gases.size();
            double bandBottom = liquidTop;
            for (int i = 0; i < gases.size(); i++) {
                FluidStack gas = gases.get(i);
                double bandTop = Math.min(1, bandBottom + bandHeight);
                drawGas(
                    poseStack, buffer, light, gas, bandBottom, bandTop,
                    (float) ((double) gas.getAmount() / renderAmount),
                    i == 0 && liquidTop <= 0
                );
                bandBottom = bandTop;
            }
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

    /**
     * 绘制一种气体的整段带。带高由参与显示的气体种类均分液面之上的空隙决定，
     * 该气体自身的储量只通过 {@code alphaFill}（0..1）缩放着色透明度表达。
     *
     * @param renderBottom 是否绘制带的底面；仅当带下方既无液体也不是另一条气体带时为 true，避免相邻流体面共面打架
     */
    private static void drawGas(
        PoseStack poseStack,
        MultiBufferSource buffer,
        int light,
        FluidStack gas,
        double bandBottom,
        double bandTop,
        float alphaFill,
        boolean renderBottom
    ) {
        float height = MAX_Y - MIN_Y;
        float minY = (float) (MIN_Y + bandBottom * height);
        float maxY = (float) (MIN_Y + bandTop * height);
        FluidRenderHelper.INSTANCE.renderFluidBox(
            gas,
            MIN_X, minY, MIN_Z,
            MAX_X, maxY, MAX_Z,
            buffer, poseStack, light,
            renderBottom, alphaFill
        );
    }
}
