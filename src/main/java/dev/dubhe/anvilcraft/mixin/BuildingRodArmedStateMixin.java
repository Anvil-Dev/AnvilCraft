package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.dubhe.anvilcraft.client.building.BuildingRodHandItemState;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArmedEntityRenderState.class)
abstract class BuildingRodArmedStateMixin {
    @WrapOperation(method = "<init>", at = @At(value = "NEW", target = "net/minecraft/client/renderer/item/ItemStackRenderState"))
    private ItemStackRenderState anvilcraft$prepareRodHand(Operation<ItemStackRenderState> original) {
        return new BuildingRodHandItemState();
    }

    @Inject(method = "extractArmedEntityRenderState", at = @At("HEAD"))
    private static void anvilcraft$beginRod(
        LivingEntity entity, ArmedEntityRenderState state, ItemModelResolver resolver, float partialTicks, CallbackInfo ci
    ) {
        BuildingRodHandItemState.begin(entity, state);
    }

    @Inject(method = "extractArmedEntityRenderState", at = @At("TAIL"))
    private static void anvilcraft$extractRod(
        LivingEntity entity, ArmedEntityRenderState state, ItemModelResolver resolver, float partialTicks, CallbackInfo ci
    ) {
        BuildingRodHandItemState.extract(entity, state);
    }
}
