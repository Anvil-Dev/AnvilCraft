package dev.dubhe.anvilcraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.client.renderer.FluidTankRenderUtil;
import dev.dubhe.anvilcraft.client.renderer.entity.state.FluidTankMinecartRenderState;
import dev.dubhe.anvilcraft.client.renderer.item.state.FluidTankItemRenderState;
import dev.dubhe.anvilcraft.entity.FluidTankMinecartEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.entity.AbstractMinecartRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

/// 在储罐矿车上渲染出罐内流体
public class FluidTankMinecartRenderer
    extends AbstractMinecartRenderer<FluidTankMinecartEntity, FluidTankMinecartRenderState> {
    public FluidTankMinecartRenderer(EntityRendererProvider.Context context) {
        super(context, ModelLayers.MINECART);
    }

    @Override
    public FluidTankMinecartRenderState createRenderState() {
        return new FluidTankMinecartRenderState();
    }

    @Override
    public void extractRenderState(
        FluidTankMinecartEntity entity,
        FluidTankMinecartRenderState state,
        float partialTicks
    ) {
        super.extractRenderState(entity, state, partialTicks);
        FluidStack fluid = entity.getSyncedFluid();
        if (fluid.isEmpty()) {
            state.setResource(null);
            return;
        }
        state.setResource(FluidResource.of(fluid));
        state.setAmount(fluid.getAmount());
        state.setFill(Mth.clamp((float) fluid.getAmount() / entity.getCapacity(), 0.0F, 1.0F));
    }

    @Override
    protected void submitMinecartContents(
        FluidTankMinecartRenderState state,
        BlockModelRenderState blockModel,
        PoseStack poseStack,
        SubmitNodeCollector collector,
        int lightCoords
    ) {
        super.submitMinecartContents(state, blockModel, poseStack, collector, lightCoords);
        FluidResource resource = state.getResource();
        if (resource == null || resource.isEmpty()) return;
        FluidTankRenderUtil.submit(resource, state.getAmount(), state.getFill(), poseStack, collector,
            lightCoords, FluidTankItemRenderState.FLUID_RENDER_TYPE);
    }
}
