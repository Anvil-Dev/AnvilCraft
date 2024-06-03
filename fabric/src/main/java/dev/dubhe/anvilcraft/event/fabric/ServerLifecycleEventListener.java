package dev.dubhe.anvilcraft.event.fabric;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.chargecollector.ThermoManager;
import dev.dubhe.anvilcraft.api.event.server.ServerEndDataPackReloadEvent;
import dev.dubhe.anvilcraft.api.event.server.ServerStartedEvent;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.api.world.load.RandomChuckTickLoadManager;
import dev.dubhe.anvilcraft.util.fabric.ServerHooks;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.CloseableResourceManager;

public class ServerLifecycleEventListener {
    /**
     * 初始化
     */
    public static void init() {
        ServerLifecycleEvents.SERVER_STARTED.register(ServerLifecycleEventListener::serverStarted);
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register(ServerLifecycleEventListener::endDataPackReload);
        ServerLifecycleEvents.SERVER_STOPPING.register(ServerLifecycleEventListener::onServerStopping);
        ServerLifecycleEvents.SERVER_STOPPED.register(ServerLifecycleEventListener::onServerStopped);
        ServerTickEvents.START_SERVER_TICK.register(ServerLifecycleEventListener::startTick);
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
        ThermoManager.tick();
        RandomChuckTickLoadManager.tick();
    }

    private static void onServerStopping(MinecraftServer server) {
        PowerGrid.isServerClosing = true;
    }

    private static void onServerStopped(MinecraftServer server) {
        PowerGrid.isServerClosing = false;
        PowerGrid.clear();
        ThermoManager.clear();
    }
}
