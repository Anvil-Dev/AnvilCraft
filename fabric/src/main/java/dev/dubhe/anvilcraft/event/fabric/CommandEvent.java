package dev.dubhe.anvilcraft.event.fabric;

import dev.dubhe.anvilcraft.init.ModCommands;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class CommandEvent {
    /**
     * 初始化
     */
    public static void init() {
        CommandRegistrationCallback.EVENT.register(
            (dispatcher, registryAccess, environment) -> ModCommands.register(dispatcher)
        );
    }
}
