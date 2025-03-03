package dev.dubhe.anvilcraft.api.event.anvil;

import dev.dubhe.anvilcraft.entity.FallingGiantAnvilEntity;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.EntityEvent;

@Getter
public class GiantAnvilFallOnLandEvent extends EntityEvent {
    private final float fallDistance;
    private final FallingGiantAnvilEntity entity;
    private final Level level;
    private final BlockPos pos;

    /**
     * 实体事件
     *
     * @param entity 实体
     * @param pos    位置
     * @param level  世界
     */
    public GiantAnvilFallOnLandEvent(FallingGiantAnvilEntity entity, BlockPos pos, Level level, float fallDistance) {
        super(entity);
        this.entity = entity;
        this.fallDistance = fallDistance;
        this.level = level;
        this.pos = pos;
    }
}
