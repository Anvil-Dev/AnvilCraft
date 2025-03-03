package dev.dubhe.anvilcraft.api.event.anvil;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.EntityEvent;

@Getter
public class AnvilHurtEntityEvent extends EntityEvent {
    private final FallingBlockEntity entity;
    private final Entity hurtedEntity;
    private final float damage;
    private final Level level;
    private final BlockPos pos;

    /**
     * 铁砧伤害实体事件
     *
     * @param entity       铁砧实体
     * @param pos          位置
     * @param level        世界
     * @param hurtedEntity 被伤害的实体
     * @param damage       伤害
     */
    public AnvilHurtEntityEvent(
        FallingBlockEntity entity,
        BlockPos pos,
        Level level,
        Entity hurtedEntity,
        float damage
    ) {
        super(entity);
        this.entity = entity;
        this.hurtedEntity = hurtedEntity;
        this.damage = damage;
        this.level = level;
        this.pos = pos;
    }
}
