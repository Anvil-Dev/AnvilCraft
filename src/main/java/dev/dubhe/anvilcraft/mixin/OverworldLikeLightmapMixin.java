package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import dev.dubhe.anvilcraft.client.support.OverworldLikeClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapRenderStateExtractor.class)
public class OverworldLikeLightmapMixin {
    @Inject(method = "extract", at = @At("RETURN"))
    private void anvilcraft$eclipseLight(LightmapRenderState state, float partialTick, CallbackInfo ci) {
        var level = Minecraft.getInstance().level;
        if (state.needsUpdate && level != null && CelestialTravelManager.isOverworldLike(level.dimension())) {
            state.skyFactor = OverworldLikeClientState.modifySkyDarken(level, state.skyFactor);
        }
    }
}
