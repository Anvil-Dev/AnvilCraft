package dev.dubhe.anvilcraft.mixin;

import dev.anvilcraft.lib.v2.wheel.client.gui.component.WheelWidget;
import dev.anvilcraft.lib.v2.wheel.client.gui.screen.WheelScreen;
import dev.dubhe.anvilcraft.client.event.WheelLifecycleEventListener;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 将锤子状态选择轮的滚轮事件转交给扇区控件。 */
@SuppressWarnings("UnresolvedMixinReference")
@Mixin(value = WheelScreen.class, remap = false)
public abstract class WheelScreenMixin {
    @Shadow
    private @Nullable WheelWidget wheelWidget;

    @Inject(method = "mouseScrolled(DDDD)Z", at = @At("HEAD"), cancellable = true)
    private void scrollHammerSelection(
        double mouseX,
        double mouseY,
        double scrollX,
        double scrollY,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (!WheelLifecycleEventListener.isHammerWheelOpen() || this.wheelWidget == null) return;
        cir.setReturnValue(this.wheelWidget.mouseScrolled(mouseX, mouseY, scrollX, scrollY));
    }
}
