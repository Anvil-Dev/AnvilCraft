package dev.dubhe.anvilcraft.event.forge;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.forge.DataPackReloadedEvent;
import dev.dubhe.anvilcraft.api.event.server.ServerEndDataPackReloadEvent;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class ServerLifecycleEvent {
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
    public static void onTick(@NotNull TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        PowerGrid.tickGrid();
    }

    /**
     * @param event 服务器关闭事件
     */
    @SubscribeEvent
    public static void onServerStopped(@NotNull ServerStoppedEvent event) {
        PowerGrid.isServerClosing = false;
        PowerGrid.clear();
    }

    /**
     * @param event 服务器关闭事件
     */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        PowerGrid.isServerClosing = true;
    }
}
