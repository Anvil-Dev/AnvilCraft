package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.celestial.StellarTrackLibrary;
import dev.dubhe.anvilcraft.recipe.anvil.outcome.RoyalPreferenceOutcome;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Unit;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class ReloadEventListener {
    private static final Identifier ROYAL_PREFERENCE = AnvilCraft.of("royal_preference");

    @SuppressWarnings("ConstantValue")
    @SubscribeEvent
    public static void onServerReload(AddServerReloadListenersEvent event) {
        event.addListener(AnvilCraft.of("stellar_tracks"), (state, executor, barrier, reloadExecutor) ->
            barrier.wait(Unit.INSTANCE).thenRunAsync(() -> StellarTrackLibrary.reload(state.resourceManager()), reloadExecutor));
        event.addListener(
            ReloadEventListener.ROYAL_PREFERENCE,
            (_, _, barrier, _) -> {
                MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                if (server != null && server.overworld() != null) {
                    RoyalPreferenceOutcome.RoyalPreference.initRoyalPreference(server.overworld().getSeed());
                }
                return barrier.wait(Unit.INSTANCE).thenAccept(ignored -> {
                });
            }
        );
    }
}
