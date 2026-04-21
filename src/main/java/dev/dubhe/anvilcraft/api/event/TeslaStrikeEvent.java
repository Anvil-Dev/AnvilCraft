package dev.dubhe.anvilcraft.api.event;

import dev.dubhe.anvilcraft.block.entity.TeslaTowerBlockEntity;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.ICancellableEvent;

@Getter
public abstract class TeslaStrikeEvent extends BlockEntityEvent {
    private final TeslaTowerBlockEntity entity;

    public TeslaStrikeEvent(Level level, TeslaTowerBlockEntity entity) {
        super(level, entity);
        this.entity = entity;
    }

    @Getter
    public static class TargetEntity extends TeslaStrikeEvent implements ICancellableEvent {
        private final Entity target;

        public TargetEntity(Level level, TeslaTowerBlockEntity entity, Entity target) {
            super(level, entity);
            this.target = target;
        }
    }

    @Getter
    public static class TargetBlock extends TeslaStrikeEvent implements ICancellableEvent {
        private final BlockPos targetPos;
        private final BlockState targetState;

        public TargetBlock(Level level, TeslaTowerBlockEntity entity, BlockPos targetPos) {
            super(level, entity);
            this.targetPos = targetPos;
            this.targetState = level.getBlockState(targetPos);
        }
    }
}
