package dev.dubhe.anvilcraft.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.anvilcraft.lib.v2.rendering.bloom.BloomPostEffect;
import dev.anvilcraft.lib.v2.rendering.cachedber.pipeline.CachedRenderingChunk;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import dev.dubhe.anvilcraft.client.renderer.post.LaserBloomPostEffect;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CachedRenderingChunk.class)
public abstract class LaserBloomCaptureMixin {
    @WrapOperation(method = "renderLayers", at = @At(value = "INVOKE", target =
        "Ldev/anvilcraft/lib/v2/rendering/bloom/BloomPostEffect;beginBloomDraw()V"))
    private void anvilcraft$beginLaserBloom(BloomPostEffect effect, Operation<Void> original, @Local RenderType renderType) {
        if (renderType == ModRenderTypes.LASER_TRANSLUCENT_BLOOM) {
            LaserBloomPostEffect.beginDraw();
        } else {
            original.call(effect);
        }
    }

    @WrapOperation(method = "renderLayers", at = @At(value = "INVOKE", target =
        "Ldev/anvilcraft/lib/v2/rendering/bloom/BloomPostEffect;endBloomDraw()V"))
    private void anvilcraft$endLaserBloom(BloomPostEffect effect, Operation<Void> original, @Local RenderType renderType) {
        if (renderType == ModRenderTypes.LASER_TRANSLUCENT_BLOOM) {
            LaserBloomPostEffect.endDraw();
        } else {
            original.call(effect);
        }
    }
}
