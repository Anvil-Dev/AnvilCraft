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
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

public class LargeFluidTankBlockEntityRenderer implements BlockEntityRenderer<LargeFluidTankBlockEntity> {
    public LargeFluidTankBlockEntityRenderer(BlockEntityRendererProvider.Context ignore) {
    }

    @Override
    public AABB getRenderBoundingBox(LargeFluidTankBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(1, 1, 1);
    }

    @Override
    public void render(
        LargeFluidTankBlockEntity tank, float tickDelta, PoseStack ms, MultiBufferSource vertexConsumers, int light, int overlay) {
        if (tank.getTank().getFluid().isEmpty()) return;
        if (!tank.isMainPart()) return;

        /*
         *
         * // Uncomment to allow the liquid to rotate with the tank ms.pushPose(); ms.translate(0.5, 0.5, 0.5);
         * FacingToRotation.get(tank.getForward(), tank.getUp()).push(ms); ms.translate(-0.5, -0.5, -0.5);
         */
        float fill = (float) tank.getTank().getFluid().getAmount() / tank.getTank().getCapacity();
        if (fill <= 0.025) fill = 0.025f;
        fill = Mth.clamp(fill, 0, 1);

        drawFluidInTank(ms, vertexConsumers, light, tank.getTank().getFluid(), fill);

        // ms.popPose();
    }

    private static final float TANK_W = 4 / 16f + 0.001f; // avoiding Z-fighting

    public static void drawFluidInTank(PoseStack ps, MultiBufferSource mbs, int light, FluidStack fluid, float fill) {
        float height = 3 - 2 * TANK_W;

        float minX = TANK_W - 1;
        float minY = TANK_W - 1;
        float minZ = TANK_W - 1;
        float maxX = 2 - TANK_W;
        float maxY = 2 - TANK_W;
        float maxZ = 2 - TANK_W;

        FluidType attributes = fluid.getFluid().getFluidType();
        if (attributes.isLighterThanAir()) {
            minY = maxY - fill * height;
        } else {
            maxY = minY + fill * height;
        }

        FluidRenderHelper.INSTANCE.renderFluidBox(
            fluid,
            minX, minY, minZ,
            maxX, maxY, maxZ,
            mbs, ps, light,
            true, false
        );
    }
}
