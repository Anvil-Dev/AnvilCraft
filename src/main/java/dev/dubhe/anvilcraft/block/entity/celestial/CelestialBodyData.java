package dev.dubhe.anvilcraft.block.entity.celestial;

import net.minecraft.nbt.CompoundTag;
import org.jspecify.annotations.Nullable;

public sealed interface CelestialBodyData permits RockyPlanetData, GiantPlanetData, StarData, SpecialCelestialBodyData {

    CelestialBodyType type();

    /**
     * The matched body class from the diagram.
     */
    CelestialBodyClass bodyClass();

    RingType ringType();

    int size();

    float axialTilt();

    /**
     * Rotation speed level (0-5).
     * <ul>
     *   <li>0 = Very Slow</li>
     *   <li>1 = Slow</li>
     *   <li>2 = Medium</li>
     *   <li>3 = Fast</li>
     *   <li>4 = Very Fast</li>
     *   <li>5 = Super Fast</li>
     * </ul>
     */
    int rotationSpeed();

    int magneticFieldStrength();

    /**
     * Convert rotation speed level to visual rotation multiplier (deg/tick).
     */
    static float getVisualRotationSpeed(int level) {
        return switch (level) {
            case 0 -> 0.1f;
            case 1 -> 0.5f;
            case 2 -> 1.0f;
            case 3 -> 1.5f;
            case 4 -> 3.0f;
            default -> 100.0f; // 5+ = Super Fast
        };
    }

    /**
     * Compute the raw visual scale of the body (without the {@link #BODY_SCALE_FACTOR} multiplier).
     * Black holes and neutron stars use fixed values; other bodies use a piecewise size→scale mapping.
     */
    default float bodyScale() {
        if (this instanceof StarData star) {
            if (star.bodyClass() == CelestialBodyClass.BLACK_HOLE) return 1.5f;
            if (star.bodyClass() == CelestialBodyClass.NEUTRON_STAR) return 0.8f;
        }
        int size = this.size();
        if (size <= 20) {
            return 1.5f * (0.2f + (size - 1) * 0.8f / 19f);
        } else {
            float t = (size - 20) / 44f;
            return 1.5f * (1.0f + t * t * 1.63f);
        }
    }

    // === Ring-system / body-scale constants (shared by rendering and gravity) ===

    /** Full visual scale multiplier of a body (BODY_SCALE_FACTOR × bodyScale = full visual size). */
    float BODY_SCALE_FACTOR = 10.0f / 1.5f;
    /** Ratio between the ring-system scale and the body scale. */
    float RING_TO_BODY_RATIO = 1.8f;
    /** Extra factor so non-amplified R1-R3 rings (roughly half the inner radius of R4-R6) keep the same body spacing. */
    float RING_SMALL_INNER_RADIUS_FACTOR = 2.0f;
    /** Upper bound of the visual compensation applied to the accumulated "in" bone tilt. */
    float INNER_BONE_BOOST_MAX = 5.5f;
    /** Decay rate of the "in" bone visual compensation with respect to bodyScale. */
    float INNER_BONE_BOOST_RATE = 0.8f;
    /** Default ring scale used when there is no body. */
    float BASE_RING_SCALE = 6.0f;

    /**
     * Compute the full ring-system scale for the given body data (without redstone interpolation).
     */
    static float ringSystemScale(@Nullable CelestialBodyData data, boolean isAmplify) {
        if (data == null) return BASE_RING_SCALE;
        float bodyS = data.bodyScale();
        float proportional = bodyS * BODY_SCALE_FACTOR * RING_TO_BODY_RATIO;
        if (data instanceof StarData) {
            float inBoneBoost = Math.max(0.0f, INNER_BONE_BOOST_MAX - bodyS * INNER_BONE_BOOST_RATE);
            return proportional + inBoneBoost;
        } else {
            float inBoneBoost = Math.max(0.0f, INNER_BONE_BOOST_MAX * 1.5f - bodyS * INNER_BONE_BOOST_RATE);
            return proportional * RING_SMALL_INNER_RADIUS_FACTOR + inBoneBoost;
        }
    }

    /**
     * Compute the dynamic body-center height for the given body data (without redstone interpolation).
     */
    static float dynamicCenterY(@Nullable CelestialBodyData data, boolean isAmplify) {
        if (data == null) return isAmplify ? 6.5f : 4.5f;
        float ringScale = ringSystemScale(data, isAmplify);
        float baseHeight = isAmplify ? 2.5f : 1.5f;
        float height = baseHeight + ringScale * 0.74f;
        if (!(data instanceof StarData)) {
            float bodyS = data.bodyScale();
            float planetMinBS = 0.3f;
            float planetMaxBS = 1.5f;
            float t = Math.min(1.0f, Math.max(0.0f, (bodyS - planetMinBS) / (planetMaxBS - planetMinBS)));
            float planetReduction = isAmplify
                ? 0.5f + t * 1.5f
                : 4.0f + t * 1.5f;
            height -= planetReduction;
        }
        return height;
    }

    CompoundTag toTag();

    static CelestialBodyData fromTag(CompoundTag tag) {
        String typeName = tag.getStringOr("bodyType", "rocky_planet");
        CelestialBodyType type = CelestialBodyType.fromName(typeName);
        return switch (type) {
            case ROCKY_PLANET -> RockyPlanetData.fromTag(tag);
            case GIANT_PLANET -> GiantPlanetData.fromTag(tag);
            case STAR -> StarData.fromTag(tag);
            case SPECIAL -> SpecialCelestialBodyData.fromTag(tag);
        };
    }

    static CelestialBodyClass readClass(CompoundTag tag, CelestialBodyType bodyType) {
        String className = tag.getStringOr("bodyClass", "");
        if (!className.isEmpty()) {
            return CelestialBodyClass.valueOf(className);
        }
        return switch (bodyType) {
            case ROCKY_PLANET -> CelestialBodyClass.ROCKY_NO_LIQUID;
            case GIANT_PLANET -> CelestialBodyClass.GAS_GIANT;
            case STAR -> CelestialBodyClass.M_MAIN;
            case SPECIAL -> CelestialBodyClass.LARGE_MOON;
        };
    }
}
