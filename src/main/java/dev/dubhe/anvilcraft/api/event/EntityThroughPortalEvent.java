package dev.dubhe.anvilcraft.api.event;

import dev.dubhe.anvilcraft.api.portal.PortalType;
import lombok.Getter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.entity.EntityEvent;

@Getter
public class EntityThroughPortalEvent extends EntityEvent implements ICancellableEvent {
    private final Level level;
    private final PortalType type;

    public EntityThroughPortalEvent(Level level, Entity entity, PortalType type) {
        super(entity);
        this.level = level;
        this.type = type;
    }
}
