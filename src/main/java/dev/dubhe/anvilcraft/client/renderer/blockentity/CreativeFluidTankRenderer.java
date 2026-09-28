package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.entity.CreativeFluidTankBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.FluidTankRenderUtil;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.FluidHandlerRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.Nullable;

public class CreativeFluidTankRenderer extends BaseFluidHandlerHolderRenderer<CreativeFluidTankBlockEntity, FluidHandlerRenderState> {
    private static final float TANK_W = 1 / 16F + 0.001F;

    public CreativeFluidTankRenderer(BlockEntityRendererProvider.Context ignored) {
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
        return 1.0F;
    }

    @Override
    public void submit(FluidHandlerRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.getResource() == null) return;
        FluidTankRenderUtil.submit(state.getResource(), state.getAmount(), state.getFill(), pose, collector,
            state.lightCoords, FLUID_RENDER_TYPE);
    }

    @Override
    protected void updateTankW(
        CreativeFluidTankBlockEntity be,
        FluidHandlerRenderState state,
        float partialTicks,
        Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        state.setTankW(CreativeFluidTankRenderer.TANK_W);
    }
}
