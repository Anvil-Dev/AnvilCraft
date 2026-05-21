package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.entity.BaseLaserBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.RenderState;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.LaserRenderState;
import dev.dubhe.anvilcraft.client.renderer.laser.LaserCompiler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class LaserBlockEntityRenderer implements BlockEntityRenderer<BaseLaserBlockEntity, LaserRenderState> {
    @SuppressWarnings("unused")
    public LaserBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public AABB getRenderBoundingBox(BaseLaserBlockEntity blockEntity) {
        int renderDistance = Minecraft.getInstance().options.getEffectiveRenderDistance() * 16;
        return AABB.ofSize(
            blockEntity.getBlockPos().getCenter(),
            renderDistance * 2,
            renderDistance * 2,
            renderDistance * 2
        );
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public LaserRenderState createRenderState() {
        return new LaserRenderState();
    }

    @Override
    public void extractRenderState(
        BaseLaserBlockEntity blockEntity,
        LaserRenderState state,
        float partialTicks,
        Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.extract(blockEntity);
    }

    @Override
    public void submit(LaserRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (RenderState.isEnhancedRenderingAvailable()) return;
        if (state.blockEntity == null) return;
        poseStack.pushPose();
        poseStack.translate(0.5f, 0.5f, 0.5f);
        poseStack.mulPose(state.rotation);
        LaserCompiler.submit(
            poseStack,
            state,
            submitNodeCollector,
            false
        );
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }
}
