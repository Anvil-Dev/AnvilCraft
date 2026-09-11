package dev.dubhe.anvilcraft.mixin.mun.compat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.dubhe.anvilcraft.client.renderer.mun.MunSurfaceRenderer;
import org.embeddedt.embeddium.impl.world.WorldSlice;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = WorldSlice.class, remap = false)
abstract class EmbMunTerrainShadeMixin {
    @ModifyReturnValue(method = "getShade", at = @At("RETURN"))
    private float anvilcraft$munTerrainShade(float original) {
        return MunSurfaceRenderer.usesTerrainShader() ? 1 : original;
    }
}
