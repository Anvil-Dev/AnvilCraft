package dev.dubhe.anvilcraft.api.event.server;

import net.minecraft.server.MinecraftServer;

public class ServerEndTickEvent extends ServerEvent {
    public ServerEndTickEvent(MinecraftServer server) {
        super(server);
    }
}
