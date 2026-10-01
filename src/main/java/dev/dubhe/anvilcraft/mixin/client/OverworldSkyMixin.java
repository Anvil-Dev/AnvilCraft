package dev.dubhe.anvilcraft.mixin.client;

import dev.dubhe.anvilcraft.client.renderer.mun.OverworldSkyRenderer;
import net.minecraft.client.renderer.SkyRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRenderer.class)
public abstract class OverworldSkyMixin {
    @Inject(method = "renderSunMoonAndStars", at = @At("HEAD"))
    private void anvilcraft$captureAtmosphere(CallbackInfo ci) {
        OverworldSkyRenderer.begin();
    }

    @Inject(method = {"renderSun", "renderMoon"}, at = @At("HEAD"), cancellable = true)
    private void anvilcraft$replaceCelestialBodies(CallbackInfo ci) {
        if (OverworldSkyRenderer.isActive()) ci.cancel();
    }

    @Inject(method = "renderSunMoonAndStars", at = @At("RETURN"))
    private void anvilcraft$renderCelestialBodies(CallbackInfo ci) {
        OverworldSkyRenderer.render();
    }
}
