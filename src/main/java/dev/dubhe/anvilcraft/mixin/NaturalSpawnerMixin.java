package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.api.world.load.ChunkFeatureManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(NaturalSpawner.class)
public class NaturalSpawnerMixin {

    @Inject(
        method = "spawnForChunk",
        at = @At("HEAD")
    )
    private static void anvilcraft$onSpawnForChunkStart(ServerLevel level, LevelChunk chunk,
        NaturalSpawner.SpawnState state, List<MobCategory> spawningCategories, CallbackInfo ci) {
        ChunkFeatureManager.CURRENT_SPAWNING_CHUNK.set(chunk.getPos());
    }

    @Inject(
        method = "spawnForChunk",
        at = @At("RETURN")
    )
    private static void anvilcraft$onSpawnForChunkEnd(CallbackInfo ci) {
        ChunkFeatureManager.CURRENT_SPAWNING_CHUNK.remove();
    }
}