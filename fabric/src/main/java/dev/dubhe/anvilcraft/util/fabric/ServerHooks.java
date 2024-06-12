package dev.dubhe.anvilcraft.util.fabric;

import net.minecraft.server.MinecraftServer;

import lombok.Getter;
import lombok.Setter;

public class ServerHooks {
    @Setter
    @Getter
    private static MinecraftServer server = null;
}
