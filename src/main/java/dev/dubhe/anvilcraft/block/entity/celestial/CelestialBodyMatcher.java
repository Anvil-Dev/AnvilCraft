package dev.dubhe.anvilcraft.block.entity.celestial;

import com.mojang.blaze3d.platform.NativeImage;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/// 三步天体匹配引擎，通过图表PNG进行匹配。
/// 每张64x64图表将砧子计数映射到像素颜色，从而识别天体类别。
/// 使用类加载器加载图表（同时适用于服务端和客户端）。
public final class CelestialBodyMatcher {

    private static final String DIR = "assets/anvilcraft/textures/misc";

    /// 图表文件路径
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

    /// 预计算的合法（时间,空间,质量,能量）组合
    private static BitSet validAmplified;
    private static BitSet validNormal;
    private static boolean precomputed = false;

    private CelestialBodyMatcher() {
    }

    /// === 公共API ===

    /// 尝试根据四个砧子计数匹配天体。
    /// 图表为64x64像素。砧子计数范围为1-64。
    /// 像素(1,1)位于左下角 → 零索引(x=0, y=63)。
    /// 像素(64,64)位于右上角 → 零索引(x=63, y=0)。
    /// x = count - 1, y = 64 - count（因为PNG原点是左上角）。
    private static final int DIAG_SIZE = 64;

    @Nullable
    public static CelestialBodyData match(
        int time, int space, int mass, int energy, boolean isAmplified, RandomSource random
    ) {
        ensureLoaded();

        /// 第一步：质径图（mass=x, space=y取反）
        CelestialBodyClass bodyClass = lookupClass(massRadiusImage, toX(mass), toY(space));
        if (bodyClass == null) return null;

        /// 恒星类天体需要增幅器
        if (bodyClass.isStellar() && !isAmplified) return null;

        /// 第二步：温度-年龄图（time=x, energy=y取反）
        if (!step2(toX(time), toY(energy), bodyClass)) return null;

        /// 第三步：年龄-半径图（time=x, space=y取反，用于恒星和褐矮星）
        if (bodyClass.needsStep3() && !step3(toX(time), toY(space), bodyClass)) return null;

        /// 生成渲染数据
        return generateBodyData(bodyClass, time, space, mass, energy, random);
    }

    /// 将砧子计数（1-64）映射到图表的零索引x像素坐标。
    public static int toX(int count) {
        return Math.clamp(count - 1, 0, DIAG_SIZE - 1);
    }

    /// 将砧子计数（1-64）映射到图表的零索引y像素坐标（翻转：下→上）。
    public static int toY(int count) {
        return Math.clamp(DIAG_SIZE - count, 0, DIAG_SIZE - 1);
    }

    /// 将四个砧子计数（1-64）编码为单个int索引，用于位集查询。
    private static int encode(int time, int space, int mass, int energy) {
        return ((time - 1) << 18) | ((space - 1) << 12) | ((mass - 1) << 6) | (energy - 1);
    }

    /// === 预计算所有合法组合 ===

    @SuppressWarnings("checkstyle:NeedBraces")
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

    /// 提前触发预计算（例如在打开锻星砧界面时），使首次提示查询无延迟。
    public static void warmup() {
        ensurePrecomputed();
    }

    /// === 提示范围查询 ===

    /// 根据已放置的部分砧子计数，获取某一种砧子的合法范围。
    /// 计数值为0表示"未知/尚未放置"。
    ///
    /// time - 时间砧子计数（0表示未知）
    /// space - 空间砧子计数（0表示未知）
    /// mass - 质量砧子计数（0表示未知）
    /// energy - 能量砧子计数（0表示未知）
    /// isAmplified - 是否启用了增幅器模式
    /// targetIndex - 要查询的砧子类型（0=时间, 1=空间, 2=质量, 3=能量）
    /// 返回 {@code [min, max]} 或 {@code null}（如果不存在合法范围）
    public static int @Nullable [] getValidRange(int time, int space, int mass, int energy, boolean isAmplified, int targetIndex) {
        ensurePrecomputed();
        BitSet bitset = isAmplified ? validAmplified : validNormal;
        if (bitset == null) return null;

        int[] counts = {time, space, mass, energy};

        /// 快速路径：如果其他槽位没有砧子，则整个1-64范围都有效
        boolean allUnknown = true;
        for (int i = 0; i < 4; i++) {
            if (i != targetIndex && counts[i] > 0) {
                allUnknown = false;
                break;
            }
        }
        if (allUnknown) return new int[] {1, 64};

        /// 收集未知索引（排除当前目标）
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

    /// 如果存在至少一种未知索引的赋值方案使完整的4元组在位集中有效，则返回true。
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

    /// === 图表加载（基于类加载器，服务端可用） ===

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

    /// === 图查表找 ===

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

    /// === 步骤逻辑 ===

    private static boolean step2(int time, int energy, CelestialBodyClass bodyClass) {
        if (bodyClass.step2UsesSp()) {
            return getRgb(ageTempSpImage, time, energy) == bodyClass.rgb();
        }
        if (bodyClass.isPlanetary() && bodyClass != CelestialBodyClass.BROWN_DWARF) {
            /// 行星类：第二步匹配颜色；岩石行星统一使用0x339933
            return getRgb(ageTempImage, time, energy) == bodyClass.step2MatchRgb();
        }
        /// 主序星：直接匹配RGB
        return getRgb(ageTempImage, time, energy) == bodyClass.rgb();
    }

    private static boolean step3(int time, int space, CelestialBodyClass bodyClass) {
        return getRgb(ageRadiusImage, time, space) == bodyClass.rgb();
    }

    /// === 天体数据生成 ===

    @SuppressWarnings("checkstyle:MethodLength")
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

    /// === 大卫星 ===
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

    /// === 岩石行星 ===
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

    /// === 褐矮星 ===
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

    /// === 气态行星 ===
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

    /// === 恒星 ===
    private static CelestialBodyData generateStar(
        CelestialBodyClass bodyClass, int energy, int space, RandomSource random
    ) {
        int size = sizeForSpace(space);
        /// 从star_color_temperature.png中根据能量砧子计数所在行获取颜色
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

    /// === 辅助方法 ===

    /// 将空间砧子计数（1-64）线性映射为天体大小。
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

    /// 加权磁场强度：根据给定的概率返回0、1或2级。
    private static int weightedMagnetic(RandomSource random, float p0, float p1, float p2) {
        float f = random.nextFloat();
        if (f < p0) return 0;
        if (f < p0 + p1) return 1;
        return 2;
    }

    private static float randomAxialTilt(RandomSource random) {
        /// 偏向0°：平方分布 → 约1%接近90°（平坦）
        float raw = random.nextFloat();
        return 90f * raw * raw;
    }

    /// 随机自转速度等级（0-5）。
    /// - 0 = 极慢（1%）
    /// - 1 = 慢（25%）
    /// - 2 = 中等（48%）
    /// - 3 = 快（25%）
    /// - 4 = 极快（1%）
    /// - 5 = 超快（0.1%）
    private static int randomRotationSpeed(RandomSource random) {
        float f = random.nextFloat();
        if (f < 0.01f) return 0;
        if (f < 0.26f) return 1;
        if (f < 0.74f) return 2;
        if (f < 0.99f) return 3;
        if (f < 0.999f) return 4;
        return 5; /// 超快
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

    /// === 公开图表像素查询（用于UI指南显示） ===

    /// 获取质径图中(mass, space)坐标处的RGB颜色。
    /// 返回0x000000表示黑色（无匹配）。
    public static int getMassRadiusRgb(int mass, int space) {
        ensureLoaded();
        return getRgb(massRadiusImage, toX(mass), toY(space));
    }

    /// 获取年龄-温度图中(time, energy)坐标处的RGB颜色。
    /// 返回0x000000表示黑色（无匹配）。
    public static int getAgeTempRgb(int time, int energy) {
        ensureLoaded();
        return getRgb(ageTempImage, toX(time), toY(energy));
    }

    /// 获取年龄-温度-sp图中(time, energy)坐标处的RGB颜色。
    /// 返回0x000000表示黑色（无匹配）。
    public static int getAgeTempSpRgb(int time, int energy) {
        ensureLoaded();
        return getRgb(ageTempSpImage, toX(time), toY(energy));
    }

    /// 获取年龄-半径图中(time, space)坐标处的RGB颜色。
    /// 返回0x000000表示黑色（无匹配）。
    public static int getAgeRadiusRgb(int time, int space) {
        ensureLoaded();
        return getRgb(ageRadiusImage, toX(time), toY(space));
    }

    /// === 像素扫描（用于恒星演化加速器） ===

    /// 在年龄-温度图中从(x, y)向右统计非黑色像素数量，
    /// 直到遇到纯黑色像素(0x000000)或到达右边缘。
    /// 从x+1开始（当前像素代表恒星当前状态，剩余寿命从下一个像素开始计算）。
    /// 用于确定剩余主序星寿命。
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

    /// 在年龄-温度-sp图中从(x, y)向下（更低能量，更高PNG的Y坐标）统计非黑色像素，
    /// 直到遇到纯黑色或到达底部边缘。
    /// 从y+1开始（当前像素代表恒星当前状态）。
    /// 用于确定剩余巨星/超巨星阶段寿命。
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

    /// 统计年龄-温度-sp图中x列当前彩色段中的非黑色像素总数。
    /// 从任意黑色分隔后的第一个非黑色像素扫描到下一个黑色分隔，
    /// 统计所有彩色像素。用于计算巨星阶段时间占比。
    public static int countTotalColoredPixelsInAgeTempSpColumn(int x, int startY) {
        ensureLoaded();
        if (ageTempSpImage == null) return 0;
        /// 从startY向上扫描，找到该彩色段的顶部（上方第一个黑色像素处）
        int segmentTop = startY;
        for (int scanY = startY - 1; scanY >= 0; scanY--) {
            if (getRgb(ageTempSpImage, x, scanY) == 0x000000) break;
            segmentTop = scanY;
        }
        /// 从segmentTop向下扫描，统计所有彩色像素直到遇到黑色
        int count = 0;
        for (int scanY = segmentTop; scanY < DIAG_SIZE; scanY++) {
            int rgb = getRgb(ageTempSpImage, x, scanY);
            if (rgb == 0x000000) break;
            count++;
        }
        return count;
    }

    /// 根据能量砧子计数从star_color_temperature.png获取RGB颜色。
    /// 公开方法，供恒星演化加速器创建残骸时使用。
    public static int[] getStarColor(int energy) {
        return getStarColorFromTempDiagram(energy);
    }
}
