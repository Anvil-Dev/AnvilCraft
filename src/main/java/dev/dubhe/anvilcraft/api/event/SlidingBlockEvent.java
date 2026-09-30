package dev.dubhe.anvilcraft.api.event;

import dev.dubhe.anvilcraft.entity.SlidingBlockEntity;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.EntityEvent;
import org.apache.commons.lang3.tuple.Triple;

import java.util.Optional;

/**
 * 滑轨滑动实体的生命周期。{@link Start} 在带结构数据的构造器完成时发布，方块移除时机由调用者决定；
 * {@link Stop} 在结构写回世界之后发布。
 */
@Getter
public class SlidingBlockEvent extends EntityEvent {
    private final SlidingBlockEntity entity;

    public SlidingBlockEvent(SlidingBlockEntity entity) {
        super(entity);
        this.entity = entity;
    }

    @Getter
    public static class Start extends SlidingBlockEvent {
        private final Level level;
        private final BlockPos origin;
        private final Direction movement;
        private final Iterable<Triple<BlockPos, BlockState, Optional<BlockEntity>>> blocks;

        public Start(
            SlidingBlockEntity entity,
            Level level,
            BlockPos origin,
            Direction movement,
            Iterable<Triple<BlockPos, BlockState, Optional<BlockEntity>>> blocks
        ) {
            super(entity);
            this.level = level;
            this.origin = origin;
            this.movement = movement;
            this.blocks = blocks;
        }
    }

    @Getter
    public static class Stop extends SlidingBlockEvent {
        private final Level level;

        public Stop(SlidingBlockEntity entity) {
            super(entity);
            this.level = entity.level();
        }
    }
}
