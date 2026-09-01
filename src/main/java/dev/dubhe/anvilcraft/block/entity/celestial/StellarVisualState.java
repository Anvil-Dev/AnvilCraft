package dev.dubhe.anvilcraft.block.entity.celestial;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * 恒星渲染用的浮点快照。
 *
 * <p>该类型不属于 {@link StarData}，也不会参与巨构资格、引力或资源计算。
 * 服务端和客户端都可以用同一组数值采样，从而避免每 20 tick 改写离散天体数据。</p>
 */
public record StellarVisualState(
    float radius,
    float temperature,
    float luminosity,
    int surfaceColor,
    float emission,
    float envelopeOpacity,
    float coreRadius,
    float ejectaRadius,
    float pulsationAmplitude,
    float pulsationFrequency,
    String surfaceStyle
) {
    public static final Codec<StellarVisualState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.FLOAT.fieldOf("radius").forGetter(StellarVisualState::radius),
        Codec.FLOAT.fieldOf("temperature").forGetter(StellarVisualState::temperature),
        Codec.FLOAT.fieldOf("luminosity").forGetter(StellarVisualState::luminosity),
        Codec.INT.fieldOf("surfaceColor").forGetter(StellarVisualState::surfaceColor),
        Codec.FLOAT.fieldOf("emission").forGetter(StellarVisualState::emission),
        Codec.FLOAT.fieldOf("envelopeOpacity").forGetter(StellarVisualState::envelopeOpacity),
        Codec.FLOAT.fieldOf("coreRadius").forGetter(StellarVisualState::coreRadius),
        Codec.FLOAT.fieldOf("ejectaRadius").forGetter(StellarVisualState::ejectaRadius),
        Codec.FLOAT.fieldOf("pulsationAmplitude").forGetter(StellarVisualState::pulsationAmplitude),
        Codec.FLOAT.fieldOf("pulsationFrequency").forGetter(StellarVisualState::pulsationFrequency),
        Codec.STRING.optionalFieldOf("surfaceStyle", "default").forGetter(StellarVisualState::surfaceStyle)
    ).apply(instance, StellarVisualState::new));

    /** 没有演化状态时使用的中性快照。 */
    public static final StellarVisualState DEFAULT = new StellarVisualState(
        1.0f,
        5800.0f,
        1.0f,
        0xFFF3D0,
        1.0f,
        0.0f,
        1.0f,
        0.0f,
        0.0f,
        0.0f,
        "default"
    );

    public StellarVisualState {
        radius = finiteAtLeast(radius, 0.01f);
        temperature = finiteAtLeast(temperature, 100.0f);
        luminosity = finiteAtLeast(luminosity, 0.0f);
        surfaceColor &= 0xFFFFFF;
        emission = finiteAtLeast(emission, 0.0f);
        envelopeOpacity = clampFinite(envelopeOpacity, 0.0f, 1.0f);
        coreRadius = finiteAtLeast(coreRadius, 0.0f);
        ejectaRadius = finiteAtLeast(ejectaRadius, 0.0f);
        pulsationAmplitude = clampFinite(pulsationAmplitude, 0.0f, 0.95f);
        pulsationFrequency = finiteAtLeast(pulsationFrequency, 0.0f);
        surfaceStyle = surfaceStyle == null || surfaceStyle.isBlank() ? "default" : surfaceStyle;
    }

    /** 兼容只提供基础物理量的调用方。 */
    public StellarVisualState(
        float radius,
        float temperature,
        float luminosity,
        int surfaceColor,
        float emission,
        float envelopeOpacity,
        float coreRadius,
        float ejectaRadius,
        float pulsationAmplitude,
        float pulsationFrequency
    ) {
        this(
            radius,
            temperature,
            luminosity,
            surfaceColor,
            emission,
            envelopeOpacity,
            coreRadius,
            ejectaRadius,
            pulsationAmplitude,
            pulsationFrequency,
            "default"
        );
    }

    private static float finiteAtLeast(float value, float minimum) {
        return Float.isFinite(value) ? Math.max(minimum, value) : minimum;
    }

    private static float clampFinite(float value, float min, float max) {
        return Float.isFinite(value) ? Math.clamp(value, min, max) : min;
    }

    public float red() {
        return ((surfaceColor >> 16) & 0xFF) / 255.0f;
    }

    public float green() {
        return ((surfaceColor >> 8) & 0xFF) / 255.0f;
    }

    public float blue() {
        return (surfaceColor & 0xFF) / 255.0f;
    }

    public int surfaceColorRgb() {
        return surfaceColor;
    }

    public float[] surfaceColorComponents() {
        return new float[] {red(), green(), blue()};
    }

    /**
     * 以单调且无过冲的平滑曲线连接两个视觉快照。
     *
     * <p>{@code from} 是阶段入口状态（上一节点），{@code to} 是本阶段自身的目标状态。
     * 半径、温度和光度在阶段内部过渡到本节点的值，因此“红巨星支膨胀”“AGB 膨胀”发生在
     * 对应阶段内部，而不会提前到上一个阶段的末尾。带脉动的阶段在前段就完成过渡，剩下
     * 的时间留给可见的脉冲；这条包络同时是束星环的依据，因此环在整个阶段里保持本阶段
     * 的最大尺寸，不跟着脉冲一起缩放。脉动参数和表面风格是本阶段的固有特征，全程直接
     * 取 {@code to}，不做跨阶段混合。</p>
     */
    public static StellarVisualState interpolate(StellarVisualState from, StellarVisualState to, float progress) {
        float safeProgress = Float.isFinite(progress) ? Math.clamp(progress, 0.0f, 1.0f) : 0.0f;
        float t = riseCurve(safeProgress, to.pulsationAmplitude, to.pulsationFrequency);
        float temperature = lerp(from.temperature, to.temperature, t);
        return new StellarVisualState(
            lerp(from.radius, to.radius, t),
            temperature,
            lerp(from.luminosity, to.luminosity, t),
            colorForTemperature(temperature),
            lerp(from.emission, to.emission, t),
            lerp(from.envelopeOpacity, to.envelopeOpacity, t),
            lerp(from.coreRadius, to.coreRadius, t),
            lerp(from.ejectaRadius, to.ejectaRadius, t),
            to.pulsationAmplitude,
            to.pulsationFrequency,
            to.surfaceStyle
        );
    }

    /** 脉动阶段用前 1/(次数+1) 的时间完成过渡，余下时间留给脉冲；其余阶段整段平滑过渡。 */
    private static float riseCurve(float progress, float amplitude, float cycles) {
        if (amplitude <= 0.0f || cycles <= 0.0f) return smoothstep(progress);
        return smoothstep(Math.clamp(progress * (cycles + 1.0f), 0.0f, 1.0f));
    }

    private static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }

    private static float smoothstep(float value) {
        return value * value * (3.0f - 2.0f * value);
    }

    /** 在近似线性光空间插值 RGB，避免温度过渡中间出现灰暗色。 */
    public static int interpolateColor(int from, int to, float progress) {
        float t = Float.isFinite(progress) ? Math.clamp(progress, 0.0f, 1.0f) : 0.0f;
        int r = blendChannel((from >> 16) & 0xFF, (to >> 16) & 0xFF, t);
        int g = blendChannel((from >> 8) & 0xFF, (to >> 8) & 0xFF, t);
        int b = blendChannel(from & 0xFF, to & 0xFF, t);
        return (r << 16) | (g << 8) | b;
    }

    private static int blendChannel(int from, int to, float progress) {
        float a = srgbToLinear(from / 255.0f);
        float b = srgbToLinear(to / 255.0f);
        return Math.clamp(Math.round(linearToSrgb(a + (b - a) * progress) * 255.0f), 0, 255);
    }

    private static float srgbToLinear(float value) {
        return value <= 0.04045f ? value / 12.92f : (float) Math.pow((value + 0.055f) / 1.055f, 2.4f);
    }

    private static float linearToSrgb(float value) {
        return value <= 0.0031308f ? value * 12.92f : 1.055f * (float) Math.pow(value, 1.0 / 2.4) - 0.055f;
    }

    /** 根据有效温度得到稳定且偏高饱和度的近似黑体颜色，服务端无需依赖客户端纹理。 */
    public static int colorForTemperature(float kelvin) {
        float safeKelvin = Float.isFinite(kelvin) ? kelvin : 5800.0f;
        float clampedKelvin = Math.clamp(safeKelvin, 1000.0f, 40000.0f);
        float temperature = clampedKelvin / 100.0f;
        float red;
        float green;
        float blue;
        if (temperature <= 66.0f) {
            red = 255.0f;
            green = 99.4708f * (float) Math.log(temperature) - 161.1196f;
            blue = temperature <= 19.0f
                ? 0.0f
                : 138.5177f * (float) Math.log(temperature - 10.0f) - 305.0448f;
        } else {
            red = 329.6987f * (float) Math.pow(temperature - 60.0f, -0.1332048f);
            green = 288.1222f * (float) Math.pow(temperature - 60.0f, -0.0755148f);
            blue = 255.0f;
        }
        int r = Math.clamp(Math.round(red), 0, 255);
        int g = Math.clamp(Math.round(green), 0, 255);
        int b = Math.clamp(Math.round(blue), 0, 255);
        return vividifyBlackbodyColor((r << 16) | (g << 8) | b, clampedKelvin);
    }

    public static int surfaceColorForTemperature(float kelvin) {
        return colorForTemperature(kelvin);
    }

    /** 根据旧版表面分类给没有演化状态的恒星提供一致的近似有效温度。 */
    public static float temperatureForSurfaceClass(CelestialBodyClass bodyClass, int energy) {
        if (bodyClass == CelestialBodyClass.WHITE_DWARF) return 14000.0f;
        if (bodyClass == CelestialBodyClass.NEUTRON_STAR) return 100000.0f;
        if (bodyClass == CelestialBodyClass.BLACK_HOLE) return 1000.0f;
        String name = bodyClass.name();
        float base = name.startsWith("M_") ? 3200.0f
            : name.startsWith("K_") ? 4500.0f
            : name.startsWith("G_") ? 5700.0f
            : name.startsWith("F_") ? 7000.0f
            : name.startsWith("A_") ? 9000.0f
            : name.startsWith("B_") ? 15000.0f
            : name.startsWith("O_") ? 30000.0f : 5800.0f;
        if (name.endsWith("_GIANT")) base *= 0.88f;
        if (name.endsWith("_SUPERGIANT")) base *= 0.82f;
        return Math.clamp(base + (Math.clamp(energy, 1, 64) - 32) * 18.0f, 1000.0f, 40000.0f);
    }

    /**
     * 提升黑体色的饱和度，同时在 6000K 附近保留接近白色的过渡带。
     * 原始黑体公式在 4500--6000K 的 RGB 差异很小，经过半透明立方体叠加后
     * 会被渲染成灰白色；这里只调整视觉颜色，不改变快照中的温度。
     */
    private static int vividifyBlackbodyColor(int rgb, float kelvin) {
        float red = ((rgb >> 16) & 0xFF) / 255.0f;
        float green = ((rgb >> 8) & 0xFF) / 255.0f;
        float blue = (rgb & 0xFF) / 255.0f;
        float max = Math.max(red, Math.max(green, blue));
        float min = Math.min(red, Math.min(green, blue));
        float delta = max - min;
        if (max <= 1.0e-6f || delta <= 1.0e-6f) return rgb;

        float hue;
        if (max == red) {
            hue = 60.0f * ((green - blue) / delta);
            if (hue < 0.0f) hue += 360.0f;
        } else if (max == green) {
            hue = 60.0f * ((blue - red) / delta + 2.0f);
        } else {
            hue = 60.0f * ((red - green) / delta + 4.0f);
        }

        float saturation = delta / max;
        float targetSaturation;
        if (kelvin < 6000.0f) {
            float warm = Math.clamp((6000.0f - kelvin) / 3000.0f, 0.0f, 1.0f);
            targetSaturation = 0.42f + warm * 0.25f;
            // 暖色恒星的视觉色更接近黄橙，而不是低饱和的粉白色。
            hue = blendHue(hue, 38.0f, 0.65f);
        } else if (kelvin < 8000.0f) {
            // 太阳色温附近保留白色窗口，避免跨越绿色区域。
            targetSaturation = saturation * 1.35f;
        } else {
            targetSaturation = Math.max(0.30f, saturation * 1.45f);
            hue = 220.0f;
        }
        saturation = Math.clamp(Math.max(saturation * 1.35f, targetSaturation), 0.0f, 0.82f);
        return hsvToRgb(hue, saturation, max);
    }

    private static float blendHue(float from, float to, float amount) {
        float delta = ((to - from + 540.0f) % 360.0f) - 180.0f;
        return (from + delta * Math.clamp(amount, 0.0f, 1.0f) + 360.0f) % 360.0f;
    }

    private static int hsvToRgb(float hue, float saturation, float value) {
        float chroma = value * saturation;
        float h = ((hue % 360.0f) + 360.0f) % 360.0f / 60.0f;
        float x = chroma * (1.0f - Math.abs(h % 2.0f - 1.0f));
        float red;
        float green;
        float blue;
        if (h < 1.0f) {
            red = chroma;
            green = x;
            blue = 0.0f;
        } else if (h < 2.0f) {
            red = x;
            green = chroma;
            blue = 0.0f;
        } else if (h < 3.0f) {
            red = 0.0f;
            green = chroma;
            blue = x;
        } else if (h < 4.0f) {
            red = 0.0f;
            green = x;
            blue = chroma;
        } else if (h < 5.0f) {
            red = x;
            green = 0.0f;
            blue = chroma;
        } else {
            red = chroma;
            green = 0.0f;
            blue = x;
        }
        float match = value - chroma;
        return (Math.clamp(Math.round((red + match) * 255.0f), 0, 255) << 16)
            | (Math.clamp(Math.round((green + match) * 255.0f), 0, 255) << 8)
            | Math.clamp(Math.round((blue + match) * 255.0f), 0, 255);
    }

    /** 脉冲峰值在阶段内的总衰减比例，与脉冲深度成正比。 */
    private static final float PULSE_PEAK_DECAY = 0.25f;
    /** 末段收敛区间：脉冲和峰值衰减在阶段结束前回到包络，保证阶段边界连续。 */
    private static final float PULSE_EXIT_START = 0.92f;

    /**
     * 在阶段包络上叠加脉冲后的视觉半径；不改变玩法半径。
     *
     * <p>脉冲只向内收缩（峰值等于包络），因此束星环按包络取值即可在整个阶段保持本阶段
     * 的最大尺寸，不会跟着恒星一起呼吸。{@link #pulsationFrequency} 是“每个阶段脉动几
     * 次”，用阶段进度而不是绝对游戏刻推进：脉动次数不再取决于阶段被分到多少刻，频率在
     * 阶段之间变化时也不会因为“绝对刻 × 频率”整体平移而让相位瞬间跳到随机位置——那正是
     * 旧实现里换阶段出现高频抖动的原因。每个脉冲的峰值逐次降低，末段再收敛回包络。</p>
     */
    public float radiusAt(double phaseProgress) {
        if (pulsationAmplitude <= 0.0f || pulsationFrequency <= 0.0f) return radius;
        double progress = Double.isFinite(phaseProgress) ? Math.clamp(phaseProgress, 0.0, 1.0) : 0.0;
        double riseFraction = 1.0 / (pulsationFrequency + 1.0);
        double pulseProgress = Math.clamp((progress - riseFraction) / (1.0 - riseFraction), 0.0, 1.0);
        double exit = Math.clamp((progress - PULSE_EXIT_START) / (1.0 - PULSE_EXIT_START), 0.0, 1.0);
        double hold = 1.0 - exit * exit * (3.0 - 2.0 * exit);
        double dip = 0.5 - 0.5 * Math.cos(pulseProgress * pulsationFrequency * Math.PI * 2.0);
        double peak = 1.0 - PULSE_PEAK_DECAY * pulsationAmplitude * pulseProgress * hold;
        return radius * (float) (peak * (1.0 - pulsationAmplitude * dip * hold));
    }

    /** 返回只替换视觉半径的快照，保持其它物理量和玩法字段不变。 */
    public StellarVisualState withRadius(float newRadius) {
        float safe = Math.max(0.01f, newRadius);
        float ratio = radius <= 0.01f ? 1.0f : safe / radius;
        return new StellarVisualState(
            safe,
            temperature,
            luminosity,
            surfaceColor,
            emission,
            envelopeOpacity,
            coreRadius * ratio,
            ejectaRadius * ratio,
            pulsationAmplitude,
            pulsationFrequency,
            surfaceStyle
        );
    }

    /** 只改变本体视觉半径，保持独立抛射壳和核心层的尺寸不变。 */
    public StellarVisualState withVisualRadius(float newRadius) {
        return new StellarVisualState(
            Math.max(0.01f, newRadius),
            temperature,
            luminosity,
            surfaceColor,
            emission,
            envelopeOpacity,
            coreRadius,
            ejectaRadius,
            pulsationAmplitude,
            pulsationFrequency,
            surfaceStyle
        );
    }

    /** 只替换表面颜色，用于从发现时颜色平滑过渡到轨道颜色。 */
    public StellarVisualState withSurfaceColor(int color) {
        return new StellarVisualState(
            radius,
            temperature,
            luminosity,
            color,
            emission,
            envelopeOpacity,
            coreRadius,
            ejectaRadius,
            pulsationAmplitude,
            pulsationFrequency,
            surfaceStyle
        );
    }
}
