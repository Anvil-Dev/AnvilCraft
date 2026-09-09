package dev.dubhe.anvilcraft.mixin.compat;

import dev.dubhe.anvilcraft.client.renderer.MunSurfaceRenderer;
import org.embeddedt.embeddium.impl.render.chunk.ShaderChunkRenderer;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ShaderChunkRenderer.class, remap = false)
abstract class EmbMunShaderRendererMixin {
    @Inject(method = "begin", at = @At("RETURN"))
    private void anvilcraft$munTerrainUniforms(TerrainRenderPass pass, CallbackInfo ci) {
        MunSurfaceRenderer.setupSodiumUniforms(!pass.isReverseOrder());
    }
}
