package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import dev.dubhe.anvilcraft.client.support.OverworldLikeClientState;
import dev.dubhe.anvilcraft.client.support.PowerGridSupport;
import dev.dubhe.anvilcraft.util.EnchantedGoldBlockPositions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(ClientLevel.class)
abstract class ClientLevelMixin {
    @ModifyReturnValue(method = "getSkyDarken(F)F", at = @At("RETURN"))
    private float anvilcraft$addOverworldLikeEclipseDarken(float original) {
        ClientLevel level = (ClientLevel) (Object) this;
        if (!CelestialTravelManager.isOverworldLike(level.dimension())) return original;
        return OverworldLikeClientState.modifySkyDarken(level, original);
    }

    @ModifyReturnValue(method = "getSkyColor", at = @At("RETURN"))
    private Vec3 anvilcraft$darkenOverworldLikeSkyColor(Vec3 original) {
        ClientLevel level = (ClientLevel) (Object) this;
        if (!CelestialTravelManager.isOverworldLike(level.dimension())) return original;
        return original.scale(OverworldLikeClientState.environmentColorMultiplier(level));
    }

    @ModifyReturnValue(method = "getCloudColor", at = @At("RETURN"))
    private Vec3 anvilcraft$darkenOverworldLikeCloudColor(Vec3 original) {
        ClientLevel level = (ClientLevel) (Object) this;
        if (!CelestialTravelManager.isOverworldLike(level.dimension())) return original;
        return original.scale(OverworldLikeClientState.environmentColorMultiplier(level));
    }

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
        OverworldLikeClientState.clear();
        PowerGridSupport.clearAllGrid();
        EnchantedGoldBlockPositions.clear();
    }

    @Inject(method = "onChunkLoaded(Lnet/minecraft/world/level/ChunkPos;)V", at = @At("TAIL"))
    void scanEnchantedGoldBlocks(ChunkPos chunkPos, CallbackInfo ci) {
        LevelChunk chunk = ((ClientLevel) (Object) this).getChunk(chunkPos.x, chunkPos.z);
        EnchantedGoldBlockPositions.scanChunk(chunk);
    }
}
