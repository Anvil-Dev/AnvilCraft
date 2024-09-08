package dev.dubhe.anvilcraft.event.forge;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.RipeningManager;
import dev.dubhe.anvilcraft.api.chargecollector.ThermoManager;
import dev.dubhe.anvilcraft.api.event.forge.DataPackReloadedEvent;
import dev.dubhe.anvilcraft.api.event.server.ServerEndDataPackReloadEvent;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.api.world.load.RandomChuckTickLoadManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class ServerLifecycleEventListener {
    /**
     * @param event 服务器启动事件
     */
    @SubscribeEvent
    public static void serverStarted(@NotNull ServerStartedEvent event) {
        AnvilCraft.EVENT_BUS.post(new dev.dubhe.anvilcraft.api.event.server.ServerStartedEvent(event.getServer()));
    }

    /**
     * @param event 数据包重载事件
     */
    @SubscribeEvent
    public static void onDataPackReloaded(@NotNull DataPackReloadedEvent event) {
        MinecraftServer server = event.getServer();
        CloseableResourceManager resourceManager = event.getResourceManager();
        AnvilCraft.EVENT_BUS.post(new ServerEndDataPackReloadEvent(server, resourceManager));
    }

    /**
     * @param event 服务器刻事件
     */
    @SubscribeEvent
    public static void onTick(@NotNull ServerTickEvent.Pre event) {
        PowerGrid.tickGrid();
        RipeningManager.tickAll();
        ThermoManager.tick();
        RandomChuckTickLoadManager.tick();
    }

    /**
     * @param event 服务器关闭事件
     */
    @SubscribeEvent
    public static void onServerStopped(@NotNull ServerStoppedEvent event) {
        PowerGrid.isServerClosing = false;
        PowerGrid.clear();
        ThermoManager.clear();
    }

    /**
     * @param event 服务器关闭事件
     */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        PowerGrid.isServerClosing = true;
    }
}
