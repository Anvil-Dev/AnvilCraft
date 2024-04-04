package dev.dubhe.anvilcraft.api.event.server;

import lombok.Getter;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

@Getter
public class ServerLevelLoadEvent extends ServerEvent {
    private final ServerLevel level;
    public ServerLevelLoadEvent(MinecraftServer server, ServerLevel level) {
        super(server);
        this.level = level;
    }
}
