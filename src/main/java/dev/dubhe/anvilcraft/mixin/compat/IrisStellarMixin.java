package dev.dubhe.anvilcraft.mixin.compat;

import dev.dubhe.anvilcraft.client.renderer.blockentity.celestial.StellarEmissionRenderer;
import dev.dubhe.anvilcraft.integration.iris.CelestialIrisRenderer;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = IrisRenderingPipeline.class, remap = false)
abstract class IrisStellarMixin {
    @Inject(method = "beginLevelRendering", at = @At("HEAD"))
    private void anvilcraft$beginStellarFrame(CallbackInfo ci) {
        CelestialIrisRenderer.beginFrame();
    }

    @Inject(method = "finalizeLevelRendering", at = @At("RETURN"))
    private void anvilcraft$composeStars(CallbackInfo ci) {
        StellarEmissionRenderer.renderAfterShaders();
    }

    @Inject(method = "destroy", at = @At("HEAD"))
    private void anvilcraft$releaseStellarBuffers(CallbackInfo ci) {
        CelestialIrisRenderer.reset();
    }
}
