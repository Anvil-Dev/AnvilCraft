package dev.dubhe.anvilcraft.api.event.server;

import lombok.Getter;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.Event;

@Getter
public class ServerEvent extends Event {
    private final MinecraftServer server;

    public ServerEvent(MinecraftServer server) {
        this.server = server;
    }
}
