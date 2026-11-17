package dev.dubhe.anvilcraft.block.entity.celestial;

import java.util.Locale;

/** 为天体界面和预设物品提供一致的名称分类。 */
public final class CelestialBodyNames {
    private static final String PREFIX = "screen.anvilcraft.cfa.";

    private CelestialBodyNames() {
    }

    /** 返回包含温度、液体和大气差异的天体名称翻译键。 */
    public static String key(CelestialBodyData body) {
        if (body instanceof RockyPlanetData rocky) {
            return CelestialBodyNames.rockyTypeKey(rocky);
        }
        if (body instanceof SpecialCelestialBodyData special) {
            return CelestialBodyNames.PREFIX + "class.special." + special.name();
        }
        if (body instanceof StarData star && star.specialRedDwarf()) {
            return CelestialBodyNames.PREFIX + "class.mutated_red_dwarf";
        }
        return CelestialBodyNames.PREFIX + "class." + body.bodyClass().name().toLowerCase(Locale.ROOT);
    }

    private static String rockyTypeKey(RockyPlanetData body) {
        Temperature temperature = body.temperature();
        LiquidCoverage liquid = body.liquidCoverage();
        boolean hasAtmosphere = body.hasAtmosphere();
        boolean hasLiquid = liquid != LiquidCoverage.NONE;

        if (temperature == Temperature.FREEZING) {
            if (!hasLiquid && !hasAtmosphere) {
                return CelestialBodyNames.PREFIX + "class.freezing_no_liquid_no_atmos";
            }
            if (!hasLiquid) {
                return CelestialBodyNames.PREFIX + "class.freezing_no_liquid_atmos";
            }
            return CelestialBodyNames.PREFIX + "class.freezing_liquid";
        }
        if (temperature == Temperature.SCORCHED) {
            if (!hasLiquid && !hasAtmosphere) {
                return CelestialBodyNames.PREFIX + "class.scorched_no_liquid_no_atmos";
            }
            if (!hasLiquid) {
                return CelestialBodyNames.PREFIX + "class.scorched_no_liquid_atmos";
            }
            return CelestialBodyNames.PREFIX + "class.scorched_liquid";
        }
        if (!hasAtmosphere) {
            return CelestialBodyNames.PREFIX + "class.deathly_planet";
        }
        if (!hasLiquid) {
            return CelestialBodyNames.PREFIX + "class.desert_planet";
        }
        return switch (liquid) {
            case LOW -> CelestialBodyNames.temperatureTypeKey(temperature, "riverbank");
            case MEDIUM -> CelestialBodyNames.temperatureTypeKey(temperature, "land_ocean");
            case HIGH -> CelestialBodyNames.temperatureTypeKey(temperature, "ocean");
            default -> CelestialBodyNames.PREFIX + "class.deathly_planet";
        };
    }

    private static String temperatureTypeKey(Temperature temperature, String suffix) {
        String prefix = switch (temperature) {
            case COLD -> "cold";
            case HOT -> "hot";
            default -> "mild";
        };
        return CelestialBodyNames.PREFIX + "class." + prefix + "_" + suffix;
    }

}
