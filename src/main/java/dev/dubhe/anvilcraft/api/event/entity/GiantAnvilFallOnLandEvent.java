package dev.dubhe.anvilcraft.api.event.entity;

import dev.dubhe.anvilcraft.entity.FallingGiantAnvilEntity;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

@Getter
public class GiantAnvilFallOnLandEvent extends EntityEvent<FallingGiantAnvilEntity> {
    private final float fallDistance;

    /**
     * 实体事件
     *
     * @param entity 实体
     * @param pos    位置
     * @param level  世界
     */
    public GiantAnvilFallOnLandEvent(FallingGiantAnvilEntity entity, BlockPos pos, Level level, float fallDistance) {
        super(entity, pos, level);
        this.fallDistance = fallDistance;
    }
}
