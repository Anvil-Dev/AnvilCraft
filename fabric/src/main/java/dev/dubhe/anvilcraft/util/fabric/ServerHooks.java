package dev.dubhe.anvilcraft.util.fabric;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.server.MinecraftServer;

public class ServerHooks {
    @Setter
    @Getter
    private static MinecraftServer server = null;
}
