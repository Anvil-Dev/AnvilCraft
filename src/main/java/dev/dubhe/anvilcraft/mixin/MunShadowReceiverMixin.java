package dev.dubhe.anvilcraft.mixin;

import com.mojang.blaze3d.opengl.GlCommandEncoder;
import com.mojang.blaze3d.opengl.GlRenderPass;
import dev.dubhe.anvilcraft.client.renderer.mun.MunSurfaceRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(GlCommandEncoder.class)
public class MunShadowReceiverMixin {
    @Inject(method = "trySetup", at = @At("RETURN"), cancellable = true)
    private void anvilcraft$shadowResources(GlRenderPass pass, Collection<String> dynamicUniforms, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        var pipeline = ((MunGlRenderPassAccessor) pass).anvilcraft$moonPipeline();
        if (pipeline != null && !MunSurfaceRenderer.bindShadows(pipeline)) cir.setReturnValue(false);
    }
}
