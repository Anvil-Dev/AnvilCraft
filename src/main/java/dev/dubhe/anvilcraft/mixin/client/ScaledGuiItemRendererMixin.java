package dev.dubhe.anvilcraft.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.dubhe.anvilcraft.client.support.ScaledGuiItemAtlases;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiItemAtlas;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
abstract class ScaledGuiItemRendererMixin {
    @Shadow @Final private GuiRenderState renderState;
    @Shadow @Final private MultiBufferSource.BufferSource bufferSource;
    @Shadow @Final private SubmitNodeCollector submitNodeCollector;
    @Shadow @Final private FeatureRenderDispatcher featureRenderDispatcher;
    @Unique private @Nullable ScaledGuiItemAtlases anvilcraft$scaledItems;

    @Inject(method = "prepareItemElements", at = @At("HEAD"))
    private void anvilcraft$prepareScaledItems(CallbackInfo ci) {
        if (this.anvilcraft$scaledItems == null) {
            this.anvilcraft$scaledItems = new ScaledGuiItemAtlases(
                this.submitNodeCollector, this.featureRenderDispatcher, this.bufferSource);
        }
        this.anvilcraft$scaledItems.prepare(this.renderState,
            Minecraft.getInstance().gameRenderer.getGameRenderState().windowRenderState.guiScale);
    }

    @WrapOperation(
        method = "*",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiItemAtlas;getOrUpdate("
            + "Lnet/minecraft/client/renderer/item/TrackingItemStackRenderState;)Lnet/minecraft/client/gui/render/GuiItemAtlas$SlotView;")
    )
    private GuiItemAtlas.@Nullable SlotView anvilcraft$scaledSlot(
        GuiItemAtlas atlas, TrackingItemStackRenderState state, Operation<GuiItemAtlas.SlotView> original,
        @Local(argsOnly = true) GuiItemRenderState item
    ) {
        if (this.anvilcraft$scaledItems != null) {
            GuiItemAtlas.SlotView scaled = this.anvilcraft$scaledItems.get(item);
            if (scaled != null) return scaled;
        }
        return original.call(atlas, state);
    }

    @Inject(method = "endFrame", at = @At("TAIL"))
    private void anvilcraft$endScaledFrame(CallbackInfo ci) {
        if (this.anvilcraft$scaledItems != null) this.anvilcraft$scaledItems.endFrame();
    }

    @Inject(method = {"close", "invalidateItemAtlas"}, at = @At("HEAD"))
    private void anvilcraft$releaseScaledItems(CallbackInfo ci) {
        if (this.anvilcraft$scaledItems != null) this.anvilcraft$scaledItems.close();
    }
}
