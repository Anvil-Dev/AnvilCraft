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
import net.minecraft.client.Minecraft;
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

/** 锤子轮盘按当前页同步方块预览，单页轮盘保留滚轮选区操作。 */
@SuppressWarnings("UnresolvedMixinReference")
@Mixin(value = WheelScreen.class, remap = false)
public abstract class WheelScreenMixin implements IHasHammerEffect {
    @Unique
    private @Nullable HammerPreviewState anvilcraft$hammerPreview;
    @Unique
    private int anvilcraft$pageSlot;
    @Shadow
    @Final
    private WheelMenuModel model;

    @Shadow
    private int currentPageIndex;

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
        float scale = AnvilCraft.CLIENT_CONFIG.ui.anvilHammerWheelScale;
        args.set(4, 55 * scale);
        args.set(5, 105 * scale);
        args.set(7, (int) (15 * scale));
    }

    @Inject(method = "rebuildWheelWidget", at = @At("TAIL"))
    private void scaleHammerWheelText(CallbackInfo ci) {
        if (this.wheelWidget == null || !WheelLifecycleEventListener.isHammerWheelModel(this.model)) return;
        ((IWheelWidgetExtension) this.wheelWidget)
            .anvilcraft$setTextScale(AnvilCraft.CLIENT_CONFIG.ui.anvilHammerWheelScale);
    }

    @Inject(method = "mouseScrolled(DDDD)Z", at = @At("HEAD"), cancellable = true)
    private void scrollHammerSelection(
        double mouseX,
        double mouseY,
        double scrollX,
        double scrollY,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (this.anvilcraft$hammerPreview == null || this.wheelWidget == null) {
            return;
        }
        if (this.wheelWidget.isClosingAnimationStarted()) {
            cir.setReturnValue(true);
            return;
        }
        if (this.model.pageCount() > 1) {
            this.anvilcraft$pageSlot = Math.max(0, this.wheelWidget.getCurrentSectionIndex());
            return;
        }
        cir.setReturnValue(this.wheelWidget.mouseScrolled(mouseX, mouseY, scrollX, scrollY));
    }

    @Inject(method = "mouseScrolled(DDDD)Z", at = @At("RETURN"))
    private void anvilcraft$selectAfterPageTurn(
        double mouseX, double mouseY, double scrollX, double scrollY, CallbackInfoReturnable<Boolean> cir
    ) {
        if (scrollY != 0 && this.anvilcraft$hammerPreview != null && this.wheelWidget != null && this.model.pageCount() > 1) {
            int first = this.currentPageIndex * this.model.slotsPerPage();
            this.wheelWidget.checkMousePos(mouseX, mouseY);
            if (this.wheelWidget.getCurrentSectionIndex() < 0) {
                int lastSlot = this.model.rootEntries().size() - first - 1;
                this.wheelWidget.setCurrentIndex(Math.min(this.anvilcraft$pageSlot, lastSlot));
            }
            ((IWheelWidgetExtension) this.wheelWidget).anvilcraft$holdSelection(mouseX, mouseY);
            this.anvilcraft$hammerPreview.select(first + this.wheelWidget.getCurrentSectionIndex());
        }
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void anvilcraft$openCurrentPage(CallbackInfo ci) {
        if (this.anvilcraft$hammerPreview == null) {
            this.anvilcraft$hammerPreview = WheelLifecycleEventListener.hammerPreview(this.model);
        }
        if (this.anvilcraft$hammerPreview != null) {
            this.currentPageIndex = this.anvilcraft$hammerPreview.currentIndex() / this.model.slotsPerPage();
        }
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void anvilcraft$beginHammerPreview(CallbackInfo ci) {
        if (this.anvilcraft$hammerPreview == null) {
            return;
        }
        this.anvilcraft$hammerPreview.begin();
        if (this.wheelWidget != null) {
            this.wheelWidget.setCurrentIndex(this.anvilcraft$hammerPreview.currentIndex() % this.model.slotsPerPage());
            var client = Minecraft.getInstance();
            var window = client.getWindow();
            ((IWheelWidgetExtension) this.wheelWidget).anvilcraft$holdSelection(
                client.mouseHandler.getScaledXPos(window), client.mouseHandler.getScaledYPos(window));
        }
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void anvilcraft$updateHammerPreview(CallbackInfo ci) {
        if (this.anvilcraft$hammerPreview == null || this.wheelWidget == null) return;
        if (!this.wheelWidget.isClosingAnimationStarted()) {
            if (!this.anvilcraft$hammerPreview.isValid()) {
                ((Screen) (Object) this).onClose();
                return;
            }
            int slot = this.wheelWidget.getCurrentSectionIndex();
            this.anvilcraft$hammerPreview.select(slot < 0 ? -1 : this.currentPageIndex * this.model.slotsPerPage() + slot);
        }
    }

    @Inject(method = "triggerSelectedOrClose", at = @At("HEAD"), cancellable = true)
    private void anvilcraft$blockHammerActionDuringClose(CallbackInfo ci) {
        if (this.anvilcraft$hammerPreview != null && this.wheelWidget != null && this.wheelWidget.isClosingAnimationStarted()) {
            ci.cancel();
        }
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void anvilcraft$endHammerPreview(CallbackInfo ci) {
        if (this.anvilcraft$hammerPreview != null) {
            this.anvilcraft$hammerPreview.end();
            if (this.wheelWidget != null) {
                // 关闭动画的最后一帧仍会访问控件，此时屏幕即将释放背景缓冲。
                this.wheelWidget.setFrostedBackground(null);
            }
        }
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
