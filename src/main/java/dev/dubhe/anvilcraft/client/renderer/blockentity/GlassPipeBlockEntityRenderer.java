package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.entity.fluid.GlassPipeBlockEntity;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.GlassPipeRenderState;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.PipeCheckValveRenderState;
import dev.dubhe.anvilcraft.client.support.FluidRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.Nullable;

import java.util.Set;

public class GlassPipeBlockEntityRenderer extends PipeCheckValveBERenderer<GlassPipeBlockEntity> {
    public GlassPipeBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public GlassPipeRenderState createRenderState() {
        return new GlassPipeRenderState();
    }

    @Override
    public void extractRenderState(
        GlassPipeBlockEntity be, PipeCheckValveRenderState base, float partialTicks, Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        super.extractRenderState(be, base, partialTicks, cameraPosition, breakProgress);
        var state = (GlassPipeRenderState) base;
        state.fluid = FluidResource.of(be.getDisplayFluid());
        state.blockState = be.getBlockState();
        state.directions = Set.copyOf(be.getDisplayDirections());
        state.alpha = be.isShowingGas() ? be.getGasAlpha() : 1.0F;
        state.sprite = null;
        if (state.fluid.isEmpty()) return;
        var model = FluidRenderHelper.getModel(Minecraft.getInstance().getModelManager().getFluidStateModelSet(), state.fluid.getFluid());
        var tint = model.fluidTintSource();
        state.color = tint == null ? -1 : tint.colorAsStack(state.fluid.toStack(1));
        state.sprite = model.stillMaterial().sprite();
        state.opaque = state.fluid.getFluid() == NeoForgeMod.MILK.value();
    }

    @Override
    public void submit(PipeCheckValveRenderState base, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(base, pose, collector, camera);
        var state = (GlassPipeRenderState) base;
        if (state.fluid.isEmpty() || state.directions.isEmpty() || state.sprite == null) return;
        collector.submitCustomGeometry(pose, state.opaque ? ModRenderTypes.CUTOUT_BLOCK : ModRenderTypes.GLASS_PIPE_FLUID,
            (matrix, vertices) -> GlassPipeFluidBERenderer.renderDisplayFluid(
                state, state.blockState, state.directions, state.alpha, matrix, vertices, state.lightCoords));
    }
}
