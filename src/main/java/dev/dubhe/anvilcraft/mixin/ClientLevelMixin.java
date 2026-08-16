package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.client.support.PowerGridSupport;
import dev.dubhe.anvilcraft.util.EnchantedGoldBlockPositions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(ClientLevel.class)
abstract class ClientLevelMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    void onLevelLoad(
        ClientPacketListener connection,
        ClientLevel.ClientLevelData clientLevelData,
        ResourceKey<?> dimension,
        Holder<?> dimensionType,
        int viewDistance,
        int serverSimulationDistance,
        Supplier<?> profiler,
        LevelRenderer levelRenderer,
        boolean isDebug,
        long biomeZoomSeed,
        CallbackInfo ci
    ) {
        PowerGridSupport.clearAllGrid();
        EnchantedGoldBlockPositions.clear();
    }

    @Inject(method = "onChunkLoaded(Lnet/minecraft/world/level/ChunkPos;)V", at = @At("TAIL"))
    void scanEnchantedGoldBlocks(ChunkPos chunkPos, CallbackInfo ci) {
        LevelChunk chunk = ((ClientLevel) (Object) this).getChunk(chunkPos.x, chunkPos.z);
        EnchantedGoldBlockPositions.scanChunk(chunk);
    }
}
