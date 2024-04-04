package dev.dubhe.anvilcraft.utils.fabric;

import lombok.Getter;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

public class ServerHooks {
    @Getter
    private static MinecraftServer server = null;

    public static void init() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> ServerHooks.server = server);
    }
}
