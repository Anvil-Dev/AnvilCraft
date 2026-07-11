package dev.dubhe.anvilcraft.block.entity.celestial;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * 基于 PNG 星图的三步天体匹配引擎。
 * 每张 64×64 星图把砧子数量映射为天体类别像素颜色，并通过类加载器在服务端和客户端读取。
 *
 * <p>26.1 的 {@code NativeImage} 仅客户端可用，因此改用服务端同样可用的
 * {@link javax.imageio.ImageIO} 和 {@link BufferedImage}。后者返回 ARGB（0xAARRGGBB），
 * 与旧版 ABGR 像素顺序不同，读取时需要按新顺序提取 RGB。</p>
 */
public final class CelestialBodyMatcher {

    private static final String DIR = "assets/anvilcraft/textures/misc";

    // 星图资源路径
    private static final String MASS_RADIUS = DIR + "/mass_radius_diagram_pixel.png";
    private static final String AGE_TEMP = DIR + "/age_temp_diagram_pixel.png";
    private static final String AGE_TEMP_SP = DIR + "/age_temp_diagram_pixel_sp.png";
    private static final String AGE_RADIUS = DIR + "/age_radius_diagram_pixel.png";
    private static final String STAR_COLOR_TEMP = "assets/anvilcraft/textures/block/celestial_body/star_color_temperature.png";

    private static @Nullable BufferedImage massRadiusImage;
    private static @Nullable BufferedImage ageTempImage;
    private static @Nullable BufferedImage ageTempSpImage;
    private static @Nullable BufferedImage ageRadiusImage;
    private static @Nullable BufferedImage starColorTempImage;
    private static boolean loadAttempted = false;

    // 预计算的合法（时间、空间、质量、能量）组合
    private static @Nullable BitSet validAmplified;
    private static @Nullable BitSet validNormal;
    private static boolean precomputed = false;

    private CelestialBodyMatcher() {
    }

    // === 公共接口 ===

    /** 根据四种砧子数量尝试匹配天体。 */
    // 星图为 64×64 像素，砧子数量范围为 1-64。
    // 左下角 (1,1) 对应零基坐标 (0,63)，右上角 (64,64) 对应 (63,0)。
    private static final int DIAG_SIZE = 64;

    @Nullable
    public static CelestialBodyData match(
        int time, int space, int mass, int energy, boolean isAmplified, RandomSource random
    ) {
        ensureLoaded();

        // 第一步：质量-半径图，质量为 X，空间为反向 Y。
        CelestialBodyClass bodyClass = lookupClass(massRadiusImage, toX(mass), toY(space));
        if (bodyClass == null) return null;

        // 恒星类天体必须使用增幅器。
        if (bodyClass.isStellar() && !isAmplified) return null;

        // 第二步：温度-年龄图，时间为 X，能量为反向 Y。
        if (!step2(toX(time), toY(energy), bodyClass)) return null;

        // 第三步：年龄-半径图，供恒星和褐矮星继续细分。
        if (bodyClass.needsStep3() && !step3(toX(time), toY(space), bodyClass)) return null;

        // 根据匹配结果生成完整天体数据。
        return generateBodyData(bodyClass, time, space, mass, energy, random);
    }

    /** 将 1-64 的砧子数量映射到星图零基 X 坐标。 */
    public static int toX(int count) {
        return Math.clamp(count - 1, 0, DIAG_SIZE - 1);
    }

    /** 将 1-64 的砧子数量映射到反向的星图零基 Y 坐标。 */
    public static int toY(int count) {
        return Math.clamp(DIAG_SIZE - count, 0, DIAG_SIZE - 1);
    }

    /** 将四种砧子数量编码为位集使用的单个整数索引。 */
    private static int encode(int time, int space, int mass, int energy) {
        return ((time - 1) << 18) | ((space - 1) << 12) | ((mass - 1) << 6) | (energy - 1);
    }

    // === 全部合法组合预计算 ===

    private static void ensurePrecomputed() {
        if (precomputed) return;
        ensureLoaded();
        precomputed = true;

        if (massRadiusImage == null) return;

        validAmplified = new BitSet(1 << 24);
        validNormal = new BitSet(1 << 24);

        for (int mass = 1; mass <= 64; mass++) {
            int mx = toX(mass);
            for (int space = 1; space <= 64; space++) {
                int sy = toY(space);
                CelestialBodyClass bodyClass = lookupClass(massRadiusImage, mx, sy);
                if (bodyClass == null) continue;

                int massSpaceBase = ((space - 1) << 12) | ((mass - 1) << 6);

                for (int time = 1; time <= 64; time++) {
                    int tx = toX(time);

                    boolean step3Ok = !bodyClass.needsStep3() || step3(tx, sy, bodyClass);
                    if (!step3Ok) continue;

                    int timeBase = ((time - 1) << 18) | massSpaceBase;

                    for (int energy = 1; energy <= 64; energy++) {
                        int ey = toY(energy);
                        if (step2(tx, ey, bodyClass)) {
                            int index = timeBase | (energy - 1);
                            validAmplified.set(index);
                            if (!bodyClass.isStellar()) {
                                validNormal.set(index);
                            }
                        }
                    }
                }
            }
        }
    }

    /** 提前触发预计算，避免界面首次查询有效范围时卡顿。 */
    public static void warmup() {
        ensurePrecomputed();
    }

    // === 界面提示范围查询 ===

    /**
     * 根据部分已知数量查询某种砧子的合法范围 [min, max]，数量 0 表示尚未放置。
     */
    public static int @Nullable [] getValidRange(int time, int space, int mass, int energy, boolean isAmplified, int targetIndex) {
        ensurePrecomputed();
        BitSet bitset = isAmplified ? validAmplified : validNormal;
        if (bitset == null) return null;

        int[] counts = {time, space, mass, energy};

        boolean allUnknown = true;
        for (int i = 0; i < 4; i++) {
            if (i != targetIndex && counts[i] > 0) {
                allUnknown = false;
                break;
            }
        }
        if (allUnknown) return new int[] {1, 64};

        java.util.List<Integer> unknownIndices = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            if (i != targetIndex && counts[i] <= 0) {
                unknownIndices.add(i);
            }
        }

        int min = 65;
        int max = 0;
        int[] test = counts.clone();

        for (int candidate = 1; candidate <= 64; candidate++) {
            test[targetIndex] = candidate;
            if (anyValid(bitset, test, unknownIndices)) {
                if (candidate < min) min = candidate;
                max = candidate;
            }
        }

        if (min > max) return null;
        return new int[] {min, max};
    }

    private static boolean anyValid(BitSet bitset, int[] counts, List<Integer> unknownIndices) {
        if (unknownIndices.isEmpty()) {
            return bitset.get(encode(counts[0], counts[1], counts[2], counts[3]));
        }
        return anyValidRecursive(bitset, counts, unknownIndices, 0);
    }

    private static boolean anyValidRecursive(BitSet bitset, int[] counts, List<Integer> unknownIndices, int depth) {
        if (depth == unknownIndices.size()) {
            return bitset.get(encode(counts[0], counts[1], counts[2], counts[3]));
        }
        int idx = unknownIndices.get(depth);
        for (int val = 1; val <= 64; val++) {
            counts[idx] = val;
            if (anyValidRecursive(bitset, counts, unknownIndices, depth + 1)) {
                return true;
            }
        }
        return false;
    }

    // === 通过类加载器读取星图，兼容服务端 ===

    private static void ensureLoaded() {
        if (loadAttempted) return;
        loadAttempted = true;
        massRadiusImage = loadImage(MASS_RADIUS);
        ageTempImage = loadImage(AGE_TEMP);
        ageTempSpImage = loadImage(AGE_TEMP_SP);
        ageRadiusImage = loadImage(AGE_RADIUS);
    }

    private static BufferedImage loadImage(String classpath) {
        try (InputStream is = AnvilCraft.class.getClassLoader().getResourceAsStream(classpath)) {
            if (is == null) {
                AnvilCraft.LOGGER.warn("CelestialBodyMatcher: missing diagram {}", classpath);
                return null;
            }
            return ImageIO.read(is);
        } catch (Exception e) {
            AnvilCraft.LOGGER.warn("CelestialBodyMatcher: failed to load {}", classpath, e);
            return null;
        }
    }

    @Nullable
    private static BufferedImage loadStarColorTemp() {
        if (starColorTempImage == null) {
            starColorTempImage = loadImage(STAR_COLOR_TEMP);
        }
        return starColorTempImage;
    }

    // === 星图查找 ===

    /** 从 {@link BufferedImage} 的 ARGB 像素中提取 RGB。 */
    private static int getRgb(@Nullable BufferedImage image, int x, int y) {
        if (image == null) return 0;
        int xi = Math.clamp(x, 0, image.getWidth() - 1);
        int yi = Math.clamp(y, 0, image.getHeight() - 1);
        int argb = image.getRGB(xi, yi);
        // BufferedImage 使用 ARGB 顺序。
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return (r << 16) | (g << 8) | b;
    }

    @Nullable
    private static CelestialBodyClass lookupClass(@Nullable BufferedImage image, int x, int y) {
        if (image == null) return null;
        int rgb = getRgb(image, x, y);
        return CelestialBodyClass.fromRgb(rgb);
    }

    // === 三步匹配逻辑 ===

    private static boolean step2(int time, int energy, CelestialBodyClass bodyClass) {
        if (bodyClass.step2UsesSp()) {
            return getRgb(ageTempSpImage, time, energy) == bodyClass.rgb();
        }
        if (bodyClass.isPlanetary() && bodyClass != CelestialBodyClass.BROWN_DWARF) {
            // 行星类在第二步匹配颜色；岩石行星统一接受 0x339933。
            return getRgb(ageTempImage, time, energy) == bodyClass.step2MatchRgb();
        }
        // 主序星
        return getRgb(ageTempImage, time, energy) == bodyClass.rgb();
    }

    private static boolean step3(int time, int space, CelestialBodyClass bodyClass) {
        return getRgb(ageRadiusImage, time, space) == bodyClass.rgb();
    }

    // === 天体数据生成 ===

    private static CelestialBodyData generateBodyData(
        CelestialBodyClass bodyClass, int time, int space, int mass, int energy, RandomSource random
    ) {
        return switch (bodyClass) {
            case LARGE_MOON -> generateLargeMoon(space, energy, random);
            case ROCKY_NO_LIQUID, ROCKY_LOW_LIQUID, ROCKY_MED_LIQUID, ROCKY_HIGH_LIQUID -> generateRockyPlanet(
                bodyClass, energy, space, random);
            case ICE_GIANT -> generateGiantPlanet(bodyClass, PressureType.ICE, space, random);
            case GAS_GIANT -> generateGiantPlanet(bodyClass, PressureType.GAS, space, random);
            case BROWN_DWARF -> generateBrownDwarf(space, energy, random);
            default -> generateStar(bodyClass, energy, space, random);
        };
    }

    // === 大型卫星 ===
    private static CelestialBodyData generateLargeMoon(int space, int energy, RandomSource random) {
        int size = sizeForSpace(space);
        int mag = random.nextFloat() < 0.5f ? 0 : 1;
        Temperature temperature = energyToTemperature(energy);
        return new RockyPlanetData(
            CelestialBodyClass.LARGE_MOON,
            false, LiquidCoverage.NONE, temperature,
            RingType.NONE, size,
            random.nextInt(16), 0,
            randomAxialTilt(random), randomRotationSpeed(random), mag
        );
    }

    // === 岩石行星 ===
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

    // === 褐矮星 ===
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

    // === 巨行星 ===
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

    // === 恒星 ===
    private static CelestialBodyData generateStar(
        CelestialBodyClass bodyClass, int energy, int space, RandomSource random
    ) {
        int size = sizeForSpace(space);
        int[] rgb = getStarColorFromTempDiagram(energy);
        int mag = random.nextFloat() < 0.10f ? 5 : 4;
        int rotSpeed = bodyClass == CelestialBodyClass.BLACK_HOLE ? 0 : randomRotationSpeed(random);
        float axialTilt = 0f;
        return new StarData(
            bodyClass,
            size, rgb[0], rgb[1], rgb[2],
            axialTilt, rotSpeed, mag, energy,
            null
        );
    }

    // === 辅助方法 ===

    private static int sizeForSpace(int space) {
        return Math.clamp(space, 1, 64);
    }

    public static Temperature energyToTemperature(int energy) {
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

    private static int weightedMagnetic(RandomSource random, float p0, float p1, float p2) {
        float f = random.nextFloat();
        if (f < p0) return 0;
        if (f < p0 + p1) return 1;
        return 2;
    }

    private static float randomAxialTilt(RandomSource random) {
        float raw = random.nextFloat();
        return 90f * raw * raw;
    }

    private static int randomRotationSpeed(RandomSource random) {
        float f = random.nextFloat();
        if (f < 0.01f) return 0;
        if (f < 0.26f) return 1;
        if (f < 0.74f) return 2;
        if (f < 0.99f) return 3;
        if (f < 0.999f) return 4;
        return 5;
    }

    private static int[] getStarColorFromTempDiagram(int energy) {
        BufferedImage img = loadStarColorTemp();
        if (img == null) {
            return new int[]{255, 255, 255};
        }
        int row = toY(energy);
        int argb = img.getRGB(0, row);
        // BufferedImage 的像素格式为 AARRGGBB。
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return new int[] {r, g, b};
    }

    // === 供界面星图指南使用的公开像素查询 ===

    public static int getMassRadiusRgb(int mass, int space) {
        ensureLoaded();
        return getRgb(massRadiusImage, toX(mass), toY(space));
    }

    public static int getAgeTempRgb(int time, int energy) {
        ensureLoaded();
        return getRgb(ageTempImage, toX(time), toY(energy));
    }

    public static int getAgeTempSpRgb(int time, int energy) {
        ensureLoaded();
        return getRgb(ageTempSpImage, toX(time), toY(energy));
    }

    public static int getAgeRadiusRgb(int time, int space) {
        ensureLoaded();
        return getRgb(ageRadiusImage, toX(time), toY(space));
    }

    // === 恒星演化加速器使用的像素扫描 ===

    public static int countPixelsRightInAgeTemp(int x, int y) {
        ensureLoaded();
        if (ageTempImage == null) return 0;
        int count = 0;
        for (int scanX = x + 1; scanX < DIAG_SIZE; scanX++) {
            int rgb = getRgb(ageTempImage, scanX, y);
            if (rgb == 0x000000) break;
            count++;
        }
        return count;
    }

    public static int countPixelsDownInAgeTempSp(int x, int y) {
        ensureLoaded();
        if (ageTempSpImage == null) return 0;
        int count = 0;
        for (int scanY = y + 1; scanY < DIAG_SIZE; scanY++) {
            int rgb = getRgb(ageTempSpImage, x, scanY);
            if (rgb == 0x000000) break;
            count++;
        }
        return count;
    }

    public static int countTotalColoredPixelsInAgeTempSpColumn(int x, int startY) {
        ensureLoaded();
        if (ageTempSpImage == null) return 0;
        int segmentTop = startY;
        for (int scanY = startY - 1; scanY >= 0; scanY--) {
            if (getRgb(ageTempSpImage, x, scanY) == 0x000000) break;
            segmentTop = scanY;
        }
        int count = 0;
        for (int scanY = segmentTop; scanY < DIAG_SIZE; scanY++) {
            int rgb = getRgb(ageTempSpImage, x, scanY);
            if (rgb == 0x000000) break;
            count++;
        }
        return count;
    }

    public static int[] getStarColor(int energy) {
        return getStarColorFromTempDiagram(energy);
    }
}
