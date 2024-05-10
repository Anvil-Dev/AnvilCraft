package dev.dubhe.anvilcraft.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
abstract class ItemInHandRendererMixin {

    @Unique
    private static final ModelResourceLocation anvilCraft$HOLDING_ITEM =
            new ModelResourceLocation("anvilcraft", "crab_claw_holding_item", "inventory");
    @Unique
    private static final ModelResourceLocation anvilCraft$HOLDING_BLOCK =
            new ModelResourceLocation("anvilcraft", "crab_claw_holding_block", "inventory");

    @Shadow private ItemStack offHandItem;
    @Shadow private ItemStack mainHandItem;

    @Shadow @Final private ItemRenderer itemRenderer;

    @Shadow public abstract void renderItem(
            LivingEntity entity,
            ItemStack itemStack,
            ItemDisplayContext displayContext,
            boolean leftHand,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int seed
    );

    @Redirect(
            method = "renderArmWithItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z",
                    ordinal = 0)
    )
    private boolean isEmpty(ItemStack instance) {
        if (this.offHandItem.is(ModItems.CRAB_CLAW.get())) return false;
        return instance.isEmpty();
    }

    @Inject(
            method = "renderArmWithItem",
            at = @At(
                    value = "INVOKE",
                    ordinal = 1,
                    target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;"
                            + "renderItem(Lnet/minecraft/world/entity/LivingEntity;"
                            + "Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;"
                            + "ZLcom/mojang/blaze3d/vertex/PoseStack;"
                            + "Lnet/minecraft/client/renderer/MultiBufferSource;I)V"
            ),
            cancellable = true
    )
    private void renderOffHandCrabClaw(
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
        if (this.offHandItem.is(ModItems.CRAB_CLAW.get()) && !this.mainHandItem.is(ModItems.CRAB_CLAW.get())) {
            if (hand == InteractionHand.OFF_HAND) {
                poseStack.popPose();
                ci.cancel();
                return;
            }
            if (this.mainHandItem.isEmpty()) {
                this.renderItem(
                        player,
                        this.offHandItem,
                        ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
                        false,
                        poseStack,
                        buffer,
                        combinedLight
                );
                return;
            }
            boolean isBlockItem =
                    this.itemRenderer.getModel(this.mainHandItem, player.level(), player, combinedLight).isGui3d()
                    && this.mainHandItem.getItem() instanceof BlockItem;
            switch (stack.getUseAnimation()) {
                case EAT:
                case DRINK:
                    if (
                            player.isUsingItem()
                            && player.getUseItemRemainingTicks() > 0
                            && player.getUsedItemHand() == hand
                    ) {
                        poseStack.translate(0, -0.25, 0.05);
                    }
                    break;
                case NONE:
                    break;
                default:
                    return;
            }
            this.itemRenderer.render(
                    this.offHandItem,
                    ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, false,
                    poseStack, buffer,
                    combinedLight,
                    OverlayTexture.NO_OVERLAY,
                    this.itemRenderer.getItemModelShaper().getModelManager().getModel(
                            isBlockItem ? anvilCraft$HOLDING_BLOCK : anvilCraft$HOLDING_ITEM
                    )
            );
            if (isBlockItem) {
                poseStack.mulPose(Axis.YP.rotationDegrees(60f));
                poseStack.mulPose(Axis.XP.rotationDegrees(25f));
                poseStack.scale(0.5f, 0.5f, 0.5f);
                poseStack.translate(0.25, 0.4, -0.1);
            } else {
                poseStack.mulPose(Axis.ZP.rotationDegrees(5f));
                poseStack.translate(0, 0.3, -0.05);
            }
        }
    }
}
