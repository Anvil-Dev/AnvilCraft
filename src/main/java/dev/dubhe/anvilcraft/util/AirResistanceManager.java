package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
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
        // 月球大气稀薄，保留一半原版阻力以免无法减速
        registerDimensionAirResistance(CelestialTravelManager.MUN_LEVEL, 0.5);
    }

    private AirResistanceManager() {
    }

    public static void registerDimensionAirResistance(ResourceKey<Level> dimension, double airResistance) {
        DIMENSION_AIR_RESISTANCE_MAP.put(dimension, Math.max(0.0, airResistance));
    }

    public static double getDimensionAirResistance(Level level) {
        return DIMENSION_AIR_RESISTANCE_MAP.getOrDefault(level.dimension(), DEFAULT_AIR_RESISTANCE);
    }

    /** Rescales a vanilla air resistance multiplier by the dimension's air resistance factor. */
    public static double drag(Level level, double vanillaDrag) {
        double airResistance = getDimensionAirResistance(level);
        if (airResistance == DEFAULT_AIR_RESISTANCE) return vanillaDrag;
        return 1.0 - (1.0 - vanillaDrag) * airResistance;
    }

    public static float drag(Level level, float vanillaDrag) {
        return (float) drag(level, (double) vanillaDrag);
    }

    public static double drag(Entity entity, double vanillaDrag) {
        return drag(entity.level(), vanillaDrag);
    }

    public static float drag(Entity entity, float vanillaDrag) {
        return drag(entity.level(), vanillaDrag);
    }
}
