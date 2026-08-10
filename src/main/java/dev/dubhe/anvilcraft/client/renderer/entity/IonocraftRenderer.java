package dev.dubhe.anvilcraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.client.init.ModModelLayers;
import dev.dubhe.anvilcraft.client.renderer.entity.model.IonocraftModel;
import dev.dubhe.anvilcraft.client.renderer.entity.state.IonocraftRenderState;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.entity.IonocraftEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public class IonocraftRenderer extends EntityRenderer<IonocraftEntity, IonocraftRenderState> {
    public static final Identifier TEXTURE = SharedTextures.texture("entity/ionocraft");
    private final IonocraftModel model;

    public IonocraftRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new IonocraftModel(context.bakeLayer(ModModelLayers.IONOCRAFT));
    }

    @Override
    public IonocraftRenderState createRenderState() {
        return new IonocraftRenderState();
    }

    @Override
    public void submit(IonocraftRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.scale(1, -1, 1);
        poseStack.translate(0, -1.5F, 0);
        collector.submitModel(
            this.model,
            state,
            poseStack,
            IonocraftRenderer.TEXTURE,
            state.lightCoords,
            OverlayTexture.NO_OVERLAY,
            state.outlineColor,
            null
        );
        poseStack.popPose();
    }
}
