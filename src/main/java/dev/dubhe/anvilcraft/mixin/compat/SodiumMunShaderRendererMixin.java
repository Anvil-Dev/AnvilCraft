package dev.dubhe.anvilcraft.mixin.compat;

import dev.dubhe.anvilcraft.client.renderer.MunSurfaceRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.ShaderChunkRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ShaderChunkRenderer.class, remap = false)
abstract class SodiumMunShaderRendererMixin {
    @Inject(method = "begin", at = @At("RETURN"))
    private void anvilcraft$munTerrainUniforms(TerrainRenderPass pass, CallbackInfo ci) {
        MunSurfaceRenderer.setupSodiumUniforms();
    }
}
