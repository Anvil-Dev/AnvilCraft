package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.client.building.BuildingRodItemRenderer;
import dev.dubhe.anvilcraft.client.event.BigRedButtonInputListener;
import dev.dubhe.anvilcraft.client.renderer.item.ItemInHandRendererManager;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
abstract class ItemInHandRendererMixin {
    @Shadow
    private ItemStack offHandItem;

    @Shadow
    private ItemStack mainHandItem;

    @Shadow
    public abstract void renderItem(
        LivingEntity entity,
        ItemStack itemStack,
        ItemDisplayContext displayContext,
        boolean leftHand,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int seed
    );

    @Unique
    private ItemInHandRendererManager anvilcraft$manager = null;

    @ModifyExpressionValue(method = "renderHandsWithItems", at = @At(value = "FIELD",
        target = "Lnet/minecraft/client/renderer/ItemInHandRenderer$HandRenderSelection;renderOffHand:Z"))
    private boolean anvilcraft$showOffhandRod(boolean original, float partialTick, PoseStack pose,
                                             MultiBufferSource.BufferSource buffers, LocalPlayer player, int light) {
        return original || player.getOffhandItem().is(ModItems.BUILDING_ROD);
    }

    @Inject(method = "renderItem", at = @At("HEAD"), cancellable = true)
    private void anvilcraft$renderBuildingRod(
        LivingEntity entity, ItemStack stack, ItemDisplayContext context, boolean leftHand,
        PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci
    ) {
        boolean offhand = leftHand != (entity.getMainArm() == HumanoidArm.LEFT);
        if (BuildingRodItemRenderer.hideHand(entity, offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND)) {
            ci.cancel();
        } else if (stack.is(ModItems.BUILDING_ROD)) {
            BuildingRodItemRenderer.renderHeld(entity, stack, context, leftHand, pose, buffers, light);
            ci.cancel();
        }
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(Minecraft minecraft, EntityRenderDispatcher entityRenderDispatcher, ItemRenderer itemRenderer, CallbackInfo ci) {
        anvilcraft$manager = new ItemInHandRendererManager(itemRenderer, this::renderItem);
    }

    @ModifyVariable(method = "renderArmWithItem", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private float anvilcraft$holdButtonHand(
        float original, AbstractClientPlayer player, float partialTicks, float pitch, InteractionHand hand,
        float swingProgress, ItemStack stack, float equippedProgress, PoseStack poseStack, MultiBufferSource buffer, int combinedLight
    ) {
        return BigRedButtonInputListener.getHandSwingProgress(hand, partialTicks, original);
    }

    @WrapOperation(
        method = "renderArmWithItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z",
            ordinal = 0
        )
    )
    private boolean isEmpty(ItemStack instance, Operation<Boolean> original) {
        if (this.offHandItem.is(ModItems.CRAB_CLAW.get())) return false;
        return original.call(instance);
    }

    @Inject(
        method = "renderArmWithItem",
        at =
        @At(
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
    private void renderArmWithItem(
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
        if (this.anvilcraft$manager == null) return;
        this.anvilcraft$manager.setMainHandItem(this.mainHandItem);
        this.anvilcraft$manager.setOffHandItem(this.offHandItem);
        this.anvilcraft$manager.render(
            player,
            partialTicks,
            pitch,
            hand,
            swingProgress,
            stack,
            equippedProgress,
            poseStack,
            buffer,
            combinedLight,
            ci
        );
    }
}
