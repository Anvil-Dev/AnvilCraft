package dev.dubhe.anvilcraft.block.entity;

import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * 锻星砧相关方块实体共用的服务端到客户端同步入口。
 */
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
