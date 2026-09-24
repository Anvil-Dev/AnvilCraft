package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.client.renderer.mun.MunClientSky;
import dev.dubhe.anvilcraft.client.renderer.mun.MunLightmapState;
import dev.dubhe.anvilcraft.client.renderer.mun.MunSurfaceRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapRenderStateExtractor.class)
public class MunLightmapExtractionMixin {
    @Inject(method = "extract", at = @At("RETURN"))
    private void anvilcraft$extractMoonLightmap(LightmapRenderState state, float partialTick, CallbackInfo ci) {
        boolean enabled = MunSurfaceRenderer.lightingRequested();
        ((MunLightmapState) state).anvilcraft$setMunLighting(enabled);
        var client = Minecraft.getInstance();
        if (enabled && state.needsUpdate && client.level != null) {
            state.skyFactor = MunClientSky.sunlight(client.level, client.gameRenderer.getMainCamera().position());
            state.blockFactor += 0.1F;
        }
    }
}
