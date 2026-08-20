package dev.dubhe.anvilcraft.block.entity;

import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;

/** Shared server-to-client synchronization for CFA block entities. */
final class CfaBlockEntitySync {
    private CfaBlockEntitySync() {
    }

    static void sendToTracking(BlockEntity blockEntity, @Nullable Packet<?> packet) {
        if (packet == null || !(blockEntity.getLevel() instanceof ServerLevel level)) return;
        for (var player : level.getChunkSource().chunkMap.getPlayers(
            level.getChunkAt(blockEntity.getBlockPos()).getPos(), false
        )) {
            player.connection.send(packet);
        }
    }
}
