package dev.dubhe.anvilcraft.block.entity.celestial;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * 恒星轨道和事件 profile 的统一索引。
 *
 * <p>正式资源可以通过 {@link #replaceTracks(List)} 注入；内置轨道是资源缺失时的
 * 确定性回退，并覆盖现有全部恒星表面分类。数值是经过压缩的近似控制点，不是
 * 天体物理求解器输出。</p>
 */
public final class StellarTrackLibrary {
    /** 0.8 M_sun K 型回放/截图验收轨道的稳定 ID。 */
    public static final String GOLDEN_K_TRACK_ID = "k_main_0_8_golden";
    /** 离线轨道采样使用的物理质量锚点（太阳质量，非游戏坐标）。 */
    public static final List<Float> PHYSICAL_MASS_ANCHORS = List.of(
        0.1f, 0.3f, 0.5f, 0.8f, 1.0f, 1.5f, 2.0f, 5.0f, 8.0f, 9.0f,
        12.0f, 15.0f, 18.0f, 20.0f, 25.0f, 40.0f, 60.0f, 100.0f
    );
    /** 质量族相对时间压缩指数，范围按计划保持在约 -2 到 -3。 */
    public static final float MASS_TIME_EXPONENT = -2.5f;
    /** 轨道矩阵明确覆盖的 21 种恒星表面分类。 */
    public static final List<CelestialBodyClass> EVOLVABLE_SURFACE_CLASSES = List.of(
        CelestialBodyClass.M_MAIN,
        CelestialBodyClass.K_MAIN,
        CelestialBodyClass.G_MAIN,
        CelestialBodyClass.F_MAIN,
        CelestialBodyClass.A_MAIN,
        CelestialBodyClass.B_MAIN,
        CelestialBodyClass.O_MAIN,
        CelestialBodyClass.M_GIANT,
        CelestialBodyClass.K_GIANT,
        CelestialBodyClass.G_GIANT,
        CelestialBodyClass.F_GIANT,
        CelestialBodyClass.A_GIANT,
        CelestialBodyClass.B_GIANT,
        CelestialBodyClass.O_GIANT,
        CelestialBodyClass.M_SUPERGIANT,
        CelestialBodyClass.K_SUPERGIANT,
        CelestialBodyClass.G_SUPERGIANT,
        CelestialBodyClass.F_SUPERGIANT,
        CelestialBodyClass.A_SUPERGIANT,
        CelestialBodyClass.B_SUPERGIANT,
        CelestialBodyClass.O_SUPERGIANT
    );
    private static final List<StellarMassBand> DEFAULT_MASS_BANDS = List.of(
        new StellarMassBand("very_low", 1, 8, 0.08f, 0.50f, "very_low_mass", ""),
        new StellarMassBand("low", 9, 30, 0.50f, 1.00f, "low_mass", ""),
        new StellarMassBand("intermediate", 31, 46, 1.00f, 2.00f, "intermediate_mass", ""),
        new StellarMassBand("super_agb", 47, 54, 8.00f, 10.00f, "super_agb", "ELECTRON_CAPTURE"),
        new StellarMassBand("high_hydrogen", 55, 58, 10.00f, 18.00f, "high_hydrogen", "CORE_COLLAPSE_II_P"),
        new StellarMassBand("high_stripped", 59, 62, 18.00f, 40.00f, "stripped", "DIRECT_COLLAPSE"),
        new StellarMassBand("extreme", 63, 64, 40.00f, 100.00f, "extreme", "DIRECT_COLLAPSE")
    );

    private static volatile Map<String, StellarEventProfile> eventProfiles =
        Collections.unmodifiableMap(new LinkedHashMap<>(StellarEventProfile.defaults()));
    public static final com.mojang.serialization.Codec<List<StellarMassBand>> MASS_BANDS_CODEC =
        StellarMassBand.CODEC.listOf();
    public static final com.mojang.serialization.Codec<List<StellarTrack>> TRACKS_CODEC = StellarTrack.CODEC.listOf();
    private static volatile List<StellarMassBand> massBands = DEFAULT_MASS_BANDS;
    private static final Map<String, StellarTrack> DEFAULT_TRACKS = buildDefaultTracks();
    private static volatile Map<String, StellarTrack> tracks = DEFAULT_TRACKS;

    private StellarTrackLibrary() {
    }

    /** 返回当前资源版本的所有质量带。 */
    public static List<StellarMassBand> massBands() {
        return massBands;
    }

    /** 返回当前缓存的全部轨道。 */
    public static List<StellarTrack> tracks() {
        return List.copyOf(tracks.values());
    }

    public static List<StellarTrack> defaultTracks() {
        return List.copyOf(DEFAULT_TRACKS.values());
    }

    /** 返回内置事件 profile 的只读视图。 */
    public static Map<String, StellarEventProfile> eventProfiles() {
        return eventProfiles;
    }

    /** 按稳定 ID 查找轨道。 */
    @Nullable
    public static StellarTrack track(String trackId) {
        if (trackId == null || trackId.isBlank()) return null;
        StellarTrack direct = tracks.get(trackId);
        if (direct == null) direct = tracks.get(trackId.toLowerCase(java.util.Locale.ROOT));
        if (direct == null) direct = DEFAULT_TRACKS.get(trackId.toLowerCase(java.util.Locale.ROOT));
        if (direct != null) return direct;
        String normalizedId = trackId.toLowerCase(java.util.Locale.ROOT);
        if (normalizedId.endsWith("_ii_l")) {
            String baseId = normalizedId.substring(0, normalizedId.length() - "_ii_l".length());
            StellarTrack base = lookupTrack(baseId);
            return base == null ? null : base.withEventProfile("CORE_COLLAPSE_II_L", "CORE_COLLAPSE_II_L")
                .withIdAndTerminalProfile(normalizedId, "CORE_COLLAPSE_II_L");
        }
        if (normalizedId.endsWith("_stripped_ic")) {
            String baseId = normalizedId.substring(0, normalizedId.length() - "_stripped_ic".length()) + "_stripped";
            StellarTrack base = lookupTrack(baseId);
            return base == null ? null : base.withEventProfile("STRIPPED_IC", "STRIPPED_IC")
                .withIdAndTerminalProfile(normalizedId, "STRIPPED_IC");
        }
        if (normalizedId.endsWith("_super_agb_wd")) {
            String baseId = normalizedId.substring(0, normalizedId.length() - "_super_agb_wd".length()) + "_super_agb";
            StellarTrack base = lookupTrack(baseId);
            if (base == null) return null;
            List<PhaseNode> nodes = base.phaseNodes().stream()
                .map(node -> node.hasEventProfile() && node.eventProfileId().equals("ELECTRON_CAPTURE")
                    ? new PhaseNode(node.phaseId(), node.durationWeight(), node.radius(), node.temperature(),
                        node.luminosity(), node.envelopeFraction(), node.pulsationAmplitude(),
                        node.pulsationFrequency(), node.surfaceStyle())
                    : node)
                .toList();
            return new StellarTrack(normalizedId, base.massBand(), base.surfaceClassFamily(), base.variantRules(), nodes, "");
        }
        return null;
    }

    /** 语义更明确的轨道选择别名，供外部扩展和测试使用。 */
    @Nullable
    public static StellarTrack selectTrack(int initialMass, CelestialBodyClass surfaceClass, long bodySeed) {
        return select(initialMass, surfaceClass, bodySeed);
    }

    @Nullable
    public static StellarTrack getTrack(String trackId) {
        return track(trackId);
    }

    public static StellarTrack goldenKTrack() {
        return Objects.requireNonNull(track(GOLDEN_K_TRACK_ID));
    }

    /** 按 ID 查找事件；未知 profile 使用直接坍缩作为保守回退。 */
    public static StellarEventProfile eventProfile(String profileId) {
        StellarEventProfile profile = eventProfiles.get(profileId);
        if (profile == null && profileId != null) {
            for (Map.Entry<String, StellarEventProfile> entry : eventProfiles.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(profileId)) {
                    profile = entry.getValue();
                    break;
                }
            }
        }
        if (profile != null) return profile;
        profile = eventProfiles.get("DIRECT_COLLAPSE");
        return profile == null ? StellarEventProfile.defaults().get("DIRECT_COLLAPSE") : profile;
    }

    public static StellarEventProfile byEventId(String profileId) {
        return eventProfile(profileId);
    }

    /** 根据游戏质量砧子值查找质量带。 */
    public static StellarMassBand massBand(int initialMass) {
        int mass = Math.clamp(initialMass, 1, 64);
        for (StellarMassBand band : massBands) {
            if (band.contains(mass)) return band;
        }
        return massBands.get(massBands.size() - 1);
    }

    /**
     * 按计划中的优先级选择轨道：质量、表面分类、可复现变体。
     * 相同输入在服务端和客户端必定得到相同 ID。
     */
    @Nullable
    public static StellarTrack select(int initialMass, CelestialBodyClass surfaceClass, long bodySeed) {
        return select(initialMass, surfaceClass, false, bodySeed);
    }

    @Nullable
    public static StellarTrack select(
        int initialMass,
        CelestialBodyClass surfaceClass,
        boolean specialRedDwarf,
        long bodySeed
    ) {
        if (surfaceClass == CelestialBodyClass.BROWN_DWARF) return null;
        if (isRemnant(surfaceClass)) {
            String id = "remnant_" + surfaceClass.name().toLowerCase(java.util.Locale.ROOT);
            return lookupTrack(id);
        }
        String family = massBand(initialMass).trackFamily();
        if (family.equals("high_stripped")) family = "stripped";
        if ((isCoolGiant(surfaceClass) || isCoolMain(surfaceClass))
            && !family.equals("low_mass") && !family.equals("intermediate_mass")) {
            // 保留超 AGB/高质量质量带的终局事件；只有质量带本身落在
            // 低中质量族时才回退到对应的冷星轨道。
            family = initialMass <= 8 ? "very_low_mass" : initialMass >= 47 ? family : "low_mass";
        }
        if (specialRedDwarf || (surfaceClass == CelestialBodyClass.M_MAIN && initialMass <= 8)) {
            family = "very_low_mass";
        } else if (family.equals("high_stripped") || family.equals("stripped")) {
            family = chooseStrippedVariant(bodySeed) ? "stripped" : "high_hydrogen";
        }
        String id = surfaceClass.name().toLowerCase(java.util.Locale.ROOT) + "_" + family;
        StellarTrack selected = lookupTrack(id);
        if (selected != null) return applyVariant(adaptInitialSurfaceTemperature(selected, surfaceClass), family, bodySeed);
        selected = lookupTrack(surfaceClass.name().toLowerCase(java.util.Locale.ROOT) + "_low_mass");
        if (selected != null) return applyVariant(adaptInitialSurfaceTemperature(selected, surfaceClass), "low_mass", bodySeed);
        StellarTrack fallback = lookupTrack("m_main_low_mass");
        return fallback == null ? null : adaptInitialSurfaceTemperature(fallback, surfaceClass);
    }

    /**
     * 将共享质量模板的首个可见节点校准到发现时的光谱分类。
     * 质量带仍决定半径轨道和终局，只有视觉温度被调整，避免 G/K 主序星
     * 因为落在中等质量模板而在演化第一帧突然变成白蓝色。
     */
    private static StellarTrack adaptInitialSurfaceTemperature(
        StellarTrack track,
        CelestialBodyClass surfaceClass
    ) {
        int startIndex = startingPhaseIndex(track, surfaceClass);
        if (startIndex < 0 || startIndex >= track.phaseNodes().size()) return track;
        PhaseNode source = track.phaseNodes().get(startIndex);
        float target = StellarVisualState.temperatureForSurfaceClass(surfaceClass, 32);
        if (!Float.isFinite(target) || Math.abs(source.temperature() - target) < 1.0f) return track;
        List<PhaseNode> nodes = new ArrayList<>(track.phaseNodes());
        nodes.set(startIndex, new PhaseNode(
            source.phaseId(),
            source.durationWeight(),
            source.radius(),
            target,
            source.luminosity(),
            source.envelopeFraction(),
            source.pulsationAmplitude(),
            source.pulsationFrequency(),
            source.surfaceStyle(),
            source.eventProfileId()
        ));
        return new StellarTrack(
            track.trackId(),
            track.massBand(),
            track.surfaceClassFamily(),
            track.variantRules(),
            nodes,
            track.terminalProfile()
        );
    }

    /** 根据已持久化的表面分类重建客户端与服务端一致的首阶段视觉节点。 */
    public static StellarTrack adaptForSurfaceClass(StellarTrack track, CelestialBodyClass surfaceClass) {
        return adaptInitialSurfaceTemperature(track, surfaceClass);
    }

    private static StellarTrack applyVariant(StellarTrack track, String family, long bodySeed) {
        int variant = variant(bodySeed, 3) & 3;
        String suffix = "_" + family;
        String classId = track.trackId().endsWith(suffix)
            ? track.trackId().substring(0, track.trackId().length() - suffix.length())
            : track.surfaceClassFamily().toLowerCase(java.util.Locale.ROOT);
        if (family.equals("high_hydrogen") && (variant & 1) != 0) {
            return track.withEventProfile(
                "CORE_COLLAPSE_II_L",
                "CORE_COLLAPSE_II_L"
            ).withIdAndTerminalProfile(classId + "_" + family + "_ii_l", "CORE_COLLAPSE_II_L");
        }
        if (family.equals("stripped") && (variant & 1) != 0) {
            return track.withEventProfile("STRIPPED_IC", "STRIPPED_IC")
                .withIdAndTerminalProfile(classId + "_" + family + "_ic", "STRIPPED_IC");
        }
        if (family.equals("super_agb")) {
            // 超 AGB 质量带（47--54）的终局玩法结果是白矮星，因此必须走氧氖白矮星分支：
            // 剥掉电子俘获事件，否则会出现"爆发一次却生成白矮星"的矛盾表现。
            // 电子俘获超新星要等质量带能给出中子星结果时才重新启用。
            List<PhaseNode> nodes = track.phaseNodes().stream()
                .map(node -> node.hasEventProfile() && node.eventProfileId().equals("ELECTRON_CAPTURE")
                    ? new PhaseNode(node.phaseId(), node.durationWeight(), node.radius(), node.temperature(),
                        node.luminosity(), node.envelopeFraction(), node.pulsationAmplitude(),
                        node.pulsationFrequency(), node.surfaceStyle())
                    : node)
                .toList();
            return new StellarTrack(classId + "_super_agb_wd", track.massBand(), track.surfaceClassFamily(),
                track.variantRules(), nodes, "");
        }
        return track;
    }

    /** 根据初始表面分类给出合法的起始阶段，不把已演化恒星重置到主序。 */
    public static int startingPhaseIndex(StellarTrack track, CelestialBodyClass surfaceClass) {
        if (isRemnant(surfaceClass)) return Math.max(0, track.phaseNodes().size() - 1);
        String name = surfaceClass.name();
        if (name.endsWith("_GIANT")) {
            boolean blue = surfaceClass.name().startsWith("A_") || surfaceClass.name().startsWith("B_")
                || surfaceClass.name().startsWith("O_");
            return firstAvailable(track, blue
                ? List.of(StellarEvolutionPhase.BLUE_SUPERGIANT, StellarEvolutionPhase.BLUE_LOOP,
                    StellarEvolutionPhase.RGB, StellarEvolutionPhase.RED_SUPERGIANT, StellarEvolutionPhase.AGB)
                : List.of(StellarEvolutionPhase.RGB, StellarEvolutionPhase.RED_SUPERGIANT,
                    StellarEvolutionPhase.AGB, StellarEvolutionPhase.SUBGIANT));
        }
        if (name.endsWith("_SUPERGIANT")) {
            boolean blue = surfaceClass.name().startsWith("A_") || surfaceClass.name().startsWith("B_")
                || surfaceClass.name().startsWith("O_");
            return firstAvailable(track, blue
                ? List.of(StellarEvolutionPhase.BLUE_SUPERGIANT, StellarEvolutionPhase.BLUE_LOOP,
                    StellarEvolutionPhase.WOLF_RAYET, StellarEvolutionPhase.LBV, StellarEvolutionPhase.PRE_COLLAPSE)
                : List.of(StellarEvolutionPhase.RED_SUPERGIANT, StellarEvolutionPhase.RGB,
                    StellarEvolutionPhase.AGB, StellarEvolutionPhase.PRE_COLLAPSE));
        }
        return indexOf(track, StellarEvolutionPhase.MAIN_SEQUENCE);
    }

    private static int firstAvailable(StellarTrack track, List<StellarEvolutionPhase> phases) {
        for (StellarEvolutionPhase phase : phases) {
            for (int index = 0; index < track.phaseNodes().size(); index++) {
                if (track.phaseNodes().get(index).phaseId() == phase) return index;
            }
        }
        return 0;
    }

    private static int indexOf(StellarTrack track, StellarEvolutionPhase phase) {
        for (int i = 0; i < track.phaseNodes().size(); i++) {
            if (track.phaseNodes().get(i).phaseId() == phase) return i;
        }
        return 0;
    }

    public static boolean isRemnant(CelestialBodyClass bodyClass) {
        return bodyClass == CelestialBodyClass.WHITE_DWARF
            || bodyClass == CelestialBodyClass.NEUTRON_STAR
            || bodyClass == CelestialBodyClass.BLACK_HOLE;
    }

    /** 返回某个发现分类允许经过的阶段集合，供资源校验和 UI 使用。 */
    public static List<StellarEvolutionPhase> allowedPhases(CelestialBodyClass bodyClass) {
        if (bodyClass == CelestialBodyClass.WHITE_DWARF) {
            return List.of(StellarEvolutionPhase.WHITE_DWARF_COOLING);
        }
        if (bodyClass == CelestialBodyClass.NEUTRON_STAR || bodyClass == CelestialBodyClass.BLACK_HOLE) {
            return List.of(StellarEvolutionPhase.REMNANT_SETTLE);
        }
        if (bodyClass.name().startsWith("O_")
            || ((bodyClass.name().startsWith("A_") || bodyClass.name().startsWith("B_"))
                && bodyClass.name().endsWith("_SUPERGIANT"))) {
            return List.of(
                StellarEvolutionPhase.MAIN_SEQUENCE,
                StellarEvolutionPhase.BLUE_SUPERGIANT,
                StellarEvolutionPhase.RED_SUPERGIANT,
                StellarEvolutionPhase.LBV,
                StellarEvolutionPhase.WOLF_RAYET,
                StellarEvolutionPhase.PRE_COLLAPSE,
                StellarEvolutionPhase.EVENT_PRELUDE,
                StellarEvolutionPhase.EVENT_COLLAPSE,
                StellarEvolutionPhase.EVENT_EJECTA,
                StellarEvolutionPhase.REMNANT_SETTLE
            );
        }
        if (bodyClass.name().startsWith("A_") || bodyClass.name().startsWith("B_")) {
            return List.of(
                StellarEvolutionPhase.MAIN_SEQUENCE,
                StellarEvolutionPhase.SUBGIANT,
                StellarEvolutionPhase.RGB,
                StellarEvolutionPhase.BLUE_LOOP,
                StellarEvolutionPhase.AGB,
                StellarEvolutionPhase.POST_AGB,
                StellarEvolutionPhase.PPN,
                StellarEvolutionPhase.WHITE_DWARF_COOLING
            );
        }
        if (isCoolGiant(bodyClass)) {
            return List.of(
                StellarEvolutionPhase.RGB,
                StellarEvolutionPhase.HELIUM_FLASH,
                StellarEvolutionPhase.HORIZONTAL_BRANCH,
                StellarEvolutionPhase.RED_CLUMP,
                StellarEvolutionPhase.BLUE_LOOP,
                StellarEvolutionPhase.AGB,
                StellarEvolutionPhase.POST_AGB,
                StellarEvolutionPhase.PPN,
                StellarEvolutionPhase.WHITE_DWARF_COOLING
            );
        }
        if (isCoolMain(bodyClass)) {
            return List.of(
                StellarEvolutionPhase.MAIN_SEQUENCE,
                StellarEvolutionPhase.SUBGIANT,
                StellarEvolutionPhase.RGB,
                StellarEvolutionPhase.HELIUM_FLASH,
                StellarEvolutionPhase.HORIZONTAL_BRANCH,
                StellarEvolutionPhase.RED_CLUMP,
                StellarEvolutionPhase.BLUE_LOOP,
                StellarEvolutionPhase.AGB,
                StellarEvolutionPhase.POST_AGB,
                StellarEvolutionPhase.PPN,
                StellarEvolutionPhase.WHITE_DWARF_COOLING
            );
        }
        return List.of(
            StellarEvolutionPhase.MAIN_SEQUENCE,
            StellarEvolutionPhase.SUBGIANT,
            StellarEvolutionPhase.RGB,
            StellarEvolutionPhase.RED_SUPERGIANT,
            StellarEvolutionPhase.HELIUM_FLASH,
            StellarEvolutionPhase.HORIZONTAL_BRANCH,
            StellarEvolutionPhase.RED_CLUMP,
            StellarEvolutionPhase.BLUE_LOOP,
            StellarEvolutionPhase.AGB,
            StellarEvolutionPhase.POST_AGB,
            StellarEvolutionPhase.PPN,
            StellarEvolutionPhase.WHITE_DWARF_COOLING
        );
    }

    public static boolean isPhaseAllowed(CelestialBodyClass bodyClass, StellarEvolutionPhase phase) {
        return allowedPhases(bodyClass).contains(phase);
    }

    /** 用确定性哈希派生金属丰度、旋转和包层变体。 */
    public static int variant(long bodySeed, int salt) {
        long value = bodySeed ^ (0x9E3779B97F4A7C15L * (salt + 1L));
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return (int) value;
    }

    public static int metallicityVariant(long bodySeed) {
        return variant(bodySeed, 0) & 3;
    }

    public static int rotationVariant(long bodySeed) {
        return variant(bodySeed, 1) & 3;
    }

    public static int binaryVariant(long bodySeed) {
        return variant(bodySeed, 2) & 3;
    }

    private static boolean chooseStrippedVariant(long seed) {
        return (variant(seed, 2) & 1) == 0;
    }

    private static boolean isCoolGiant(CelestialBodyClass surfaceClass) {
        return surfaceClass == CelestialBodyClass.M_GIANT
            || surfaceClass == CelestialBodyClass.K_GIANT
            || surfaceClass == CelestialBodyClass.G_GIANT
            || surfaceClass == CelestialBodyClass.F_GIANT;
    }

    private static boolean isCoolMain(CelestialBodyClass surfaceClass) {
        return surfaceClass == CelestialBodyClass.M_MAIN
            || surfaceClass == CelestialBodyClass.K_MAIN
            || surfaceClass == CelestialBodyClass.G_MAIN
            || surfaceClass == CelestialBodyClass.F_MAIN;
    }

    @Nullable
    private static StellarTrack lookupTrack(String id) {
        StellarTrack track = tracks.get(id);
        return track == null ? DEFAULT_TRACKS.get(id) : track;
    }

    /** 资源重载入口；非法轨道不会替换当前缓存。 */
    public static void replaceTracks(List<StellarTrack> loadedTracks) {
        if (loadedTracks == null || loadedTracks.isEmpty()) return;
        Map<String, StellarTrack> next = new LinkedHashMap<>();
        for (StellarTrack track : loadedTracks) {
            track.validate();
            for (PhaseNode node : track.phaseNodes()) {
                if (node.hasEventProfile() && !containsEventProfile(node.eventProfileId())) {
                    throw new IllegalArgumentException("轨道引用未知事件 profile: " + node.eventProfileId());
                }
            }
            if (next.put(track.trackId(), track) != null) {
                throw new IllegalArgumentException("重复恒星轨道 ID: " + track.trackId());
            }
        }
        tracks = Collections.unmodifiableMap(next);
    }

    /** 校验当前轨道矩阵，返回首个错误信息；空字符串表示通过。 */
    public static String validateTracks() {
        if (massBands.isEmpty()) return "没有质量带";
        int expectedMass = 1;
        for (StellarMassBand band : massBands) {
            if (band.minGameMass() > expectedMass) return "质量带存在空洞: " + expectedMass;
            if (band.minGameMass() < expectedMass && expectedMass != 1) {
                return "质量带存在重叠: " + band.id();
            }
            expectedMass = Math.max(expectedMass, band.maxGameMass() + 1);
        }
        if (expectedMass <= 64) return "质量带未覆盖游戏质量 64";
        for (CelestialBodyClass surfaceClass : EVOLVABLE_SURFACE_CLASSES) {
            boolean covered = tracks.values().stream()
                .anyMatch(track -> track.surfaceClassFamily().equals(surfaceClass.name()));
            if (!covered) return "缺少表面分类轨道: " + surfaceClass.name();
        }
        for (StellarTrack track : tracks.values()) {
            try {
                track.validate();
            } catch (IllegalArgumentException exception) {
                return exception.getMessage();
            }
            for (PhaseNode node : track.phaseNodes()) {
                if (node.hasEventProfile() && !containsEventProfile(node.eventProfileId())) {
                    return "轨道引用未知事件 profile: " + node.eventProfileId();
                }
            }
        }
        return "";
    }

    /** 从数据包 JSON 解码轨道；解码或校验失败时保留当前缓存并返回 false。 */
    public static boolean replaceTracksFromJson(JsonElement json) {
        try {
            return TRACKS_CODEC.parse(JsonOps.INSTANCE, json).result().map(value -> {
                boolean templates = value.stream().anyMatch(track -> track.trackId().startsWith("template_"));
                replaceTracks(templates ? expandTemplates(value) : value);
                return true;
            }).orElse(false);
        } catch (RuntimeException exception) {
            AnvilCraft.LOGGER.warn("恒星轨道校验失败，保留上一版本缓存: {}", exception.getMessage());
            return false;
        }
    }

    /** 从 JSON 更新事件 profile 缓存。 */
    public static boolean replaceEventProfilesFromJson(JsonElement json) {
        try {
            return StellarEventProfile.CODEC.listOf().parse(JsonOps.INSTANCE, json).result().map(value -> {
                Map<String, StellarEventProfile> next = new LinkedHashMap<>();
                for (StellarEventProfile profile : value) {
                    boolean duplicate = next.keySet().stream()
                        .anyMatch(key -> key.equalsIgnoreCase(profile.profileId()));
                    if (duplicate) {
                        throw new IllegalArgumentException("重复事件 profile: " + profile.profileId());
                    }
                    next.put(profile.profileId(), profile);
                }
                if (next.isEmpty()) return false;
                eventProfiles = Collections.unmodifiableMap(next);
                return true;
            }).orElse(false);
        } catch (RuntimeException exception) {
            AnvilCraft.LOGGER.warn("恒星事件 profile 校验失败，保留上一版本缓存: {}", exception.getMessage());
            return false;
        }
    }

    /**
     * 读取随模组发布的数据文件，并把模板展开为每一种表面分类的轨道。
     * 资源包缺失或校验失败时继续使用内置回退，不影响旧存档加载。
     */
    public static void loadBundledResources() {
        try (InputStream stream = AnvilCraft.class.getClassLoader()
            .getResourceAsStream("data/anvilcraft/stellar_event_profiles.json")) {
            if (stream != null) {
                replaceEventProfilesFromJson(JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)));
            }
        } catch (Exception exception) {
            AnvilCraft.LOGGER.warn("恒星事件 profile 资源加载失败，使用内置默认值", exception);
        }
        try (InputStream stream = AnvilCraft.class.getClassLoader()
            .getResourceAsStream("data/anvilcraft/stellar_mass_bands.json")) {
            if (stream != null) {
                replaceMassBandsFromJson(JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)));
            }
        } catch (Exception exception) {
            AnvilCraft.LOGGER.warn("恒星质量带资源加载失败，使用内置默认值", exception);
        }
        try (InputStream stream = AnvilCraft.class.getClassLoader()
            .getResourceAsStream("data/anvilcraft/stellar_tracks.json")) {
            if (stream == null) return;
            var parsed = TRACKS_CODEC.parse(
                JsonOps.INSTANCE,
                JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
            ).result().orElse(List.of());
            List<StellarTrack> expanded = expandTemplates(parsed);
            if (!expanded.isEmpty()) replaceTracks(expanded);
            String validation = validateTracks();
            if (!validation.isEmpty()) AnvilCraft.LOGGER.warn("恒星轨道校验提示: {}", validation);
        } catch (Exception exception) {
            AnvilCraft.LOGGER.warn("恒星轨道资源加载失败，使用内置默认值", exception);
        }
    }

    /** 从数据包资源管理器重载轨道和 profile；失败时保持上一次有效缓存。 */
    public static void reload(ResourceManager resourceManager) {
        reloadJson(resourceManager, "stellar_event_profiles.json", StellarTrackLibrary::replaceEventProfilesFromJson);
        reloadJson(resourceManager, "stellar_mass_bands.json", StellarTrackLibrary::replaceMassBandsFromJson);
        reloadJson(resourceManager, "stellar_tracks.json", StellarTrackLibrary::replaceTracksFromJson);
    }

    private static void reloadJson(
        ResourceManager resourceManager,
        String path,
        java.util.function.Predicate<JsonElement> consumer
    ) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath("anvilcraft", path);
        try {
            var resource = resourceManager.getResource(location).orElse(null);
            if (resource == null) return;
            try (InputStream stream = resource.open()) {
                consumer.test(JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)));
            }
        } catch (Exception exception) {
            AnvilCraft.LOGGER.warn("恒星演化资源重载失败: {}", location, exception);
        }
    }

    private static List<StellarTrack> expandTemplates(List<StellarTrack> templates) {
        List<StellarTrack> expanded = new ArrayList<>();
        for (StellarTrack template : templates) {
            String family = familyId(template.massBand());
            if (family.isEmpty()) continue;
            for (CelestialBodyClass cls : EVOLVABLE_SURFACE_CLASSES) {
                expanded.add(new StellarTrack(
                    cls.name().toLowerCase(java.util.Locale.ROOT) + "_" + family,
                    template.massBand(),
                    cls.name(),
                    template.variantRules(),
                    template.phaseNodes(),
                    template.terminalProfile()
                ));
            }
        }
        expanded.add(createRemnantTrack("remnant_white_dwarf", "white_dwarf"));
        expanded.add(createRemnantTrack("remnant_neutron_star", "neutron_star"));
        expanded.add(createRemnantTrack("remnant_black_hole", "black_hole"));
        StellarTrack golden = expanded.stream()
            .filter(track -> track.trackId().equals("k_main_low_mass"))
            .findFirst()
            .orElse(null);
        if (golden != null) {
            expanded.add(golden.withIdAndTerminalProfile(GOLDEN_K_TRACK_ID, golden.terminalProfile()));
        }
        return expanded;
    }

    private static String familyId(String massBand) {
        return switch (massBand) {
            case "very_low", "very_low_mass" -> "very_low_mass";
            case "low", "low_mass" -> "low_mass";
            case "intermediate", "intermediate_mass" -> "intermediate_mass";
            case "super_agb" -> "super_agb";
            case "high_hydrogen" -> "high_hydrogen";
            case "stripped" -> "stripped";
            case "extreme" -> "extreme";
            default -> "";
        };
    }

    private static boolean containsEventProfile(String id) {
        return eventProfiles.keySet().stream().anyMatch(key -> key.equalsIgnoreCase(id));
    }

    /** 从数据包 JSON 解码质量带映射。 */
    public static boolean replaceMassBandsFromJson(JsonElement json) {
        try {
            return MASS_BANDS_CODEC.parse(JsonOps.INSTANCE, json).result().map(value -> {
                if (value.isEmpty()) return false;
                List<StellarMassBand> sorted = value.stream()
                    .sorted(java.util.Comparator.comparingInt(StellarMassBand::minGameMass))
                    .toList();
                int expected = 1;
                for (StellarMassBand band : sorted) {
                    if (band.minGameMass() != expected || band.maxGameMass() < band.minGameMass()) return false;
                    if (familyId(band.trackFamily()).isEmpty()) return false;
                    expected = Math.max(expected, band.maxGameMass() + 1);
                }
                if (expected <= 64) return false;
                massBands = List.copyOf(sorted);
                return true;
            }).orElse(false);
        } catch (RuntimeException exception) {
            AnvilCraft.LOGGER.warn("恒星质量带校验失败，保留上一版本缓存: {}", exception.getMessage());
            return false;
        }
    }

    /** 恢复内置轨道，供资源重载失败或测试隔离使用。 */
    public static void resetDefaults() {
        eventProfiles = Collections.unmodifiableMap(new LinkedHashMap<>(StellarEventProfile.defaults()));
        massBands = DEFAULT_MASS_BANDS;
        tracks = DEFAULT_TRACKS;
    }

    private static Map<String, StellarTrack> buildDefaultTracks() {
        Map<String, StellarTrack> result = new LinkedHashMap<>();
        EnumMap<CelestialBodyClass, String> families = new EnumMap<>(CelestialBodyClass.class);
        for (CelestialBodyClass cls : EVOLVABLE_SURFACE_CLASSES) {
            families.put(cls, familyForClass(cls));
        }
        for (Map.Entry<CelestialBodyClass, String> entry : families.entrySet()) {
            CelestialBodyClass cls = entry.getKey();
            String classId = cls.name().toLowerCase(java.util.Locale.ROOT);
            for (String family : List.of("very_low_mass", "low_mass", "intermediate_mass", "super_agb",
                "high_hydrogen", "stripped", "extreme")) {
                result.put(classId + "_" + family, createTrack(classId, family, cls));
            }
        }
        result.put("remnant_white_dwarf", createRemnantTrack("remnant_white_dwarf", "white_dwarf"));
        result.put("remnant_neutron_star", createRemnantTrack("remnant_neutron_star", "neutron_star"));
        result.put("remnant_black_hole", createRemnantTrack("remnant_black_hole", "black_hole"));
        result.put(GOLDEN_K_TRACK_ID, result.get("k_main_low_mass").withIdAndTerminalProfile(
            GOLDEN_K_TRACK_ID,
            result.get("k_main_low_mass").terminalProfile()
        ));
        return Collections.unmodifiableMap(result);
    }

    private static boolean isStellarSurfaceClass(CelestialBodyClass cls) {
        return cls.isStellar() && cls != CelestialBodyClass.WHITE_DWARF
            && cls != CelestialBodyClass.NEUTRON_STAR && cls != CelestialBodyClass.BLACK_HOLE;
    }

    private static String familyForClass(CelestialBodyClass cls) {
        if (cls == CelestialBodyClass.M_MAIN || cls == CelestialBodyClass.K_MAIN
            || cls == CelestialBodyClass.G_MAIN || cls == CelestialBodyClass.F_MAIN
            || cls == CelestialBodyClass.M_GIANT || cls == CelestialBodyClass.K_GIANT
            || cls == CelestialBodyClass.G_GIANT || cls == CelestialBodyClass.F_GIANT) {
            return "low_mass";
        }
        if (cls == CelestialBodyClass.A_MAIN || cls == CelestialBodyClass.B_MAIN
            || cls == CelestialBodyClass.A_GIANT || cls == CelestialBodyClass.B_GIANT) {
            return "intermediate_mass";
        }
        return "high_hydrogen";
    }

    private static StellarTrack createTrack(String classId, String family, CelestialBodyClass cls) {
        List<PhaseNode> nodes = switch (family) {
            case "very_low_mass" -> veryLowNodes();
            case "intermediate_mass" -> intermediateNodes();
            case "super_agb" -> superAgbNodes();
            case "high_hydrogen" -> massiveHydrogenNodes();
            case "stripped" -> strippedNodes();
            case "extreme" -> extremeNodes();
            default -> lowMassNodes();
        };
        String terminal = switch (family) {
            case "super_agb" -> "ELECTRON_CAPTURE";
            case "high_hydrogen" -> "CORE_COLLAPSE_II_P";
            case "stripped" -> "STRIPPED_IB";
            case "extreme" -> "PULSATIONAL_PAIR";
            default -> "";
        };
        return new StellarTrack(
            classId + "_" + family,
            family,
            cls.name(),
            List.of("metallicity_from_seed", "rotation_from_seed", "envelope_from_seed"),
            nodes,
            terminal
        );
    }

    private static StellarTrack createRemnantTrack(String id, String kind) {
        float radius = kind.equals("white_dwarf") ? 0.12f : 0.08f;
        float temp = kind.equals("black_hole") ? 1000.0f : 12000.0f;
        PhaseNode node = new PhaseNode(
            kind.equals("white_dwarf") ? StellarEvolutionPhase.WHITE_DWARF_COOLING : StellarEvolutionPhase.REMNANT_SETTLE,
            1.0f,
            radius,
            temp,
            0.1f,
            0.0f,
            0.0f,
            0.0f,
            "remnant"
        );
        return new StellarTrack(id, "remnant", kind, List.of(), List.of(node), "");
    }

    @SuppressWarnings("checkstyle:MethodName")
    private static PhaseNode n(
        StellarEvolutionPhase phase,
        float weight,
        float radius,
        float temperature,
        float luminosity,
        float envelope,
        float pulse,
        float frequency,
        String style
    ) {
        return new PhaseNode(phase, weight, radius, temperature, luminosity, envelope, pulse, frequency, style);
    }

    private static PhaseNode event(
        StellarEvolutionPhase phase,
        float weight,
        float radius,
        float temperature,
        float luminosity,
        String style,
        String profile
    ) {
        return new PhaseNode(phase, weight, radius, temperature, luminosity, 0.5f, 0.0f, 0.0f, style, profile);
    }

    private static List<PhaseNode> veryLowNodes() {
        return List.of(
            n(StellarEvolutionPhase.MAIN_SEQUENCE, 96.0f, 0.38f, 3200.0f, 0.05f, 0.9f, 0.0f, 0.0f, "stable_granules"),
            n(StellarEvolutionPhase.BLUE_LOOP, 8.0f, 0.45f, 7000.0f, 0.12f, 0.7f, 0.0f, 0.0f, "blue_dwarf_like"),
            n(StellarEvolutionPhase.WHITE_DWARF_COOLING, 4.0f, 0.12f, 16000.0f, 0.08f, 0.0f, 0.0f, 0.0f, "cooling_core")
        );
    }

    private static List<PhaseNode> lowMassNodes() {
        return List.of(
            n(StellarEvolutionPhase.MAIN_SEQUENCE, 62.0f, 0.72f, 5100.0f, 0.65f, 0.95f, 0.0f, 0.0f, "stable_granules"),
            n(StellarEvolutionPhase.SUBGIANT, 10.0f, 1.10f, 5400.0f, 1.8f, 0.85f, 0.006f, 0.003f, "slow_convection"),
            n(StellarEvolutionPhase.RGB, 8.0f, 3.8f, 4100.0f, 8.0f, 0.72f, 0.06f, 0.0025f, "large_convection"),
            event(StellarEvolutionPhase.HELIUM_FLASH, 1.0f, 3.1f, 9000.0f, 18.0f, "helium_flash", "HELIUM_FLASH"),
            n(StellarEvolutionPhase.RED_CLUMP, 5.0f, 1.55f, 5100.0f, 3.5f, 0.68f, 0.004f, 0.002f, "red_clump"),
            n(StellarEvolutionPhase.HORIZONTAL_BRANCH, 3.0f, 1.35f, 7200.0f, 4.5f, 0.45f, 0.004f, 0.002f, "horizontal_branch"),
            event(StellarEvolutionPhase.AGB, 5.0f, 5.2f, 3500.0f, 14.0f, "thermal_pulse", "AGB_THERMAL_PULSE"),
            n(StellarEvolutionPhase.POST_AGB, 1.5f, 0.55f, 12000.0f, 6.0f, 0.18f, 0.03f, 0.003f, "blue_core"),
            n(StellarEvolutionPhase.PPN, 1.0f, 0.34f, 20000.0f, 3.0f, 0.08f, 0.04f, 0.004f, "dust_shell"),
            n(StellarEvolutionPhase.WHITE_DWARF_COOLING, 3.0f, 0.12f, 14000.0f, 0.12f, 0.0f, 0.0f, 0.0f, "cooling_core")
        );
    }

    private static List<PhaseNode> intermediateNodes() {
        return List.of(
            n(StellarEvolutionPhase.MAIN_SEQUENCE, 48.0f, 0.95f, 8500.0f, 6.0f, 0.9f, 0.0f, 0.0f, "hot_granules"),
            n(StellarEvolutionPhase.SUBGIANT, 9.0f, 1.45f, 7200.0f, 10.0f, 0.8f, 0.006f, 0.003f, "slow_convection"),
            n(StellarEvolutionPhase.RGB, 7.0f, 3.2f, 5000.0f, 24.0f, 0.65f, 0.06f, 0.0025f, "large_convection"),
            n(StellarEvolutionPhase.BLUE_LOOP, 5.0f, 1.8f, 10000.0f, 30.0f, 0.5f, 0.004f, 0.002f, "blue_loop"),
            event(StellarEvolutionPhase.AGB, 5.0f, 5.8f, 3900.0f, 48.0f, "thermal_pulse", "AGB_THERMAL_PULSE"),
            n(StellarEvolutionPhase.POST_AGB, 1.5f, 0.50f, 18000.0f, 12.0f, 0.12f, 0.03f, 0.003f, "blue_core"),
            n(StellarEvolutionPhase.PPN, 1.0f, 0.32f, 30000.0f, 8.0f, 0.05f, 0.04f, 0.004f, "dust_shell"),
            n(StellarEvolutionPhase.WHITE_DWARF_COOLING, 3.0f, 0.12f, 16000.0f, 0.2f, 0.0f, 0.0f, 0.0f, "cooling_core")
        );
    }

    private static List<PhaseNode> superAgbNodes() {
        return List.of(
            n(StellarEvolutionPhase.MAIN_SEQUENCE, 42.0f, 1.2f, 15000.0f, 50.0f, 0.9f, 0.0f, 0.0f, "hot_granules"),
            n(StellarEvolutionPhase.RED_SUPERGIANT, 12.0f, 6.5f, 3800.0f, 180.0f, 0.55f, 0.10f, 0.002f, "large_convection"),
            event(StellarEvolutionPhase.AGB, 8.0f, 7.2f, 3500.0f, 220.0f, "super_agb_pulse", "AGB_THERMAL_PULSE"),
            n(StellarEvolutionPhase.PRE_COLLAPSE, 2.0f, 5.5f, 5000.0f, 160.0f, 0.18f, 0.12f, 0.0025f, "mass_loss"),
            event(StellarEvolutionPhase.EVENT_PRELUDE, 1.0f, 4.8f, 6500.0f, 140.0f, "electron_capture", "ELECTRON_CAPTURE"),
            event(StellarEvolutionPhase.EVENT_COLLAPSE, 1.0f, 0.3f, 20000.0f, 300.0f, "electron_capture", "ELECTRON_CAPTURE"),
            event(StellarEvolutionPhase.EVENT_EJECTA, 2.0f, 0.2f, 18000.0f, 80.0f, "thin_ejecta", "ELECTRON_CAPTURE"),
            event(StellarEvolutionPhase.REMNANT_SETTLE, 1.0f, 0.1f, 12000.0f, 1.0f, "remnant", "ELECTRON_CAPTURE")
        );
    }

    private static List<PhaseNode> massiveHydrogenNodes() {
        return List.of(
            n(StellarEvolutionPhase.MAIN_SEQUENCE, 45.0f, 1.55f, 28000.0f, 180.0f, 0.95f, 0.0f, 0.0f, "blue_hot"),
            n(StellarEvolutionPhase.BLUE_SUPERGIANT, 10.0f, 2.2f, 18000.0f, 420.0f, 0.82f, 0.006f, 0.003f, "blue_supergiant"),
            n(StellarEvolutionPhase.RED_SUPERGIANT, 8.0f, 8.5f, 3600.0f, 900.0f, 0.65f, 0.12f, 0.002f, "red_supergiant"),
            n(StellarEvolutionPhase.PRE_COLLAPSE, 2.0f, 7.0f, 3900.0f, 760.0f, 0.55f, 0.12f, 0.0025f, "pre_collapse"),
            event(StellarEvolutionPhase.EVENT_PRELUDE, 1.0f, 6.5f, 4000.0f, 700.0f, "event_prelude", "CORE_COLLAPSE_II_P"),
            event(StellarEvolutionPhase.EVENT_COLLAPSE, 1.0f, 0.25f, 18000.0f, 1600.0f, "event_collapse", "CORE_COLLAPSE_II_P"),
            event(StellarEvolutionPhase.EVENT_EJECTA, 3.0f, 0.16f, 12000.0f, 400.0f, "hydrogen_ejecta", "CORE_COLLAPSE_II_P"),
            event(StellarEvolutionPhase.REMNANT_SETTLE, 2.0f, 0.10f, 10000.0f, 1.0f, "remnant", "CORE_COLLAPSE_II_P")
        );
    }

    private static List<PhaseNode> strippedNodes() {
        return List.of(
            n(StellarEvolutionPhase.MAIN_SEQUENCE, 38.0f, 1.45f, 32000.0f, 260.0f, 0.75f, 0.0f, 0.0f, "blue_hot"),
            n(StellarEvolutionPhase.BLUE_SUPERGIANT, 8.0f, 2.0f, 22000.0f, 500.0f, 0.48f, 0.008f, 0.003f, "blue_supergiant"),
            n(StellarEvolutionPhase.LBV, 5.0f, 3.6f, 12000.0f, 800.0f, 0.25f, 0.12f, 0.003f, "lbv_ejection"),
            n(StellarEvolutionPhase.WOLF_RAYET, 8.0f, 0.9f, 60000.0f, 1200.0f, 0.08f, 0.06f, 0.003f, "wolf_rayet"),
            n(StellarEvolutionPhase.PRE_COLLAPSE, 2.0f, 0.75f, 50000.0f, 1000.0f, 0.04f, 0.10f, 0.002f, "pre_collapse"),
            event(StellarEvolutionPhase.EVENT_PRELUDE, 1.0f, 0.7f, 50000.0f, 900.0f, "stripped_prelude", "STRIPPED_IB"),
            event(StellarEvolutionPhase.EVENT_COLLAPSE, 1.0f, 0.12f, 30000.0f, 2200.0f, "stripped_collapse", "STRIPPED_IB"),
            event(StellarEvolutionPhase.EVENT_EJECTA, 2.0f, 0.08f, 20000.0f, 500.0f, "thin_ejecta", "STRIPPED_IB"),
            event(StellarEvolutionPhase.REMNANT_SETTLE, 2.0f, 0.06f, 12000.0f, 1.0f, "remnant", "STRIPPED_IB")
        );
    }

    private static List<PhaseNode> extremeNodes() {
        return List.of(
            n(StellarEvolutionPhase.MAIN_SEQUENCE, 36.0f, 1.9f, 42000.0f, 900.0f, 0.65f, 0.0f, 0.0f, "blue_hot"),
            n(StellarEvolutionPhase.LBV, 8.0f, 4.0f, 14000.0f, 1800.0f, 0.30f, 0.18f, 0.003f, "lbv_ejection"),
            n(StellarEvolutionPhase.WOLF_RAYET, 8.0f, 1.4f, 80000.0f, 3000.0f, 0.05f, 0.08f, 0.003f, "wolf_rayet"),
            n(StellarEvolutionPhase.PRE_COLLAPSE, 2.0f, 1.1f, 60000.0f, 2500.0f, 0.02f, 0.12f, 0.002f, "pair_pre_collapse"),
            event(StellarEvolutionPhase.EVENT_PRELUDE, 3.0f, 1.0f, 55000.0f, 2200.0f, "pair_pulses", "PULSATIONAL_PAIR"),
            event(StellarEvolutionPhase.EVENT_COLLAPSE, 2.0f, 0.12f, 50000.0f, 5000.0f, "pair_collapse", "PAIR_INSTABILITY"),
            event(StellarEvolutionPhase.EVENT_EJECTA, 4.0f, 0.05f, 30000.0f, 600.0f, "pair_ejecta", "PAIR_INSTABILITY"),
            event(StellarEvolutionPhase.REMNANT_SETTLE, 2.0f, 0.04f, 10000.0f, 1.0f, "remnant", "DIRECT_COLLAPSE")
        );
    }
}
