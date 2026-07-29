package dev.dubhe.anvilcraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.client.renderer.entity.state.FluidTankMinecartRenderState;
import dev.dubhe.anvilcraft.client.renderer.item.state.FluidTankItemRenderState;
import dev.dubhe.anvilcraft.client.support.FluidRenderHelper;
import dev.dubhe.anvilcraft.entity.FluidTankMinecartEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.entity.AbstractMinecartRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.Nullable;

/// 在储罐矿车上渲染出罐内流体
public class FluidTankMinecartRenderer
    extends AbstractMinecartRenderer<FluidTankMinecartEntity, FluidTankMinecartRenderState> {
    /// 与流体储罐方块实体渲染保持一致的壁厚，避免与外壳 Z-fighting
    private static final float TANK_W = 1 / 16F + 0.001F;

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
        state.setFill(Mth.clamp((float) fluid.getAmount() / entity.getCapacity(), 0.025F, 1.0F));
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
        FluidModel model = FluidRenderHelper.getModel(
            Minecraft.getInstance().getModelManager().getFluidStateModelSet(),
            resource.getFluid()
        );
        var tintSource = model.fluidTintSource();
        int tintColor = tintSource != null ? tintSource.colorAsStack(resource.toStack(1)) : -1;
        TextureAtlasSprite sprite = model.stillMaterial().sprite();
        float maxY = TANK_W + (1 - 2 * TANK_W) * state.getFill();
        collector.submitCustomGeometry(
            poseStack,
            FluidTankItemRenderState.FLUID_RENDER_TYPE,
            (pose, buffer) -> FluidRenderHelper.INSTANCE.renderFluidBox(
                sprite,
                resource,
                TANK_W,
                TANK_W,
                TANK_W,
                1 - TANK_W,
                maxY,
                1 - TANK_W,
                tintColor,
                buffer,
                pose,
                lightCoords,
                true,
                false
            )
        );
    }
}
