package dev.dubhe.anvilcraft.api.event.server;

import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

import lombok.Getter;

@Getter
public class ServerEvent extends Event implements IModBusEvent {
    private final MinecraftServer server;

    public ServerEvent(MinecraftServer server) {
        this.server = server;
    }
}
