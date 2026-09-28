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
import dev.dubhe.anvilcraft.client.renderer.LargeFluidTankRenderUtil;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.LayeredFluidTankRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class LargeFluidTankRenderer implements BlockEntityRenderer<LargeFluidTankBlockEntity, LayeredFluidTankRenderState> {
    public LargeFluidTankRenderer(BlockEntityRendererProvider.Context ignored) {
    }

    @Override
    public LayeredFluidTankRenderState createRenderState() {
        return new LayeredFluidTankRenderState();
    }

    @Override
    public boolean shouldRender(LargeFluidTankBlockEntity blockEntity, Vec3 cameraPosition) {
        return blockEntity.isMainPart() && BlockEntityRenderer.super.shouldRender(blockEntity, cameraPosition);
    }

    @Override
    public void extractRenderState(
        LargeFluidTankBlockEntity be, LayeredFluidTankRenderState state, float partialTicks,
        Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        state.layers = be.isMainPart() ? LargeFluidTankRenderUtil.layers(be.getStoredFluids(), be.isEnhanced()) : List.of();
    }

    @Override
    public void submit(LayeredFluidTankRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        for (var layer : state.layers) {
            LargeFluidTankRenderUtil.submit(layer, true, pose, collector, state.lightCoords,
                BaseFluidHandlerHolderRenderer.FLUID_RENDER_TYPE);
        }
    }

    @Override
    public AABB getRenderBoundingBox(LargeFluidTankBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(1);
    }
}
