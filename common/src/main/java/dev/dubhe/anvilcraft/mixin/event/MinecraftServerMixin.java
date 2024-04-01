package dev.dubhe.anvilcraft.mixin.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.server.ServerEndDataPackReloadEvent;
import dev.dubhe.anvilcraft.api.event.server.ServerEndTickEvent;
import dev.dubhe.anvilcraft.api.event.server.ServerLevelLoadEvent;
import dev.dubhe.anvilcraft.api.event.server.ServerLevelUnloadEvent;
import dev.dubhe.anvilcraft.api.event.server.ServerStartDataPackReloadEvent;
import dev.dubhe.anvilcraft.api.event.server.ServerStartTickEvent;
import dev.dubhe.anvilcraft.api.event.server.ServerStartedEvent;
import dev.dubhe.anvilcraft.api.event.server.ServerStartingEvent;
import dev.dubhe.anvilcraft.api.event.server.ServerStoppedEvent;
import dev.dubhe.anvilcraft.api.event.server.ServerStoppingEvent;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.ServerLevelData;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;

@Mixin(value = MinecraftServer.class)
abstract class MinecraftServerMixin {
    @Shadow
    private MinecraftServer.ReloadableResources resources;

    @Unique
    private final MinecraftServer server = (MinecraftServer) (Object) this;

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;initServer()Z"), method = "runServer")
    private void beforeSetupServer(CallbackInfo info) {
        AnvilCraft.EVENT_BUS.post(new ServerStartingEvent(server));
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;buildServerStatus()Lnet/minecraft/network/protocol/status/ServerStatus;"), method = "runServer")
    private void afterSetupServer(CallbackInfo info) {
        AnvilCraft.EVENT_BUS.post(new ServerStartedEvent(server));
    }

    @Inject(at = @At("HEAD"), method = "stopServer")
    private void beforeShutdownServer(CallbackInfo info) {
        AnvilCraft.EVENT_BUS.post(new ServerStoppingEvent(server));
    }

    @Inject(at = @At("TAIL"), method = "stopServer")
    private void afterShutdownServer(CallbackInfo info) {
        AnvilCraft.EVENT_BUS.post(new ServerStoppedEvent(server));
    }

    @Inject(method = "tickServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;tickChildren(Ljava/util/function/BooleanSupplier;)V"))
    private void onStartTick(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        AnvilCraft.EVENT_BUS.post(new ServerStartTickEvent(server));
    }

    @Inject(at = @At("TAIL"), method = "tickServer")
    private void onEndTick(BooleanSupplier shouldKeepTicking, CallbackInfo info) {
        AnvilCraft.EVENT_BUS.post(new ServerEndTickEvent(server));
    }

    @Inject(method = "createLevels", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getDataStorage()Lnet/minecraft/world/level/storage/DimensionDataStorage;"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void onLoadLevel(ChunkProgressListener listener, CallbackInfo ci, ServerLevelData serverLevelData, boolean bl, Registry<LevelStem> registry, WorldOptions worldOptions, long l, long m, List<CustomSpawner> list, LevelStem levelStem, ServerLevel serverLevel) {
        AnvilCraft.EVENT_BUS.post(new ServerLevelLoadEvent(server, serverLevel));
    }

    @Inject(method = "createLevels", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/border/WorldBorder;addListener(Lnet/minecraft/world/level/border/BorderChangeListener;)V"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void onLoadLevel(ChunkProgressListener listener, CallbackInfo ci, ServerLevelData serverLevelData, boolean bl, Registry<LevelStem> registry, WorldOptions worldOptions, long l, long m, List<CustomSpawner> list, LevelStem levelStem, ServerLevel serverLevel, DimensionDataStorage dimensionDataStorage, WorldBorder worldBorder, RandomSequences randomSequences, Iterator<Map.Entry<ResourceKey<LevelStem>, LevelStem>> var16, Map.Entry<ResourceKey<LevelStem>, LevelStem> entry, ResourceKey<LevelStem> resourceKey, ResourceKey<Level> resourceKey2, DerivedLevelData derivedLevelData, ServerLevel serverLevel2) {
        AnvilCraft.EVENT_BUS.post(new ServerLevelLoadEvent(server, serverLevel2));
    }

    @Inject(method = "stopServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;close()V"), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private void onUnloadLevelAtShutdown(CallbackInfo ci, Iterator<ServerLevel> levels, ServerLevel level) {
        AnvilCraft.EVENT_BUS.post(new ServerLevelUnloadEvent(server, level));
    }

    @Inject(method = "reloadResources", at = @At("HEAD"))
    private void startResourceReload(Collection<String> collection, CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        AnvilCraft.EVENT_BUS.post(new ServerStartDataPackReloadEvent(server, this.resources.resourceManager()));
    }

    @Inject(method = "reloadResources", at = @At("TAIL"))
    private void endResourceReload(Collection<String> collection, @NotNull CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        cir.getReturnValue().handleAsync((value, throwable) -> {
            AnvilCraft.EVENT_BUS.post(new ServerEndDataPackReloadEvent(server, this.resources.resourceManager()));
            return value;
        }, (MinecraftServer) (Object) this);
    }
}
