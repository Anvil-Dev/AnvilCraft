package dev.dubhe.anvilcraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.init.ModModelLayers;
import dev.dubhe.anvilcraft.client.renderer.entity.model.MagnetizedNodeModel;
import dev.dubhe.anvilcraft.client.renderer.entity.state.MagnetizedNodeRenderState;
import dev.dubhe.anvilcraft.entity.MagnetizedNodeEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public class MagnetizedNodeEntityRenderer extends EntityRenderer<MagnetizedNodeEntity, MagnetizedNodeRenderState> {
    public static final Identifier TEXTURE = AnvilCraft.of("textures/entity/magnetized_node.png");

    private final MagnetizedNodeModel model;

    public MagnetizedNodeEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new MagnetizedNodeModel(context.bakeLayer(ModModelLayers.MAGNETIZED_NODE));
    }

    @Override
    public MagnetizedNodeRenderState createRenderState() {
        return new MagnetizedNodeRenderState();
    }

    @Override
    public void extractRenderState(MagnetizedNodeEntity entity, MagnetizedNodeRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.getRotation().copyFrom(entity.rotatingState);
    }

    @Override
    public void submit(
        MagnetizedNodeRenderState state,
        PoseStack pose,
        SubmitNodeCollector collector,
        CameraRenderState camera
    ) {
        pose.pushPose();
        pose.translate(0, -1.31F, 0);
        collector.order(0).submitModel(
            this.model,
            state,
            pose,
            TEXTURE,
            state.lightCoords,
            OverlayTexture.NO_OVERLAY,
            state.outlineColor,
            null
        );
        pose.popPose();
        super.submit(state, pose, collector, camera);
    }
}
