package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.client.renderer.mun.MunSurfaceRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class MunShadowLevelMixin {
    @Inject(method = "onChunkLoaded", at = @At("RETURN"))
    private void anvilcraft$loadedShadowChunk(ChunkPos pos, CallbackInfo ci) {
        MunSurfaceRenderer.onChunkChanged((ClientLevel) (Object) this, pos);
    }

    @Inject(method = "setBlocksDirty", at = @At("RETURN"))
    private void anvilcraft$changedShadowModel(BlockPos pos, BlockState previous, BlockState state, CallbackInfo ci) {
        if (previous == state) MunSurfaceRenderer.onModelChanged((ClientLevel) (Object) this, pos);
    }

    @Inject(method = "sendBlockUpdated", at = @At("RETURN"))
    private void anvilcraft$updatedShadowModel(BlockPos pos, BlockState previous, BlockState state, int flags, CallbackInfo ci) {
        if (previous == state) MunSurfaceRenderer.onModelChanged((ClientLevel) (Object) this, pos);
    }
}
