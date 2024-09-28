package dev.dubhe.anvilcraft.event.server;

import dev.dubhe.anvilcraft.api.hammer.HammerManager;
import dev.dubhe.anvilcraft.api.world.load.LevelLoadManager;
import dev.dubhe.anvilcraft.init.ModHammerInits;

import net.neoforged.neoforge.event.server.ServerStartedEvent;

public class ServerEventListener {
    /**
     * 服务器开启事件
     *
     * @param event 事件
     */
    public static void onServerStarted(ServerStartedEvent event) {
        ModHammerInits.init();
        HammerManager.register();
        LevelLoadManager.notifyServerStarted();
    }
}
