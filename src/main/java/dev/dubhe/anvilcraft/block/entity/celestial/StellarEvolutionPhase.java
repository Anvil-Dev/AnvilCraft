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
    MAIN_SEQUENCE("main_sequence"),
    SUBGIANT("subgiant"),
    RGB("rgb"),
    HELIUM_FLASH("helium_flash"),
    HORIZONTAL_BRANCH("horizontal_branch"),
    RED_CLUMP("red_clump"),
    BLUE_LOOP("blue_loop"),
    AGB("agb"),
    POST_AGB("post_agb"),
    PPN("ppn"),
    BLUE_SUPERGIANT("blue_supergiant"),
    RED_SUPERGIANT("red_supergiant"),
    LBV("lbv"),
    WOLF_RAYET("wolf_rayet"),
    PRE_COLLAPSE("pre_collapse"),
    EVENT_PRELUDE("event_prelude"),
    EVENT_COLLAPSE("event_collapse"),
    EVENT_EJECTA("event_ejecta"),
    REMNANT_SETTLE("remnant_settle"),
    WHITE_DWARF_COOLING("white_dwarf_cooling");

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

    /** 映射到旧版 1--4 粗阶段，仅供旧 UI 和玩法判断使用。 */
    public int legacyStage() {
        return switch (this) {
            case MAIN_SEQUENCE -> 1;
            case EVENT_PRELUDE, EVENT_COLLAPSE, EVENT_EJECTA, REMNANT_SETTLE -> 3;
            case WHITE_DWARF_COOLING -> 4;
            default -> 2;
        };
    }

    /** 将旧版粗阶段映射为最接近的新阶段，仅用于旧存档 UI 回退。 */
    public static StellarEvolutionPhase fromLegacyStage(int stage) {
        return switch (Math.clamp(stage, 1, 4)) {
            case 1 -> MAIN_SEQUENCE;
            case 2 -> RGB;
            case 3 -> EVENT_COLLAPSE;
            default -> WHITE_DWARF_COOLING;
        };
    }

    /** 是否属于爆发/坍缩视觉窗口。 */
    public boolean isEventPhase() {
        return this == EVENT_PRELUDE || this == EVENT_COLLAPSE || this == EVENT_EJECTA
            || this == REMNANT_SETTLE;
    }

    /** 是否是非爆发的残骸冷却阶段。 */
    public boolean isRemnantPhase() {
        return this == WHITE_DWARF_COOLING || this == REMNANT_SETTLE;
    }

    /** 用于资源校验的近似阶段顺序；允许轨道跳过未采样阶段。 */
    public int order() {
        return switch (this) {
            case MAIN_SEQUENCE -> 10;
            case SUBGIANT -> 20;
            case RGB -> 30;
            case HELIUM_FLASH -> 40;
            case RED_CLUMP, HORIZONTAL_BRANCH, BLUE_LOOP -> 50;
            case RED_SUPERGIANT, BLUE_SUPERGIANT -> 55;
            case AGB -> 60;
            case POST_AGB -> 70;
            case PPN -> 75;
            case LBV -> 80;
            case WOLF_RAYET -> 85;
            case PRE_COLLAPSE -> 90;
            case EVENT_PRELUDE -> 100;
            case EVENT_COLLAPSE -> 110;
            case EVENT_EJECTA -> 120;
            case REMNANT_SETTLE, WHITE_DWARF_COOLING -> 130;
        };
    }
}
