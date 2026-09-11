package dev.dubhe.anvilcraft.block.entity.celestial;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.util.StringRepresentable;

import javax.annotation.Nullable;

/**
 * 恒星内部演化阶段。
 *
 * <p>该枚举描述的是演化轨道，而不是 {@link CelestialBodyClass} 的表面光谱分类。
 * 字符串 ID 会写入存档和网络数据，不能依赖枚举序号。</p>
 */
public enum StellarEvolutionPhase implements StringRepresentable {
    FULLY_CONVECTIVE_MAIN_SEQUENCE("fully_convective_main_sequence"),
    RADIATIVE_CORE_MAIN_SEQUENCE("radiative_core_main_sequence"),
    PRE_WHITE_DWARF("pre_white_dwarf"),
    HOOK("hook"),
    HERTZSPRUNG_GAP("hertzsprung_gap"),
    SECOND_HERTZSPRUNG_GAP("second_hertzsprung_gap"),
    NON_EXPLOSIVE_CONTRACTION("non_explosive_contraction"),
    SHELL_HYDROGEN_BURNING("shell_hydrogen_burning"),
    PLANETARY_NEBULA("planetary_nebula"),
    NAKED_HELIUM_STAR("naked_helium_star"),
    BREATHING_PULSES("breathing_pulses"),
    AGB_MANQUE("agb_manque"),
    SECONDARY_RED_CLUMP("secondary_red_clump"),
    NONDEGENERATE_CORE_HELIUM_BURNING("nondegenerate_core_helium_burning"),
    HELIUM_MAIN_SEQUENCE("helium_main_sequence"),
    EARLY_AGB("early_agb"),
    THERMAL_PULSING_AGB("thermal_pulsing_agb"),
    EARLY_SUPER_AGB("early_super_agb"),
    THERMAL_PULSING_AGB_SUPERWIND("thermal_pulsing_agb_superwind"),
    RED_SUPERGIANT_SUPERWIND("red_supergiant_superwind"),
    SUPERWIND_CONTRACTION("superwind_contraction"),
    SUPERWIND("superwind"),
    CONTRACTION("contraction"),
    ENVELOPE_STRIPPING("envelope_stripping"),
    BARE_CORE_WR("bare_core_wr"),
    SUPERNOVA("supernova"),
    DIRECT_COLLAPSE("direct_collapse"),
    PPISN("ppisn"),
    PISN("pisn"),
    BROWN_DWARF_COOLING("brown_dwarf_cooling"),
    MAIN_SEQUENCE("main_sequence"),
    SUBGIANT("subgiant"),
    RGB("rgb"),
    HELIUM_FLASH("helium_flash"),
    RED_CLUMP("red_clump"),
    BLUE_LOOP("blue_loop"),
    RED_SUPERGIANT("red_supergiant"),
    WOLF_RAYET("wolf_rayet");

    /** 稳定的资源/存档 ID。 */
    private final String id;

    /** 用于数据文件和网络协议的阶段 codec。 */
    public static final Codec<StellarEvolutionPhase> CODEC = Codec.STRING.comapFlatMap(
        id -> {
            StellarEvolutionPhase phase = fromId(id);
            return phase == null
                ? DataResult.error(() -> "未知恒星演化阶段: " + id)
                : DataResult.success(phase);
        },
        StellarEvolutionPhase::getSerializedName
    );

    StellarEvolutionPhase(String id) {
        this.id = id;
    }

    @Override
    public String getSerializedName() {
        return this.id;
    }

    public String id() {
        return this.id;
    }

    /** 根据稳定 ID 查找阶段，未知值返回 null 以便调用方执行兼容回退。 */
    @Nullable
    public static StellarEvolutionPhase fromId(String id) {
        if (id == null) return null;
        for (StellarEvolutionPhase phase : values()) {
            if (phase.id.equalsIgnoreCase(id) || phase.name().equalsIgnoreCase(id)) return phase;
        }
        return null;
    }

    /** 根据稳定 ID 查找阶段，未知值使用主序作为安全默认值。 */
    public static StellarEvolutionPhase fromIdOrDefault(String id) {
        StellarEvolutionPhase phase = fromId(id);
        return phase == null ? MAIN_SEQUENCE : phase;
    }

    /** 巨构资格判断使用的粗阶段：主序、膨胀、终局事件。 */
    public int acceleratorStage() {
        return switch (this) {
            case MAIN_SEQUENCE, FULLY_CONVECTIVE_MAIN_SEQUENCE, RADIATIVE_CORE_MAIN_SEQUENCE -> 1;
            case SUPERNOVA, DIRECT_COLLAPSE, PPISN, PISN -> 3;
            default -> 2;
        };
    }
}
