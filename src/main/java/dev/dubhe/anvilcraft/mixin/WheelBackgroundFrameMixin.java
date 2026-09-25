package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.client.support.WheelBackgroundCapture;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
abstract class WheelBackgroundFrameMixin {
    @Inject(method = "extractGui", at = @At("HEAD"))
    private void anvilcraft$beginGuiFrame(DeltaTracker tracker, boolean renderLevel, boolean resourcesLoaded, CallbackInfo callback) {
        WheelBackgroundCapture.beginFrame();
    }

    @Inject(method = "render", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V"))
    private void anvilcraft$captureWorldBeforeGui(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo callback) {
        WheelBackgroundCapture.beforeGui();
    }
}
