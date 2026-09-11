package dev.dubhe.anvilcraft.mixin.mun;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.dubhe.anvilcraft.client.renderer.mun.MunSurfaceRenderer;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RenderChunkRegion.class)
abstract class MunTerrainShadeMixin {
    @ModifyReturnValue(method = "getShade", at = @At("RETURN"))
    private float anvilcraft$munTerrainShade(float original) {
        return MunSurfaceRenderer.usesTerrainShader() ? 1 : original;
    }
}
