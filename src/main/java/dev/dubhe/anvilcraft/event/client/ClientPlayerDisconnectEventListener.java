package dev.dubhe.anvilcraft.event.client;

import dev.anvilcraft.lib.event.SubscribeEvent;
import dev.dubhe.anvilcraft.api.event.client.ClientPlayerDisconnectEvent;
import dev.dubhe.anvilcraft.api.sound.SoundHelper;

public class ClientPlayerDisconnectEventListener {
    @SuppressWarnings("unused")
    @SubscribeEvent
    public void onClientPlayerDisconnect(ClientPlayerDisconnectEvent event) {
        SoundHelper.INSTANCE.clear();
    }
}
