package dev.dubhe.anvilcraft.client.gui.screen.cfa;

import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyData;
import dev.dubhe.anvilcraft.block.entity.celestial.GiantPlanetData;
import dev.dubhe.anvilcraft.block.entity.celestial.LiquidCoverage;
import dev.dubhe.anvilcraft.block.entity.celestial.RockyPlanetData;
import dev.dubhe.anvilcraft.block.entity.celestial.SpecialCelestialBodyData;
import dev.dubhe.anvilcraft.block.entity.celestial.StarData;
import dev.dubhe.anvilcraft.block.entity.celestial.Temperature;
import dev.dubhe.anvilcraft.inventory.CelestialForgingAnvilMenu;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 为天体信息面板生成本地化文本。
 */
public final class CelestialBodyInfoFormatter {
    private static final String PREFIX = "screen.anvilcraft.cfa.";

    private CelestialBodyInfoFormatter() {
    }

    /**
     * 格式化天体在界面中显示的全部属性。
     */
    public static List<Component> format(
        CelestialBodyData body,
        int ageAnvilCount,
        int massAnvilCount,
        float offsetAge,
        float offsetRadius,
        float offsetMass
    ) {
        List<Component> lines = new ArrayList<>();
        boolean isError = body instanceof SpecialCelestialBodyData special && special.isErrorPlanet();

        lines.add(Component.translatable(PREFIX + "type", Component.translatable(typeKey(body))));
        lines.add(measurement(
            "age", isError ? "???" : CelestialForgingAnvilMenu.formatAgeOffset(ageAnvilCount, offsetAge)
        ));
        lines.add(measurement(
            "radius", isError ? "???" : CelestialForgingAnvilMenu.formatRadiusOffset(body.size(), offsetRadius)
        ));
        lines.add(measurement(
            "mass", isError ? "???" : CelestialForgingAnvilMenu.formatMassOffset(massAnvilCount, offsetMass)
        ));

        switch (body) {
            case SpecialCelestialBodyData special -> addSpecialBodyLines(lines, special);
            case StarData star -> addStarLines(lines, star);
            case RockyPlanetData rocky -> addRockyPlanetLines(lines, rocky);
            case GiantPlanetData giant -> addGiantPlanetLines(lines, giant);
        }
        return lines;
    }

    private static Component measurement(String name, String value) {
        return Component.translatable(PREFIX + name, Component.literal(value));
    }

    private static String typeKey(CelestialBodyData body) {
        if (body instanceof RockyPlanetData rocky) return rockyTypeKey(rocky);
        if (body instanceof SpecialCelestialBodyData special) return PREFIX + "class.special." + special.name();
        return PREFIX + "class." + body.bodyClass().name().toLowerCase(Locale.ROOT);
    }

    private static void addSpecialBodyLines(List<Component> lines, SpecialCelestialBodyData body) {
        if (body.isErrorPlanet()) {
            for (String name : List.of("temp", "atmos", "liquid", "mag", "spin", "tilt")) {
                lines.add(measurement(name, "???"));
            }
            return;
        }
        addPlanetLines(
            lines,
            body.temperature(),
            body.hasAtmosphere(),
            body.liquidCoverage(),
            body.magneticFieldStrength(),
            body.rotationSpeed(),
            body.axialTilt()
        );
    }

    private static void addStarLines(List<Component> lines, StarData body) {
        lines.add(magneticFieldText(body.magneticFieldStrength()));
        lines.add(rotationText(body.rotationSpeed()));
        if (body.axialTilt() > 0.1f) {
            lines.add(axialTiltText(body.axialTilt()));
        }
    }

    private static void addRockyPlanetLines(List<Component> lines, RockyPlanetData body) {
        addPlanetLines(
            lines,
            body.temperature(),
            body.hasAtmosphere(),
            body.liquidCoverage(),
            body.magneticFieldStrength(),
            body.rotationSpeed(),
            body.axialTilt()
        );
    }

    private static void addPlanetLines(
        List<Component> lines,
        @Nullable Temperature temperature,
        boolean hasAtmosphere,
        @Nullable LiquidCoverage liquidCoverage,
        int magneticFieldStrength,
        int rotationSpeed,
        float axialTilt
    ) {
        lines.add(temperatureText(temperature));
        lines.add(atmosphereText(hasAtmosphere));
        lines.add(liquidText(liquidCoverage));
        lines.add(magneticFieldText(magneticFieldStrength));
        lines.add(rotationText(rotationSpeed));
        lines.add(axialTiltText(axialTilt));
    }

    private static void addGiantPlanetLines(List<Component> lines, GiantPlanetData body) {
        if (!body.brownDwarf()) {
            lines.add(Component.translatable(
                PREFIX + "pressure",
                Component.translatable(PREFIX + "pressure." + body.pressureType().getSerializedName())
            ));
        }
        lines.add(Component.translatable(
            PREFIX + "wind",
            Component.translatable(PREFIX + "wind." + body.windSpeed().getSerializedName())
        ));
        lines.add(magneticFieldText(body.magneticFieldStrength()));
        lines.add(rotationText(body.rotationSpeed()));
        lines.add(axialTiltText(body.axialTilt()));
    }

    private static Component temperatureText(@Nullable Temperature temperature) {
        String key = temperature == null ? PREFIX + "none" : PREFIX + "temp." + temperature.getSerializedName();
        return Component.translatable(PREFIX + "temp", Component.translatable(key));
    }

    private static Component atmosphereText(boolean hasAtmosphere) {
        return Component.translatable(
            PREFIX + "atmos", Component.translatable(hasAtmosphere ? PREFIX + "atmos.yes" : PREFIX + "none")
        );
    }

    private static Component liquidText(@Nullable LiquidCoverage coverage) {
        String key = coverage == null ? PREFIX + "none" : PREFIX + "liquid." + coverage.getSerializedName();
        return Component.translatable(PREFIX + "liquid", Component.translatable(key));
    }

    private static Component magneticFieldText(int level) {
        String key = switch (level) {
            case 0 -> PREFIX + "none";
            case 1 -> PREFIX + "mag.very_weak";
            case 2 -> PREFIX + "mag.weak";
            case 3 -> PREFIX + "mag.medium";
            case 4 -> PREFIX + "mag.strong";
            case 5 -> PREFIX + "mag.very_strong";
            default -> PREFIX + "mag.extreme";
        };
        return Component.translatable(PREFIX + "mag", Component.translatable(key));
    }

    private static Component rotationText(int level) {
        String key = switch (level) {
            case 0 -> PREFIX + "spin.very_slow";
            case 1 -> PREFIX + "spin.slow";
            case 2 -> PREFIX + "spin.medium";
            case 3 -> PREFIX + "spin.fast";
            case 4 -> PREFIX + "spin.very_fast";
            default -> PREFIX + "spin.super_fast";
        };
        return Component.translatable(PREFIX + "spin", Component.translatable(key));
    }

    private static Component axialTiltText(float tilt) {
        return Component.translatable(PREFIX + "tilt", formatThreeSignificantFigures(tilt) + "°");
    }

    private static String rockyTypeKey(RockyPlanetData body) {
        Temperature temperature = body.temperature();
        LiquidCoverage liquid = body.liquidCoverage();
        boolean hasAtmosphere = body.hasAtmosphere();
        boolean hasLiquid = liquid != LiquidCoverage.NONE;

        if (temperature == Temperature.FREEZING) {
            if (!hasLiquid && !hasAtmosphere) return PREFIX + "class.freezing_no_liquid_no_atmos";
            if (!hasLiquid) return PREFIX + "class.freezing_no_liquid_atmos";
            return PREFIX + "class.freezing_liquid";
        }
        if (temperature == Temperature.SCORCHED) {
            if (!hasLiquid && !hasAtmosphere) return PREFIX + "class.scorched_no_liquid_no_atmos";
            if (!hasLiquid) return PREFIX + "class.scorched_no_liquid_atmos";
            return PREFIX + "class.scorched_liquid";
        }
        if (!hasAtmosphere) return PREFIX + "class.deathly_planet";
        if (!hasLiquid) return PREFIX + "class.desert_planet";
        return switch (liquid) {
            case LOW -> temperatureTypeKey(temperature, "riverbank");
            case MEDIUM -> temperatureTypeKey(temperature, "land_ocean");
            case HIGH -> temperatureTypeKey(temperature, "ocean");
            default -> PREFIX + "class.deathly_planet";
        };
    }

    private static String temperatureTypeKey(Temperature temperature, String suffix) {
        String prefix = switch (temperature) {
            case COLD -> "cold";
            case HOT -> "hot";
            default -> "mild";
        };
        return PREFIX + "class." + prefix + "_" + suffix;
    }

    @SuppressWarnings("MalformedFormatString")
    private static String formatThreeSignificantFigures(double value) {
        if (Math.abs(value) < 1e-9) return "0";
        int power = (int) Math.floor(Math.log10(Math.abs(value)));
        int digits = Math.clamp(2 - power, 0, 6);
        return String.format(Locale.US, "%." + digits + "f", value);
    }
}
