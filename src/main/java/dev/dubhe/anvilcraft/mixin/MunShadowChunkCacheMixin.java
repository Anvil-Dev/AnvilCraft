package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.client.renderer.mun.MunSurfaceRenderer;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientChunkCache.class)
public class MunShadowChunkCacheMixin {
    @Shadow
    @Final
    private ClientLevel level;

    @Inject(method = "drop", at = @At("RETURN"))
    private void anvilcraft$unloadedShadowChunk(ChunkPos pos, CallbackInfo ci) {
        MunSurfaceRenderer.onChunkChanged(this.level, pos);
    }
}
