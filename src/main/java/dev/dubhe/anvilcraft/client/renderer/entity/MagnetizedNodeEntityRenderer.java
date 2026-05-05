package dev.dubhe.anvilcraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.init.ModModelLayers;
import dev.dubhe.anvilcraft.entity.MagnetizedNodeEntity;
import dev.dubhe.anvilcraft.entity.model.MagnetizedNodeModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public class MagnetizedNodeEntityRenderer extends EntityRenderer<MagnetizedNodeEntity> {
    public static final Identifier MAGNETIZED_NODE_TEXTURE = AnvilCraft.of("textures/entity/magnetized_node.png");

    private final MagnetizedNodeModel model;

    public MagnetizedNodeEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = new MagnetizedNodeModel(context.bakeLayer(ModModelLayers.MAGNETIZED_NODE));
    }

    @Override
    public Identifier getTextureLocation(MagnetizedNodeEntity magnetizedNodeEntity) {
        return MAGNETIZED_NODE_TEXTURE;
    }

    @Override
    public void render(
        MagnetizedNodeEntity entity,
        float entityYaw,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedLight
    ) {
        poseStack.pushPose();
        poseStack.translate(0, -1.31F, 0);
        float ageInTicks = entity.tickCount + partialTick;
        model.setupAnim(entity, 0F, 0F, ageInTicks, 0F, 0F);
        VertexConsumer consumer = bufferSource.getBuffer(model.renderType(MAGNETIZED_NODE_TEXTURE));
        this.model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
}
