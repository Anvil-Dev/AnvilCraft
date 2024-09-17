package dev.dubhe.anvilcraft.api.event.server;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.CloseableResourceManager;

import lombok.Getter;

@Getter
public class ServerEndDataPackReloadEvent extends ServerEvent {
    private final CloseableResourceManager resourceManager;

    public ServerEndDataPackReloadEvent(MinecraftServer server, CloseableResourceManager resourceManager) {
        super(server);
        this.resourceManager = resourceManager;
    }
}
