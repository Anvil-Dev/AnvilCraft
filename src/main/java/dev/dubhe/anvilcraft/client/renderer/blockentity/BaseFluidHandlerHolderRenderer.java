package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.api.fluid.IFluidHandlerHolder;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.FluidHandlerRenderState;
import dev.dubhe.anvilcraft.client.support.FluidRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public abstract class BaseFluidHandlerHolderRenderer<B extends BlockEntity & IFluidHandlerHolder, S extends FluidHandlerRenderState>
    implements BlockEntityRenderer<B, S> {

    protected abstract void updateTankW(
        B be,
        S state,
        float partialTicks,
        Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    );

    @Override
    public void extractRenderState(
        B be,
        S state,
        float partialTicks,
        Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        ResourceHandler<FluidResource> tank = be.getFluidHandler();
        FluidResource resource = tank.getResource(0);
        if (resource.isEmpty()) return;
        state.setResource(resource);
        state.setFill((float) tank.getAmountAsInt(0) / tank.getCapacityAsInt(0, resource));
        if (state.getFill() <= 0.025) state.setFill(0.025F);
        this.updateTankW(be, state, partialTicks, cameraPosition, breakProgress);
    }

    @Override
    public void submit(S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        FluidResource resource = state.getResource();
        if (resource == null) return;
        FluidModel model = FluidRenderHelper.getModel(
            Minecraft.getInstance().getModelManager().getFluidStateModelSet(),
            resource.getFluid()
        );
        TextureAtlasSprite sprite = model.stillMaterial().sprite();
        int tintColor = model.fluidTintSource().colorAsStack(resource.toStack(1));
        submitNodeCollector.submitCustomGeometry(
            poseStack,
            RenderTypes.entityTranslucent(sprite.atlasLocation()),
            (pose, buffer) -> FluidRenderHelper.INSTANCE.renderFluidBox(
                sprite,
                resource,
                state.getMinX(),
                state.getMinY(),
                state.getMinZ(),
                state.getMaxX(),
                state.getMaxY(),
                state.getMaxZ(),
                tintColor,
                buffer,
                pose,
                state.lightCoords,
                false,
                false
            )
        );
    }
}
