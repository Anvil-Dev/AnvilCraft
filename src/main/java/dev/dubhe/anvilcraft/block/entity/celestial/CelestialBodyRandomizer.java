package dev.dubhe.anvilcraft.block.entity.celestial;

import dev.dubhe.anvilcraft.util.MassRadiusDiagram;
import net.minecraft.util.RandomSource;

public class CelestialBodyRandomizer {

    public static CelestialBodyData randomize(boolean isAmplified, RandomSource random) {
        if (isAmplified) {
            return randomizeStar(random);
        } else {
            return randomizePlanet(random);
        }
    }

    private static CelestialBodyData randomizePlanet(RandomSource random) {
        // 50% rocky planet, 50% giant planet
        if (random.nextBoolean()) {
            return randomizeRockyPlanet(random);
        } else {
            return randomizeGiantPlanet(random);
        }
    }

    private static StarData randomizeStar(RandomSource random) {
        MassRadiusDiagram.ensureLoaded();

        if (MassRadiusDiagram.isLoadFailed()) {
            // Fallback: old behavior with random size, color from legacy gradient
            int size = random.nextIntBetweenInclusive(1, 28);
            float[] rgb = MassRadiusDiagram.starColorFallback(size);
            return new StarData(
                size,
                Math.clamp((int) (rgb[0] * 255), 0, 255),
                Math.clamp((int) (rgb[1] * 255), 0, 255),
                Math.clamp((int) (rgb[2] * 255), 0, 255),
                random.nextFloat() * 180f,
                0.5f + random.nextFloat() * 2.0f
            );
        }

        // Primary path: use mass-radius diagram
        MassRadiusDiagram.StarPixel pixel = MassRadiusDiagram.pickRandomStar(random);
        int size = MassRadiusDiagram.yToSize(pixel.y());

        // Small random jitter on size for natural variation
        size += random.nextIntBetweenInclusive(-2, 2);
        size = Math.clamp(size, 1, 28);

        return new StarData(size, pixel.r(), pixel.g(), pixel.b(), random.nextFloat() * 180f,
            0.5f + random.nextFloat() * 2.0f);
    }

    private static GiantPlanetData randomizeGiantPlanet(RandomSource random) {
        PressureType pressureType = random.nextBoolean() ? PressureType.GAS : PressureType.ICE;
        WindSpeed windSpeed = random.nextBoolean() ? WindSpeed.HIGH : WindSpeed.VERY_HIGH;

        // Ring: NONE 30%, WEAK 50%, STRONG 20%
        RingType ringType = weightedRing(random, 0.3f, 0.5f, 0.2f);

        int size = random.nextIntBetweenInclusive(1, 8);

        int paletteBaseRow = random.nextIntBetweenInclusive(0, 15);
        int paletteOverlayRow;
        do {
            paletteOverlayRow = random.nextIntBetweenInclusive(0, 15);
        } while (paletteOverlayRow == paletteBaseRow);

        return new GiantPlanetData(pressureType, windSpeed, ringType, size, paletteBaseRow, paletteOverlayRow,
            random.nextFloat() * 180f, 0.5f + random.nextFloat() * 2.0f);
    }

    private static RockyPlanetData randomizeRockyPlanet(RandomSource random) {
        boolean hasAtmosphere = random.nextBoolean();
        LiquidCoverage liquidCoverage = weightedLiquid(random);

        // Determine temperature based on atmosphere and liquid
        Temperature temperature;
        if (!hasAtmosphere) {
            temperature = random.nextBoolean() ? Temperature.FREEZING : Temperature.SCORCHED;
        } else {
            // Has atmosphere
            if (liquidCoverage == LiquidCoverage.NONE) {
                // Has atmosphere, no liquid: any of 5 temperatures
                temperature = Temperature.values()[random.nextInt(Temperature.values().length)];
            } else {
                // Has atmosphere, has liquid: COLD, MILD, HOT only
                temperature = switch (random.nextInt(3)) {
                    case 0 -> Temperature.COLD;
                    case 1 -> Temperature.MILD;
                    default -> Temperature.HOT;
                };
            }
        }

        // Ring: NONE 70%, WEAK 20%, STRONG 10%
        RingType ringType = weightedRing(random, 0.7f, 0.2f, 0.1f);

        int size = random.nextIntBetweenInclusive(1, 8);

        // Palette rows: upper half (0-7) for base, lower half (8-15) for overlay
        int paletteBaseRow;
        int paletteOverlayRow;
        if (liquidCoverage == LiquidCoverage.NONE) {
            // No overlay, base can use any row 0-15
            paletteBaseRow = random.nextIntBetweenInclusive(0, 15);
            paletteOverlayRow = 0; // unused
        } else {
            // Base from upper half, overlay from lower half
            paletteBaseRow = random.nextIntBetweenInclusive(0, 7);
            paletteOverlayRow = random.nextIntBetweenInclusive(8, 15);
        }

        return new RockyPlanetData(hasAtmosphere, liquidCoverage, temperature, ringType, size,
            paletteBaseRow, paletteOverlayRow, random.nextFloat() * 180f, 0.5f + random.nextFloat() * 2.0f);
    }

    private static LiquidCoverage weightedLiquid(RandomSource random) {
        float f = random.nextFloat();
        if (f < 0.25f) return LiquidCoverage.NONE;
        if (f < 0.55f) return LiquidCoverage.LOW;
        if (f < 0.80f) return LiquidCoverage.MEDIUM;
        return LiquidCoverage.HIGH;
    }

    private static RingType weightedRing(RandomSource random, float noneChance, float weakChance, float strongChance) {
        float f = random.nextFloat();
        if (f < noneChance) return RingType.NONE;
        if (f < noneChance + weakChance) return RingType.WEAK;
        return RingType.STRONG;
    }
}
