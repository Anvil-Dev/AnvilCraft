package dev.dubhe.anvilcraft.api.event.server;

import net.minecraft.server.MinecraftServer;

public class ServerStartTickEvent extends ServerEvent {
    public ServerStartTickEvent(MinecraftServer server) {
        super(server);
    }
}
