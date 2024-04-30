package dev.dubhe.anvilcraft.event.fabric;

import dev.dubhe.anvilcraft.api.chargecollector.ChargeCollectorManager;
import dev.dubhe.anvilcraft.client.renderer.PowerGridRenderer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

public class ServerWorldEventListener {
    /**
     * 初始化
     */
    public static void init() {
        ServerWorldEvents.UNLOAD.register(ServerWorldEventListener::onUnload);
    }

    private static void onUnload(MinecraftServer server, Level level) {
        PowerGridRenderer.cleanAllGrid();
        ChargeCollectorManager.cleanMap();
    }
}
