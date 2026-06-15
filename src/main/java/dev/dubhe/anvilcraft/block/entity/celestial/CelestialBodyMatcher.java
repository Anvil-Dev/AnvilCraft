package dev.dubhe.anvilcraft.block.entity.celestial;

import com.mojang.blaze3d.platform.NativeImage;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;

/**
 * Three-step celestial body matching engine using diagram PNGs.
 * Each 64×64 diagram maps anvil counts to pixel colors that identify body classes.
 * Uses classloader-based loading (works on both server and client).
 */
public final class CelestialBodyMatcher {

    private static final String DIR = "assets/anvilcraft/textures/misc";

    // Diagram files
    private static final String MASS_RADIUS = DIR + "/mass_radius_diagram_pixel.png";
    private static final String AGE_TEMP = DIR + "/age_temp_diagram_pixel.png";
    private static final String AGE_TEMP_SP = DIR + "/age_temp_diagram_pixel_sp.png";
    private static final String AGE_RADIUS = DIR + "/age_radius_diagram_pixel.png";
    private static final String STAR_COLOR_TEMP = "assets/anvilcraft/textures/block/celestial_body/star_color_temperature.png";

    private static NativeImage massRadiusImage;
    private static NativeImage ageTempImage;
    private static NativeImage ageTempSpImage;
    private static NativeImage ageRadiusImage;
    private static NativeImage starColorTempImage;
    private static boolean loadAttempted = false;

    private CelestialBodyMatcher() {
    }

    // === Public API ===

    /**
     * Try to match celestial body from the four anvil counts.
     */
    // Diagrams are 64×64 pixels. Anvil counts range 1–64.
    // Pixel (1,1) bottom-left → 0-indexed (x=0, y=63).
    // Pixel (64,64) top-right → 0-indexed (x=63, y=0).
    // x = count - 1, y = 64 - count (since PNG origin is top-left).
    private static final int DIAG_SIZE = 64;

    @Nullable
    public static CelestialBodyData match(
        int time, int space, int mass, int energy, boolean isAmplified, RandomSource random
    ) {
        ensureLoaded();

        // Step 1: Mass-Radius diagram (mass=x, space=y inverted)
        CelestialBodyClass bodyClass = lookupClass(massRadiusImage, toX(mass), toY(space));
        if (bodyClass == null) return null;

        // Stellar bodies require amplifier
        if (bodyClass.isStellar() && !isAmplified) return null;

        // Step 2: Temperature-Age diagram (time=x, energy=y inverted)
        if (!step2(toX(time), toY(energy), bodyClass)) return null;

        // Step 3: Age-Radius diagram (time=x, space=y inverted, for stellar+brown dwarf)
        if (bodyClass.needsStep3() && !step3(toX(time), toY(space), bodyClass)) return null;

        // Generate rendering data
        return generateBodyData(bodyClass, time, space, mass, energy, random);
    }

    /**
     * Maps anvil count (1–64) to 0-indexed diagram x pixel.
     */
    private static int toX(int count) {
        return Math.clamp(count - 1, 0, DIAG_SIZE - 1);
    }

    /**
     * Maps anvil count (1–64) to 0-indexed diagram y pixel (inverted: bottom→top).
     */
    private static int toY(int count) {
        return Math.clamp(DIAG_SIZE - count, 0, DIAG_SIZE - 1);
    }

    // === Diagram loading (classloader-based — works server-side) ===

    @SuppressWarnings("checkstyle:NeedBraces")
    private static void ensureLoaded() {
        if (loadAttempted) return;
        loadAttempted = true;
        massRadiusImage = loadImage(MASS_RADIUS);
        ageTempImage = loadImage(AGE_TEMP);
        ageTempSpImage = loadImage(AGE_TEMP_SP);
        ageRadiusImage = loadImage(AGE_RADIUS);
    }

    private static NativeImage loadImage(String classpath) {
        try (InputStream is = AnvilCraft.class.getClassLoader().getResourceAsStream(classpath)) {
            if (is == null) {
                AnvilCraft.LOGGER.warn("CelestialBodyMatcher: missing diagram {}", classpath);
                return null;
            }
            return NativeImage.read(is);
        } catch (Exception e) {
            AnvilCraft.LOGGER.warn("CelestialBodyMatcher: failed to load {}", classpath, e);
            return null;
        }
    }

    @Nullable
    private static NativeImage loadStarColorTemp() {
        if (starColorTempImage == null) {
            starColorTempImage = loadImage(STAR_COLOR_TEMP);
        }
        return starColorTempImage;
    }

    // === Diagram lookups ===

    @Nullable
    private static CelestialBodyClass lookupClass(NativeImage image, int x, int y) {
        if (image == null) return null;
        int xi = Math.clamp(x, 0, image.getWidth() - 1);
        int yi = Math.clamp(y, 0, image.getHeight() - 1);
        int argb = image.getPixelRGBA(xi, yi);
        int r = argb & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = (argb >> 16) & 0xFF;
        int rgb = (r << 16) | (g << 8) | b;
        return CelestialBodyClass.fromRgb(rgb);
    }

    private static int getRgb(NativeImage image, int x, int y) {
        if (image == null) return 0;
        int xi = Math.clamp(x, 0, image.getWidth() - 1);
        int yi = Math.clamp(y, 0, image.getHeight() - 1);
        int argb = image.getPixelRGBA(xi, yi);
        int r = argb & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = (argb >> 16) & 0xFF;
        return (r << 16) | (g << 8) | b;
    }

    // === Step logic ===

    private static boolean step2(int time, int energy, CelestialBodyClass bodyClass) {
        if (bodyClass.step2UsesSp()) {
            return getRgb(ageTempSpImage, time, energy) == bodyClass.rgb();
        }
        if (bodyClass.isPlanetary() && bodyClass != CelestialBodyClass.BROWN_DWARF) {
            // Planetary: step 2 match color; rocky planets all accept 0x339933
            return getRgb(ageTempImage, time, energy) == bodyClass.step2MatchRgb();
        }
        // Main sequence
        return getRgb(ageTempImage, time, energy) == bodyClass.rgb();
    }

    private static boolean step3(int time, int space, CelestialBodyClass bodyClass) {
        return getRgb(ageRadiusImage, time, space) == bodyClass.rgb();
    }

    // === Body data generation ===

    @SuppressWarnings("checkstyle:MethodLength")
    private static CelestialBodyData generateBodyData(
        CelestialBodyClass bodyClass, int time, int space, int mass, int energy, RandomSource random
    ) {
        return switch (bodyClass) {
            case LARGE_MOON -> generateLargeMoon(space, random);
            case ROCKY_NO_LIQUID, ROCKY_LOW_LIQUID, ROCKY_MED_LIQUID, ROCKY_HIGH_LIQUID -> generateRockyPlanet(
                bodyClass, energy, space, random);
            case ICE_GIANT -> generateGiantPlanet(bodyClass, PressureType.ICE, space, random);
            case GAS_GIANT -> generateGiantPlanet(bodyClass, PressureType.GAS, space, random);
            case BROWN_DWARF -> generateBrownDwarf(space, energy, random);
            default -> generateStar(bodyClass, energy, space, random);
        };
    }

    // === Large Moon ===
    private static CelestialBodyData generateLargeMoon(int space, RandomSource random) {
        int size = sizeForSpace(space);
        int mag = random.nextFloat() < 0.5f ? 0 : 1;
        return new RockyPlanetData(
            CelestialBodyClass.LARGE_MOON,
            false, LiquidCoverage.NONE, Temperature.FREEZING,
            RingType.NONE, size,
            random.nextInt(16), 0,
            randomAxialTilt(random), randomRotationSpeed(random), mag
        );
    }

    // === Rocky Planet ===
    private static CelestialBodyData generateRockyPlanet(
        CelestialBodyClass bodyClass, int energy, int space, RandomSource random
    ) {
        LiquidCoverage liquid = switch (bodyClass) {
            case ROCKY_NO_LIQUID -> LiquidCoverage.NONE;
            case ROCKY_LOW_LIQUID -> LiquidCoverage.LOW;
            case ROCKY_MED_LIQUID -> LiquidCoverage.MEDIUM;
            case ROCKY_HIGH_LIQUID -> LiquidCoverage.HIGH;
            default -> LiquidCoverage.NONE;
        };
        boolean hasAtmosphere = random.nextFloat() < 0.2f;
        Temperature temperature = energyToTemperature(energy);
        RingType ring = weightedRing(random, 0.97f, 0.02f, 0.01f);
        int size = sizeForSpace(space);
        int baseRow = random.nextInt(8);
        int overlayRow = liquid == LiquidCoverage.NONE ? 0 : 8 + random.nextInt(8);
        int mag = weightedMagnetic(random, 0.10f, 0.80f, 0.10f);

        return new RockyPlanetData(
            bodyClass,
            hasAtmosphere, liquid, temperature, ring, size,
            baseRow, overlayRow,
            randomAxialTilt(random), randomRotationSpeed(random), mag
        );
    }

    // === Brown Dwarf ===
    private static CelestialBodyData generateBrownDwarf(int space, int energy, RandomSource random) {
        int size = sizeForSpace(space);
        int baseRow = random.nextInt(16);
        int overlayRow;
        do {
            overlayRow = random.nextInt(16);
        } while (overlayRow == baseRow);
        int mag = weightedMagnetic(random, 0.01f, 0.49f, 0.50f);
        return new GiantPlanetData(
            CelestialBodyClass.BROWN_DWARF,
            PressureType.GAS, WindSpeed.HIGH, RingType.NONE, size,
            baseRow, overlayRow,
            randomAxialTilt(random), randomRotationSpeed(random), mag, true
        );
    }

    // === Giant Planet ===
    private static CelestialBodyData generateGiantPlanet(
        CelestialBodyClass bodyClass, PressureType pressure, int space, RandomSource random
    ) {
        RingType ring = weightedRing(random, 0.70f, 0.20f, 0.10f);
        int size = sizeForSpace(space);
        int baseRow = random.nextInt(16);
        int overlayRow;
        do {
            overlayRow = random.nextInt(16);
        } while (overlayRow == baseRow);
        WindSpeed wind = random.nextBoolean() ? WindSpeed.HIGH : WindSpeed.VERY_HIGH;
        int mag = weightedMagnetic(random, 0.01f, 0.49f, 0.50f);
        return new GiantPlanetData(
            bodyClass,
            pressure, wind, ring, size,
            baseRow, overlayRow,
            randomAxialTilt(random), randomRotationSpeed(random), mag, false
        );
    }

    // === Star ===
    private static CelestialBodyData generateStar(
        CelestialBodyClass bodyClass, int energy, int space, RandomSource random
    ) {
        int size = sizeForSpace(space);
        // Get color from star_color_temperature.png at row = energy
        int[] rgb = getStarColorFromTempDiagram(energy);
        int mag = random.nextFloat() < 0.10f ? 5 : 4;
        return new StarData(
            bodyClass,
            size, rgb[0], rgb[1], rgb[2],
            0f, randomRotationSpeed(random), mag, energy  // stars have no axial tilt
        );
    }

    // === Helpers ===

    /**
     * Linear mapping from space anvil count (1–64) to celestial body size.
     */
    private static int sizeForSpace(int space) {
        return Math.clamp(space, 1, 64);
    }

    private static Temperature energyToTemperature(int energy) {
        if (energy <= 12) return Temperature.FREEZING;
        if (energy <= 15) return Temperature.COLD;
        if (energy == 16) return Temperature.MILD;
        if (energy <= 22) return Temperature.HOT;
        return Temperature.SCORCHED;
    }

    private static RingType weightedRing(RandomSource random, float noneChance, float weakChance, float strongChance) {
        float f = random.nextFloat();
        if (f < noneChance) return RingType.NONE;
        if (f < noneChance + weakChance) return RingType.WEAK;
        return RingType.STRONG;
    }

    /**
     * Weighted magnetic field: gives level 0, 1, or 2 with given probabilities.
     */
    private static int weightedMagnetic(RandomSource random, float p0, float p1, float p2) {
        float f = random.nextFloat();
        if (f < p0) return 0;
        if (f < p0 + p1) return 1;
        return 2;
    }

    private static float randomAxialTilt(RandomSource random) {
        // Skew toward 0°: square distribution → ~1% near 90° (flat)
        float raw = random.nextFloat();
        return 90f * raw * raw;
    }

    private static float randomRotationSpeed(RandomSource random) {
        // Discrete weighted values: 0.1 (1%), 0.5 (25%), 1.0 (48%), 1.5 (25%), 3.0 (1%)
        float f = random.nextFloat();
        if (f < 0.01f) return 0.1f;
        if (f < 0.26f) return 0.5f;
        if (f < 0.74f) return 1.0f;
        if (f < 0.99f) return 1.5f;
        return 3.0f;
    }

    @SuppressWarnings("checkstyle:NeedBraces")
    private static int[] getStarColorFromTempDiagram(int energy) {
        NativeImage img = loadStarColorTemp();
        if (img == null) return new int[] {255, 255, 255};
        int row = toY(energy);
        int argb = img.getPixelRGBA(0, row);
        int r = argb & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = (argb >> 16) & 0xFF;
        return new int[] {r, g, b};
    }
}
