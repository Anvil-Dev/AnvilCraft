package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.entity.StorageFluidPortBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.FluidHandlerRenderState;
import dev.dubhe.anvilcraft.client.support.FluidRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class StorageFluidPortBlockEntityRenderer
    extends BaseFluidHandlerHolderRenderer<StorageFluidPortBlockEntity, FluidHandlerRenderState> {
    public StorageFluidPortBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public FluidHandlerRenderState createRenderState() {
        return new FluidHandlerRenderState();
    }

    @Override
    protected void updateTankW(
        StorageFluidPortBlockEntity port, FluidHandlerRenderState state, float partialTick, Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        state.setTankW(3 / 16F + 0.001F);
    }

    @Override
    public void extractRenderState(
        StorageFluidPortBlockEntity port, FluidHandlerRenderState state, float partialTick, Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        super.extractRenderState(port, state, partialTick, cameraPosition, breakProgress);
        state.setFill(Mth.clamp((float) port.getFluid().getAmount() / StorageFluidPortBlockEntity.CAPACITY_MB, 0, 1));
    }

    @Override
    public void submit(FluidHandlerRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        var fluid = state.getResource();
        if (fluid == null || state.getFill() <= 0) return;
        if (!fluid.getFluidType().isLighterThanAir()) {
            super.submit(state, pose, collector, camera);
            return;
        }
        var model = FluidRenderHelper.getModel(Minecraft.getInstance().getModelManager().getFluidStateModelSet(), fluid.getFluid());
        var tint = model.fluidTintSource();
        int color = tint == null ? -1 : tint.colorAsStack(fluid.toStack(1));
        var sprite = model.stillMaterial().sprite();
        collector.submitCustomGeometry(pose, FLUID_RENDER_TYPE, (matrix, buffer) -> FluidRenderHelper.INSTANCE.renderFluidBox(
            sprite, fluid, state.getMinX(), state.getMinY(), state.getMinZ(), state.getMaxX(), state.getMaxY(), state.getMaxZ(),
            color, buffer, matrix, state.lightCoords, true, false, state.getFill()
        ));
    }
}
