package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.amulet.AmuletManager;
import dev.dubhe.anvilcraft.api.amulet.effect.ActAsScarecrowAmuletEffect;
import net.minecraft.util.Unit;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class ServerReloadEventListener {
    @SubscribeEvent
    public static void onServerReload(AddServerReloadListenersEvent event) {
        event.addListener(AnvilCraft.of("amulets"), (state, executor, barrier, reloadExecutor) ->
            barrier.wait(Unit.INSTANCE).thenRunAsync(() -> {
                AmuletManager.clear();
                ActAsScarecrowAmuletEffect.clear();
            }, reloadExecutor));
    }
}
