package dev.dubhe.anvilcraft.api.event.forge;

import lombok.Getter;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraftforge.event.server.ServerLifecycleEvent;

@Getter
public class DataPackReloadedEvent extends ServerLifecycleEvent {
    private final CloseableResourceManager resourceManager;

    public DataPackReloadedEvent(MinecraftServer server, CloseableResourceManager resourceManager) {
        super(server);
        this.resourceManager = resourceManager;
    }
}
