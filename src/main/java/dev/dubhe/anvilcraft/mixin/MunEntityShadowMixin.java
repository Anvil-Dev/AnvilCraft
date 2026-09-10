package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.support.MunClientSky;
import dev.dubhe.anvilcraft.config.AnvilCraftClientConfig.MunLightingQuality;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
abstract class MunEntityShadowMixin {
    @Inject(method = "renderShadow", at = @At("HEAD"), cancellable = true)
    private static void anvilcraft$hideMunShadow(CallbackInfo ci) {
        if (MunClientSky.isMun() && AnvilCraft.CLIENT_CONFIG.munLightingQuality == MunLightingQuality.STANDARD) ci.cancel();
    }
}
