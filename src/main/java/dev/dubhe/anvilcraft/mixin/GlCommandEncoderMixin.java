package dev.dubhe.anvilcraft.mixin;

import com.mojang.blaze3d.opengl.GlCommandEncoder;
import dev.dubhe.anvilcraft.client.support.GravitationalLensManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GlCommandEncoder.class)
public class GlCommandEncoderMixin {
    /**
     * Inject after the pipeline/program is set up in trySetup().
     * At this point GL_CURRENT_PROGRAM is valid, so we can bind the lens UBO.
     */
    @Inject(method = "trySetup", at = @At("RETURN"))
    void afterSetup(CallbackInfoReturnable<Boolean> cir) {
        GravitationalLensManager.bindLensUboIfNeeded();
    }
}
