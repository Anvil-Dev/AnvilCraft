package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import dev.dubhe.anvilcraft.client.renderer.mun.MunClientSky;
import dev.dubhe.anvilcraft.client.renderer.mun.MunLightmapState;
import dev.dubhe.anvilcraft.client.renderer.mun.MunSurfaceRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapRenderStateExtractor.class)
public class MunLightmapExtractionMixin {
    @WrapOperation(
        method = "extract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;hasEffect(Lnet/minecraft/core/Holder;)Z"
        )
    )
    private boolean anvilcraft$disableMoonNightVision(
        LocalPlayer player,
        Holder<MobEffect> effect,
        Operation<Boolean> original
    ) {
        if (effect.equals(MobEffects.NIGHT_VISION) && player.level().dimension().equals(CelestialTravelManager.MUN_LEVEL)) {
            return false;
        }
        return original.call(player, effect);
    }

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
