package dev.dubhe.anvilcraft.mixin;

import dev.anvilcraft.lib.v2.wheel.api.WheelMenuModel;
import dev.anvilcraft.lib.v2.wheel.client.gui.component.WheelWidget;
import dev.anvilcraft.lib.v2.wheel.client.gui.screen.WheelScreen;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.injection.wheel.IWheelWidgetExtension;
import dev.dubhe.anvilcraft.client.event.WheelLifecycleEventListener;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** 将锤子状态选择轮的滚轮事件转交给扇区控件。 */
@SuppressWarnings("UnresolvedMixinReference")
@Mixin(value = WheelScreen.class, remap = false)
public abstract class WheelScreenMixin {
    @Shadow
    @Final
    private WheelMenuModel model;

    @Shadow
    private @Nullable WheelWidget wheelWidget;

    @ModifyArgs(
        method = "rebuildWheelWidget",
        at = @At(
            value = "INVOKE",
            target = "Ldev/anvilcraft/lib/v2/wheel/client/gui/component/WheelWidget;"
                     + "<init>(IIIIFFLjava/util/List;I)V"
        )
    )
    private void scaleHammerWheel(Args args) {
        if (!WheelLifecycleEventListener.isHammerWheelModel(this.model)) return;
        float scale = AnvilCraft.CLIENT_CONFIG.anvilHammerRadialMenuScale;
        args.set(4, args.<Float>get(4) * scale);
        args.set(5, args.<Float>get(5) * scale);
        args.set(7, Math.round(args.<Integer>get(7) * scale));
    }

    @Inject(method = "rebuildWheelWidget", at = @At("TAIL"))
    private void scaleHammerWheelText(CallbackInfo ci) {
        if (this.wheelWidget == null || !WheelLifecycleEventListener.isHammerWheelModel(this.model)) return;
        ((IWheelWidgetExtension) this.wheelWidget)
            .anvilcraft$setTextScale(AnvilCraft.CLIENT_CONFIG.anvilHammerRadialMenuScale);
    }

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
