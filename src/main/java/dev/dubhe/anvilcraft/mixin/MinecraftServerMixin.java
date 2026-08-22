package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import dev.dubhe.anvilcraft.mixin.accessor.MinecraftServerAccessor;
import dev.dubhe.anvilcraft.worldgen.OverworldLikeGenerationBootstrap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.level.biome.BiomeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** Gives the built-in overworld-like level an independent biome zoom seed. */
@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Inject(method = "createLevels", at = @At("HEAD"))
    private void anvilcraft$prepareOverworldLikeGeneration(ChunkProgressListener listener, CallbackInfo ci) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        OverworldLikeGenerationBootstrap.prepare(
            server,
            ((MinecraftServerAccessor) server).getStorageSource()
        );
    }

    @ModifyArgs(
        method = "createLevels",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;<init>("
                + "Lnet/minecraft/server/MinecraftServer;"
                + "Ljava/util/concurrent/Executor;"
                + "Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;"
                + "Lnet/minecraft/world/level/storage/ServerLevelData;"
                + "Lnet/minecraft/resources/ResourceKey;"
                + "Lnet/minecraft/world/level/dimension/LevelStem;"
                + "Lnet/minecraft/server/level/progress/ChunkProgressListener;"
                + "ZJ"
                + "Ljava/util/List;"
                + "Z"
                + "Lnet/minecraft/world/RandomSequences;)V"
        )
    )
    private void anvilcraft$useOverworldLikeBiomeSeed(Args args) {
        if (!CelestialTravelManager.OVERWORLD_LIKE_LEVEL.equals(args.get(4))) return;
        MinecraftServer server = args.get(0);
        long dimensionSeed = OverworldLikeGenerationBootstrap.getActiveSeed(server);
        args.set(8, BiomeManager.obfuscateSeed(dimensionSeed));
    }
}
