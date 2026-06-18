package dev.dubhe.anvilcraft.init.entity;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
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
    public static final ResourceKey<DamageType> HEATER_BURN = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        AnvilCraft.of("heater_burn")
    );
    public static final ResourceKey<DamageType> PLASMA_JET = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        AnvilCraft.of("plasma_jets")
    );
    public static final ResourceKey<DamageType> GAMMA_LASER = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        AnvilCraft.of("gamma_laser")
    );

    @ApiStatus.Internal
    public static void bootstrap(BootstrapContext<DamageType> ctx) {
        ctx.register(LASER, new DamageType("anvilcraft.laser", 0.1f, DamageEffects.BURNING));
        ctx.register(LOST_IN_TIME, new DamageType("anvilcraft.lost_in_time", 0.1f));
        ctx.register(HEATER_BURN, new DamageType("anvilcraft.heater_burn", 0.1f, DamageEffects.BURNING));
        ctx.register(PLASMA_JET, new DamageType("anvilcraft.plasma_jets", 0.1f, DamageEffects.BURNING));
        ctx.register(GAMMA_LASER, new DamageType("anvilcraft.gamma_laser", 0.0f, DamageEffects.BURNING));
    }

    public static DamageSource laser(Level level) {
        return ModDamageTypes.source(ModDamageTypes.LASER, level);
    }

    public static DamageSource lostInTime(Level level) {
        return ModDamageTypes.source(ModDamageTypes.LOST_IN_TIME, level);
    }

    public static DamageSource heaterBurn(Level level) {
        return ModDamageTypes.source(ModDamageTypes.HEATER_BURN, level);
    }

    public static DamageSource plasmaJets(Level level) {
        return ModDamageTypes.source(ModDamageTypes.PLASMA_JET, level);
    }

    public static DamageSource gammaLaser(Level level) {
        return ModDamageTypes.source(ModDamageTypes.GAMMA_LASER, level);
    }

    private static DamageSource source(ResourceKey<DamageType> key, LevelReader level) {
        Registry<DamageType> registry = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        return new DamageSource(registry.getHolderOrThrow(key));
    }
}
