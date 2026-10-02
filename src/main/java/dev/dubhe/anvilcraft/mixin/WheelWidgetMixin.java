package dev.dubhe.anvilcraft.mixin;

import dev.anvilcraft.lib.v2.wheel.client.gui.component.WheelFrostedBackground;
import dev.anvilcraft.lib.v2.wheel.client.gui.component.WheelWidget;
import dev.dubhe.anvilcraft.api.injection.wheel.IWheelWidgetExtension;
import dev.dubhe.anvilcraft.client.event.WheelLifecycleEventListener;
import dev.dubhe.anvilcraft.client.support.WheelBackgroundCapture;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 对齐轮盘背景绘制，并保留锤子滚轮选区。 */
@SuppressWarnings("UnresolvedMixinReference")
@Mixin(value = WheelWidget.class, remap = false)
public abstract class WheelWidgetMixin implements IWheelWidgetExtension {
    @Shadow
    private @Nullable WheelFrostedBackground frostedBackground;

    @Mutable
    @Shadow
    @Final
    private float textScale;
    @Unique
    private boolean anvilcraft$keepScrolledSelection;
    @Unique
    private double anvilcraft$scrollMouseX;
    @Unique
    private double anvilcraft$scrollMouseY;

    @ModifyArg(method = "renderDisc", at = @At(value = "INVOKE",
        target = "Ldev/anvilcraft/lib/v2/rendering/sdf/SdfGraphics;color(I)Ldev/anvilcraft/lib/v2/rendering/sdf/SdfGraphics;"), index = 0)
    private int anvilcraft$sourceDiscOpacity(int color) {
        return this.frostedBackground == null ? color : WheelBackgroundCapture.sourceDiscColor(color);
    }

    @Inject(method = "renderFrostedBackground", at = @At("HEAD"))
    private void anvilcraft$separateDiscFromBackground(GuiGraphicsExtractor graphics, float progress, CallbackInfo callback) {
        if (this.frostedBackground != null && progress > 0) WheelBackgroundCapture.afterDisc(graphics);
    }

    @Override
    public void anvilcraft$setTextScale(float scale) {
        this.textScale *= scale;
    }

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
