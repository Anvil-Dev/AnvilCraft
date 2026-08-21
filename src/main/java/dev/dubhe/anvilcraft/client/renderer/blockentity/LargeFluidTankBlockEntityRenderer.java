/*
 * Original Code Copyright (C) 2013 - 2020 AlgorithmX2 et al
 * Source: https://github.com/AppliedEnergistics/Applied-Energistics-2
 *
 * This file is part of "Applied Energistics 2" project, which is licensed under
 * the GNU Lesser General Public License Version 3 (LGPLv3).
 *
 * --- MODIFICATIONS ---
 * This file has been modified for use in AnvilCraft.
 * Modifications made by: TB_pig
 * Modification date: 2026/2/12
 * These modifications continue to be licensed under LGPLv3.
 * -------------------------------------------------------------
 */

package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.entity.LargeFluidTankBlockEntity;
import dev.dubhe.anvilcraft.client.support.FluidRenderHelper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Comparator;
import java.util.List;

public class LargeFluidTankBlockEntityRenderer implements BlockEntityRenderer<LargeFluidTankBlockEntity> {
    public LargeFluidTankBlockEntityRenderer(BlockEntityRendererProvider.Context ignore) {
    }

    @Override
    public AABB getRenderBoundingBox(LargeFluidTankBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(1, 1, 1);
    }

    @Override
    public void render(
        LargeFluidTankBlockEntity tank,
        float tickDelta,
        PoseStack ms,
        MultiBufferSource vertexConsumers,
        int light,
        int overlay
    ) {
        if (!tank.isMainPart()) return;
        List<FluidStack> fluids = tank.getStoredFluids().stream()
            .filter(fluid -> !fluid.isEmpty())
            .sorted(Comparator
                .comparingInt(FluidStack::getAmount)
                .reversed()
                .thenComparing(fluid -> BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString()))
            .toList();
        if (fluids.isEmpty()) return;

        long totalAmount = fluids.stream().mapToLong(FluidStack::getAmount).sum();
        long renderAmount = tank.isEnhanced()
            ? Math.max(totalAmount, LargeFluidTankBlockEntity.INFINITY_THRESHOLD)
            : LargeFluidTankBlockEntity.BASE_CAPACITY;
        double layerBottom = 0;
        for (FluidStack fluid : fluids) {
            if (layerBottom >= 1) break;
            double layerTop = Math.min(1, layerBottom + (double) fluid.getAmount() / renderAmount);
            drawFluidInTank(ms, vertexConsumers, light, fluid, layerBottom, layerTop);
            layerBottom = layerTop;
        }
    }

    private static final float TANK_W = 4 / 16f + 0.001f; // avoiding Z-fighting

    public static void drawFluidInTank(
        PoseStack ps,
        MultiBufferSource mbs,
        int light,
        FluidStack fluid,
        double layerBottom,
        double layerTop
    ) {
        float height = 3 - 2 * TANK_W;

        float minX = TANK_W - 1;
        float minZ = TANK_W - 1;
        float maxX = 2 - TANK_W;
        float maxZ = 2 - TANK_W;

        if (fluid.getFluid().getFluidType().isLighterThanAir()) {
            // Gas always fills the whole tank; its share is conveyed by opacity.
            FluidRenderHelper.INSTANCE.renderFluidBox(
                fluid,
                minX, TANK_W - 1, minZ,
                maxX, 2 - TANK_W, maxZ,
                mbs, ps, light,
                true, (float) (layerTop - layerBottom)
            );
            return;
        }

        float minY = (float) (TANK_W - 1 + layerBottom * height);
        float maxY = (float) (TANK_W - 1 + layerTop * height);

        FluidRenderHelper.INSTANCE.renderFluidBox(
            fluid,
            minX, minY, minZ,
            maxX, maxY, maxZ,
            mbs, ps, light,
            true, false
        );
    }
}
