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
import dev.dubhe.anvilcraft.block.entity.TankFluidLayer;
import dev.dubhe.anvilcraft.client.support.FluidRenderHelper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;

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
        for (TankFluidLayer layer : TankFluidLayer.of(tank.getStoredFluids(), tank.isEnhanced())) {
            if (layer.fluid().getFluidType().isLighterThanAir()) {
                drawGasInTank(
                    ms, vertexConsumers, light, layer.fluid(), layer.bottom(), layer.top(), layer.opacity(), layer.renderBottom()
                );
            } else {
                drawFluidInTank(ms, vertexConsumers, light, layer.fluid(), layer.bottom(), layer.top());
            }
        }
    }

    private static final float TANK_W = TankFluidLayer.INSET;

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

    /**
     * 绘制一种气体的整段带。带高由参与显示的气体种类均分液面之上的空隙决定，
     * 该气体自身的储量只通过 {@code alphaFill}（0..1）缩放着色透明度表达。
     *
     * @param renderBottom 是否绘制带的底面；仅当带下方既无液体也不是另一条气体带时为 true，避免相邻流体面共面打架
     */
    public static void drawGasInTank(
        PoseStack ps,
        MultiBufferSource mbs,
        int light,
        FluidStack gas,
        double bandBottom,
        double bandTop,
        float alphaFill,
        boolean renderBottom
    ) {
        float height = 3 - 2 * TANK_W;

        float minX = TANK_W - 1;
        float minZ = TANK_W - 1;
        float maxX = 2 - TANK_W;
        float maxZ = 2 - TANK_W;
        float minY = (float) (TANK_W - 1 + bandBottom * height);
        float maxY = (float) (TANK_W - 1 + bandTop * height);

        FluidRenderHelper.INSTANCE.renderFluidBox(
            gas,
            minX, minY, minZ,
            maxX, maxY, maxZ,
            mbs, ps, light,
            renderBottom, alphaFill
        );
    }
}
