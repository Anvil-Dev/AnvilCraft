package dev.dubhe.anvilcraft.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import dev.dubhe.anvilcraft.client.renderer.mun.MunSurfaceRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderPass.class)
public class MunSurfacePassMixin {
    @ModifyVariable(method = "setPipeline", at = @At("HEAD"), argsOnly = true)
    private RenderPipeline anvilcraft$moonPipeline(RenderPipeline original) {
        return MunSurfaceRenderer.replace(original);
    }

    @Inject(method = "setPipeline", at = @At("TAIL"))
    private void anvilcraft$moonUniforms(RenderPipeline pipeline, CallbackInfo ci) {
        MunSurfaceRenderer.bind((RenderPass) (Object) this, pipeline);
    }
}
