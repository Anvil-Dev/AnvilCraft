package dev.dubhe.anvilcraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.entity.ThrownHeavyHalberdEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;

public class ThrownHeavyHalberdRenderer<T extends ThrownHeavyHalberdEntity> extends EntityRenderer<T> {
    private final ItemRenderer itemRenderer;

    public ThrownHeavyHalberdRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(T entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffer, int light) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, entity.yRotO, entity.getYRot()) + 90.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(45.0F - Mth.lerp(partialTick, entity.xRotO, entity.getXRot())));
        pose.translate(0.31F, -0.31F, 0.0F);
        this.itemRenderer.renderStatic(
            entity.getWeaponItem(),
            ItemDisplayContext.FIXED,
            light,
            OverlayTexture.NO_OVERLAY,
            pose,
            buffer,
            entity.level(),
            entity.getId()
        );
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
