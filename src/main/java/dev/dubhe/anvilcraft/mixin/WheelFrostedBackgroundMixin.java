package dev.dubhe.anvilcraft.mixin;

import dev.anvilcraft.lib.v2.wheel.client.gui.component.WheelFrostedBackground;
import dev.dubhe.anvilcraft.client.support.WheelBackgroundCapture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.TextureSetup;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = WheelFrostedBackground.class, remap = false)
abstract class WheelFrostedBackgroundMixin {
    @Unique
    private @Nullable TextureSetup anvilcraft$texture;
    @Unique
    private int anvilcraft$width;
    @Unique
    private int anvilcraft$height;

    @Inject(method = "capture", at = @At("HEAD"), cancellable = true)
    private void anvilcraft$deferCapture(CallbackInfoReturnable<TextureSetup> callback) {
        if (WheelBackgroundCapture.isRefreshing()) return;
        var target = Minecraft.getInstance().getMainRenderTarget();
        WheelBackgroundCapture.request((WheelFrostedBackground) (Object) this, target.width, target.height);
        if (this.anvilcraft$texture != null && this.anvilcraft$width == target.width && this.anvilcraft$height == target.height) {
            callback.setReturnValue(this.anvilcraft$texture);
        }
    }

    @Inject(method = "capture", at = @At("RETURN"))
    private void anvilcraft$rememberTexture(CallbackInfoReturnable<TextureSetup> callback) {
        var target = Minecraft.getInstance().getMainRenderTarget();
        this.anvilcraft$width = target.width;
        this.anvilcraft$height = target.height;
        this.anvilcraft$texture = callback.getReturnValue();
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void anvilcraft$forgetCapture(CallbackInfo callback) {
        WheelBackgroundCapture.forget((WheelFrostedBackground) (Object) this);
        this.anvilcraft$texture = null;
    }
}
