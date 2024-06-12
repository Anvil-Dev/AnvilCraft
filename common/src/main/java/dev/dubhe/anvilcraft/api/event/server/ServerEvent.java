package dev.dubhe.anvilcraft.api.event.server;

import net.minecraft.server.MinecraftServer;

import lombok.Getter;

@Getter
public class ServerEvent {
    private final MinecraftServer server;

    public ServerEvent(MinecraftServer server) {
        this.server = server;
    }
}
