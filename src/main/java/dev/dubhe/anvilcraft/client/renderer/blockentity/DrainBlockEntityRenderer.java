package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.entity.fluid.DrainBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.DrainRenderState;
import dev.dubhe.anvilcraft.client.support.FluidRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.Nullable;

public class DrainBlockEntityRenderer
    extends BaseFluidHandlerHolderRenderer<DrainBlockEntity, DrainRenderState> {
    private static final float TANK_W = 1 / 16F + 0.001F;
    private static final float COLUMN_INSET = 5 / 16F;

    public DrainBlockEntityRenderer(BlockEntityRendererProvider.Context ignored) {
    }

    @Override
    public DrainRenderState createRenderState() {
        return new DrainRenderState();
    }

    @Override
    protected void updateTankW(
        DrainBlockEntity be,
        DrainRenderState state,
        float partialTicks,
        Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        state.setTankW(TANK_W);
        int bottomY = be.getColumnBottomY();
        if (bottomY != Integer.MIN_VALUE) {
            state.setColumnMinY(bottomY - be.getBlockPos().getY());
        }
    }

    @Override
    public void submit(
        DrainRenderState state,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        CameraRenderState camera
    ) {
        super.submit(state, poseStack, submitNodeCollector, camera);
        FluidResource resource = state.getResource();
        float minY = state.getColumnMinY();
        if (resource == null || minY >= 0) return;

        FluidModel model = FluidRenderHelper.getModel(
            Minecraft.getInstance().getModelManager().getFluidStateModelSet(),
            resource.getFluid()
        );
        var tintSource = model.fluidTintSource();
        int tintColor = tintSource != null ? tintSource.colorAsStack(resource.toStack(1)) : -1;
        TextureAtlasSprite flowing = model.flowingMaterial().sprite();
        submitNodeCollector.submitCustomGeometry(
            poseStack,
            FLUID_RENDER_TYPE,
            (pose, buffer) -> FluidRenderHelper.INSTANCE.renderFluidBox(
                flowing,
                resource,
                COLUMN_INSET,
                minY,
                COLUMN_INSET,
                1 - COLUMN_INSET,
                0,
                1 - COLUMN_INSET,
                tintColor,
                buffer,
                pose,
                state.lightCoords,
                false,
                false
            )
        );
    }

    @Override
    public AABB getRenderBoundingBox(DrainBlockEntity be) {
        BlockPos pos = be.getBlockPos();
        AABB self = new AABB(pos);
        int bottomY = be.getColumnBottomY();
        if (bottomY == Integer.MIN_VALUE || bottomY >= pos.getY()) return self;
        return self.expandTowards(0, bottomY - pos.getY(), 0);
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public boolean shouldRender(DrainBlockEntity be, Vec3 cameraPos) {
        double viewDistance = this.getViewDistance();
        return this.getRenderBoundingBox(be).distanceToSqr(cameraPos) <= viewDistance * viewDistance;
    }
}
