package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.dubhe.anvilcraft.api.rendering.CacheableBERenderingPipeline;
import dev.dubhe.anvilcraft.entity.FluidTankMinecartEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
abstract class MinecraftClientMixin {
    @Inject(
        method = "updateLevelInEngines",
        at = @At("HEAD")
    )
    void updateLevel(ClientLevel level, CallbackInfo ci) {
        CacheableBERenderingPipeline.updateLevel(level);
    }

    @WrapOperation(
        method = "pickBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;"
                     + "getPickedResult(Lnet/minecraft/world/phys/HitResult;)Lnet/minecraft/world/item/ItemStack;"
        )
    )
    private ItemStack preserveFluidTankMinecartDataWhenControlDown(
        Entity entity,
        HitResult hitResult,
        Operation<ItemStack> original
    ) {
        if (Screen.hasControlDown() && entity instanceof FluidTankMinecartEntity tankMinecart) {
            return tankMinecart.createDropStack();
        }
        return original.call(entity, hitResult);
    }
}
