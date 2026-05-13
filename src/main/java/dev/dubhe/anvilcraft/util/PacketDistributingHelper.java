package dev.dubhe.anvilcraft.util;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.entity.Entity;

public class PacketDistributingHelper {
    public static void sendToPlayersTrackingEntity(Entity entity, Packet<? super ClientGamePacketListener> packet) {
        if (entity.level().isClientSide()) {
            throw new IllegalStateException("Cannot send clientbound payloads on the client");
        } else if (entity.level().getChunkSource() instanceof ServerChunkCache chunkCache) {
            chunkCache.sendToTrackingPlayers(entity, packet);
        }
    }
}
