package dev.dubhe.anvilcraft.api.event.anvil;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;

import lombok.Getter;
import lombok.Setter;
import net.neoforged.neoforge.event.entity.EntityEvent;


@Getter
public class AnvilFallOnLandEvent extends EntityEvent {
    @Setter
    private boolean isAnvilDamage;
    private final FallingBlockEntity entity;
    private final float fallDistance;
    private final Level level;
    private final BlockPos pos;

    /**
     * 铁砧落地事件
     *
     * @param level        世界
     * @param pos          位置
     * @param entity       铁砧实体
     * @param fallDistance 下落距离
     */
    public AnvilFallOnLandEvent(Level level, BlockPos pos, FallingBlockEntity entity, float fallDistance) {
        super(entity);
        this.entity = entity;
        this.level = level;
        this.pos = pos;
        this.fallDistance = fallDistance;
        this.isAnvilDamage = false;
    }
}
