package dev.dubhe.anvilcraft.init.entity;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus;

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
        return ((DamageSourceExtra) level.damageSources()).anvilcraft$laser();
    }

    public static DamageSource lostInTime(Level level) {
        return ((DamageSourceExtra) level.damageSources()).anvilcraft$lostInTime();
    }

    @ApiStatus.Internal
    public static void bootstrap(BootstrapContext<DamageType> ctx) {
        ctx.register(LASER, new DamageType("anvilcraft.laser", 0.1f, DamageEffects.BURNING));
        ctx.register(LOST_IN_TIME, new DamageType("anvilcraft.lost_in_time", 0.1f));
    }

    public interface DamageSourceExtra {
        DamageSource anvilcraft$laser();

        DamageSource anvilcraft$lostInTime();
    }
}
