package dev.dubhe.anvilcraft.mixin.client;

import dev.anvilcraft.lib.v2.rendering.glitch.GlitchPostEffect;
import dev.dubhe.anvilcraft.client.gui.screen.SmartPlacerPreviewRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = GlitchPostEffect.class, remap = false)
abstract class SmartPlacerScanTimeMixin {
    @ModifyArg(
        method = "process", index = 0,
        at = @At(value = "INVOKE", target = "Ldev/anvilcraft/lib/v2/rendering/glitch/GlitchParametersUbo;setGameTime(F)V")
    )
    private float anvilcraft$scanClock(float original) {
        return SmartPlacerPreviewRenderer.isRenderingScan() ? (System.currentTimeMillis() % 100000) / 1000.0F : original;
    }
}
