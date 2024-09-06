package dev.dubhe.anvilcraft.api.event.forge;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityEvent;

@Getter
public class AnvilEvent extends EntityEvent {
    private final FallingBlockEntity entity;

    public AnvilEvent(FallingBlockEntity entity) {
        super(entity);
        this.entity = entity;
    }

    @Getter
    public static class OnLand extends AnvilEvent {
        private final Level level;
        private final BlockPos pos;
        private final float fallDistance;
        @Setter
        private boolean isAnvilDamage = false;

        /**
         * 铁砧落地事件
         *
         * @param level        世界
         * @param pos          位置
         * @param entity       铁砧
         * @param fallDistance 下落距离
         */
        public OnLand(Level level, BlockPos pos, FallingBlockEntity entity, float fallDistance) {
            super(entity);
            this.level = level;
            this.pos = pos;
            this.fallDistance = fallDistance;
        }
    }

    @Getter
    public static class HurtEntity extends AnvilEvent {
        private final BlockPos pos;
        private final Level level;
        private final Entity hurtedEntity;
        private final float damage;

        /**
         * 铁砧伤害实体
         *
         * @param entity       铁砧
         * @param pos          位置
         * @param level        世界
         * @param hurtedEntity 被伤害的实体
         * @param damage       伤害
         */
        public HurtEntity(FallingBlockEntity entity, BlockPos pos, Level level, Entity hurtedEntity, float damage) {
            super(entity);
            this.pos = pos;
            this.level = level;
            this.hurtedEntity = hurtedEntity;
            this.damage = damage;
        }
    }
}
