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
import dev.dubhe.anvilcraft.block.entity.FluidTankBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

import static dev.dubhe.anvilcraft.client.renderer.blockentity.LargeFluidTankBlockEntityRenderer.renderFluidCube;

public class FluidTankBlockEntityRenderer implements BlockEntityRenderer<FluidTankBlockEntity> {
    public FluidTankBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
        FluidTankBlockEntity tank, float tickDelta, PoseStack ms, MultiBufferSource vertexConsumers, int light, int overlay) {
        if (tank.getTank().getFluid().isEmpty()) return;

        /*
         *
         * // Uncomment to allow the liquid to rotate with the tank ms.pushPose(); ms.translate(0.5, 0.5, 0.5);
         * FacingToRotation.get(tank.getForward(), tank.getUp()).push(ms); ms.translate(-0.5, -0.5, -0.5);
         */
        float fill = Math.min((float) tank.getTank().getFluid().getAmount() / tank.getTank().getCapacity(), 1);

        drawFluidInTank(
            ms,
            vertexConsumers,
            light,
            tank.getTank().getFluid(),
            fill
        );

        // ms.popPose();
    }

    private static final float TANK_W = 1 / 16f + 0.001f; // avoiding Z-fighting

    public static void drawFluidInTank(PoseStack ps, MultiBufferSource mbs, int light, FluidStack fluid, float fill) {
        // From Modern Industrialization
        var renderProps = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
            .apply(renderProps.getStillTexture(fluid));

        int color = renderProps.getTintColor(fluid);

        // Top and bottom positions of the fluid inside the tank
        float height = 1 - 2 * TANK_W;

        float minX = TANK_W;
        float minY = TANK_W;
        float minZ = TANK_W;
        float maxX = 1 - TANK_W;
        float maxY = 1 - TANK_W;
        float maxZ = 1 - TANK_W;

        FluidType attributes = fluid.getFluid().getFluidType();
        if (attributes.isLighterThanAir()) {
            minY = maxY - fill * height;
        } else {
            maxY = minY + fill * height;
        }

        renderFluidCube(ps, mbs, light, sprite, color, minX, minY, minZ, maxX, maxY, maxZ);
    }

}
