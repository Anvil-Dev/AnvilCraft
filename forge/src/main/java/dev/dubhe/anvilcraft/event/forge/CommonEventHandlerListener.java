package dev.dubhe.anvilcraft.event.forge;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.client.ClientPlayerDisconnectEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class CommonEventHandlerListener {

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void clientPlayerDisconnectEvent(@NotNull PlayerEvent.PlayerLoggedOutEvent playerLoggedOutEvent) {
        AnvilCraft.EVENT_BUS.post(new ClientPlayerDisconnectEvent());
    }
}
