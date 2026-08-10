package dev.dubhe.anvilcraft.client.renderer.blockentity;

import dev.dubhe.anvilcraft.block.entity.CreativeFluidTankBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.FluidHandlerRenderState;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.phys.Vec3;
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
