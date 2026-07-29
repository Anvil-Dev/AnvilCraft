package dev.dubhe.anvilcraft.api.event;

import dev.dubhe.anvilcraft.api.pointer.ITargetPointer;
import dev.dubhe.anvilcraft.block.entity.SmartBlockPlacerBlockEntity.ExecutionPhase;
import dev.dubhe.anvilcraft.block.entity.SmartBlockPlacerBlockEntity.OperationMode;
import dev.dubhe.anvilcraft.block.entity.SmartBlockPlacerBlockEntity.TargetMode;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

import javax.annotation.Nullable;

@Getter
public class SmartBlockPlacerFindPointerEvent extends Event implements ICancellableEvent {
    private final ServerLevel level;
    private final BlockPos sourcePos;
    private final Direction facing;
    private final OperationMode operation;
    private final TargetMode target;
    private final ExecutionPhase phase;
    private final @Nullable BlockState requiredState;
    @Setter
    private @Nullable ITargetPointer pointer;

    public SmartBlockPlacerFindPointerEvent(
        ServerLevel level,
        BlockPos sourcePos,
        Direction facing,
        OperationMode operation,
        TargetMode target,
        ExecutionPhase phase,
        @Nullable BlockState requiredState
    ) {
        this.level = level;
        this.sourcePos = sourcePos;
        this.facing = facing;
        this.operation = operation;
        this.target = target;
        this.phase = phase;
        this.requiredState = requiredState;
    }
}
