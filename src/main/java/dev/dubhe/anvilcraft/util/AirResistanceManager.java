package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-dimension air resistance, the counterpart of the dimension gravity table in {@link GravityManager}.
 *
 * <p>Vanilla applies air resistance as a multiplier on an entity's velocity every tick, with a separate
 * hardcoded constant per entity kind.  A factor of {@code 1.0} keeps those constants untouched, {@code 0.0}
 * removes air resistance completely so velocity no longer decays, and values in between keep that fraction
 * of the speed vanilla would have taken away.</p>
 */
public final class AirResistanceManager {
    /** Vanilla air resistance, used for every dimension that is not registered here. */
    public static final double DEFAULT_AIR_RESISTANCE = 1.0;

    private static final Map<ResourceKey<Level>, Double> DIMENSION_AIR_RESISTANCE_MAP = new HashMap<>();

    static {
        registerDimensionAirResistance(CelestialTravelManager.VOID_PLANET_LEVEL, 0.0);
        registerDimensionAirResistance(CelestialTravelManager.MUN_LEVEL, 0.1);
    }

    private AirResistanceManager() {
    }

    public static void registerDimensionAirResistance(ResourceKey<Level> dimension, double airResistance) {
        if (!Double.isFinite(airResistance)) {
            throw new IllegalArgumentException("Air resistance must be finite");
        }
        DIMENSION_AIR_RESISTANCE_MAP.put(dimension, Math.max(0.0, airResistance));
    }

    public static double getDimensionAirResistance(Level level) {
        return DIMENSION_AIR_RESISTANCE_MAP.getOrDefault(level.dimension(), DEFAULT_AIR_RESISTANCE);
    }

    /** Rescales a vanilla air resistance multiplier by the dimension's air resistance factor. */
    public static double drag(Level level, double vanillaDrag) {
        double airResistance = getDimensionAirResistance(level);
        if (airResistance == DEFAULT_AIR_RESISTANCE) return vanillaDrag;
        return Math.clamp(1.0 - (1.0 - vanillaDrag) * airResistance, 0.0, 1.0);
    }

    public static float drag(Level level, float vanillaDrag) {
        return (float) drag(level, (double) vanillaDrag);
    }

    public static double drag(Entity entity, double vanillaDrag) {
        if (isCreativeFlying(entity)) return vanillaDrag;
        return drag(entity.level(), vanillaDrag);
    }

    public static float drag(Entity entity, float vanillaDrag) {
        if (isCreativeFlying(entity)) return vanillaDrag;
        return drag(entity.level(), vanillaDrag);
    }

    public static boolean isCreativeFlying(Entity entity) {
        return entity instanceof Player player && player.getAbilities().flying;
    }

    /** Passive lift can cancel gravity, but cannot turn gravity into upward thrust. */
    public static double elytraLift(Entity entity, double vanillaLift) {
        if (isCreativeFlying(entity)) return vanillaLift;
        return Math.clamp(vanillaLift * getDimensionAirResistance(entity.level()), 0.0, 1.0);
    }

    /** Thin air reduces the wing's conversion of falling or forward speed into lift. */
    public static double elytraResponse(Entity entity, double vanillaResponse) {
        if (isCreativeFlying(entity)) return vanillaResponse;
        return vanillaResponse * Math.min(1.0, getDimensionAirResistance(entity.level()));
    }
}
