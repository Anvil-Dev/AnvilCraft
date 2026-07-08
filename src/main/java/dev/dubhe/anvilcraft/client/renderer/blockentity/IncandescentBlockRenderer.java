package dev.dubhe.anvilcraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.lib.v2.rendering.ALRPostEffects;
import dev.dubhe.anvilcraft.block.entity.heatable.IncandescentBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.RenderState;
import dev.dubhe.anvilcraft.client.renderer.blockentity.state.IncandescentBlockRenderState;
import dev.dubhe.anvilcraft.client.support.FeatureRendererSupport;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class IncandescentBlockRenderer implements BlockEntityRenderer<IncandescentBlockEntity, IncandescentBlockRenderState> {
    private static final float BLOOM_SCALE = 1.0025F;

    @SuppressWarnings("unused")
    public IncandescentBlockRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public IncandescentBlockRenderState createRenderState() {
        return new IncandescentBlockRenderState();
    }

    @Override
    public void extractRenderState(
        IncandescentBlockEntity be,
        IncandescentBlockRenderState state,
        float partialTicks,
        Vec3 cameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        state.setModel(FeatureRendererSupport.initialize(be.getBlockState(), be));
    }

    @Override
    public void submit(
        IncandescentBlockRenderState state,
        PoseStack pose,
        SubmitNodeCollector collector,
        CameraRenderState camera
    ) {
        if (!RenderState.isEnhancedRenderingAvailable() || !RenderState.isBloomEffectEnabled()) return;

        ALRPostEffects.getBloomPostEffect().drawBloomed((bloomCollector, bloomPose) -> {
            bloomPose.pushPose();
            bloomPose.translate(
                state.blockPos.getX() - camera.pos.x,
                state.blockPos.getY() - camera.pos.y,
                state.blockPos.getZ() - camera.pos.z
            );
            bloomPose.translate(0.5F, 0.5F, 0.5F);
            bloomPose.scale(BLOOM_SCALE, BLOOM_SCALE, BLOOM_SCALE);
            bloomPose.translate(-0.5F, -0.5F, -0.5F);
            state.getModel().submit(
                bloomPose,
                bloomCollector,
                LightCoordsUtil.pack(15, 15),
                OverlayTexture.NO_OVERLAY,
                0
            );
            bloomPose.popPose();
        });
    }
}
