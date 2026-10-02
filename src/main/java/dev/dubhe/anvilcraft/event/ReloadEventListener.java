package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.celestial.StellarTrackLibrary;
import dev.dubhe.anvilcraft.recipe.anvil.outcome.RoyalPreferenceOutcome;
import net.minecraft.util.Unit;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class ReloadEventListener {
    @SubscribeEvent
    public static void onServerReload(AddServerReloadListenersEvent event) {
        event.addListener(AnvilCraft.of("stellar_tracks"), (state, executor, barrier, reloadExecutor) ->
            barrier.wait(Unit.INSTANCE).thenRunAsync(() -> StellarTrackLibrary.reload(state.resourceManager()), reloadExecutor));
    }

    @SubscribeEvent
    public static void onServerTagsUpdated(TagsUpdatedEvent.ServerDataLoad event) {
        RoyalPreferenceOutcome.RoyalPreference.clear();
    }
}
