package dev.dubhe.anvilcraft.api.event.entity;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;

@Setter
@Getter
public class AnvilFallOnLandEvent extends EntityEvent<FallingBlockEntity> {
    private boolean isAnvilDamage;
    private final float fallDistance;

    /**
     * 铁砧落地事件
     *
     * @param level        世界
     * @param pos          位置
     * @param entity       铁砧实体
     * @param fallDistance 下落距离
     */
    public AnvilFallOnLandEvent(Level level, BlockPos pos, FallingBlockEntity entity, float fallDistance) {
        super(entity, pos, level);
        this.fallDistance = fallDistance;
        this.isAnvilDamage = false;
    }
}
