package dev.dubhe.anvilcraft.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.block.item.HasMobBlockItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

public class HasMobBlockItemRenderer extends AbstractItemInHandRenderer {
    protected HasMobBlockItemRenderer(ItemRenderer itemRenderer, IItemRenderer renderer) {
        super(itemRenderer, renderer);
    }

    public static void renderGuiItem(
        PoseStack pose,
        ItemStack stack,
        int x,
        int y
    ) {
        Entity mobFromItem = HasMobBlockItemRenderer.getMobFromItem(stack);
        if (mobFromItem == null) {
            return;
        }
        pose.pushPose();
        pose.translate(x + 8, y + 8, 160.0f);
        pose.scale(16.0f, -16.0f, 16.0f);
        pose.mulPose(Axis.XP.rotationDegrees(30));
        pose.mulPose(Axis.YP.rotationDegrees(-45));
        float size = 0.52943f * 0.8f;
        float max = Math.max(mobFromItem.getBbWidth(), mobFromItem.getBbHeight());
        if ((double) max > 1.0) {
            size /= max;
        }
        pose.translate(0.0f, -0.19f, 0.0f);
        pose.scale(size, size, size);
        EntityRenderer<? super Entity> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(mobFromItem);
        renderer.render(
            mobFromItem,
            0,
            0,
            pose,
            Minecraft.getInstance().renderBuffers().bufferSource(),
            LightTexture.FULL_BRIGHT
        );
        pose.popPose();
    }

    @Override
    public void render(
        AbstractClientPlayer player,
        float partialTicks,
        float pitch,
        InteractionHand hand,
        float swingProgress,
        ItemStack stack,
        float equippedProgress,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int combinedLight,
        CallbackInfo ci
    ) {
        Entity mobFromItem = HasMobBlockItemRenderer.getMobFromItem(stack);
        if (mobFromItem == null) {
            return;
        }
        poseStack.translate(0.0f, 0.14f, 0.0f);
        poseStack.pushPose();
        float size = 0.52943f * 0.4f * 0.8f;
        float max = Math.max(mobFromItem.getBbWidth(), mobFromItem.getBbHeight());
        if ((double) max > 1.0) {
            size /= max;
        }
        poseStack.translate(0.0f, -0.14f, 0.0f);
        poseStack.scale(size, size, size);
        if (hand == InteractionHand.OFF_HAND) {
            poseStack.mulPose(Axis.YP.rotationDegrees(45));
        } else {
            poseStack.mulPose(Axis.YP.rotationDegrees(-45));
        }
        EntityRenderer<? super Entity> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(mobFromItem);
        renderer.render(
            mobFromItem,
            0,
            0,
            poseStack,
            Minecraft.getInstance().renderBuffers().bufferSource(),
            LightTexture.FULL_BRIGHT
        );
        poseStack.popPose();
    }

    public static @Nullable Entity getMobFromItem(ItemStack stack) {
        if (!(stack.getItem() instanceof HasMobBlockItem)) {
            return null;
        }
        if (Minecraft.getInstance().level == null) {
            return null;
        }
        return HasMobBlockItem.getMobFromItem(Minecraft.getInstance().level, stack);
    }
}
