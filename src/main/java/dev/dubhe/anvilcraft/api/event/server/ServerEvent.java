package dev.dubhe.anvilcraft.api.event.server;

import lombok.Getter;
import net.minecraft.server.MinecraftServer;

@Getter
public class ServerEvent {
    private final MinecraftServer server;

    public ServerEvent(MinecraftServer server) {
        this.server = server;
    }
}
