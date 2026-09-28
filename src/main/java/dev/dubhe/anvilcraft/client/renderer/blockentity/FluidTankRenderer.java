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
import dev.dubhe.anvilcraft.client.renderer.FluidTankRenderUtil;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.FluidHandlerRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.Nullable;

public class FluidTankRenderer extends BaseFluidHandlerHolderRenderer<FluidTankBlockEntity, FluidHandlerRenderState> {
    private static final float TANK_W = 1 / 16F + 0.001F; // avoiding Z-fighting

    public FluidTankRenderer(BlockEntityRendererProvider.Context ignored) {
    }

    @Override
    public FluidHandlerRenderState createRenderState() {
        return new FluidHandlerRenderState();
    }

    @Override
    protected float minimumFill() {
        return 0;
    }

    @Override
    public float getFill(ResourceHandler<FluidResource> tank) {
        return Mth.clamp(super.getFill(tank), 0.0F, 1.0F);
    }

    @Override
    public void submit(FluidHandlerRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.getResource() == null) return;
        FluidTankRenderUtil.submit(state.getResource(), state.getAmount(), state.getFill(), pose, collector,
            state.lightCoords, FLUID_RENDER_TYPE);
    }

    @Override
    protected void updateTankW(
        FluidTankBlockEntity be,
        FluidHandlerRenderState state,
        float partialTicks,
        Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        state.setTankW(FluidTankRenderer.TANK_W);
    }
}
