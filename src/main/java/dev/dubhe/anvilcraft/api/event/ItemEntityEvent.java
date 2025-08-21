package dev.dubhe.anvilcraft.api.event;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityEvent;

@Getter
public class ItemEntityEvent extends EntityEvent {
    private final Level level;
    private final ItemEntity entity;

    public ItemEntityEvent(Level level, ItemEntity entity) {
        super(entity);
        this.level = level;
        this.entity = entity;
    }

    @Getter
    public static class InToBlock extends ItemEntityEvent {
        private final BlockPos blockPos;
        private final Vec3 pos;
        private final Vec3 motion;

        public InToBlock(Level level, ItemEntity entity, BlockPos blockPos, Vec3 pos, Vec3 motion) {
            super(level, entity);
            this.blockPos = blockPos;
            this.pos = pos;
            this.motion = motion;
        }
    }
}
