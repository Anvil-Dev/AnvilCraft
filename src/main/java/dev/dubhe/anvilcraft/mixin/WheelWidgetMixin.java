package dev.dubhe.anvilcraft.mixin;

import dev.anvilcraft.lib.v2.wheel.client.gui.component.WheelWidget;
import dev.dubhe.anvilcraft.client.event.WheelLifecycleEventListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 保持锤子状态选择轮通过滚轮选中的扇区，直到鼠标再次移动。 */
@SuppressWarnings("UnresolvedMixinReference")
@Mixin(value = WheelWidget.class, remap = false)
public abstract class WheelWidgetMixin {
    @Unique
    private boolean anvilcraft$keepScrolledSelection;
    @Unique
    private double anvilcraft$scrollMouseX;
    @Unique
    private double anvilcraft$scrollMouseY;

    @Inject(method = "mouseScrolled(DDDD)Z", at = @At("HEAD"))
    private void rememberHammerScroll(
        double mouseX,
        double mouseY,
        double scrollX,
        double scrollY,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (!WheelLifecycleEventListener.isHammerWheelOpen() || scrollY == 0.0) return;
        this.anvilcraft$keepScrolledSelection = true;
        this.anvilcraft$scrollMouseX = mouseX;
        this.anvilcraft$scrollMouseY = mouseY;
    }

    @Inject(method = "checkMousePos(DD)V", at = @At("HEAD"), cancellable = true)
    private void keepHammerScrollSelection(double mouseX, double mouseY, CallbackInfo ci) {
        if (!WheelLifecycleEventListener.isHammerWheelOpen() || !this.anvilcraft$keepScrolledSelection) return;
        double deltaX = mouseX - this.anvilcraft$scrollMouseX;
        double deltaY = mouseY - this.anvilcraft$scrollMouseY;
        if (deltaX * deltaX + deltaY * deltaY > 4.0) {
            this.anvilcraft$keepScrolledSelection = false;
            return;
        }
        ci.cancel();
    }
}
