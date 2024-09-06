package dev.dubhe.anvilcraft.api.event.server.block;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ServerBlockEntityLoadEvent extends ServerBlockEntityEvent {
    public ServerBlockEntityLoadEvent(ServerLevel serverLevel, BlockEntity blockEntity) {
        super(serverLevel, blockEntity);
    }
}
