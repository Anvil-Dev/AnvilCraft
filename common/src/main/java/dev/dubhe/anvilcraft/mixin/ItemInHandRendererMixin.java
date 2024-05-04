package dev.dubhe.anvilcraft.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
                ci.cancel();
                return;
            }
            ModelResourceLocation location =
                    this.mainHandItem.getItem() instanceof BlockItem
                            ? anvilCraft$HOLDING_BLOCK : anvilCraft$HOLDING_ITEM;
            this.itemRenderer.render(
                    this.offHandItem,
                    ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, false,
                    poseStack, buffer,
                    combinedLight,
                    OverlayTexture.NO_OVERLAY,
                    this.itemRenderer.getItemModelShaper().getModelManager().getModel(location)
            );
            poseStack.translate(0.1, 0.3, -0.3);
        }
    }
}
