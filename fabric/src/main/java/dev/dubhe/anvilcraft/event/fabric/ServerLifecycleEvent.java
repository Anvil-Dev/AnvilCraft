package dev.dubhe.anvilcraft.event.fabric;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.server.ServerEndDataPackReloadEvent;
import dev.dubhe.anvilcraft.api.event.server.ServerStartedEvent;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.utils.fabric.ServerHooks;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.CloseableResourceManager;

public class ServerLifecycleEvent {
    /**
     * 初始化
     */
    public static void init() {
        ServerLifecycleEvents.SERVER_STARTED.register(ServerLifecycleEvent::serverStarted);
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register(ServerLifecycleEvent::endDataPackReload);
        ServerTickEvents.START_WORLD_TICK.register(ServerLifecycleEvent::startWorldTick);
    }

    public static void serverStarted(MinecraftServer server) {
        ServerHooks.setServer(server);
        AnvilCraft.EVENT_BUS.post(new ServerStartedEvent(server));
    }

    public static void endDataPackReload(
        MinecraftServer server, CloseableResourceManager resourceManager, boolean success
    ) {
        AnvilCraft.EVENT_BUS.post(new ServerEndDataPackReloadEvent(server, resourceManager));
    }

    public static void startWorldTick(ServerLevel world) {
        PowerGrid.tickGrid();
    }
}
