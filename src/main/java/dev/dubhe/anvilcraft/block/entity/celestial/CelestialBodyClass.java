package dev.dubhe.anvilcraft.block.entity.celestial;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/// 从天体质径图可识别的所有天体类别枚举。
/// 每个常量存储其图表像素颜色（RGB）和分类标志。
public enum CelestialBodyClass {
    /// === 行星类（无需增幅器） ===
    LARGE_MOON(0x999966, false, false, false),
    ROCKY_NO_LIQUID(0x669933, false, false, false),
    ROCKY_LOW_LIQUID(0x339933, false, false, false),
    ROCKY_MED_LIQUID(0x339999, false, false, false),
    ROCKY_HIGH_LIQUID(0x33CCCC, false, false, false),
    ICE_GIANT(0x336699, false, false, false),
    GAS_GIANT(0x666699, false, false, false),
    BROWN_DWARF(0x330000, false, false, true),

    /// === 主序星 ===
    M_MAIN(0x660000, true, true, false),
    K_MAIN(0xCC6600, true, true, false),
    G_MAIN(0xCC9933, true, true, false),
    F_MAIN(0xCCCC66, true, true, false),
    A_MAIN(0xCCCCCC, true, true, false),
    B_MAIN(0x66CCCC, true, true, false),
    O_MAIN(0x0066CC, true, true, false),

    /// === 红巨星 ===
    M_GIANT(0x990000, true, false, true),
    K_GIANT(0xFF6600, true, false, true),
    G_GIANT(0xFFCC00, true, false, true),
    F_GIANT(0xFFFF66, true, false, true),

    /// === 蓝巨星 ===
    A_GIANT(0xCCFFCC, true, false, true),
    B_GIANT(0x66FFFF, true, false, true),
    O_GIANT(0x0099FF, true, false, true),

    /// === 红超巨星 ===
    M_SUPERGIANT(0xFF0000, true, false, true),
    K_SUPERGIANT(0xFF9900, true, false, true),
    G_SUPERGIANT(0xFFCC66, true, false, true),
    F_SUPERGIANT(0xFFFF99, true, false, true),

    /// === 蓝超巨星 ===
    A_SUPERGIANT(0xFFFFFF, true, false, true),
    B_SUPERGIANT(0x99FFFF, true, false, true),
    O_SUPERGIANT(0x33CCFF, true, false, true),

    /// === 白矮星 ===
    WHITE_DWARF(0x666666, true, false, true),

    /// === 恒星残骸（特殊渲染） ===
    NEUTRON_STAR(0x000001, true, false, false),
    BLACK_HOLE(0x000002, true, false, false);

    private final int rgb;
    /// 该天体是否需要增幅器模式（Lombok生成getter）。
    @Getter
    private final boolean stellar;
    /// 是否为主序星 —— 第二步使用age_temp图表，需要第三步。
    /// （Lombok生成getter）
    @Getter
    private final boolean mainSequence;
    private final boolean step2UsesSp;

    private static final Map<Integer, CelestialBodyClass> BY_RGB = new HashMap<>();

    static {
        for (CelestialBodyClass c : values()) {
            BY_RGB.put(c.rgb, c);
        }
    }

    CelestialBodyClass(int rgb, boolean stellar, boolean mainSequence, boolean step2UsesSp) {
        this.rgb = rgb;
        this.stellar = stellar;
        this.mainSequence = mainSequence;
        this.step2UsesSp = step2UsesSp;
    }

    public int rgb() {
        return rgb;
    }

    /// 第二步是否使用age_temp_sp图表而非age_temp。
    public boolean step2UsesSp() {
        return step2UsesSp;
    }

    /// 是否需要第三步（age_radius图表查找）。
    public boolean needsStep3() {
        return stellar || this == BROWN_DWARF;
    }

    /// 是否为极端致密天体（黑洞或中子星），需要奇异水晶才能存储。
    public boolean isExtreme() {
        return this == BLACK_HOLE || this == NEUTRON_STAR;
    }

    /// 是否为行星类天体（包括褐矮星，不包括大卫星）。
    public boolean isPlanetary() {
        return !stellar;
    }

    /// 是否为岩石行星类型（用于第二步特殊匹配）。
    public boolean isRockyPlanet() {
        return this == ROCKY_NO_LIQUID || this == ROCKY_LOW_LIQUID
               || this == ROCKY_MED_LIQUID || this == ROCKY_HIGH_LIQUID;
    }

    /// 该天体类别在第二步中匹配的颜色值。岩石行星统一使用ROCKY_LOW_LIQUID的RGB。
    public int step2MatchRgb() {
        return isRockyPlanet() ? ROCKY_LOW_LIQUID.rgb : rgb;
    }

    @Nullable
    public static CelestialBodyClass fromRgb(int rgb) {
        return BY_RGB.get(rgb);
    }
}
