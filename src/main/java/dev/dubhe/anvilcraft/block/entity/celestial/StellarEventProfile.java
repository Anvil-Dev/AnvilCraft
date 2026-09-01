package dev.dubhe.anvilcraft.block.entity.celestial;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 恒星终局事件的视觉参数。
 *
 * <p>事件只描述渲染时间线和最终残骸提示，实际方块破坏、伤害和残骸写回仍由
 * 加速器的既有玩法路径负责。曲线中的半径是相对于事件开始时本体半径的比例。</p>
 */
public final class StellarEventProfile implements StringRepresentable {
    /** 通用事件时间线段。 */
    public enum TimelinePhase {
        PRECURSOR,
        CORE_COLLAPSE,
        SHOCK_BREAKOUT,
        EJECTA_EXPANSION,
        FADE
    }

    /** 事件最终残骸种类。NONE 表示完全解体。 */
    public enum RemnantKind {
        WHITE_DWARF("white_dwarf"),
        NEUTRON_STAR("neutron_star"),
        BLACK_HOLE("black_hole"),
        NONE("none");

        private final String id;

        public static final Codec<RemnantKind> CODEC = Codec.STRING.comapFlatMap(
            id -> {
                RemnantKind kind = fromId(id);
                return kind == null
                    ? DataResult.error(() -> "未知残骸种类: " + id)
                    : DataResult.success(kind);
            },
            RemnantKind::getSerializedName
        );

        RemnantKind(String id) {
            this.id = id;
        }

        public String getSerializedName() {
            return this.id;
        }

        @javax.annotation.Nullable
        private static RemnantKind fromId(String id) {
            if (id == null) return null;
            for (RemnantKind kind : values()) {
                if (kind.id.equalsIgnoreCase(id) || kind.name().equalsIgnoreCase(id)) return kind;
            }
            return null;
        }
    }

    public static final Codec<StellarEventProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("profileId").forGetter(StellarEventProfile::profileId),
        Codec.INT.fieldOf("precursorTicks").forGetter(StellarEventProfile::precursorTicks),
        Codec.INT.fieldOf("collapseTicks").forGetter(StellarEventProfile::collapseTicks),
        Codec.INT.fieldOf("ejectaTicks").forGetter(StellarEventProfile::ejectaTicks),
        Codec.INT.fieldOf("fadeTicks").forGetter(StellarEventProfile::fadeTicks),
        Codec.FLOAT.listOf().fieldOf("coreRadiusCurve").forGetter(StellarEventProfile::coreRadiusCurve),
        Codec.FLOAT.listOf().fieldOf("ejectaRadiusCurve").forGetter(StellarEventProfile::ejectaRadiusCurve),
        Codec.FLOAT.fieldOf("peakEmission").forGetter(StellarEventProfile::peakEmission),
        Codec.INT.listOf().fieldOf("palette").forGetter(StellarEventProfile::palette),
        Codec.INT.fieldOf("shellCount").forGetter(StellarEventProfile::shellCount),
        Codec.INT.fieldOf("rayCount").forGetter(StellarEventProfile::rayCount),
        Codec.FLOAT.fieldOf("rayLength").forGetter(StellarEventProfile::rayLength),
        Codec.FLOAT.fieldOf("asymmetry").forGetter(StellarEventProfile::asymmetry),
        RemnantKind.CODEC.fieldOf("remnantKind").forGetter(StellarEventProfile::remnantKind)
    ).apply(instance, StellarEventProfile::new));

    private final String profileId;
    private final int precursorTicks;
    private final int collapseTicks;
    private final int ejectaTicks;
    private final int fadeTicks;
    private final List<Float> coreRadiusCurve;
    private final List<Float> ejectaRadiusCurve;
    private final float peakEmission;
    private final List<Integer> palette;
    private final int shellCount;
    private final int rayCount;
    private final float rayLength;
    private final float asymmetry;
    private final RemnantKind remnantKind;

    public StellarEventProfile(
        String profileId,
        int precursorTicks,
        int collapseTicks,
        int ejectaTicks,
        int fadeTicks,
        List<Float> coreRadiusCurve,
        List<Float> ejectaRadiusCurve,
        float peakEmission,
        List<Integer> palette,
        int shellCount,
        int rayCount,
        float rayLength,
        float asymmetry,
        RemnantKind remnantKind
    ) {
        this.profileId = Objects.requireNonNull(profileId);
        this.precursorTicks = Math.max(0, precursorTicks);
        this.collapseTicks = Math.max(1, collapseTicks);
        this.ejectaTicks = Math.max(0, ejectaTicks);
        this.fadeTicks = Math.max(0, fadeTicks);
        this.coreRadiusCurve = normaliseCurve(coreRadiusCurve, 1.0f);
        this.ejectaRadiusCurve = normaliseCurve(ejectaRadiusCurve, 0.0f);
        this.peakEmission = Float.isFinite(peakEmission) ? Math.max(0.0f, peakEmission) : 0.0f;
        this.palette = palette == null || palette.isEmpty()
            ? List.of(0xFFFFFF)
            : palette.stream().map(value -> value == null ? 0xFFFFFF : value & 0xFFFFFF).toList();
        this.shellCount = Math.max(0, shellCount);
        this.rayCount = Math.max(0, rayCount);
        this.rayLength = Float.isFinite(rayLength) ? Math.max(0.0f, rayLength) : 0.0f;
        this.asymmetry = Float.isFinite(asymmetry) ? Math.clamp(asymmetry, 0.0f, 1.0f) : 0.0f;
        this.remnantKind = Objects.requireNonNull(remnantKind);
    }

    private static List<Float> normaliseCurve(List<Float> curve, float fallback) {
        if (curve == null || curve.isEmpty()) return List.of(fallback);
        return curve.stream()
            .map(value -> value == null || !Float.isFinite(value) ? fallback : Math.max(0.0f, value))
            .toList();
    }

    public String profileId() {
        return this.profileId;
    }

    public String id() {
        return this.profileId;
    }

    @Override
    public String getSerializedName() {
        return this.profileId;
    }

    public int precursorTicks() {
        return this.precursorTicks;
    }

    public int collapseTicks() {
        return this.collapseTicks;
    }

    public int ejectaTicks() {
        return this.ejectaTicks;
    }

    public int fadeTicks() {
        return this.fadeTicks;
    }

    public List<Float> coreRadiusCurve() {
        return this.coreRadiusCurve;
    }

    public List<Float> ejectaRadiusCurve() {
        return this.ejectaRadiusCurve;
    }

    public float peakEmission() {
        return this.peakEmission;
    }

    public List<Integer> palette() {
        return this.palette;
    }

    /** 按事件进度在 profile 调色板中平滑取色。 */
    public int color(float progress) {
        if (palette.size() == 1) return palette.get(0);
        float t = safeProgress(progress) * (palette.size() - 1);
        int index = Math.min((int) Math.floor(t), palette.size() - 2);
        return StellarVisualState.interpolateColor(palette.get(index), palette.get(index + 1), t - index);
    }

    public int shellCount() {
        return this.shellCount;
    }

    public int rayCount() {
        return this.rayCount;
    }

    public float rayLength() {
        return this.rayLength;
    }

    public float asymmetry() {
        return this.asymmetry;
    }

    public RemnantKind remnantKind() {
        return this.remnantKind;
    }

    /** 返回整个事件时间线长度，保证至少包含坍缩窗口。 */
    public int totalTicks() {
        long total = (long) this.precursorTicks + this.collapseTicks + this.ejectaTicks + this.fadeTicks;
        return (int) Math.clamp(total, 1L, Integer.MAX_VALUE);
    }

    public int shockBreakoutTick() {
        long total = (long) this.precursorTicks + this.collapseTicks;
        return (int) Math.clamp(total, 0L, Integer.MAX_VALUE);
    }

    public TimelinePhase timelinePhase(float progress) {
        int tick = Math.round(safeProgress(progress) * totalTicks());
        if (tick < precursorTicks) return TimelinePhase.PRECURSOR;
        if (tick < shockBreakoutTick()) return TimelinePhase.CORE_COLLAPSE;
        if (tick == shockBreakoutTick()) return TimelinePhase.SHOCK_BREAKOUT;
        if (tick < shockBreakoutTick() + ejectaTicks) return TimelinePhase.EJECTA_EXPANSION;
        return TimelinePhase.FADE;
    }

    /**
     * 根据事件进度采样核心半径曲线。
     *
     * <p>AGB 热脉冲的胀缩由阶段级脉动负责（见 {@code StellarVisualState#radiusAt}），
     * 这里不再叠加第二个正弦，否则两个不同周期的振荡叠在一起会变成高频抖动。</p>
     */
    public float coreRadius(float progress) {
        float value = sampleCurve(this.coreRadiusCurve, progress);
        if (this.profileId.equals("PULSATIONAL_PAIR")) {
            float envelope = (float) Math.sin(progress * Math.PI);
            value *= 1.0f + 0.08f * envelope
                * (float) Math.sin(progress * 3.0f * Math.PI * 2.0);
        }
        return Math.max(0.0f, value);
    }

    /** 根据事件进度采样抛射物半径曲线。 */
    public float ejectaRadius(float progress) {
        float value = sampleCurve(this.ejectaRadiusCurve, progress);
        if (this.profileId.equals("AGB_THERMAL_PULSE")) {
            // 使用连续的呼吸曲线，避免在脉冲边界瞬间归零造成细碎抖动。
            float pulse = 0.5f - 0.5f * (float) Math.cos(progress * Math.PI * 2.0);
            value *= 0.72f + pulse * 0.28f;
        }
        return Math.max(0.0f, value);
    }

    /** 曲线可能达到的最大抛射半径，用于包围盒预留空间。 */
    public float maxEjectaRadius() {
        float maximum = 0.0f;
        for (Float value : ejectaRadiusCurve) {
            if (value != null && Float.isFinite(value)) maximum = Math.max(maximum, value);
        }
        return maximum;
    }

    /** 曲线可能达到的最大核心半径。 */
    public float maxCoreRadius() {
        float maximum = 0.0f;
        for (Float value : coreRadiusCurve) {
            if (value != null && Float.isFinite(value)) maximum = Math.max(maximum, value);
        }
        float pulseMargin = this.profileId.equals("PULSATIONAL_PAIR") ? 1.08f : 1.0f;
        return maximum * pulseMargin;
    }

    /** 计算带有峰值和末段淡出的发光强度。 */
    public float emission(float progress) {
        float t = safeProgress(progress);
        float rise = smoothstep(Math.clamp(t * 4.0f, 0.0f, 1.0f));
        float fade = t > 0.72f ? smoothstep(Math.clamp((1.0f - t) / 0.28f, 0.0f, 1.0f)) : 1.0f;
        float pulse = 1.0f;
        if (this.profileId.equals("AGB_THERMAL_PULSE") || this.profileId.equals("PULSATIONAL_PAIR")) {
            float cycles = this.profileId.equals("PULSATIONAL_PAIR") ? 3.0f : 2.0f;
            float wave = 0.5f + 0.5f * (float) Math.sin(t * cycles * Math.PI * 2.0);
            pulse = 0.78f + 0.22f * wave;
        }
        return this.peakEmission * pulse * Math.clamp(rise, 0.0f, 1.0f) * Math.clamp(fade, 0.0f, 1.0f);
    }

    private static float smoothstep(float value) {
        return value * value * (3.0f - 2.0f * value);
    }

    private static float sampleCurve(List<Float> curve, float progress) {
        if (curve.isEmpty()) return 0.0f;
        if (curve.size() == 1) return curve.get(0);
        float t = safeProgress(progress) * (curve.size() - 1);
        int low = Math.min((int) Math.floor(t), curve.size() - 2);
        float fraction = t - low;
        fraction = fraction * fraction * (3.0f - 2.0f * fraction);
        return curve.get(low) + (curve.get(low + 1) - curve.get(low)) * fraction;
    }

    private static float safeProgress(float progress) {
        return Float.isFinite(progress) ? Math.clamp(progress, 0.0f, 1.0f) : 0.0f;
    }

    /** 内置 profile，作为数据包缺失时的确定性兼容回退。 */
    public static Map<String, StellarEventProfile> defaults() {
        return Map.ofEntries(
            entry("HELIUM_FLASH", 4, 4, 0, 12, List.of(1.0f, 0.82f, 1.05f), List.of(0.0f, 0.0f),
                1.8f, List.of(0xFFF0A0, 0xFFFFFF), 1, 0, 0.0f, 0.0f, RemnantKind.NONE),
            entry("AGB_THERMAL_PULSE", 8, 8, 24, 12, List.of(1.0f),
                List.of(0.0f, 0.5f, 1.0f), 1.5f, List.of(0xFFB060, 0xFFE0A0), 3, 0, 2.0f, 0.12f,
                RemnantKind.NONE),
            entry("CORE_COLLAPSE_II_P", 20, 8, 6, 4, List.of(1.0f, 0.28f, 0.12f),
                List.of(0.0f, 0.35f, 1.0f), 5.0f, List.of(0xFF5A24, 0xFFD080, 0xFFFFFF), 4, 24, 12.0f,
                0.10f, RemnantKind.NEUTRON_STAR),
            entry("CORE_COLLAPSE_II_L", 12, 6, 5, 3, List.of(1.0f, 0.22f, 0.10f),
                List.of(0.0f, 0.5f, 1.0f), 5.5f, List.of(0xFF7430, 0xFFE0B0, 0xFFFFFF), 3, 20, 15.0f,
                0.16f, RemnantKind.NEUTRON_STAR),
            entry("STRIPPED_IB", 10, 5, 4, 3, List.of(1.0f, 0.18f, 0.06f),
                List.of(0.0f, 0.7f, 1.0f), 6.0f, List.of(0xA8D8FF, 0xFFFFFF), 2, 16, 18.0f, 0.24f,
                RemnantKind.NEUTRON_STAR),
            entry("STRIPPED_IC", 8, 4, 4, 2, List.of(1.0f, 0.14f, 0.03f),
                List.of(0.0f, 0.85f, 1.0f), 7.0f, List.of(0xC8E8FF, 0xFFFFFF), 1, 12, 22.0f, 0.30f,
                RemnantKind.BLACK_HOLE),
            entry("ELECTRON_CAPTURE", 14, 6, 5, 3, List.of(1.0f, 0.35f, 0.16f),
                List.of(0.0f, 0.5f, 0.9f), 2.4f, List.of(0xFFAA70, 0xFFFFFF), 2, 18, 10.0f, 0.42f,
                RemnantKind.NEUTRON_STAR),
            entry("DIRECT_COLLAPSE", 20, 12, 6, 4, List.of(1.0f, 0.72f, 0.16f, 0.04f),
                List.of(0.0f, 0.15f, 0.35f), 1.8f, List.of(0x402020, 0x806040), 1, 8, 8.0f, 0.18f,
                RemnantKind.BLACK_HOLE),
            entry("PULSATIONAL_PAIR", 24, 8, 6, 4, List.of(1.0f, 1.15f, 0.9f, 1.1f, 0.2f),
                List.of(0.0f, 0.5f, 1.0f), 4.2f, List.of(0xFF8A40, 0xFFE0A0), 5, 28, 20.0f, 0.34f,
                RemnantKind.BLACK_HOLE),
            entry("PAIR_INSTABILITY", 18, 10, 6, 4, List.of(1.0f, 0.16f, 0.0f),
                List.of(0.0f, 0.7f, 1.3f), 8.0f, List.of(0xFFD080, 0xFFFFFF), 6, 32, 26.0f, 0.28f,
                RemnantKind.NONE)
        );
    }

    private static Map.Entry<String, StellarEventProfile> entry(
        String id,
        int precursor,
        int collapse,
        int ejecta,
        int fade,
        List<Float> core,
        List<Float> ejectaCurve,
        float emission,
        List<Integer> palette,
        int shells,
        int rays,
        float rayLength,
        float asymmetry,
        RemnantKind remnant
    ) {
        return Map.entry(id, new StellarEventProfile(
            id, precursor, collapse, ejecta, fade, core, ejectaCurve, emission, palette,
            shells, rays, rayLength, asymmetry, remnant
        ));
    }
}
