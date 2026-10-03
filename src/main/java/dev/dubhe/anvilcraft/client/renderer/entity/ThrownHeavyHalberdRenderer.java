package dev.dubhe.anvilcraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.client.renderer.entity.state.ThrownHeavyHalberdRenderState;
import dev.dubhe.anvilcraft.entity.ThrownHeavyHalberdEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class ThrownHeavyHalberdRenderer<T extends ThrownHeavyHalberdEntity> extends EntityRenderer<T, ThrownHeavyHalberdRenderState> {
    private final ItemModelResolver itemModelResolver;

    public ThrownHeavyHalberdRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemModelResolver = context.getItemModelResolver();
    }

    @Override
    public ThrownHeavyHalberdRenderState createRenderState() {
        return new ThrownHeavyHalberdRenderState();
    }

    @Override
    public void extractRenderState(T entity, ThrownHeavyHalberdRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.setYaw(entity.getYRot(partialTicks));
        state.setPitch(entity.getXRot(partialTicks));
        ItemStack stack = entity.getWeaponItem().copy();
        if (!stack.isEmpty()) {
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, entity.isFoil());
        }
        this.itemModelResolver.updateForNonLiving(state.getItem(), stack, ItemDisplayContext.FIXED, entity);
    }

    @Override
    public void submit(ThrownHeavyHalberdRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(state.getYaw() + 90.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(45.0F - state.getPitch()));
        pose.translate(0.31F, -0.31F, 0.0F);
        state.getItem().submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        pose.popPose();
        super.submit(state, pose, collector, camera);
    }
}
