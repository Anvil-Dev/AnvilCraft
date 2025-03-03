package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

public class ModDamageTypes {
    public static final ResourceKey<DamageType> LASER = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        AnvilCraft.of("laser")
    );
    public static final ResourceKey<DamageType> LOST_IN_TIME = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        AnvilCraft.of("lost_in_time")
    );

    public static DamageSource laser(Level level) {
        return ((DamageSourceExtra) level.damageSources()).laser();
    }

    public static DamageSource lostInTime(Level level) {
        return ((DamageSourceExtra) level.damageSources()).lostInTime();
    }


    public interface DamageSourceExtra {
        DamageSource laser();

        DamageSource lostInTime();
    }
}
