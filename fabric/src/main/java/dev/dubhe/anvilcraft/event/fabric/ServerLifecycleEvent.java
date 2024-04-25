package dev.dubhe.anvilcraft.event.fabric;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.server.ServerEndDataPackReloadEvent;
import dev.dubhe.anvilcraft.api.event.server.ServerStartedEvent;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.util.fabric.ServerHooks;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.CloseableResourceManager;

public class ServerLifecycleEvent {
    /**
     * 初始化
     */
    public static void init() {
        ServerLifecycleEvents.SERVER_STARTED.register(ServerLifecycleEvent::serverStarted);
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register(ServerLifecycleEvent::endDataPackReload);
        ServerLifecycleEvents.SERVER_STOPPING.register(ServerLifecycleEvent::onServerStopping);
        ServerLifecycleEvents.SERVER_STOPPED.register(ServerLifecycleEvent::onServerStopped);
        ServerTickEvents.START_SERVER_TICK.register(ServerLifecycleEvent::startTick);
    }

    private static void serverStarted(MinecraftServer server) {
        ServerHooks.setServer(server);
        AnvilCraft.EVENT_BUS.post(new ServerStartedEvent(server));
    }

    private static void endDataPackReload(
        MinecraftServer server, CloseableResourceManager resourceManager, boolean success
    ) {
        AnvilCraft.EVENT_BUS.post(new ServerEndDataPackReloadEvent(server, resourceManager));
    }

    private static void startTick(MinecraftServer server) {
        PowerGrid.tickGrid();
    }

    private static void onServerStopping(MinecraftServer server) {
        PowerGrid.isServerClosing = true;
    }

    private static void onServerStopped(MinecraftServer server) {
        PowerGrid.isServerClosing = false;
        PowerGrid.clear();
    }
}
