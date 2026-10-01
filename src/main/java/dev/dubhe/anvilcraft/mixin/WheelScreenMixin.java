package dev.dubhe.anvilcraft.mixin;

import dev.anvilcraft.lib.v2.wheel.api.WheelMenuModel;
import dev.anvilcraft.lib.v2.wheel.client.gui.component.WheelWidget;
import dev.anvilcraft.lib.v2.wheel.client.gui.screen.WheelScreen;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.hammer.IHasHammerEffect;
import dev.dubhe.anvilcraft.api.injection.wheel.IWheelWidgetExtension;
import dev.dubhe.anvilcraft.client.event.WheelLifecycleEventListener;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import dev.dubhe.anvilcraft.client.support.HammerPreviewState;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** 将锤子状态选择轮的滚轮事件转交给扇区控件。 */
@SuppressWarnings("UnresolvedMixinReference")
@Mixin(value = WheelScreen.class, remap = false)
public abstract class WheelScreenMixin implements IHasHammerEffect {
    @Unique
    private @Nullable HammerPreviewState anvilcraft$hammerPreview;
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
        args.set(4, 55 * scale);
        args.set(5, 105 * scale);
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

    @Inject(method = "init", at = @At("TAIL"))
    private void anvilcraft$beginHammerPreview(CallbackInfo ci) {
        if (this.anvilcraft$hammerPreview == null) this.anvilcraft$hammerPreview = WheelLifecycleEventListener.hammerPreview(this.model);
        if (this.anvilcraft$hammerPreview == null) return;
        this.anvilcraft$hammerPreview.begin();
        if (this.wheelWidget != null) this.wheelWidget.setCurrentIndex(this.anvilcraft$hammerPreview.currentIndex());
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void anvilcraft$updateHammerPreview(CallbackInfo ci) {
        if (this.anvilcraft$hammerPreview == null || this.wheelWidget == null) return;
        if (!this.wheelWidget.isClosingAnimationStarted()) {
            if (!this.anvilcraft$hammerPreview.isValid()) {
                ((Screen) (Object) this).onClose();
                return;
            }
            this.anvilcraft$hammerPreview.select(this.wheelWidget.getCurrentSectionIndex());
        }
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void anvilcraft$endHammerPreview(CallbackInfo ci) {
        if (this.anvilcraft$hammerPreview != null) this.anvilcraft$hammerPreview.end();
    }

    @Override
    public boolean shouldRender() {
        return this.anvilcraft$hammerPreview != null && this.anvilcraft$hammerPreview.shouldRender();
    }

    @Override
    public boolean shouldSkipRebuildBlock() {
        return this.anvilcraft$hammerPreview != null && this.anvilcraft$hammerPreview.shouldSkipRebuildBlock();
    }

    @Override
    public BlockPos renderingBlockPos() {
        return this.anvilcraft$hammerPreview == null ? BlockPos.ZERO : this.anvilcraft$hammerPreview.renderingBlockPos();
    }

    @Override
    public BlockPos hiddenBlockPos() {
        return this.anvilcraft$hammerPreview == null ? BlockPos.ZERO : this.anvilcraft$hammerPreview.hiddenBlockPos();
    }

    @Override
    public BlockState renderingBlockState() {
        return this.anvilcraft$hammerPreview == null ? Blocks.AIR.defaultBlockState() : this.anvilcraft$hammerPreview.renderingBlockState();
    }

    @Override
    public RenderType renderType() {
        return this.anvilcraft$hammerPreview == null
            ? ModRenderTypes.TRANSLUCENT_COLORED_OVERLAY : this.anvilcraft$hammerPreview.renderType();
    }
}
