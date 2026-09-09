package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import dev.dubhe.anvilcraft.client.renderer.MunSurfaceRenderer;
import dev.dubhe.anvilcraft.client.support.MunClientSky;
import dev.dubhe.anvilcraft.client.support.OverworldLikeClientState;
import dev.dubhe.anvilcraft.client.support.PowerGridSupport;
import dev.dubhe.anvilcraft.util.EnchantedGoldBlockPositions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
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
        if (level.dimension().equals(CelestialTravelManager.MUN_LEVEL)) return MunClientSky.sunlight(level);
        if (!CelestialTravelManager.isOverworldLike(level.dimension())) return original;
        return OverworldLikeClientState.modifySkyDarken(level, original);
    }

    @ModifyReturnValue(method = "getSkyColor", at = @At("RETURN"))
    private Vec3 anvilcraft$darkenOverworldLikeSkyColor(Vec3 original) {
        ClientLevel level = (ClientLevel) (Object) this;
        if (level.dimension().equals(CelestialTravelManager.MUN_LEVEL)) return Vec3.ZERO;
        if (!CelestialTravelManager.isOverworldLike(level.dimension())) return original;
        return original.scale(OverworldLikeClientState.environmentColorMultiplier(level));
    }

    @ModifyReturnValue(method = {"getShade(Lnet/minecraft/core/Direction;Z)F", "getShade(FFFZ)F"}, at = @At("RETURN"))
    private float anvilcraft$useDirectionalMunShading(float original) {
        ClientLevel level = (ClientLevel) (Object) this;
        return level.dimension().equals(CelestialTravelManager.MUN_LEVEL) && MunSurfaceRenderer.usesTerrainShader() ? 1 : original;
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
        MunClientSky.clear();
        MunSurfaceRenderer.clear();
        OverworldLikeClientState.clear();
        PowerGridSupport.clearAllGrid();
        EnchantedGoldBlockPositions.clear();
    }

    @Inject(method = "onChunkLoaded(Lnet/minecraft/world/level/ChunkPos;)V", at = @At("TAIL"))
    void scanEnchantedGoldBlocks(ChunkPos chunkPos, CallbackInfo ci) {
        MunSurfaceRenderer.onChunkChanged(chunkPos);
        LevelChunk chunk = ((ClientLevel) (Object) this).getChunk(chunkPos.x, chunkPos.z);
        EnchantedGoldBlockPositions.scanChunk(chunk);
    }

    @Inject(method = "setBlocksDirty", at = @At("TAIL"))
    private void anvilcraft$updateMunOcclusion(BlockPos pos, BlockState oldState, BlockState newState, CallbackInfo ci) {
        MunSurfaceRenderer.onBlockChanged((ClientLevel) (Object) this, pos, newState);
    }

    @Inject(method = "unload", at = @At("TAIL"))
    private void anvilcraft$unloadMunOcclusion(LevelChunk chunk, CallbackInfo ci) {
        MunSurfaceRenderer.onChunkChanged(chunk.getPos());
    }
}
