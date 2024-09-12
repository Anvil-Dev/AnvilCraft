package dev.dubhe.anvilcraft.api.event.forge;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.neoforged.neoforge.event.server.ServerLifecycleEvent;

import lombok.Getter;

@Getter
public class DataPackReloadedEvent extends ServerLifecycleEvent {
    private final CloseableResourceManager resourceManager;

    public DataPackReloadedEvent(MinecraftServer server, CloseableResourceManager resourceManager) {
        super(server);
        this.resourceManager = resourceManager;
    }
}
