package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.client.renderer.mun.MunSurfaceRenderer;
import net.minecraft.client.renderer.feature.ShadowFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShadowFeatureRenderer.class)
public class MunEntityShadowMixin {
    @Inject(method = "renderTranslucent", at = @At("HEAD"), cancellable = true)
    private void anvilcraft$moonShadow(CallbackInfo ci) {
        if (MunSurfaceRenderer.shadowsRequested()) ci.cancel();
    }
}
