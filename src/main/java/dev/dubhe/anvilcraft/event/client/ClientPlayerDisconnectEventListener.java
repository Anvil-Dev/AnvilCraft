package dev.dubhe.anvilcraft.event.client;

import dev.dubhe.anvilcraft.api.event.client.ClientPlayerDisconnectEvent;
import dev.dubhe.anvilcraft.api.sound.SoundHelper;

import net.neoforged.bus.api.SubscribeEvent;

public class ClientPlayerDisconnectEventListener {
    @SuppressWarnings("unused")
    @SubscribeEvent
    public void onClientPlayerDisconnect(ClientPlayerDisconnectEvent event) {
        SoundHelper.INSTANCE.clear();
    }
}
