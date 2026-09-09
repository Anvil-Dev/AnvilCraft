package dev.dubhe.anvilcraft.mixin.compat;

import dev.dubhe.anvilcraft.integration.iris.MunIrisCompat;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = IrisRenderingPipeline.class, remap = false)
abstract class IrisMunSkyMixin {
    @Inject(method = "finalizeLevelRendering", at = @At("RETURN"))
    private void anvilcraft$vacuumSkyAfterComposition(CallbackInfo ci) {
        MunIrisCompat.renderSky();
    }
}
