package dev.dubhe.anvilcraft.event;

import dev.anvilcraft.resource.ageratum.Ageratum;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.CheckIntegrationLoadedEvent;
import dev.dubhe.anvilcraft.api.event.GuideBookEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class GuideEventListener {
    public static final String INDEX_FILE = "index";

    @SubscribeEvent
    public static void onHasGuide(GuideBookEvent.HasGuideBookEvent event) {
        event.hasGuideBook();
    }

    @SubscribeEvent
    public static void onHasGuide(CheckIntegrationLoadedEvent event) {
        if (event.getId().equals(AnvilCraft.MOD_ID)) {
            event.setLoaded();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onOpenGuide(GuideBookEvent.OpenGuideBookEvent event) {
        Ageratum.openGuide(event.getPlayer(), AnvilCraft.of(INDEX_FILE));
        event.setCanceled(true);
    }
}
