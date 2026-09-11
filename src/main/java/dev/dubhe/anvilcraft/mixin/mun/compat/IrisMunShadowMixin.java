package dev.dubhe.anvilcraft.mixin.mun.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.client.renderer.mun.compat.MunIrisCompat;
import net.irisshaders.iris.shadows.ShadowMatrices;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ShadowMatrices.class, remap = false)
abstract class IrisMunShadowMixin {
    @Inject(method = "createBaselineModelViewMatrix", at = @At("HEAD"), cancellable = true)
    private static void anvilcraft$localShadowView(
        PoseStack target, float shadowAngle, float sunPathRotation, float nearPlane, float farPlane, CallbackInfo ci
    ) {
        if (!MunIrisCompat.isEnabled()) return;
        MunIrisCompat.shadowView(target);
        ci.cancel();
    }
}
