package dev.dubhe.anvilcraft.client.renderer.laser;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.lib.v2.rendering.cachedber.renderer.CachedBlockEntityRenderer;
import dev.dubhe.anvilcraft.block.entity.BaseLaserBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.RenderState;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.LaserRenderState;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;

public class CachedLaserBlockEntityRenderer<T extends BaseLaserBlockEntity> implements CachedBlockEntityRenderer<T, LaserRenderState> {

    @Override
    public LaserRenderState createRenderState() {
        return new LaserRenderState();
    }

    @Override
    public LaserRenderState extractRenderState(BaseLaserBlockEntity blockEntity, LaserRenderState state, float partialTicks, Camera camera) {
        LaserRenderState.extractBase(blockEntity, state, null);
        state.extract(blockEntity);
        return state;
    }

    @Override
    public void submit(LaserRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (!RenderState.isEnhancedRenderingAvailable()) return;
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
}
