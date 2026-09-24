package dev.dubhe.anvilcraft.mixin.compat;

import dev.dubhe.anvilcraft.client.support.FittedItemRenderer;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.programs.ShaderMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = IrisRenderingPipeline.class, remap = false)
abstract class IrisItemPreviewMixin {
    // 手持和阴影着色器会重定向帧缓冲，标记捕获必须使用原版着色器。
    @Unique
    private static final ShaderMap anvilcraft$previewShaders = new ShaderMap(key -> null);

    @Inject(method = "getShaderMap", at = @At("HEAD"), cancellable = true)
    private void anvilcraft$usePreviewShaders(CallbackInfoReturnable<ShaderMap> cir) {
        if (FittedItemRenderer.isRenderingPreview()) cir.setReturnValue(anvilcraft$previewShaders);
    }

    @Inject(method = "shouldOverrideShaders", at = @At("HEAD"), cancellable = true)
    private void anvilcraft$allowPreviewDrawing(CallbackInfoReturnable<Boolean> cir) {
        if (FittedItemRenderer.isRenderingPreview()) cir.setReturnValue(false);
    }

    @Inject(method = "setIsMainBound", at = @At("HEAD"), cancellable = true)
    private void anvilcraft$preserveMainTarget(boolean bound, CallbackInfo ci) {
        if (FittedItemRenderer.isRenderingPreview()) ci.cancel();
    }
}
