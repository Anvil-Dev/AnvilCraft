package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.api.amulet.AmuletManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

public class ServerReloadEventListener {
    @SubscribeEvent
    public static void onServerReload(AddReloadListenerEvent event) {
        event.addListener((barrier, manager, preparationsProfiler, reloadProfiler, backgroundExecutor, gameExecutor) -> {
            AmuletManager.clear();
            // noinspection DataFlowIssue
            return barrier.wait(null);
        });
    }
}
