package dev.dubhe.anvilcraft.integration.jei.event;

import dev.anvilcraft.lib.v2.integration.Integration;
import dev.anvilcraft.lib.v2.integration.IntegrationHook;
import dev.anvilcraft.lib.v2.integration.IntegrationType;
import dev.dubhe.anvilcraft.integration.jei.category.PortalConversionCategory;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

@Integration(value = "jei", type = IntegrationType.CLIENT)
public class ReloadEventListener {
    @SuppressWarnings("unused")
    public void applyClient() {
        IntegrationHook.getModEventBus().addListener(ReloadEventListener::onReload);
    }

    public static void onReload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((barrier, manager, preparationsProfiler, reloadProfiler, backgroundExecutor, gameExecutor) -> {
            PortalConversionCategory.IMAGE_SIZE_CACHE.clear();
            return barrier.wait(null);
        });
    }
}
