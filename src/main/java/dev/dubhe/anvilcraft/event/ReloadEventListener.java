package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.recipe.anvil.outcome.RoyalPreferenceOutcome;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
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
        event.addListener(
            ReloadEventListener.ROYAL_PREFERENCE,
            (_, _, barrier, _) -> {
                MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                if (server != null && server.overworld() != null) {
                    RoyalPreferenceOutcome.RoyalPreference.initRoyalPreference(server.overworld().getSeed());
                }
                return barrier.wait(null);
            }
        );
    }
}
