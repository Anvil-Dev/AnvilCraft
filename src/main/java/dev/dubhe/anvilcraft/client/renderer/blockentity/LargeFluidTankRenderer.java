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

import dev.dubhe.anvilcraft.api.fluid.InfinityFluidTank;
import dev.dubhe.anvilcraft.block.entity.LargeFluidTankBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.FluidHandlerRenderState;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.Nullable;

public class LargeFluidTankRenderer extends BaseFluidHandlerHolderRenderer<LargeFluidTankBlockEntity, FluidHandlerRenderState> {
    private static final float TANK_W = 4 / 16F + 0.001F; // avoiding Z-fighting

    public LargeFluidTankRenderer(BlockEntityRendererProvider.Context ignored) {
    }

    @Override
    public FluidHandlerRenderState createRenderState() {
        return new FluidHandlerRenderState();
    }

    @Override
    public boolean shouldRender(LargeFluidTankBlockEntity blockEntity, Vec3 cameraPosition) {
        return blockEntity.isMainPart() && super.shouldRender(blockEntity, cameraPosition);
    }

    @Override
    public void extractRenderState(
        LargeFluidTankBlockEntity be,
        FluidHandlerRenderState state,
        float partialTicks,
        Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        if (!be.isMainPart()) {
            state.setResource(null);
            state.setFill(0.0F);
            return;
        }
        super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
    }

    @Override
    protected void updateTankW(
        LargeFluidTankBlockEntity be,
        FluidHandlerRenderState state,
        float partialTicks,
        Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        state.setTankW(-1, -1, -1, 2, 2, 2, LargeFluidTankRenderer.TANK_W);
    }

    @Override
    public float getFill(ResourceHandler<FluidResource> tank) {
        return tank instanceof InfinityFluidTank infinity && infinity.isInfinity() ? 1.0F : super.getFill(tank);
    }

    @Override
    public AABB getRenderBoundingBox(LargeFluidTankBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(1);
    }
}
