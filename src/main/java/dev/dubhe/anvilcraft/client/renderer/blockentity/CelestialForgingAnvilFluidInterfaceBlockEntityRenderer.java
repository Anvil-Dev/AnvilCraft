package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.block.cfa.interfaces.CelestialForgingAnvilInterfaceBlock;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilFluidInterfaceBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.FluidRenderLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CelestialForgingAnvilFluidInterfaceBlockEntityRenderer implements
    BlockEntityRenderer<CelestialForgingAnvilFluidInterfaceBlockEntity, CelestialForgingAnvilFluidInterfaceBlockEntityRenderer.State> {
    private static final int DISPLAY_CAPACITY = 80_000;

    public CelestialForgingAnvilFluidInterfaceBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
        CelestialForgingAnvilFluidInterfaceBlockEntity entity, State state, float partialTick,
        Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTick, cameraPosition, breakProgress);
        state.facing = entity.getBlockState().getValue(CelestialForgingAnvilInterfaceBlock.FACING);
        List<FluidStack> fluids = new ArrayList<>();
        var tank = entity.getTank();
        for (int slot = 0; slot < tank.size(); slot++) {
            if (!tank.getResource(slot).isEmpty()) fluids.add(tank.getResource(slot).toStack(tank.getAmountAsInt(slot)));
        }
        long total = fluids.stream().mapToLong(FluidStack::getAmount).sum();
        state.layers = FluidRenderLayers.create(FluidRenderLayers.sorted(fluids), Math.max(total, DISPLAY_CAPACITY));
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(180 - state.facing.toYRot()));
        pose.translate(-0.5, 0, -0.5);
        for (var layer : state.layers) {
            FluidRenderLayers.submit(layer, 5 / 16F + 0.001F, 17 / 16F + 0.001F, 7 / 16F + 0.001F,
                11 / 16F - 0.001F, 21 / 16F - 0.001F, 13 / 16F - 0.001F,
                pose, collector, state.lightCoords, BaseFluidHandlerHolderRenderer.FLUID_RENDER_TYPE);
        }
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(CelestialForgingAnvilFluidInterfaceBlockEntity entity) {
        return new AABB(entity.getBlockPos()).expandTowards(0, 5 / 16D, 0);
    }

    public static class State extends BlockEntityRenderState {
        private Direction facing = Direction.NORTH;
        private List<FluidRenderLayers.Layer> layers = List.of();
    }
}
