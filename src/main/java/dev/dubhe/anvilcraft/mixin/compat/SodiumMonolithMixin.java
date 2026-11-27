package dev.dubhe.anvilcraft.mixin.compat;

import com.llamalad7.mixinextras.sugar.Local;
import dev.dubhe.anvilcraft.client.renderer.MonolithSurfaceRenderer;
import dev.dubhe.anvilcraft.util.MonolithBlockPositions;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BlockRenderer.class, remap = false)
abstract class SodiumMonolithMixin {
    @Unique
    private boolean anvilcraft$monolith;

    @Inject(method = "renderModel", at = @At("HEAD"))
    private void anvilcraft$selectMonolith(BakedModel model, BlockState state, BlockPos pos, BlockPos origin, CallbackInfo ci) {
        this.anvilcraft$monolith = MonolithBlockPositions.isMonolith(state);
    }

    @Inject(method = "bufferQuad", at = @At(value = "INVOKE",
        target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/material/Material;bits()I"), cancellable = true)
    private void anvilcraft$skipInnerSurface(CallbackInfo ci, @Local @Nullable TextureAtlasSprite sprite) {
        if (this.anvilcraft$monolith && sprite != null && MonolithSurfaceRenderer.INNER.equals(sprite.contents().name())) {
            ci.cancel();
        }
    }
}
