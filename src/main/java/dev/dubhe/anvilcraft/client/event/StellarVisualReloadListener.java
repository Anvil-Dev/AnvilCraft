package dev.dubhe.anvilcraft.client.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.celestial.StellarTrackLibrary;
import dev.dubhe.anvilcraft.client.renderer.blockentity.celestial.CelestialBodyTextureBakery;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

/** 客户端资源重载时清理恒星动态贴图缓存。 */
@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class StellarVisualReloadListener {
    private StellarVisualReloadListener() {
    }

    @SubscribeEvent
    public static void register(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((barrier, manager, preparationsProfiler, reloadProfiler, backgroundExecutor, gameExecutor) -> {
            return barrier.wait(null).thenRunAsync(() -> {
                CelestialBodyTextureBakery.clearCache();
                StellarTrackLibrary.reload(manager);
            }, gameExecutor);
        });
    }
}
