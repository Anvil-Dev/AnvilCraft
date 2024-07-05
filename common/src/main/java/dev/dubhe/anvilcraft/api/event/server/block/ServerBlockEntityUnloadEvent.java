package dev.dubhe.anvilcraft.api.event.server.block;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ServerBlockEntityUnloadEvent extends ServerBlockEntityEvent {
    public ServerBlockEntityUnloadEvent(ServerLevel serverLevel, BlockEntity blockEntity) {
        super(serverLevel, blockEntity);
    }
}
