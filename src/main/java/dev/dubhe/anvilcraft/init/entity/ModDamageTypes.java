package dev.dubhe.anvilcraft.init.entity;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

public class ModDamageTypes {
    public static final ResourceKey<DamageType> LASER = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        AnvilCraft.of("laser")
    );
    public static final ResourceKey<DamageType> LOST_IN_TIME = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        AnvilCraft.of("lost_in_time")
    );
    public static final ResourceKey<DamageType> FALLING_GIANT_ANVIL = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        AnvilCraft.of("falling_giant_anvil")
    );
    public static final ResourceKey<DamageType> HEATER_BURN = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        AnvilCraft.of("heater_burn")
    );
    public static final ResourceKey<DamageType> GAMMA_LASER = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        AnvilCraft.of("gamma_laser")
    );
    public static final ResourceKey<DamageType> PLASMA_JET = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        AnvilCraft.of("plasma_jet")
    );

    @ApiStatus.Internal
    public static void bootstrap(BootstrapContext<DamageType> ctx) {
        ctx.register(LASER, new DamageType("anvilcraft.laser", 0.1F, DamageEffects.BURNING));
        ctx.register(LOST_IN_TIME, new DamageType("anvilcraft.lost_in_time", 0.1F));
        ctx.register(FALLING_GIANT_ANVIL, new DamageType("anvilcraft.falling_giant_anvil", 0.1F));
        ctx.register(HEATER_BURN, new DamageType("anvilcraft.heater_burn", 0.1F, DamageEffects.BURNING));
        ctx.register(GAMMA_LASER, new DamageType("anvilcraft.gamma_laser", 0.1F, DamageEffects.BURNING));
        ctx.register(PLASMA_JET, new DamageType("anvilcraft.plasma_jet", 0.1F, DamageEffects.BURNING));
    }

    public static DamageSource laser(Level level) {
        return ModDamageTypes.source(ModDamageTypes.LASER, level);
    }

    public static DamageSource laser(Level level, Entity cause) {
        return ModDamageTypes.source(ModDamageTypes.LASER, level, cause);
    }

    public static DamageSource lostInTime(Level level) {
        return ModDamageTypes.source(ModDamageTypes.LOST_IN_TIME, level);
    }

    public static DamageSource lostInTime(Level level, Entity cause) {
        return ModDamageTypes.source(ModDamageTypes.LOST_IN_TIME, level, cause);
    }

    public static DamageSource fallingGiantAnvil(Level level, @Nullable Entity cause) {
        return ModDamageTypes.source(ModDamageTypes.FALLING_GIANT_ANVIL, level, cause);
    }

    public static DamageSource heaterBurn(Level level) {
        return ModDamageTypes.source(ModDamageTypes.HEATER_BURN, level);
    }

    public static DamageSource gammaLaser(Level level) {
        return ModDamageTypes.source(ModDamageTypes.GAMMA_LASER, level);
    }

    public static DamageSource plasmaJet(Level level) {
        return ModDamageTypes.source(ModDamageTypes.PLASMA_JET, level);
    }

    private static DamageSource source(ResourceKey<DamageType> key, LevelReader level) {
        Holder.Reference<DamageType> holder = level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(key);
        return new DamageSource(holder);
    }

    @SuppressWarnings("SameParameterValue")
    private static DamageSource source(ResourceKey<DamageType> key, LevelReader level, @Nullable Entity cause) {
        Holder.Reference<DamageType> holder = level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(key);
        return new DamageSource(holder, cause);
    }
}
