package dev.dubhe.anvilcraft.event.giantanvil.shock;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;

enum HurtType {
    FIRE {
        @Override
        DamageSource damageSource(Level level) {
            return level.damageSources().inFire();
        }
    }, FROZEN {
        @Override
        DamageSource damageSource(Level level) {
            return level.damageSources().freeze();
        }
    }, SHOCK {
        @Override
        DamageSource damageSource(Level level) {
            return level.damageSources().lightningBolt();
        }
    }, VOID {
        @Override
        DamageSource damageSource(Level level) {
            return level.damageSources().fellOutOfWorld();
        }
    };

    abstract DamageSource damageSource(Level level);
}