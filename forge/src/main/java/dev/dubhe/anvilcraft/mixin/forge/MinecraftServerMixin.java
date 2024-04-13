package dev.dubhe.anvilcraft.mixin.forge;

import dev.dubhe.anvilcraft.api.event.forge.DataPackReloadedEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

@Mixin(value = MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Shadow
    private MinecraftServer.ReloadableResources resources;

    @SuppressWarnings("UnreachableCode")
    @Inject(
        method = "reloadResources", at = @At("TAIL"))
    private void endResourceReload(
        Collection<String> collection, @NotNull CallbackInfoReturnable<CompletableFuture<Void>> cir
    ) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        CloseableResourceManager resourceManager = this.resources.resourceManager();
        cir.getReturnValue().handleAsync((value, throwable) -> {
            MinecraftForge.EVENT_BUS.post(new DataPackReloadedEvent(server, resourceManager));
            return value;
        }, server);
    }
}
