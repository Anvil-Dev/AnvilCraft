package dev.dubhe.anvilcraft.mixin.compat;

import dev.dubhe.anvilcraft.integration.iris.CelestialIrisRenderer;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IrisRenderingPipeline.class)
public abstract class IrisStellarMixin {
    @Inject(method = "beginLevelRendering", at = @At("HEAD"))
    private void anvilcraft$beginCelestialFrame(CallbackInfo ci) {
        CelestialIrisRenderer.beginFrame();
    }

    @Inject(method = "finalizeLevelRendering", at = @At("RETURN"))
    private void anvilcraft$composeCelestialBodies(CallbackInfo ci) {
        CelestialIrisRenderer.render();
    }

    @Inject(method = "destroy", at = @At("HEAD"))
    private void anvilcraft$releaseCelestialFrame(CallbackInfo ci) {
        CelestialIrisRenderer.reset();
    }
}
