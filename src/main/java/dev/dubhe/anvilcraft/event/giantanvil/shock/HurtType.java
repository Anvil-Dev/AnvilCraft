package dev.dubhe.anvilcraft.event.giantanvil.shock;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

enum HurtType {
    FIRE {
        @Override
        DamageSource damageSource(Level level) {
            return level.damageSources().inFire();
        }

        @Override
        public void postApply(Level level, LivingEntity l, float fallDistance) {
            if (!l.fireImmune()) {
                l.setRemainingFireTicks((int) (Math.floor(fallDistance) * 10));
                l.setRemainingFireTicks((int) (Math.floor(fallDistance) * 10));
            }

        }
    }, FROZEN {
        @Override
        DamageSource damageSource(Level level) {
            return level.damageSources().freeze();
        }

        @Override
        public void postApply(Level level, LivingEntity l, float fallDistance) {
            l.setTicksFrozen((int) (Math.floor(fallDistance) * 10));
        }
    }, SHOCK {
        @Override
        DamageSource damageSource(Level level) {
            return level.damageSources().lightningBolt();
        }

        @Override
        public void postApply(Level level, LivingEntity l, float fallDistance) {

        }
    }, VOID {
        @Override
        DamageSource damageSource(Level level) {
            return level.damageSources().fellOutOfWorld();
        }

        @Override
        public void postApply(Level level, LivingEntity l, float fallDistance) {

        }
    };

    abstract DamageSource damageSource(Level level);

    public abstract void postApply(Level level, LivingEntity l, float fallDistance);
}