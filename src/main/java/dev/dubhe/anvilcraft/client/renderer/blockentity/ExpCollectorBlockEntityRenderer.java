package dev.dubhe.anvilcraft.client.renderer.blockentity;

import dev.dubhe.anvilcraft.block.entity.ExpCollectorBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.FluidHandlerRenderState;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class ExpCollectorBlockEntityRenderer
    extends BaseFluidHandlerHolderRenderer<ExpCollectorBlockEntity, FluidHandlerRenderState> {
    public ExpCollectorBlockEntityRenderer(BlockEntityRendererProvider.Context ignored) {
    }

    @Override
    public FluidHandlerRenderState createRenderState() {
        return new FluidHandlerRenderState();
    }

    @Override
    protected void updateTankW(
        ExpCollectorBlockEntity blockEntity,
        FluidHandlerRenderState state,
        float partialTicks,
        Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        state.setMinX(0.07F);
        state.setMinY(0.15F);
        state.setMinZ(0.07F);
        state.setMaxX(0.93F);
        state.setMaxY(0.75F);
        state.setMaxZ(0.93F);
    }
}
