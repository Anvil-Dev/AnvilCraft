package dev.dubhe.anvilcraft.api.event.entity;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;

@Getter
public class AnvilHurtEntityEvent extends EntityEvent<FallingBlockEntity> {
    private final Entity hurtedEntity;
    private final float damage;

    public AnvilHurtEntityEvent(FallingBlockEntity entity, BlockPos pos, Level level, Entity hurtedEntity, float damage) {
        super(entity, pos, level);
        this.hurtedEntity = hurtedEntity;
        this.damage = damage;
    }
}
