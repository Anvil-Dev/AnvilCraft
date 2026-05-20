package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.client.renderer.item.ItemInHandRendererManager;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
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

    @Shadow
    public abstract void renderItem(
        LivingEntity entity,
        ItemStack itemStack,
        ItemDisplayContext displayContext,
        PoseStack poseStack,
        SubmitNodeCollector collector,
        int lightCoords
    );

    @Unique
    private ItemInHandRendererManager anvilcraft$manager = null;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(
        Minecraft minecraft,
        EntityRenderDispatcher entityRenderDispatcher,
        ItemModelResolver itemModelResolver,
        CallbackInfo ci
    ) {
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
        if (this.offHandItem.is(ModItems.CRAB_CLAW.get())) {
            return false;
        }
        return original.call(instance);
    }

    @Inject(
        method = "renderArmWithItem",
        at =
        @At(
            value = "INVOKE",
            ordinal = 1,
            target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;"
                + "renderItem("
                + "Lnet/minecraft/world/entity/LivingEntity;"
                + "Lnet/minecraft/world/item/ItemStack;"
                + "Lnet/minecraft/world/item/ItemDisplayContext;"
                + "Lcom/mojang/blaze3d/vertex/PoseStack;"
                + "Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"
        ),
        cancellable = true
    )
    @SuppressWarnings("NameDoesntMatchTargetClass")
    private void renderArmWithItem(
        AbstractClientPlayer player,
        float frameInterp,
        float pitch,
        InteractionHand hand,
        float attack,
        ItemStack itemStack,
        float inverseArmHeight,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        int lightCoords,
        CallbackInfo ci
    ) {
        if (this.anvilcraft$manager == null) return;
        this.anvilcraft$manager.setMainHandItem(this.mainHandItem);
        this.anvilcraft$manager.setOffHandItem(this.offHandItem);
        boolean rendered = this.anvilcraft$manager.render(
            player,
            frameInterp,
            pitch,
            hand,
            attack,
            itemStack,
            inverseArmHeight,
            poseStack,
            submitNodeCollector,
            lightCoords
        );
        if (rendered) {
            ci.cancel();
        }
    }
}
