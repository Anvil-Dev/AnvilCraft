package dev.dubhe.anvilcraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.client.renderer.entity.model.CauldronOutletModel;
import dev.dubhe.anvilcraft.entity.CauldronOutletEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class CauldronOutletRenderer extends EntityRenderer<CauldronOutletEntity> {
    private final CauldronOutletModel model;

    public CauldronOutletRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new CauldronOutletModel(context.bakeLayer(CauldronOutletModel.LAYER_LOCATION));
    }

    @Override
    public void render(
        CauldronOutletEntity entity,
        float entityYaw,
        float partialTicks,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight
    ) {
        poseStack.pushPose();

        Direction direction = entity.getAttachedDirection();
        switch (direction) {
            case DOWN -> {
                poseStack.translate(0.0, 0.125, 0.0);
                poseStack.mulPose(Axis.ZP.rotationDegrees(180));
            }
            case NORTH -> {
                poseStack.translate(0.0, 0.18375, 0.0);
                poseStack.mulPose(Axis.YN.rotationDegrees(90));
                poseStack.mulPose(Axis.ZP.rotationDegrees(120));
            }
            case SOUTH -> {
                poseStack.translate(0.0, 0.18375, 0.0);
                poseStack.mulPose(Axis.YN.rotationDegrees(90));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-120));
            }
            case WEST -> {
                poseStack.translate(0.0, 0.18375, 0.0);
                poseStack.mulPose(Axis.ZP.rotationDegrees(120));
            }
            case EAST -> {
                poseStack.translate(0.0, 0.18375, 0.0);
                poseStack.mulPose(Axis.ZP.rotationDegrees(-120));
            }
            default -> {
                break;
            }
        }
        poseStack.scale(0.73f, 0.73f, 0.73f);

        var consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(entity)));
        this.model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, -1);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(CauldronOutletEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("anvilcraft", "textures/block/cauldron_outlet.png");
    }
}