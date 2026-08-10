package dev.dubhe.anvilcraft.block.entity.celestial;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * 质量-半径图中可识别的全部天体类别。
 * 每个枚举值保存对应像素颜色（RGB）和分类标记。
 */
public enum CelestialBodyClass {
    // === 行星类天体，无需增幅器 ===
    LARGE_MOON(0x999966, false, false, false),
    ROCKY_NO_LIQUID(0x669933, false, false, false),
    ROCKY_LOW_LIQUID(0x339933, false, false, false),
    ROCKY_MED_LIQUID(0x339999, false, false, false),
    ROCKY_HIGH_LIQUID(0x33CCCC, false, false, false),
    ICE_GIANT(0x336699, false, false, false),
    GAS_GIANT(0x666699, false, false, false),
    BROWN_DWARF(0x330000, false, false, true),

    // === 主序星 ===
    M_MAIN(0x660000, true, true, false),
    K_MAIN(0xCC6600, true, true, false),
    G_MAIN(0xCC9933, true, true, false),
    F_MAIN(0xCCCC66, true, true, false),
    A_MAIN(0xCCCCCC, true, true, false),
    B_MAIN(0x66CCCC, true, true, false),
    O_MAIN(0x0066CC, true, true, false),

    // === 红巨星 ===
    M_GIANT(0x990000, true, false, true),
    K_GIANT(0xFF6600, true, false, true),
    G_GIANT(0xFFCC00, true, false, true),
    F_GIANT(0xFFFF66, true, false, true),

    // === 蓝巨星 ===
    A_GIANT(0xCCFFCC, true, false, true),
    B_GIANT(0x66FFFF, true, false, true),
    O_GIANT(0x0099FF, true, false, true),

    // === 红超巨星 ===
    M_SUPERGIANT(0xFF0000, true, false, true),
    K_SUPERGIANT(0xFF9900, true, false, true),
    G_SUPERGIANT(0xFFCC66, true, false, true),
    F_SUPERGIANT(0xFFFF99, true, false, true),

    // === 蓝超巨星 ===
    A_SUPERGIANT(0xFFFFFF, true, false, true),
    B_SUPERGIANT(0x99FFFF, true, false, true),
    O_SUPERGIANT(0x33CCFF, true, false, true),

    // === 白矮星 ===
    WHITE_DWARF(0x666666, true, false, true),

    // === 恒星残骸，使用特殊渲染 ===
    NEUTRON_STAR(0x000001, true, false, false),
    BLACK_HOLE(0x000002, true, false, false);

    private final int rgb;
    /** 是否要求锻星砧处于增幅模式。 */
    @Getter
    private final boolean stellar;
    /** 是否为第二步使用 age_temp 且需要第三步匹配的主序星。 */
    @Getter
    private final boolean mainSequence;
    private final boolean step2UsesSp;

    private static final Map<Integer, CelestialBodyClass> BY_RGB = new HashMap<>();

    static {
        for (CelestialBodyClass c : CelestialBodyClass.values()) {
            CelestialBodyClass.BY_RGB.put(c.rgb, c);
        }
    }

    CelestialBodyClass(int rgb, boolean stellar, boolean mainSequence, boolean step2UsesSp) {
        this.rgb = rgb;
        this.stellar = stellar;
        this.mainSequence = mainSequence;
        this.step2UsesSp = step2UsesSp;
    }

    public int rgb() {
        return this.rgb;
    }

    /** 第二步是否使用 age_temp_sp 而非 age_temp。 */
    public boolean step2UsesSp() {
        return this.step2UsesSp;
    }

    /** 是否需要第三步年龄-半径图匹配。 */
    public boolean needsStep3() {
        return this.stellar || this == CelestialBodyClass.BROWN_DWARF;
    }

    /** 是否为只能写入奇点晶体的极端致密天体（黑洞或中子星）。 */
    public boolean isExtreme() {
        return this == CelestialBodyClass.BLACK_HOLE || this == CelestialBodyClass.NEUTRON_STAR;
    }

    /** 是否为行星类天体，包含褐矮星但不包含大型卫星。 */
    public boolean isPlanetary() {
        return !this.stellar;
    }

    /** 是否为第二步需要特殊处理的岩石行星类别。 */
    public boolean isRockyPlanet() {
        return this == CelestialBodyClass.ROCKY_NO_LIQUID || this == CelestialBodyClass.ROCKY_LOW_LIQUID
               || this == CelestialBodyClass.ROCKY_MED_LIQUID || this == CelestialBodyClass.ROCKY_HIGH_LIQUID;
    }

    /** 获取该类别第二步接受的颜色；全部岩石行星统一使用 ROCKY_LOW_LIQUID 的 RGB。 */
    public int step2MatchRgb() {
        return this.isRockyPlanet() ? CelestialBodyClass.ROCKY_LOW_LIQUID.rgb : this.rgb;
    }

    @Nullable
    public static CelestialBodyClass fromRgb(int rgb) {
        return CelestialBodyClass.BY_RGB.get(rgb);
    }
}
