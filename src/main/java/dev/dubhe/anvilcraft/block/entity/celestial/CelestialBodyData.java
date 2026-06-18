package dev.dubhe.anvilcraft.block.entity.celestial;

import net.minecraft.nbt.CompoundTag;

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

    CompoundTag toTag();

    static CelestialBodyData fromTag(CompoundTag tag) {
        String typeName = tag.getString("bodyType");
        CelestialBodyType type = CelestialBodyType.fromName(typeName);
        return switch (type) {
            case ROCKY_PLANET -> RockyPlanetData.fromTag(tag);
            case GIANT_PLANET -> GiantPlanetData.fromTag(tag);
            case STAR -> StarData.fromTag(tag);
            case SPECIAL -> SpecialCelestialBodyData.fromTag(tag);
        };
    }

    static CelestialBodyClass readClass(CompoundTag tag, CelestialBodyType bodyType) {
        String className = tag.getString("bodyClass");
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
