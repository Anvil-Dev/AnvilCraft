package dev.dubhe.anvilcraft.block.entity.celestial;

import net.minecraft.nbt.CompoundTag;

@SuppressWarnings({"checkstyle:all"})
public sealed interface CelestialBodyData permits RockyPlanetData, GiantPlanetData, StarData {

    CelestialBodyType type();

    /** The matched body class from the diagram. */
    CelestialBodyClass bodyClass();

    RingType ringType();

    int size();

    float axialTilt();

    float rotationSpeed();

    int magneticFieldStrength();

    CompoundTag toTag();

    static CelestialBodyData fromTag(CompoundTag tag) {
        String typeName = tag.getString("bodyType");
        CelestialBodyType type = CelestialBodyType.fromName(typeName);
        return switch (type) {
            case ROCKY_PLANET -> RockyPlanetData.fromTag(tag);
            case GIANT_PLANET -> GiantPlanetData.fromTag(tag);
            case STAR -> StarData.fromTag(tag);
        };
    }

    /** Read CelestialBodyClass from tag, with fallback for old data. */
    static CelestialBodyClass readClass(CompoundTag tag, CelestialBodyType bodyType) {
        String className = tag.getString("bodyClass");
        if (!className.isEmpty()) {
            try {
                return CelestialBodyClass.valueOf(className);
            } catch (IllegalArgumentException ignored) { }
        }
        // Fallback for old data without bodyClass
        return switch (bodyType) {
            case ROCKY_PLANET -> {
                String lc = tag.getString("liquidCoverage");
                yield switch (lc) {
                    case "none" -> CelestialBodyClass.ROCKY_NO_LIQUID;
                    case "low" -> CelestialBodyClass.ROCKY_LOW_LIQUID;
                    case "medium" -> CelestialBodyClass.ROCKY_MED_LIQUID;
                    case "high" -> CelestialBodyClass.ROCKY_HIGH_LIQUID;
                    default -> CelestialBodyClass.ROCKY_NO_LIQUID;
                };
            }
            case GIANT_PLANET -> tag.getBoolean("brownDwarf")
                ? CelestialBodyClass.BROWN_DWARF
                : CelestialBodyClass.GAS_GIANT;
            case STAR -> CelestialBodyClass.M_MAIN;
        };
    }
}
