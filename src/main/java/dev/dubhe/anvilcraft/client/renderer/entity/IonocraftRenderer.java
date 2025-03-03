package dev.dubhe.anvilcraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.init.ModModelLayers;
import dev.dubhe.anvilcraft.entity.IonocraftEntity;
import dev.dubhe.anvilcraft.entity.model.IonocraftModel;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class IonocraftRenderer extends EntityRenderer<IonocraftEntity> {
    public static final ResourceLocation TEXTURE = AnvilCraft.of("textures/entity/ionocraft.png");
    private final IonocraftModel<IonocraftEntity> model;

    public IonocraftRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = new IonocraftModel<>(context.bakeLayer(ModModelLayers.IONOCRAFT));
    }

    @Override
    public void render(
        IonocraftEntity pEntity,
        float entityYaw,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedLight
    ) {
        poseStack.pushPose();
        poseStack.scale(1, -1, 1);
        poseStack.translate(0, -1.5f, 0);
        model.setupAnim(pEntity, 0, 0, 0, 0, 0);
        VertexConsumer consumer = bufferSource.getBuffer(model.renderType(this.getTextureLocation(pEntity)));
        model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(IonocraftEntity entity) {
        return TEXTURE;
    }
}
