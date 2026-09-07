package dev.dubhe.anvilcraft.block.entity.celestial;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** 数据驱动的恒星演化轨道。 */
public final class StellarTrack {
    public static final Codec<StellarTrack> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("trackId").forGetter(StellarTrack::trackId),
        Codec.STRING.optionalFieldOf("massBand", "").forGetter(StellarTrack::massBand),
        Codec.STRING.optionalFieldOf("surfaceClassFamily", "").forGetter(StellarTrack::surfaceClassFamily),
        Codec.STRING.listOf().optionalFieldOf("variantRules", List.of()).forGetter(StellarTrack::variantRules),
        PhaseNode.CODEC.listOf().fieldOf("phaseNodes").forGetter(StellarTrack::phaseNodes),
        Codec.STRING.optionalFieldOf("terminalProfile", "").forGetter(StellarTrack::terminalProfile),
        StellarTrackDefinition.CODEC.optionalFieldOf("definition", StellarTrackDefinition.LEGACY)
            .forGetter(StellarTrack::definition)
    ).apply(instance, StellarTrack::new));

    private final StellarTrackDefinition definition;
    private final String trackId;
    private final String massBand;
    private final String surfaceClassFamily;
    private final List<String> variantRules;
    private final List<PhaseNode> phaseNodes;
    private final String terminalProfile;
    private final transient Map<Integer, List<Integer>> durationCache = new ConcurrentHashMap<>();

    public StellarTrack(
        String trackId,
        String massBand,
        String surfaceClassFamily,
        List<String> variantRules,
        List<PhaseNode> phaseNodes,
        String terminalProfile
    ) {
        this(trackId, massBand, surfaceClassFamily, variantRules, phaseNodes, terminalProfile, StellarTrackDefinition.LEGACY);
    }

    public StellarTrack(
        String trackId, String massBand, String surfaceClassFamily, List<String> variantRules,
        List<PhaseNode> phaseNodes, String terminalProfile, StellarTrackDefinition definition
    ) {
        this.definition = Objects.requireNonNull(definition);
        this.trackId = Objects.requireNonNull(trackId);
        this.massBand = Objects.requireNonNull(massBand);
        this.surfaceClassFamily = Objects.requireNonNull(surfaceClassFamily);
        this.variantRules = variantRules == null ? List.of() : List.copyOf(variantRules);
        if (phaseNodes == null || phaseNodes.isEmpty()) {
            throw new IllegalArgumentException("恒星轨道至少需要一个阶段节点");
        }
        this.phaseNodes = List.copyOf(phaseNodes);
        this.terminalProfile = terminalProfile == null ? "" : terminalProfile;
    }

    public StellarTrackDefinition definition() {
        return definition;
    }

    public boolean modern() {
        return definition.version() == 3;
    }

    public StellarTrack resolve(double coordinate, CelestialBodyClass surfaceClass) {
        if (!modern()) return this;
        if (!Double.isFinite(coordinate) || coordinate < -1 || coordinate > 1) {
            throw new IllegalArgumentException("Invalid metallicity coordinate");
        }
        return new StellarTrack(trackId, massBand, surfaceClass.name(), variantRules,
            phaseNodes.stream().map(node -> node.resolve(coordinate)).toList(), terminalProfile, definition);
    }

    public int nodeIndex(String nodeId) {
        for (int index = 0; index < phaseNodes.size(); index++) {
            if (phaseNodes.get(index).nodeId().equals(nodeId)) return index;
        }
        throw new IllegalArgumentException("Unknown stellar node: " + trackId + "/" + nodeId);
    }

    public String trackId() {
        return this.trackId;
    }

    public String massBand() {
        return this.massBand;
    }

    public String surfaceClassFamily() {
        return this.surfaceClassFamily;
    }

    public List<String> variantRules() {
        return this.variantRules;
    }

    public List<PhaseNode> phaseNodes() {
        return this.phaseNodes;
    }

    public boolean containsPhase(StellarEvolutionPhase phase) {
        return phaseNodes.stream().anyMatch(node -> node.phaseId() == phase);
    }

    public boolean hasTerminalEvent() {
        return terminalProfile != null && !terminalProfile.isBlank();
    }

    public String terminalProfile() {
        return this.terminalProfile;
    }

    /** 返回只替换终局 profile 的不可变轨道副本，用于确定性变体。 */
    public StellarTrack withTerminalProfile(String id) {
        return new StellarTrack(trackId, massBand, surfaceClassFamily, variantRules, phaseNodes, id, definition);
    }

    public StellarTrack withIdAndTerminalProfile(String id, String terminalProfileId) {
        return new StellarTrack(id, massBand, surfaceClassFamily, variantRules, phaseNodes, terminalProfileId, definition);
    }

    /** 返回把所有事件节点切换到指定 profile 的变体副本。 */
    public StellarTrack withEventProfile(String id, String terminalProfileId) {
        List<PhaseNode> nodes = phaseNodes.stream()
            .map(node -> node.hasEventProfile()
                ? new PhaseNode(
                    node.phaseId(),
                    node.durationWeight(),
                    node.radius(),
                    node.temperature(),
                    node.luminosity(),
                    node.envelopeFraction(),
                    node.pulsationAmplitude(),
                    node.pulsationFrequency(),
                    node.surfaceStyle(),
                    id
                )
                : node)
            .toList();
        return new StellarTrack(trackId, massBand, surfaceClassFamily, variantRules, nodes, terminalProfileId);
    }

    /** 返回轨道权重总和。 */
    public float totalWeight() {
        float result = 0.0f;
        for (PhaseNode node : phaseNodes) result += node.durationWeight();
        return result;
    }

    /** 返回按总预算分配且每个节点至少一 tick 的阶段时长。 */
    public List<Integer> phaseDurations(int totalTicks) {
        int budget = Math.max(totalTicks, phaseNodes.size());
        List<Integer> cached = durationCache.get(budget);
        if (cached != null) return cached;
        float weightTotal = totalWeight();
        List<Integer> durations = new ArrayList<>(phaseNodes.size());
        List<Float> fractions = new ArrayList<>(phaseNodes.size());
        int used = 0;
        for (PhaseNode node : phaseNodes) {
            float exact = budget * node.durationWeight() / weightTotal;
            int duration = Math.max(1, (int) Math.floor(exact));
            durations.add(duration);
            fractions.add(exact - (float) Math.floor(exact));
            used += duration;
        }
        if (used < budget) {
            List<Integer> order = new ArrayList<>();
            for (int i = 0; i < durations.size(); i++) order.add(i);
            order.sort(Comparator.comparingDouble(fractions::get).reversed());
            int cursor = 0;
            while (used < budget) {
                int index = order.get(cursor++ % order.size());
                durations.set(index, durations.get(index) + 1);
                used++;
            }
        } else if (used > budget) {
            List<Integer> order = new ArrayList<>();
            for (int i = 0; i < durations.size(); i++) order.add(i);
            order.sort(Comparator.comparingInt(durations::get).reversed());
            int cursor = 0;
            while (used > budget) {
                int index = order.get(cursor++ % order.size());
                if (durations.get(index) > 1) {
                    durations.set(index, durations.get(index) - 1);
                    used--;
                }
            }
        }
        List<Integer> result = List.copyOf(durations);
        if (durationCache.size() >= 16) durationCache.clear();
        durationCache.putIfAbsent(budget, result);
        return result;
    }

    /** 根据总轨道进度定位阶段和相邻节点。 */
    public PhaseSample sample(long elapsedTicks, int totalTicks) {
        List<Integer> durations = phaseDurations(totalTicks);
        long clamped = Math.max(0L, Math.min((long) Math.max(totalTicks, phaseNodes.size()), elapsedTicks));
        long cursor = 0L;
        for (int i = 0; i < phaseNodes.size(); i++) {
            int duration = durations.get(i);
            long end = cursor + duration;
            if (clamped < end || i == phaseNodes.size() - 1) {
                float progress = duration <= 0 ? 1.0f : (float) (clamped - cursor) / duration;
                progress = Math.clamp(progress, 0.0f, 1.0f);
                PhaseNode next = i + 1 < phaseNodes.size() ? phaseNodes.get(i + 1) : phaseNodes.get(i);
                return new PhaseSample(i, cursor, duration, progress, phaseNodes.get(i), next);
            }
            cursor = end;
        }
        PhaseNode last = phaseNodes.get(phaseNodes.size() - 1);
        return new PhaseSample(phaseNodes.size() - 1, cursor, durations.get(durations.size() - 1), 1.0f, last, last);
    }

    /** 在轨道总进度上插值出视觉快照；节点数值是阶段目标，因此起点取上一节点。 */
    public StellarVisualState visualAt(float progress) {
        float totalProgress = Float.isFinite(progress) ? Math.clamp(progress, 0.0f, 1.0f) : 0.0f;
        long syntheticElapsed = Math.round(totalProgress * 100000L);
        PhaseSample sample = sample(syntheticElapsed, 100000);
        return sample.node().dynamics().sample(sample.node(), phaseNodes.get(Math.max(0, sample.index() - 1)),
            sample.progress());
    }

    public StellarVisualState sampleVisual(float progress) {
        return visualAt(progress);
    }

    public StellarVisualState sampleVisual(long elapsedTicks, int totalTicks) {
        PhaseSample sample = sample(elapsedTicks, totalTicks);
        return sample.node().dynamics().sample(sample.node(), phaseNodes.get(Math.max(0, sample.index() - 1)),
            sample.progress());
    }

    public StellarVisualState sampleVisualState(float progress) {
        return visualAt(progress);
    }

    /** 生成单个节点对应的基础快照。 */
    public static StellarVisualState visualForNode(PhaseNode node) {
        float pulsationAmplitude = visualPulsationAmplitude(node);
        float pulsationFrequency = visualPulsationFrequency(node, pulsationAmplitude);
        return new StellarVisualState(
            node.radius(),
            node.temperature(),
            node.luminosity(),
            StellarVisualState.colorForTemperature(node.temperature()),
            Math.max(0.05f, (float) Math.sqrt(node.luminosity())),
            Math.clamp(node.envelopeFraction(), 0.0f, 1.0f),
            node.radius(),
            0.0f,
            pulsationAmplitude,
            pulsationFrequency,
            node.surfaceStyle()
        );
    }

    /**
     * 将轨道中的脉动参数转换为视觉脉冲深度（向内收缩的最大比例）。
     *
     * <p>稳定阶段不脉动；AGB 是热脉冲阶段，必须能明显看到本体反复大幅缩放，
     * 因此深度取到 0.5 以上。</p>
     */
    private static float visualPulsationAmplitude(PhaseNode node) {
        float configured = node.pulsationAmplitude();
        if (!node.nodeId().isBlank()) return configured;
        return switch (node.phaseId()) {
            case MAIN_SEQUENCE, HELIUM_FLASH, RED_CLUMP, HORIZONTAL_BRANCH, BLUE_LOOP,
                EVENT_PRELUDE, EVENT_COLLAPSE, EVENT_EJECTA, REMNANT_SETTLE, WHITE_DWARF_COOLING -> 0.0f;
            case SUBGIANT -> Math.min(configured, 0.01f);
            case RGB -> Math.min(configured, 0.08f);
            case AGB -> Math.clamp(Math.max(configured, 0.50f), 0.0f, 0.62f);
            case POST_AGB, PPN -> Math.min(configured, 0.04f);
            case RED_SUPERGIANT -> Math.min(configured, 0.12f);
            case BLUE_SUPERGIANT, LBV, WOLF_RAYET, PRE_COLLAPSE -> Math.min(configured, 0.10f);
            default -> configured;
        };
    }

    /**
     * 数据中的“每刻弧度”换算成“每个阶段脉动几次”的系数。
     *
     * <p>绝对角频率会让脉动次数随阶段被分配到的刻数漂移：同一条轨道在短预算下一次
     * 脉动都看不完，在长预算下又快得像抖动。改成每阶段固定次数后，AGB 无论持续几秒
     * 还是几分钟都能看完整的几次大幅度胀缩。取整是为了让最后一次脉冲正好停在峰值。</p>
     */
    private static final float PULSE_CYCLES_PER_FREQUENCY = 800.0f;

    private static float visualPulsationFrequency(PhaseNode node, float amplitude) {
        if (amplitude <= 0.0f) return 0.0f;
        if (!node.nodeId().isBlank()) return node.dynamics().pulses();
        return Math.round(Math.clamp(node.pulsationFrequency() * PULSE_CYCLES_PER_FREQUENCY, 1.0f, 4.0f));
    }

    /** 按事件 profile 叠加核心收缩和抛射物膨胀。 */
    @SuppressWarnings("checkstyle:OverloadMethodsDeclarationOrder")
    public StellarVisualState visualAt(float progress, StellarEventProfile profile) {
        StellarVisualState base = visualAt(progress);
        float eventProgress = Float.isFinite(progress) ? Math.clamp(progress, 0.0f, 1.0f) : 0.0f;
        float coreFactor = profile.coreRadius(eventProgress);
        float ejectaFactor = profile.ejectaRadius(eventProgress);
        int eventColor = profile.color(eventProgress);
        return new StellarVisualState(
            base.radius() * Math.max(0.05f, coreFactor),
            base.temperature(),
            base.luminosity(),
            StellarVisualState.interpolateColor(base.surfaceColor(), eventColor, Math.min(1.0f, eventProgress * 2.0f)),
            Math.max(base.emission(), profile.emission(eventProgress)),
            base.envelopeOpacity(),
            base.coreRadius() * Math.max(0.05f, coreFactor),
            base.radius() * ejectaFactor,
            base.pulsationAmplitude(),
            base.pulsationFrequency(),
            base.surfaceStyle()
        );
    }

    /** 检查阶段顺序和数值约束，资源重载时调用。 */
    public void validate() {
        if (trackId.isBlank()) throw new IllegalArgumentException("恒星轨道 ID 不能为空");
        java.util.Set<String> nodeIds = new java.util.HashSet<>();
        for (PhaseNode node : phaseNodes) {
            if (modern() && (node.nodeId().isBlank() || !nodeIds.add(node.nodeId()) || node.dynamics().points().isEmpty())) {
                throw new IllegalArgumentException("Missing or duplicate stellar node ID/curve: " + trackId);
            }
        }
        if (!modern()) return;
        if (definition.massAnvils() < 41 || !trackId.equals("mass_" + definition.massAnvils())) {
            throw new IllegalArgumentException("Invalid discrete stellar track: " + trackId);
        }
        definition.terminal().validate(CelestialMassTable.at(definition.massAnvils()).solarMass());
        for (String nodeId : definition.startingNodes().values()) nodeIndex(nodeId);
        StellarEvolutionPhase last = phaseNodes.getLast().phaseId();
        StellarTerminal.Kind outcome = definition.terminal().kind();
        boolean reachesOutcome = switch (outcome) {
            case WHITE_DWARF -> last == StellarEvolutionPhase.PRE_WHITE_DWARF
                || last == StellarEvolutionPhase.SHELL_HYDROGEN_BURNING || last == StellarEvolutionPhase.PLANETARY_NEBULA
                || last == StellarEvolutionPhase.AGB_MANQUE;
            case NEUTRON_STAR -> last == StellarEvolutionPhase.SUPERNOVA;
            case BLACK_HOLE -> last == StellarEvolutionPhase.SUPERNOVA || last == StellarEvolutionPhase.DIRECT_COLLAPSE
                || last == StellarEvolutionPhase.PPISN;
            case NONE -> last == StellarEvolutionPhase.PISN;
            case KEEP -> last == StellarEvolutionPhase.BROWN_DWARF_COOLING;
        };
        if (!reachesOutcome) throw new IllegalArgumentException("Final physical phase disagrees with outcome: " + trackId);
        for (PhaseNode node : phaseNodes) {
            if (node.phaseId().isRemnantPhase()) {
                throw new IllegalArgumentException("Remnants are outcomes, not stellar phases: " + trackId);
            }
            StellarNodeDynamics.EventPolicy policy = node.dynamics().eventPolicy();
            if (policy != StellarNodeDynamics.EventPolicy.VISUAL && !node.hasEventProfile()) {
                throw new IllegalArgumentException("Missing stellar event profile: " + node.nodeId());
            }
            boolean compatible = switch (policy) {
                case SUPERNOVA -> node.phaseId() == StellarEvolutionPhase.SUPERNOVA
                    && (outcome == StellarTerminal.Kind.NEUTRON_STAR || outcome == StellarTerminal.Kind.BLACK_HOLE);
                case DIRECT_COLLAPSE -> node.phaseId() == StellarEvolutionPhase.DIRECT_COLLAPSE
                    && outcome == StellarTerminal.Kind.BLACK_HOLE;
                case PPISN -> node.phaseId() == StellarEvolutionPhase.PPISN && outcome == StellarTerminal.Kind.BLACK_HOLE;
                case PISN -> node.phaseId() == StellarEvolutionPhase.PISN && outcome == StellarTerminal.Kind.NONE;
                case VISUAL -> true;
            };
            if (!compatible) throw new IllegalArgumentException("Event policy disagrees with route: " + node.nodeId());
            if (policy.destructive() && node.dynamics().response().maximumPulses() > 1) {
                throw new IllegalArgumentException("Destructive events cannot repeat");
            }
        }
    }

    /** 阶段采样结果，包含节点进度和绝对阶段起点。 */
    public record PhaseSample(
        int index,
        long phaseStart,
        int phaseDuration,
        float progress,
        PhaseNode node,
        PhaseNode nextNode
    ) {
    }
}
