package dev.dubhe.anvilcraft.api.event.fabric;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;

import java.util.Arrays;

public class AnvilEvent {
    public static final Event<OnLand> ON_LAND = EventFactory.createArrayBacked(OnLand.class,
        callbacks -> (level, pos, entity, fallDistance) -> {
            boolean isAnvilDamage = false;
            for (OnLand callback : callbacks) {
                if (callback.onLand(level, pos, entity, fallDistance)) isAnvilDamage = true;
            }
            return isAnvilDamage;
        });
    public static Event<HurtEntity> HURT_ENTITY = EventFactory.createArrayBacked(HurtEntity.class,
        callbacks -> (entity, pos, level, hurtedEntity, damage) ->
            Arrays.stream(callbacks).forEach(hurtEntity -> hurtEntity.hurt(entity, pos, level, hurtedEntity, damage))
    );

    /**
     * 铁砧落地
     */
    @FunctionalInterface
    public interface OnLand {
        boolean onLand(Level level, BlockPos pos, FallingBlockEntity entity, float fallDistance);
    }

    /**
     * 铁砧伤害实体
     */
    @FunctionalInterface
    public interface HurtEntity {
        void hurt(FallingBlockEntity entity, BlockPos pos, Level level, Entity hurtedEntity, float damage);
    }
}
