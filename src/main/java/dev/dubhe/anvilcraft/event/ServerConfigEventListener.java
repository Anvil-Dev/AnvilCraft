package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.network.ServerConfigSyncPacket;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.nio.charset.StandardCharsets;

/** NeoForge 1.21 synchronizes server configs at login but does not broadcast reloads. */
@EventBusSubscriber(modid = AnvilCraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class ServerConfigEventListener {
    private ServerConfigEventListener() {
    }

    @SubscribeEvent
    public static void onReload(ModConfigEvent.Reloading event) {
        var config = event.getConfig();
        var server = ServerLifecycleHooks.getCurrentServer();
        var loaded = config.getLoadedConfig();
        if (server == null || config.getType() != ModConfig.Type.SERVER || loaded == null) return;
        var format = loaded.config().configFormat();
        if (format.isInMemory()) return;
        byte[] contents = format.createWriter().writeToString(loaded.config()).getBytes(StandardCharsets.UTF_8);
        server.execute(() -> PacketDistributor.sendToAllPlayers(new ServerConfigSyncPacket(config.getFileName(), contents)));
    }
}
