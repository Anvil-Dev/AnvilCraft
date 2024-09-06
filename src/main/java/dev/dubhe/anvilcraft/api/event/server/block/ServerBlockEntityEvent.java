package dev.dubhe.anvilcraft.api.event.server.block;

import lombok.Getter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

@Getter
abstract class ServerBlockEntityEvent {
    private final ServerLevel serverLevel;
    private final BlockEntity blockEntity;

    ServerBlockEntityEvent(ServerLevel serverLevel, BlockEntity blockEntity) {
        this.serverLevel = serverLevel;
        this.blockEntity = blockEntity;
    }
}
