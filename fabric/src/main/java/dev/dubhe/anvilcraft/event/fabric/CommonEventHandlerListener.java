package dev.dubhe.anvilcraft.event.fabric;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.client.ClientPlayerDisconnectEvent;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

public class CommonEventHandlerListener {
    public static void inti() {
        ClientPlayConnectionEvents.DISCONNECT.register(CommonEventHandlerListener::onClientPlayerDisconnect);
    }

    private static void onClientPlayerDisconnect(ClientPacketListener handler, Minecraft client) {
        AnvilCraft.EVENT_BUS.post(new ClientPlayerDisconnectEvent());
    }
}
