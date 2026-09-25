package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.dubhe.anvilcraft.client.support.WheelBackgroundCapture;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GuiRenderer.class)
abstract class WheelGuiBlurMixin {
    @WrapOperation(method = "draw", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/GameRenderer;processBlurEffect()V"))
    private void anvilcraft$captureWheelBackground(GameRenderer renderer, Operation<Void> original) {
        if (!WheelBackgroundCapture.atBlurBoundary()) original.call(renderer);
    }
}
