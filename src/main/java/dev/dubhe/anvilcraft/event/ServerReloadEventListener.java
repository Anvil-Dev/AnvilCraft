package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.amulet.AmuletManager;
import dev.dubhe.anvilcraft.api.amulet.effect.ActAsScarecrowAmuletEffect;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class ServerReloadEventListener {
    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) {
            LargeCauldronBlockEntity.invalidateIgnitionTags();
        }
    }

    @SubscribeEvent
    public static void onServerReload(AddReloadListenerEvent event) {
        event.addListener((barrier, manager, preparationsProfiler, reloadProfiler, backgroundExecutor, gameExecutor) -> {
            AmuletManager.clear();
            ActAsScarecrowAmuletEffect.clear();
            // noinspection DataFlowIssue
            return barrier.wait(null);
        });
    }
}
