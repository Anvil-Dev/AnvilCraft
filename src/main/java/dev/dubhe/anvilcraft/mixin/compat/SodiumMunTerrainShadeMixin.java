package dev.dubhe.anvilcraft.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.dubhe.anvilcraft.client.renderer.MunSurfaceRenderer;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = LevelSlice.class, remap = false)
abstract class SodiumMunTerrainShadeMixin {
    @ModifyReturnValue(method = "getShade", at = @At("RETURN"))
    private float anvilcraft$munTerrainShade(float original) {
        return MunSurfaceRenderer.usesTerrainShader() ? 1 : original;
    }
}
