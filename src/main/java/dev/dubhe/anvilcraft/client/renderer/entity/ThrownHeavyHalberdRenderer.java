package dev.dubhe.anvilcraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.client.init.ModModelLayers;
import dev.dubhe.anvilcraft.entity.ThrownHeavyHalberdEntity;
import dev.dubhe.anvilcraft.entity.model.ThrownHeavyHalberdModel;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ThrownHeavyHalberdRenderer<T extends ThrownHeavyHalberdEntity> extends EntityRenderer<T> {
    private final ThrownHeavyHalberdModel<ThrownHeavyHalberdEntity> model;

    public ThrownHeavyHalberdRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new ThrownHeavyHalberdModel<>(context.bakeLayer(ModModelLayers.THROWN_HEAVY_HALBERD));
    }

    @Override
    public void render(T entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffer, int light) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, entity.yRotO, entity.getYRot()) - 90.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, entity.xRotO, entity.getXRot()) + 90.0F));
        pose.translate(0, -0.4, 0);
        VertexConsumer vertexconsumer = ItemRenderer.getFoilBufferDirect(
            buffer, this.model.renderType(this.getTextureLocation(entity)), false, entity.isFoil()
        );
        this.model.renderToBuffer(pose, vertexconsumer, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return entity.getTextureBase().withPrefix("textures/entity/heavy_halberd/").withSuffix(".png");
    }
}
