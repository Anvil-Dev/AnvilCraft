package dev.dubhe.anvilcraft.event.fabric;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.server.block.ServerBlockEntityLoadEvent;
import dev.dubhe.anvilcraft.api.event.server.block.ServerBlockEntityUnloadEvent;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import dev.dubhe.anvilcraft.api.event.client.ClientPlayerDisconnectEvent;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

public class CommonEventHandlerListener {
    public static void serverInit() {
        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register(CommonEventHandlerListener::onServerBlockEntityLoad);
        ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register(CommonEventHandlerListener::onServerBlockEntityUnload);
    }
    
    public static void clientInit() {
        ClientPlayConnectionEvents.DISCONNECT.register(CommonEventHandlerListener::onClientPlayerDisconnect);
    }

    private static void onServerBlockEntityLoad(BlockEntity entity, ServerLevel level) {
        AnvilCraft.EVENT_BUS.post(new ServerBlockEntityLoadEvent(level, entity));
    }

    private static void onServerBlockEntityUnload(BlockEntity entity, ServerLevel level) {
        AnvilCraft.EVENT_BUS.post(new ServerBlockEntityUnloadEvent(level, entity));
    }

    private static void onClientPlayerDisconnect(ClientPacketListener handler, Minecraft client) {
        AnvilCraft.EVENT_BUS.post(new ClientPlayerDisconnectEvent());
    }
}
