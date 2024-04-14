package dev.dubhe.anvilcraft.event.forge;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.forge.DataPackReloadedEvent;
import dev.dubhe.anvilcraft.api.event.server.ServerEndDataPackReloadEvent;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
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
     * @param event 世界刻事件
     */
    @SubscribeEvent
    public static void onWordTick(@NotNull TickEvent.LevelTickEvent event) {
        if (event.level.isClientSide) return;
        if (event.phase != TickEvent.Phase.START) return;
        PowerGrid.tickGrid();
    }
}
