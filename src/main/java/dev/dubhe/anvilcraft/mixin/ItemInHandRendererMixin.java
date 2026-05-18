package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.client.renderer.item.ItemInHandRendererManager;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
abstract class ItemInHandRendererMixin {
    @Shadow
    private ItemStack offHandItem;

    @Shadow
    private ItemStack mainHandItem;

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

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(Minecraft minecraft, EntityRenderDispatcher entityRenderDispatcher, ItemModelResolver itemModelResolver, CallbackInfo ci) {
        this.anvilcraft$manager = new ItemInHandRendererManager(itemModelResolver, this::renderItem);
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
