package dev.dubhe.anvilcraft.mixin.compat;

import dev.dubhe.anvilcraft.client.support.MagnetAnvilAnimation;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BlockRenderer.class, remap = false)
abstract class SodiumMagnetAnvilMixin {
    @Inject(method = "renderModel", at = @At("HEAD"), cancellable = true)
    private void anvilcraft$hideAnimatedAnvil(
        BlockStateModel model, BlockState state, BlockPos pos, BlockPos origin, CallbackInfo ci
    ) {
        if (MagnetAnvilAnimation.hidesBlock(pos, state)) ci.cancel();
    }
}
