package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.block.entity.StoragePortBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.StoragePortRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class StoragePortBlockEntityRenderer implements BlockEntityRenderer<StoragePortBlockEntity, StoragePortRenderState> {
    private final ItemModelResolver resolver;

    public StoragePortBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.resolver = context.itemModelResolver();
    }

    @Override
    public StoragePortRenderState createRenderState() {
        return new StoragePortRenderState();
    }

    @Override
    public void extractRenderState(
        StoragePortBlockEntity port, StoragePortRenderState state, float partialTick, Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(port, state, partialTick, cameraPosition, breakProgress);
        state.visibleFaces = 0;
        state.markedItem.clear();
        if (port.getMarkedItem().isEmpty()) return;
        this.resolver.updateForTopItem(state.markedItem, port.getMarkedItem(), ItemDisplayContext.FIXED, port.getLevel(), null, 0);
        for (Direction direction : Direction.values()) {
            if (port.isMarkedFaceVisible(direction)) state.visibleFaces |= 1 << direction.ordinal();
        }
    }

    @Override
    public void submit(StoragePortRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        for (Direction direction : Direction.values()) {
            if ((state.visibleFaces & (1 << direction.ordinal())) == 0) continue;
            pose.pushPose();
            pose.translate(0.5 + direction.getStepX() * 0.4, 0.5 + direction.getStepY() * 0.4, 0.5 + direction.getStepZ() * 0.4);
            pose.scale(0.8F, 0.8F, 0.8F);
            if (direction.getAxis() == Direction.Axis.X) {
                pose.mulPose(Axis.YP.rotationDegrees(90));
            } else if (direction.getAxis() == Direction.Axis.Y) {
                pose.mulPose(Axis.XP.rotationDegrees(90));
                if (direction == Direction.UP) pose.mulPose(Axis.ZP.rotationDegrees(180));
            }
            state.markedItem.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
    }
}
