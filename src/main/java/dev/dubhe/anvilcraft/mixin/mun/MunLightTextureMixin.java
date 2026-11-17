package dev.dubhe.anvilcraft.mixin.mun;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LightTexture.class)
abstract class MunLightTextureMixin {
    @WrapOperation(
        method = "updateLightTexture",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;hasEffect(Lnet/minecraft/core/Holder;)Z"
        )
    )
    private boolean anvilcraft$disableMunNightVision(
        LocalPlayer player,
        Holder<MobEffect> effect,
        Operation<Boolean> original
    ) {
        if (effect.equals(MobEffects.NIGHT_VISION) && player.level().dimension().equals(CelestialTravelManager.MUN_LEVEL)) {
            return false;
        }
        return original.call(player, effect);
    }
}
