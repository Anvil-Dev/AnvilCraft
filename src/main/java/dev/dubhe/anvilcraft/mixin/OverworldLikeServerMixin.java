package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import dev.dubhe.anvilcraft.mixin.accessor.MinecraftServerAccessor;
import dev.dubhe.anvilcraft.worldgen.OverworldLikeGenerationBootstrap;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(MinecraftServer.class)
abstract class OverworldLikeServerMixin {
    @Inject(method = "createLevels", at = @At("HEAD"))
    private void anvilcraft$prepareGeneration(CallbackInfo ci) {
        var server = (MinecraftServer) (Object) this;
        OverworldLikeGenerationBootstrap.prepare(server, ((MinecraftServerAccessor) server).anvilcraft$getStorageSource());
    }

    @Inject(method = "createLevels", at = @At("RETURN"))
    private void anvilcraft$configureBorder(CallbackInfo ci) {
        OverworldLikeGenerationBootstrap.configureOverworldLikeBorder((MinecraftServer) (Object) this);
    }

    @ModifyArgs(method = "createLevels", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/level/ServerLevel;<init>(Lnet/minecraft/server/MinecraftServer;"
            + "Ljava/util/concurrent/Executor;Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;"
            + "Lnet/minecraft/world/level/storage/ServerLevelData;Lnet/minecraft/resources/ResourceKey;"
            + "Lnet/minecraft/world/level/dimension/LevelStem;ZJLjava/util/List;Z)V"))
    private void anvilcraft$seedGeneration(Args args) {
        if (CelestialTravelManager.OVERWORLD_LIKE_LEVEL.equals(args.get(4))) {
            args.set(5, OverworldLikeGenerationBootstrap.seededStem(args.get(0), args.get(5)));
        }
    }
}
