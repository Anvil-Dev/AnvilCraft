package dev.dubhe.anvilcraft.api.event.server.block;

import lombok.Getter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

@Getter
abstract class ServerBlockEntityEvent extends Event implements IModBusEvent {
    private final ServerLevel serverLevel;
    private final BlockEntity blockEntity;

    ServerBlockEntityEvent(ServerLevel serverLevel, BlockEntity blockEntity) {
        this.serverLevel = serverLevel;
        this.blockEntity = blockEntity;
    }
}
