package dev.dubhe.anvilcraft.api.event.server;

import lombok.Getter;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.CloseableResourceManager;

@Getter
public class ServerStartDataPackReloadEvent extends ServerEvent {
    private final CloseableResourceManager resourceManager;
    public ServerStartDataPackReloadEvent(MinecraftServer server, CloseableResourceManager resourceManager) {
        super(server);
        this.resourceManager = resourceManager;
    }
}
