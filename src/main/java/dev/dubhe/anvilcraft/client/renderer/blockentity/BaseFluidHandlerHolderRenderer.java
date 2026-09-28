package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.fluid.IFluidResourceHandlerHolder;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.FluidHandlerRenderState;
import dev.dubhe.anvilcraft.client.support.FluidRenderHelper;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public abstract class BaseFluidHandlerHolderRenderer<B extends BlockEntity & IFluidResourceHandlerHolder, S extends FluidHandlerRenderState>
    implements BlockEntityRenderer<B, S> {

    @SuppressWarnings("deprecation")
    protected static final RenderType FLUID_RENDER_TYPE = RenderType.create(
        AnvilCraft.of("fluid_tank").toString(),
        RenderSetup.builder(RenderPipelines.TRANSLUCENT_BLOCK)
            .withTexture("Sampler0", TextureAtlas.LOCATION_BLOCKS)
            .useLightmap()
            .sortOnUpload()
            .createRenderSetup()
    );

    protected abstract void updateTankW(
        B be,
        S state,
        float partialTicks,
        Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    );

    protected float minimumFill() {
        return 0;
    }

    public float getFill(ResourceHandler<FluidResource> tank) {
        return (float) tank.getAmountAsLong(0) / tank.getCapacityAsLong(0, tank.getResource(0));
    }

    @Override
    public void extractRenderState(
        B be,
        S state,
        float partialTicks,
        Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        state.setResource(null);
        state.setFill(0.0F);
        ResourceHandler<FluidResource> tank = be.getFluidHandler();
        FluidResource resource = tank.getResource(0);
        if (resource.isEmpty()) return;
        state.setResource(resource);
        state.setAmount(tank.getAmountAsInt(0));
        state.setFill(this.getFill(tank));
        state.setFill(Math.max(this.minimumFill(), state.getFill()));
        this.updateTankW(be, state, partialTicks, cameraPosition, breakProgress);
    }

    @Override
    public void submit(S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        FluidResource resource = state.getResource();
        if (resource == null) return;
        float minY = state.getMinY();
        float maxY = minY + (state.getMaxY() - minY) * state.getFill();
        FluidRenderHelper.submitFluidBox(resource, state.getAmount(), state.getMinX(), minY, state.getMinZ(),
            state.getMaxX(), maxY, state.getMaxZ(), 1, poseStack, submitNodeCollector, state.lightCoords, FLUID_RENDER_TYPE);
    }
}
