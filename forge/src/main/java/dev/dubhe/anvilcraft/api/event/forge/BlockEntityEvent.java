package dev.dubhe.anvilcraft.api.event.forge;

import lombok.Getter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.eventbus.api.Event;

@Getter
public class BlockEntityEvent extends Event {
    private final Level level;
    private final BlockEntity entity;

    public BlockEntityEvent(Level level, BlockEntity entity) {
        this.level = level;
        this.entity = entity;
    }

    public static class ServerLoad extends BlockEntityEvent {
        public ServerLoad(Level level, BlockEntity entity) {
            super(level, entity);
        }
    }

    public static class ServerUnload extends BlockEntityEvent {
        public ServerUnload(Level level, BlockEntity entity) {
            super(level, entity);
        }
    }
}
