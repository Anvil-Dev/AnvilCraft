package dev.dubhe.anvilcraft.block.entity.celestial;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * 恒星演化的可持久化状态机。
 *
 * <p>时钟使用世界绝对游戏刻，因此区块重载、客户端迟到数据包和服务器暂停都能
 * 重新收敛。阶段视觉数据由轨道采样，不会写回玩法用的 {@link StarData}。</p>
 */
public final class StellarEvolutionState {
    public static final String TRACK_ID_KEY = "stellarTrackId";
    public static final String PHASE_ID_KEY = "stellarPhaseId";
    public static final String PHASE_INDEX_KEY = "stellarPhaseIndex";
    public static final String PHASE_START_KEY = "stellarPhaseStartGameTime";
    public static final String PHASE_DURATION_KEY = "stellarPhaseDurationTicks";
    public static final String TOTAL_START_KEY = "stellarTotalStartGameTime";
    public static final String TOTAL_DURATION_KEY = "stellarTotalDurationTicks";
    public static final String PHASE_PROGRESS_KEY = "stellarPhaseProgress";
    public static final String SCHEDULE_START_INDEX_KEY = "stellarScheduleStartIndex";
    public static final String INITIAL_MASS_KEY = "stellarInitialMass";
    public static final String INITIAL_ENERGY_KEY = "stellarInitialEnergy";
    public static final String INITIAL_SIZE_KEY = "stellarInitialSize";
    public static final String INITIAL_SURFACE_CLASS_KEY = "stellarInitialSurfaceClass";
    public static final String CURRENT_MASS_KEY = "stellarCurrentMass";
    public static final String TRACK_SEED_KEY = "stellarTrackSeed";
    public static final String METALLICITY_VARIANT_KEY = "stellarMetallicityVariant";
    public static final String ROTATION_VARIANT_KEY = "stellarRotationVariant";
    public static final String BINARY_VARIANT_KEY = "stellarBinaryVariant";
    public static final String TERMINAL_PROFILE_KEY = "stellarTerminalProfileId";
    public static final String EVENT_ID_KEY = "stellarEventId";
    public static final String EVENT_START_KEY = "stellarEventStartGameTime";
    public static final String EVENT_SEED_KEY = "stellarEventSeed";
    public static final String EVENT_TRIGGERED_KEY = "stellarEventTriggered";
    public static final String TERMINAL_APPLIED_KEY = "stellarTerminalApplied";

    private String trackId = "";
    private String phaseId = "";
    private int phaseIndex;
    private long phaseStartGameTime;
    private int phaseDurationTicks;
    private long totalStartGameTime;
    private int totalDurationTicks;
    private float phaseProgress;
    private int initialMass;
    private int initialEnergy;
    private int initialSize;
    private String initialSurfaceClass = "";
    private int currentMass;
    private long trackSeed;
    private int metallicityVariant;
    private int rotationVariant;
    private int binaryVariant;
    private String terminalProfileId = "";
    private String eventId = "";
    private long eventStartGameTime = -1L;
    private long eventSeed;
    private boolean eventTriggered;
    private boolean terminalApplied;

    /** 与运行快照一同保存；只有缺少快照的旧存档需要重新分配时长。 */
    private List<Integer> phaseDurations = List.of();
    private int scheduleStartIndex;
    private transient String cachedTrackId = "";
    private transient int cachedDurationBudget = -1;
    private transient int cachedScheduleStartIndex = -1;

    private int formatVersion = 1;
    private String nodeId = "";
    private double initialSolarMass;
    private double currentSolarMass;
    private double metallicityZ;
    private double metallicityCoordinate;
    @Nullable
    private StellarTrack trackSnapshot;
    private Map<String, StellarEventProfile> profileSnapshot = Map.of();
    private List<StellarScheduledEvent> eventPlan = List.of();
    private Set<String> appliedEvents = new LinkedHashSet<>();

    public static StellarEvolutionState beginNew(
        StellarTrack track, CelestialBodyClass surfaceClass, int initialMass, int initialEnergy, int initialSize,
        long evolutionSeed, long startGameTime, int totalDurationTicks
    ) {
        if (!track.modern() || track.definition().massAnvils() != initialMass) {
            throw new IllegalArgumentException("Stellar track does not match initial mass");
        }
        double metallicity = track.definition().metallicity().sample(evolutionSeed);
        double coordinate = track.definition().metallicity().coordinate(metallicity);
        StellarTrack resolved = track.resolve(coordinate, surfaceClass);
        StellarEvolutionState state = begin(resolved, StellarTrackLibrary.startingPhaseIndex(resolved, surfaceClass),
            initialMass, initialEnergy, initialSize, initialMass, evolutionSeed, startGameTime, totalDurationTicks);
        state.metallicityZ = metallicity;
        state.metallicityCoordinate = coordinate;
        Map<String, StellarEventProfile> resolvedProfiles = new LinkedHashMap<>();
        state.profileSnapshot.forEach((id, profile) -> resolvedProfiles.put(id, profile.resolveVisual(coordinate)));
        state.profileSnapshot = Map.copyOf(resolvedProfiles);
        return state;
    }

    public boolean modern() {
        return formatVersion == 3;
    }

    @Nullable
    public StellarTrack trackSnapshot() {
        return trackSnapshot;
    }

    public String nodeId() {
        return nodeId;
    }

    public double initialSolarMass() {
        return initialSolarMass;
    }

    public double currentSolarMass() {
        return currentSolarMass;
    }

    public double metallicityZ() {
        return metallicityZ;
    }

    public double metallicityCoordinate() {
        return metallicityCoordinate;
    }

    public List<StellarScheduledEvent> eventPlan() {
        return eventPlan;
    }

    public Set<String> appliedEvents() {
        return Set.copyOf(appliedEvents);
    }

    public StellarEventProfile eventProfile(String id) {
        StellarEventProfile profile = profileSnapshot.get(id);
        if (profile != null) return profile;
        if (modern()) throw new IllegalStateException("Missing frozen event profile: " + id);
        return StellarTrackLibrary.legacyEventProfile(id);
    }

    private StellarTrack effectiveTrack(StellarTrack track) {
        return trackSnapshot == null ? track : trackSnapshot;
    }

    private void captureProfiles(StellarTrack track) {
        Map<String, StellarEventProfile> profiles = new LinkedHashMap<>();
        for (PhaseNode node : track.phaseNodes()) {
            if (node.hasEventProfile()) {
                profiles.put(node.eventProfileId(), modern() ? StellarTrackLibrary.eventProfile(node.eventProfileId())
                    : StellarTrackLibrary.legacyEventProfile(node.eventProfileId()));
            }
        }
        profileSnapshot = Map.copyOf(profiles);
    }

    private void createEventPlan(StellarTrack track) {
        if (!modern()) return;
        List<StellarScheduledEvent> events = new ArrayList<>();
        int offset = 0;
        for (int index = scheduleStartIndex; index < track.phaseNodes().size(); index++) {
            PhaseNode node = track.phaseNodes().get(index);
            int duration = durationAt(index);
            if (node.hasEventProfile()) {
                int pulses = node.dynamics().pulses();
                StellarEventProfile profile = eventProfile(node.eventProfileId());
                for (int pulse = 0; pulse < pulses; pulse++) {
                    int start = offset + duration * pulse / pulses;
                    int end = offset + duration * (pulse + 1) / pulses;
                    int shock = start + Math.min(end - start - 1,
                        Math.round((end - start) * profile.shockBreakoutTick() / (float) profile.totalTicks()));
                    events.add(new StellarScheduledEvent(trackId + ":" + node.nodeId() + ":" + pulse,
                        node.nodeId(), node.eventProfileId(), start, end, shock, pulse,
                        mixSeed(trackSeed, index * 31L + pulse), node.dynamics().eventPolicy()));
                }
            }
            offset += duration;
        }
        eventPlan = List.copyOf(events);
    }

    public List<StellarScheduledEvent> dueEvents(long gameTime) {
        if (terminalApplied) return List.of();
        long elapsed = elapsedTicks(gameTime);
        return eventPlan.stream().filter(event -> elapsed >= event.shockOffset()
            && !appliedEvents.contains(event.instanceId())).toList();
    }

    public void markEventApplied(StellarScheduledEvent event) {
        appliedEvents.add(event.instanceId());
    }

    @Nullable
    private StellarScheduledEvent eventAt(double elapsed) {
        for (StellarScheduledEvent event : eventPlan) {
            if (elapsed >= event.startOffset() && elapsed < event.endOffset()) return event;
        }
        return null;
    }

    public static StellarEvolutionState begin(
        StellarTrack track,
        int startPhaseIndex,
        int initialMass,
        int initialEnergy,
        int initialSize,
        int currentMass,
        long trackSeed,
        long startGameTime,
        int totalDurationTicks
    ) {
        StellarEvolutionState state = new StellarEvolutionState();
        state.formatVersion = track.modern() ? 3 : 1;
        state.trackSnapshot = track;
        state.captureProfiles(track);
        state.trackId = track.trackId();
        state.initialSolarMass = CelestialMassTable.at(initialMass).solarMass();
        state.currentSolarMass = state.initialSolarMass;
        state.initialMass = initialMass;
        state.initialEnergy = initialEnergy;
        state.initialSize = initialSize;
        state.initialSurfaceClass = track.surfaceClassFamily();
        state.currentMass = currentMass;
        state.trackSeed = trackSeed;
        state.metallicityVariant = StellarTrackLibrary.variant(trackSeed, 0) & 3;
        state.metallicityCoordinate = state.metallicityVariant / 1.5 - 1;
        state.metallicityZ = StellarMetallicity.DEFAULT.at(state.metallicityCoordinate);
        state.rotationVariant = StellarTrackLibrary.variant(trackSeed, 1) & 3;
        state.binaryVariant = StellarTrackLibrary.variant(trackSeed, 2) & 3;
        state.eventSeed = mixSeed(trackSeed, 0xE771L);
        state.totalStartGameTime = startGameTime;
        state.totalDurationTicks = Math.max(track.phaseNodes().size(), totalDurationTicks);
        state.scheduleStartIndex = Math.clamp(startPhaseIndex, 0, track.phaseNodes().size() - 1);
        state.attachTrack(track);
        state.phaseIndex = state.scheduleStartIndex;
        state.phaseStartGameTime = startGameTime;
        state.phaseDurationTicks = state.durationAt(state.phaseIndex);
        state.phaseId = track.phaseNodes().get(state.phaseIndex).phaseId().getSerializedName();
        if (track.phaseNodes().get(state.phaseIndex).hasEventProfile()) {
            state.eventStartGameTime = startGameTime;
            state.eventSeed = mixSeed(trackSeed, state.phaseIndex);
            state.eventId = track.trackId() + ":" + state.phaseId + ":" + startGameTime;
        }
        state.nodeId = track.phaseNodes().get(state.phaseIndex).nodeId();
        state.createEventPlan(track);
        state.updateProgress(startGameTime);
        return state;
    }

    /** 从 NBT 读取新状态；不存在时返回空闲状态。 */
    public static StellarEvolutionState fromTag(CompoundTag tag) {
        tag = migrateResultStage(tag);
        StellarEvolutionState state = new StellarEvolutionState();
        state.trackId = tag.getString(TRACK_ID_KEY);
        state.phaseId = tag.getString(PHASE_ID_KEY);
        state.phaseIndex = Math.max(0, tag.getInt(PHASE_INDEX_KEY));
        state.scheduleStartIndex = tag.contains(SCHEDULE_START_INDEX_KEY)
            ? Math.max(0, tag.getInt(SCHEDULE_START_INDEX_KEY))
            : state.phaseIndex;
        state.phaseStartGameTime = tag.getLong(PHASE_START_KEY);
        state.phaseDurationTicks = Math.max(1, tag.getInt(PHASE_DURATION_KEY));
        state.totalStartGameTime = tag.getLong(TOTAL_START_KEY);
        state.totalDurationTicks = Math.max(1, tag.getInt(TOTAL_DURATION_KEY));
        float loadedProgress = tag.getFloat(PHASE_PROGRESS_KEY);
        state.phaseProgress = Float.isFinite(loadedProgress) ? Math.clamp(loadedProgress, 0.0f, 1.0f) : 0.0f;
        state.initialMass = tag.getInt(INITIAL_MASS_KEY);
        state.initialEnergy = tag.getInt(INITIAL_ENERGY_KEY);
        state.initialSize = tag.getInt(INITIAL_SIZE_KEY);
        state.initialSurfaceClass = tag.getString(INITIAL_SURFACE_CLASS_KEY);
        state.currentMass = tag.contains(CURRENT_MASS_KEY) ? tag.getInt(CURRENT_MASS_KEY) : state.initialMass;
        state.trackSeed = tag.getLong(TRACK_SEED_KEY);
        state.metallicityVariant = tag.getInt(METALLICITY_VARIANT_KEY);
        state.rotationVariant = tag.getInt(ROTATION_VARIANT_KEY);
        state.binaryVariant = tag.getInt(BINARY_VARIANT_KEY);
        state.terminalProfileId = tag.getString(TERMINAL_PROFILE_KEY);
        state.eventId = tag.getString(EVENT_ID_KEY);
        state.eventStartGameTime = tag.contains(EVENT_START_KEY) ? tag.getLong(EVENT_START_KEY) : -1L;
        state.eventSeed = tag.getLong(EVENT_SEED_KEY);
        state.eventTriggered = tag.getBoolean(EVENT_TRIGGERED_KEY);
        state.terminalApplied = tag.getBoolean(TERMINAL_APPLIED_KEY);
        state.formatVersion = tag.contains("stellarFormatVersion") ? tag.getInt("stellarFormatVersion") : 1;
        if (state.formatVersion < 1 || state.formatVersion > 3) throw new IllegalArgumentException("Unknown stellar state version");
        state.nodeId = tag.getString("stellarNodeId");
        state.initialSolarMass = tag.contains("stellarInitialSolarMass") ? tag.getDouble("stellarInitialSolarMass")
            : state.initialMass > 0 ? CelestialMassTable.at(state.initialMass).solarMass() : 0;
        state.currentSolarMass = tag.contains("stellarCurrentSolarMass")
            ? tag.getDouble("stellarCurrentSolarMass") : state.initialSolarMass;
        if (!tag.contains(METALLICITY_VARIANT_KEY)) state.metallicityVariant = StellarTrackLibrary.variant(state.trackSeed, 0) & 3;
        state.metallicityCoordinate = tag.contains("stellarMetallicityCoordinate")
            ? tag.getDouble("stellarMetallicityCoordinate") : state.metallicityVariant / 1.5 - 1;
        state.metallicityZ = tag.contains("stellarMetallicityZ") ? tag.getDouble("stellarMetallicityZ")
            : StellarMetallicity.DEFAULT.at(state.metallicityCoordinate);
        if (!Double.isFinite(state.metallicityZ) || state.metallicityZ <= 0
            || !Double.isFinite(state.metallicityCoordinate) || Math.abs(state.metallicityCoordinate) > 1) {
            throw new IllegalArgumentException("Invalid saved stellar metallicity");
        }
        if (tag.contains("stellarTrackSnapshot")) {
            state.trackSnapshot = StellarTrack.CODEC.parse(NbtOps.INSTANCE, tag.getCompound("stellarTrackSnapshot")).getOrThrow();
            state.trackSnapshot.validate();
            state.profileSnapshot = Codec.unboundedMap(Codec.STRING, StellarEventProfile.CODEC)
                .parse(NbtOps.INSTANCE, tag.getCompound("stellarProfileSnapshot")).getOrThrow();
            state.phaseDurations = java.util.Arrays.stream(tag.getIntArray("stellarPhaseDurations")).boxed().toList();
            state.cachedTrackId = state.trackId;
            state.cachedDurationBudget = state.totalDurationTicks;
            state.cachedScheduleStartIndex = state.scheduleStartIndex;
            state.eventPlan = StellarScheduledEvent.CODEC.listOf().parse(NbtOps.INSTANCE,
                tag.get("stellarEventPlan")).getOrThrow();
            state.appliedEvents = new LinkedHashSet<>(Codec.STRING.listOf().parse(NbtOps.INSTANCE,
                tag.get("stellarAppliedEvents")).getOrThrow());
            state.validateSnapshot();
        } else if (state.modern()) {
            throw new IllegalArgumentException("Missing active stellar snapshot");
        }
        return state;
    }

    /** 修正格式 2 末尾的结果节点，只删除额外时长，不重算真实阶段或事件。 */
    private static CompoundTag migrateResultStage(CompoundTag original) {
        if (original.getInt("stellarFormatVersion") != 2) return original;
        CompoundTag tag = original.copy();
        tag.putInt("stellarFormatVersion", 3);
        CompoundTag snapshot = tag.getCompound("stellarTrackSnapshot");
        CompoundTag definition = snapshot.getCompound("definition");
        definition.putInt("version", 3);
        String outcome = definition.getCompound("terminal").getString("kind");
        ListTag nodes = snapshot.getList("phaseNodes", Tag.TAG_COMPOUND);
        int last = nodes.size() - 1;
        if (last < 0 || outcome.equals("keep") || !nodes.getCompound(last).getString("phaseId").equals(outcome)) return tag;
        int start = tag.getInt(SCHEDULE_START_INDEX_KEY);
        int[] durations = tag.getIntArray("stellarPhaseDurations");
        if (last < 1 || start >= last || durations.length != nodes.size() - start) {
            throw new IllegalArgumentException("Invalid result-stage migration timeline");
        }
        nodes.remove(last);
        int[] remaining = java.util.Arrays.copyOf(durations, durations.length - 1);
        tag.putIntArray("stellarPhaseDurations", remaining);
        tag.putInt(TOTAL_DURATION_KEY, java.util.Arrays.stream(remaining).sum());
        if (tag.getInt(PHASE_INDEX_KEY) >= last) {
            CompoundTag finalPhase = nodes.getCompound(last - 1);
            int finalDuration = remaining[remaining.length - 1];
            tag.putInt(PHASE_INDEX_KEY, last - 1);
            tag.putString(PHASE_ID_KEY, finalPhase.getString("phaseId"));
            tag.putString("stellarNodeId", finalPhase.getString("nodeId"));
            tag.putLong(PHASE_START_KEY, tag.getLong(TOTAL_START_KEY) + tag.getInt(TOTAL_DURATION_KEY) - finalDuration);
            tag.putInt(PHASE_DURATION_KEY, finalDuration);
            tag.putFloat(PHASE_PROGRESS_KEY, 1);
        }
        return tag;
    }

    /** 将旧版四阶段 NBT 迁移成新状态，保留原总预算和当前进度。 */
    public static StellarEvolutionState migrateLegacy(
        CompoundTag tag,
        StellarTrack track,
        int fallbackMass,
        int fallbackEnergy,
        int fallbackSize,
        long now
    ) {
        int legacyStage = tag.getInt("acceleratorStage");
        int remaining = Math.max(0, tag.getInt("acceleratorTicksRemaining"));
        int total = Math.max(remaining, tag.getInt("acceleratorTicksTotal"));
        if (total <= 0) total = Math.max(track.phaseNodes().size(), 2400);
        int startIndex = phaseIndexForLegacyStage(track, legacyStage);
        StellarEvolutionState state = begin(
            track,
            startIndex,
            tag.contains("acceleratorOriginalMass") ? tag.getInt("acceleratorOriginalMass") : fallbackMass,
            tag.contains("acceleratorOriginalEnergy") ? tag.getInt("acceleratorOriginalEnergy") : fallbackEnergy,
            tag.contains("acceleratorOriginalSize") ? tag.getInt("acceleratorOriginalSize") : fallbackSize,
            fallbackMass,
            tag.contains("stellarTrackSeed") ? tag.getLong("stellarTrackSeed") : 0L,
            now - Math.max(0, total - remaining),
            total
        );
        state.eventTriggered = tag.getBoolean("quenchedSupernovaFired");
        return state;
    }

    private static int phaseIndexForLegacyStage(StellarTrack track, int legacyStage) {
        int wanted = legacyStage <= 0 ? 0 : legacyStage;
        for (int i = 0; i < track.phaseNodes().size(); i++) {
            if (track.phaseNodes().get(i).phaseId().legacyStage() >= wanted) return i;
        }
        return Math.max(0, track.phaseNodes().size() - 1);
    }

    /** 绑定轨道并重建阶段时长缓存。 */
    public void attachTrack(StellarTrack track) {
        track = effectiveTrack(track);
        if (trackSnapshot == null) {
            trackSnapshot = track;
            captureProfiles(track);
        }
        if (!track.trackId().equals(this.trackId) && !this.trackId.isEmpty()) return;
        this.trackId = track.trackId();
        this.scheduleStartIndex = Math.clamp(this.scheduleStartIndex, 0, track.phaseNodes().size() - 1);
        if (!track.trackId().equals(this.cachedTrackId)
            || this.cachedDurationBudget != this.totalDurationTicks
            || this.cachedScheduleStartIndex != this.scheduleStartIndex
            || this.phaseDurations.isEmpty()) {
            this.phaseDurations = durationsFrom(track, this.scheduleStartIndex, this.totalDurationTicks);
            this.totalDurationTicks = phaseDurations.stream().mapToInt(Integer::intValue).sum();
            this.cachedTrackId = track.trackId();
            this.cachedDurationBudget = this.totalDurationTicks;
            this.cachedScheduleStartIndex = this.scheduleStartIndex;
        }
        if (this.phaseIndex >= track.phaseNodes().size()) this.phaseIndex = track.phaseNodes().size() - 1;
        if (this.phaseId.isEmpty()) this.phaseId = track.phaseNodes().get(this.phaseIndex).phaseId().getSerializedName();
        if (this.terminalProfileId.isEmpty()) this.terminalProfileId = track.terminalProfile();
        if (this.eventStartGameTime < 0L && track.phaseNodes().get(this.phaseIndex).hasEventProfile()) {
            this.eventStartGameTime = this.phaseStartGameTime;
            this.eventSeed = mixSeed(this.trackSeed, this.phaseIndex);
            this.eventId = track.trackId() + ":" + this.phaseId + ":" + this.phaseStartGameTime;
        }
    }

    /** 资源包移除旧轨道时切换到确定性内置回退，并保留当前时间轴。 */
    public void rebindTrack(StellarTrack track) {
        if (modern()) throw new IllegalStateException("Cannot replace a running stellar snapshot");
        trackSnapshot = track;
        this.trackId = track.trackId();
        this.terminalProfileId = track.terminalProfile();
        this.cachedTrackId = "";
        this.phaseDurations = List.of();
        attachTrack(track);
    }

    /**
     * 把总预算分配到各阶段。
     *
     * <p>爆发窗口（{@link StellarEvolutionPhase#isEventPhase()}）使用 profile 里的绝对
     * 刻数而不是权重份额：核心坍缩必须在闪光之前完成收缩，残骸必须紧跟闪光出现，这两个
     * 时长不能随总预算一起放大。剩余预算按权重分配给正常演化阶段，因此总时长仍然等于旧
     * 算法给出的预算。只有预算连爆发窗口都装不下时才等比压缩爆发窗口。</p>
     */
    private static List<Integer> durationsFrom(StellarTrack track, int startIndex, int budget) {
        List<PhaseNode> nodes = track.phaseNodes().subList(startIndex, track.phaseNodes().size());
        if (track.modern()) return modernDurations(nodes, budget);
        int safeBudget = Math.max(nodes.size(), budget);
        int count = nodes.size();
        int[] ticks = new int[count];
        boolean[] absolute = new boolean[count];
        int absoluteTotal = 0;
        int weightedCount = 0;
        float weight = 0.0f;
        for (int index = 0; index < count; index++) {
            PhaseNode node = nodes.get(index);
            int eventTicks = eventWindowTicks(node);
            if (eventTicks > 0) {
                absolute[index] = true;
                ticks[index] = eventTicks;
                absoluteTotal += eventTicks;
            } else {
                weightedCount++;
                weight += node.durationWeight();
            }
        }
        int maximumAbsolute = Math.max(0, safeBudget - weightedCount);
        if (absoluteTotal > maximumAbsolute) {
            float shrink = maximumAbsolute / (float) absoluteTotal;
            absoluteTotal = 0;
            for (int index = 0; index < count; index++) {
                if (!absolute[index]) continue;
                ticks[index] = Math.max(1, Math.round(ticks[index] * shrink));
                absoluteTotal += ticks[index];
            }
        }
        int weightedBudget = Math.max(weightedCount, safeBudget - absoluteTotal);
        int used = absoluteTotal;
        for (int index = 0; index < count; index++) {
            if (absolute[index]) continue;
            ticks[index] = Math.max(1, Math.round(weightedBudget * nodes.get(index).durationWeight()
                / Math.max(weight, 0.001f)));
            used += ticks[index];
        }
        balanceDurations(ticks, absolute, used, safeBudget);
        List<Integer> result = new ArrayList<>(count);
        for (int value : ticks) result.add(value);
        return List.copyOf(result);
    }

    private static List<Integer> modernDurations(List<PhaseNode> nodes, int budget) {
        int minimum = nodes.stream().mapToInt(node -> node.dynamics().pulses()).sum();
        int remaining = Math.max(minimum, budget) - minimum;
        double weight = nodes.stream().mapToDouble(PhaseNode::durationWeight).sum();
        int[] ticks = new int[nodes.size()];
        int used = 0;
        for (int index = 0; index < nodes.size(); index++) {
            ticks[index] = nodes.get(index).dynamics().pulses()
                + (int) Math.floor(remaining * nodes.get(index).durationWeight() / weight);
            used += ticks[index];
        }
        for (int index = 0; used < Math.max(minimum, budget); index = (index + 1) % ticks.length) {
            ticks[index]++;
            used++;
        }
        return java.util.Arrays.stream(ticks).boxed().toList();
    }

    private void validateSnapshot() {
        if (trackSnapshot == null) return;
        if (!trackSnapshot.trackId().equals(trackId) || trackSnapshot.modern() != modern()
            || scheduleStartIndex < 0 || phaseIndex < scheduleStartIndex || phaseIndex >= trackSnapshot.phaseNodes().size()
            || phaseDurations.size() != trackSnapshot.phaseNodes().size() - scheduleStartIndex
            || phaseDurations.stream().anyMatch(value -> value < 1)
            || phaseDurations.stream().mapToLong(Integer::longValue).sum() != totalDurationTicks) {
            throw new IllegalArgumentException("Invalid saved stellar timeline");
        }
        if (modern() && !trackSnapshot.phaseNodes().get(phaseIndex).nodeId().equals(nodeId)) {
            throw new IllegalArgumentException("Saved stellar node ID/index disagree");
        }
        if (modern() && (trackSnapshot.definition().massAnvils() != initialMass
            || initialSolarMass != CelestialMassTable.at(initialMass).solarMass()
            || !Double.isFinite(currentSolarMass) || currentSolarMass < 0 || currentSolarMass > initialSolarMass)) {
            throw new IllegalArgumentException("Invalid saved stellar mass");
        }
        for (PhaseNode node : trackSnapshot.phaseNodes()) {
            if (node.hasEventProfile() && !profileSnapshot.containsKey(node.eventProfileId())) {
                throw new IllegalArgumentException("Missing saved event profile");
            }
        }
        for (Map.Entry<String, StellarEventProfile> entry : profileSnapshot.entrySet()) {
            if (!entry.getKey().equals(entry.getValue().profileId())) throw new IllegalArgumentException("Mismatched saved event profile");
        }
        Set<String> ids = new LinkedHashSet<>();
        for (StellarScheduledEvent event : eventPlan) {
            if (!ids.add(event.instanceId()) || !profileSnapshot.containsKey(event.profileId())
                || event.endOffset() > totalDurationTicks) throw new IllegalArgumentException("Invalid saved stellar event");
            trackSnapshot.nodeIndex(event.nodeId());
        }
        if (!ids.containsAll(appliedEvents)) throw new IllegalArgumentException("Unknown completed stellar event");
        if (modern()) {
            List<StellarScheduledEvent> savedPlan = eventPlan;
            createEventPlan(trackSnapshot);
            if (!eventPlan.equals(savedPlan)) throw new IllegalArgumentException("Saved stellar event plan disagrees with snapshot");
        }
    }

    /** 事件阶段的绝对时长；非事件阶段返回 0 表示按权重分配。 */
    private static int eventWindowTicks(PhaseNode node) {
        if (!node.hasEventProfile() || !node.phaseId().isEventPhase()) return 0;
        StellarEventProfile profile = StellarTrackLibrary.legacyEventProfile(node.eventProfileId());
        if (profile == null) return 0;
        return switch (node.phaseId()) {
            case EVENT_PRELUDE -> Math.max(1, profile.precursorTicks());
            case EVENT_COLLAPSE -> Math.max(1, profile.collapseTicks());
            case EVENT_EJECTA -> Math.max(1, profile.ejectaTicks());
            default -> Math.max(1, profile.fadeTicks());
        };
    }

    /** 把总和修正到恰好等于预算：优先增减正常阶段，尽量保住爆发窗口的绝对时长。 */
    private static void balanceDurations(int[] ticks, boolean[] absolute, int used, int budget) {
        int total = used;
        int count = ticks.length;
        if (count == 0) return;
        for (int pass = 0; pass < 2 && total != budget; pass++) {
            boolean allowAbsolute = pass == 1;
            boolean changed = true;
            while (total != budget && changed) {
                changed = false;
                for (int index = 0; index < count && total != budget; index++) {
                    if (absolute[index] && !allowAbsolute) continue;
                    if (total > budget && ticks[index] > 1) {
                        ticks[index]--;
                        total--;
                        changed = true;
                    } else if (total < budget) {
                        ticks[index]++;
                        total++;
                        changed = true;
                    }
                }
            }
        }
    }

    private int durationAt(int absoluteIndex) {
        int relative = absoluteIndex - scheduleStartIndex;
        if (relative < 0 || relative >= phaseDurations.size()) return Math.max(1, phaseDurationTicks);
        return Math.max(1, phaseDurations.get(relative));
    }

    /** 推进到绝对游戏刻，返回是否发生了阶段边界。 */
    public boolean update(long gameTime, StellarTrack track) {
        track = effectiveTrack(track);
        attachTrack(track);
        boolean changed = false;
        if (track.phaseNodes().isEmpty() || isComplete()) return false;
        while (phaseIndex < track.phaseNodes().size() - 1
            && gameTime >= phaseStartGameTime + phaseDurationTicks) {
            phaseStartGameTime += phaseDurationTicks;
            phaseIndex++;
            phaseDurationTicks = durationAt(phaseIndex);
            phaseId = track.phaseNodes().get(phaseIndex).phaseId().getSerializedName();
            PhaseNode currentNode = track.phaseNodes().get(phaseIndex);
            PhaseNode previousNode = track.phaseNodes().get(phaseIndex - 1);
            boolean startsNewEvent = currentNode.hasEventProfile()
                && (!previousNode.hasEventProfile()
                    || !previousNode.eventProfileId().equalsIgnoreCase(currentNode.eventProfileId()));
            if (startsNewEvent) {
                eventStartGameTime = phaseStartGameTime;
                eventSeed = mixSeed(trackSeed, phaseIndex);
                eventId = track.trackId() + ":" + phaseId + ":" + eventStartGameTime;
                eventTriggered = false;
            }
            changed = true;
        }
        nodeId = track.phaseNodes().get(phaseIndex).nodeId();
        updateProgress(gameTime);
        if (modern()) {
            StellarVisualState visual = structuralVisualState(track, gameTime, 0);
            double remnant = track.definition().terminal().solarMass();
            float initialEnvelope = track.phaseNodes().get(scheduleStartIndex).dynamics().points().getFirst().envelope();
            currentSolarMass = Math.clamp(remnant + (initialSolarMass - remnant)
                * Math.clamp(visual.envelopeOpacity() / Math.max(0.001f, initialEnvelope), 0, 1), remnant, initialSolarMass);
            StellarScheduledEvent event = eventAt(elapsedTicks(gameTime));
            eventId = event == null ? "" : event.instanceId();
            eventStartGameTime = event == null ? -1 : totalStartGameTime + event.startOffset();
            eventSeed = event == null ? 0 : event.seed();
            eventTriggered = event != null && appliedEvents.contains(event.instanceId());
        }
        return changed;
    }

    private void updateProgress(long gameTime) {
        if (phaseDurationTicks <= 0) {
            phaseProgress = 1.0f;
        } else {
            phaseProgress = Math.clamp(
                (float) (gameTime - phaseStartGameTime) / phaseDurationTicks,
                0.0f,
                1.0f
            );
        }
    }

    public boolean isActive() {
        return !trackId.isEmpty() && !terminalApplied;
    }

    public boolean isComplete() {
        if (terminalApplied) return true;
        if (phaseDurations.isEmpty()) return false;
        return phaseIndex >= scheduleStartIndex && phaseIndex >= phaseCount() - 1
            && phaseProgress >= 1.0f;
    }

    private int phaseCount() {
        return scheduleStartIndex + phaseDurations.size();
    }

    public String trackId() {
        return trackId;
    }

    public String phaseId() {
        return phaseId;
    }

    public int phaseIndex() {
        return phaseIndex;
    }

    public int scheduleStartIndex() {
        return scheduleStartIndex;
    }

    public long phaseStartGameTime() {
        return phaseStartGameTime;
    }

    public int phaseDurationTicks() {
        return phaseDurationTicks;
    }

    public List<Integer> phaseDurations(StellarTrack track) {
        attachTrack(track);
        return List.copyOf(phaseDurations);
    }

    public PhaseNode currentNode(StellarTrack track) {
        track = effectiveTrack(track);
        int index = Math.clamp(phaseIndex, 0, track.phaseNodes().size() - 1);
        return track.phaseNodes().get(index);
    }

    public float envelopeFraction(StellarTrack track) {
        return currentNode(track).envelopeFraction();
    }

    public float massLossBudget(StellarTrack track) {
        return currentNode(track).massLossBudget();
    }

    public long totalStartGameTime() {
        return totalStartGameTime;
    }

    public int totalDurationTicks() {
        return totalDurationTicks;
    }

    public float phaseProgress() {
        return phaseProgress;
    }

    /** 按绝对时间计算客户端当前阶段进度。 */
    public float phaseProgressAt(StellarTrack track, long gameTime, float partialTick) {
        track = effectiveTrack(track);
        attachTrack(track);
        return sampleAtAbsoluteTime(track, gameTime, partialTick).progress();
    }

    /** 按绝对时间返回客户端当前阶段 ID。 */
    public StellarEvolutionPhase phaseAt(StellarTrack track, long gameTime, float partialTick) {
        track = effectiveTrack(track);
        attachTrack(track);
        return sampleAtAbsoluteTime(track, gameTime, partialTick).node().phaseId();
    }

    public float totalProgress(long gameTime, float partialTick) {
        if (totalDurationTicks <= 0) return 1.0f;
        float frame = frameFraction(partialTick);
        return Math.clamp(
            (float) (gameTime - totalStartGameTime + frame) / totalDurationTicks,
            0.0f,
            1.0f
        );
    }

    public float progressAt(long gameTime, float partialTick) {
        return totalProgress(gameTime, partialTick);
    }

    public long elapsedTicks(long gameTime) {
        return Math.max(0L, gameTime - totalStartGameTime);
    }

    public int initialMass() {
        return initialMass;
    }

    public int initialEnergy() {
        return initialEnergy;
    }

    public int initialSize() {
        return initialSize;
    }

    public String initialSurfaceClass() {
        return initialSurfaceClass;
    }

    public int currentMass() {
        return currentMass;
    }

    public long trackSeed() {
        return trackSeed;
    }

    public int metallicityVariant() {
        return metallicityVariant;
    }

    public int rotationVariant() {
        return rotationVariant;
    }

    public int binaryVariant() {
        return binaryVariant;
    }

    public String terminalProfileId() {
        return terminalProfileId;
    }

    public String eventId() {
        return eventId;
    }

    public long eventStartGameTime() {
        return eventStartGameTime;
    }

    public long eventSeed() {
        return eventSeed;
    }

    public boolean eventTriggered() {
        return eventTriggered;
    }

    public boolean terminalApplied() {
        return terminalApplied;
    }

    public boolean isRunning() {
        return isActive();
    }

    public void markEventTriggered() {
        this.eventTriggered = true;
    }

    public void markTerminalApplied() {
        this.terminalApplied = true;
    }

    /** 将整个时间轴向后平移，用于增幅器断开期间暂停演化。 */
    public void shiftTimeline(long ticks) {
        if (ticks == 0L) return;
        this.phaseStartGameTime += ticks;
        this.totalStartGameTime += ticks;
        if (this.eventStartGameTime >= 0L) this.eventStartGameTime += ticks;
    }

    /** 当前阶段是否是会产生视觉 profile 的阶段。 */
    public boolean hasVisualEvent(StellarTrack track) {
        track = effectiveTrack(track);
        return phaseIndex >= 0 && phaseIndex < track.phaseNodes().size()
            && track.phaseNodes().get(phaseIndex).hasEventProfile();
    }

    public String currentEventProfileId(StellarTrack track) {
        if (hasVisualEvent(track)) return track.phaseNodes().get(phaseIndex).eventProfileId();
        return terminalProfileId;
    }

    /** 按绝对时间返回当前视觉事件，供客户端在网络心跳之间及时显示事件层。 */
    public String eventProfileAt(StellarTrack track, long gameTime, float partialTick) {
        track = effectiveTrack(track);
        attachTrack(track);
        TimedPhaseSample timed = sampleAtAbsoluteTime(track, gameTime, partialTick);
        return timed.node().hasEventProfile() ? timed.node().eventProfileId() : "";
    }

    /**
     * 从当前节点采样视觉快照。
     *
     * <p>节点数值是本阶段的目标状态，因此插值起点取上一节点：阶段内部完成过渡，
     * 阶段结束时正好到达本节点。脉动按阶段进度叠加在事件核心之后。</p>
     */
    public StellarVisualState visualState(StellarTrack track, long gameTime, float partialTick) {
        track = effectiveTrack(track);
        attachTrack(track);
        if (track.phaseNodes().isEmpty()) return StellarVisualState.DEFAULT;
        TimedPhaseSample timed = sampleAtAbsoluteTime(track, gameTime, partialTick);
        PhaseNode node = timed.node();
        float progress = timed.progress();
        StellarVisualState state = node.dynamics().sample(node, timed.previousNode(), progress);
        String profileId = node.hasEventProfile() ? node.eventProfileId() : "";
        if (!profileId.isBlank()) {
            StellarEventProfile profile = eventProfile(profileId);
            float eventProgress = eventProgressAt(track, timed, gameTime, frameFraction(partialTick), profileId);
            int surfaceColor = state.surfaceColor();
            state = applyEvent(state, profile, eventProgress);
            if (modern()) state = state.withSurfaceColor(surfaceColor);
        }
        return state.withVisualRadius(state.radiusAt(progress)).withWind(node.dynamics().wind(), progress);
    }

    /**
     * 返回只包含轨道结构半径的快照。事件核心收缩、抛射壳和周期脉动留给恒星本体，
     * 束星环使用此方法即可避免跟随快速的表面效果抖动。
     */
    public StellarVisualState structuralVisualState(StellarTrack track, long gameTime, float partialTick) {
        track = effectiveTrack(track);
        attachTrack(track);
        if (track.phaseNodes().isEmpty()) return StellarVisualState.DEFAULT;
        TimedPhaseSample timed = sampleAtAbsoluteTime(track, gameTime, partialTick);
        return timed.node().dynamics().sample(timed.node(), timed.previousNode(), timed.progress())
            .withWind(timed.node().dynamics().wind(), timed.progress());
    }

    private static float frameFraction(float partialTick) {
        return Float.isFinite(partialTick) ? Math.clamp(partialTick, 0.0f, 1.0f) : 0.0f;
    }

    private float eventProgressAt(
        StellarTrack track,
        TimedPhaseSample timed,
        long gameTime,
        float partialTick,
        String profileId
    ) {
        if (modern()) {
            StellarScheduledEvent event = eventAt(gameTime - totalStartGameTime + partialTick);
            return event == null ? 0 : event.progress(gameTime - totalStartGameTime + partialTick);
        }
        long start = timed.phaseOffset();
        for (int previous = timed.index() - 1; previous >= scheduleStartIndex; previous--) {
            PhaseNode node = track.phaseNodes().get(previous);
            if (!node.hasEventProfile() || !node.eventProfileId().equalsIgnoreCase(profileId)) break;
            start -= durationAt(previous);
        }
        long end = timed.phaseOffset() + timed.duration();
        for (int next = timed.index() + 1; next < track.phaseNodes().size(); next++) {
            PhaseNode node = track.phaseNodes().get(next);
            if (!node.hasEventProfile() || !node.eventProfileId().equalsIgnoreCase(profileId)) break;
            end += durationAt(next);
        }
        if (end <= start) return 1.0f;
        long elapsed = Math.max(0L, gameTime - totalStartGameTime);
        return Math.clamp((float) (elapsed - start + partialTick) / (float) (end - start), 0.0f, 1.0f);
    }

    /**
     * 按绝对时间定位视觉阶段，不依赖客户端最近一次收到的 phaseIndex。
     * 这样网络同步仍可保持低频，渲染却能在阶段边界连续前进。
     */
    private TimedPhaseSample sampleAtAbsoluteTime(StellarTrack track, long gameTime, float partialTick) {
        track = effectiveTrack(track);
        long elapsed = Math.max(0L, gameTime - totalStartGameTime);
        long clampedElapsed = Math.min((long) totalDurationTicks, elapsed);
        float frame = Float.isFinite(partialTick) ? Math.clamp(partialTick, 0.0f, 1.0f) : 0.0f;
        long cursor = 0L;
        int lastRelative = Math.max(0, phaseDurations.size() - 1);
        for (int relative = 0; relative < phaseDurations.size(); relative++) {
            int absolute = scheduleStartIndex + relative;
            int duration = durationAt(absolute);
            long end = cursor + duration;
            if (clampedElapsed < end || relative == lastRelative) {
                float progress = Math.clamp((float) (clampedElapsed - cursor + frame) / duration, 0.0f, 1.0f);
                int index = Math.clamp(absolute, 0, track.phaseNodes().size() - 1);
                PhaseNode node = track.phaseNodes().get(index);
                PhaseNode previous = track.phaseNodes().get(Math.max(scheduleStartIndex, index - 1));
                PhaseNode next = index + 1 < track.phaseNodes().size()
                    ? track.phaseNodes().get(index + 1) : node;
                return new TimedPhaseSample(index, cursor, duration, progress, previous, node, next);
            }
            cursor = end;
        }
        int last = Math.max(0, track.phaseNodes().size() - 1);
        PhaseNode node = track.phaseNodes().get(last);
        PhaseNode previous = track.phaseNodes().get(Math.max(scheduleStartIndex, last - 1));
        return new TimedPhaseSample(last, cursor, durationAt(last), 1.0f, previous, node, node);
    }

    private record TimedPhaseSample(
        int index,
        long phaseOffset,
        int duration,
        float progress,
        PhaseNode previousNode,
        PhaseNode node,
        PhaseNode nextNode
    ) {
    }

    private static StellarVisualState applyEvent(
        StellarVisualState base,
        StellarEventProfile profile,
        float progress
    ) {
        float core = Math.max(0.05f, profile.coreRadius(progress));
        float ejecta = Math.max(0.0f, profile.ejectaRadius(progress));
        int eventColor = profile.color(progress);
        return new StellarVisualState(
            base.radius() * core,
            base.temperature(),
            base.luminosity(),
            StellarVisualState.interpolateColor(base.surfaceColor(), eventColor, Math.min(1.0f, progress * 2.0f)),
            Math.max(base.emission(), profile.emission(progress)),
            base.envelopeOpacity(),
            base.coreRadius() * core,
            base.radius() * ejecta,
            base.pulsationAmplitude(),
            base.pulsationFrequency(),
            base.surfaceStyle()
        );
    }

    /** 在 profile 的冲击突破里程碑处只返回一次 true。 */
    public boolean shouldTriggerShock(long gameTime, StellarTrack track) {
        track = effectiveTrack(track);
        if (eventTriggered || !hasVisualEvent(track)) return false;
        String profileId = currentEventProfileId(track);
        if (profileId == null || profileId.isBlank()) return false;
        StellarEventProfile profile = eventProfile(profileId);
        long eventStart = eventStartGameTime < 0L ? phaseStartGameTime : eventStartGameTime;
        long eventEnd = eventEndGameTime(track);
        long available = Math.max(1L, eventEnd - eventStart);
        long shockTick = eventStart + Math.min(profile.shockBreakoutTick(), Math.max(1L, available - 1L));
        return gameTime >= shockTick;
    }

    /** 当前事件在其实际轨道窗口内的归一化进度。 */
    public float eventProgress(
        StellarTrack track,
        long gameTime,
        float partialTick,
        StellarEventProfile profile
    ) {
        attachTrack(track);
        TimedPhaseSample timed = sampleAtAbsoluteTime(track, gameTime, partialTick);
        String profileId = timed.node().hasEventProfile() ? timed.node().eventProfileId() : "";
        if (profileId.isBlank() || !profileId.equalsIgnoreCase(profile.profileId())) return 0.0f;
        return eventProgressAt(track, timed, gameTime, frameFraction(partialTick), profileId);
    }

    private long eventEndGameTime(StellarTrack track) {
        if (!hasVisualEvent(track)) return phaseStartGameTime + phaseDurationTicks;
        String profileId = currentEventProfileId(track);
        long end = phaseStartGameTime + phaseDurationTicks;
        for (int index = phaseIndex + 1; index < track.phaseNodes().size(); index++) {
            PhaseNode node = track.phaseNodes().get(index);
            if (!node.hasEventProfile() || !node.eventProfileId().equalsIgnoreCase(profileId)) break;
            end += durationAt(index);
        }
        return end;
    }

    /** 返回终局 profile 的冲击突破绝对游戏刻，用于音乐和客户端预告对齐。 */
    public long terminalShockGameTime(StellarTrack track) {
        track = effectiveTrack(track);
        attachTrack(track);
        if (modern()) {
            for (StellarScheduledEvent event : eventPlan) {
                if (event.policy().destructive()) return totalStartGameTime + event.shockOffset();
            }
            return totalStartGameTime + totalDurationTicks;
        }
        long cursor = totalStartGameTime;
        for (int index = scheduleStartIndex; index < track.phaseNodes().size(); index++) {
            PhaseNode node = track.phaseNodes().get(index);
            int duration = durationAt(index);
            if (node.hasEventProfile() && node.eventProfileId().equalsIgnoreCase(track.terminalProfile())) {
                long eventEnd = cursor + duration;
                for (int next = index + 1; next < track.phaseNodes().size(); next++) {
                    PhaseNode continuation = track.phaseNodes().get(next);
                    if (!continuation.hasEventProfile()
                        || !continuation.eventProfileId().equalsIgnoreCase(node.eventProfileId())) break;
                    eventEnd += durationAt(next);
                }
                long available = Math.max(1L, eventEnd - cursor);
                return cursor + Math.min(
                    eventProfile(node.eventProfileId()).shockBreakoutTick(),
                    Math.max(1L, available - 1L)
                );
            }
            cursor += duration;
        }
        return totalStartGameTime + totalDurationTicks;
    }

    public void save(CompoundTag tag) {
        tag.putInt("stellarFormatVersion", formatVersion);
        tag.putString("stellarNodeId", nodeId);
        tag.putDouble("stellarInitialSolarMass", initialSolarMass);
        tag.putDouble("stellarCurrentSolarMass", currentSolarMass);
        tag.putDouble("stellarMetallicityZ", metallicityZ > 0 ? metallicityZ : StellarMetallicity.DEFAULT.at(metallicityCoordinate));
        tag.putDouble("stellarMetallicityCoordinate", metallicityCoordinate);
        if (trackSnapshot != null) {
            tag.put("stellarTrackSnapshot", StellarTrack.CODEC.encodeStart(NbtOps.INSTANCE, trackSnapshot).getOrThrow());
            tag.put("stellarProfileSnapshot", Codec.unboundedMap(Codec.STRING, StellarEventProfile.CODEC)
                .encodeStart(NbtOps.INSTANCE, profileSnapshot).getOrThrow());
            tag.putIntArray("stellarPhaseDurations", phaseDurations);
            tag.put("stellarEventPlan", StellarScheduledEvent.CODEC.listOf().encodeStart(NbtOps.INSTANCE, eventPlan).getOrThrow());
            tag.put("stellarAppliedEvents", Codec.STRING.listOf()
                .encodeStart(NbtOps.INSTANCE, List.copyOf(appliedEvents)).getOrThrow());
        }
        tag.putString(TRACK_ID_KEY, trackId);
        tag.putString(PHASE_ID_KEY, phaseId);
        tag.putInt(PHASE_INDEX_KEY, phaseIndex);
        tag.putLong(PHASE_START_KEY, phaseStartGameTime);
        tag.putInt(PHASE_DURATION_KEY, phaseDurationTicks);
        tag.putLong(TOTAL_START_KEY, totalStartGameTime);
        tag.putInt(TOTAL_DURATION_KEY, totalDurationTicks);
        tag.putFloat(PHASE_PROGRESS_KEY, phaseProgress);
        tag.putInt(SCHEDULE_START_INDEX_KEY, scheduleStartIndex);
        tag.putInt(INITIAL_MASS_KEY, initialMass);
        tag.putInt(INITIAL_ENERGY_KEY, initialEnergy);
        tag.putInt(INITIAL_SIZE_KEY, initialSize);
        tag.putString(INITIAL_SURFACE_CLASS_KEY, initialSurfaceClass);
        tag.putInt(CURRENT_MASS_KEY, currentMass);
        tag.putLong(TRACK_SEED_KEY, trackSeed);
        tag.putInt(METALLICITY_VARIANT_KEY, metallicityVariant);
        tag.putInt(ROTATION_VARIANT_KEY, rotationVariant);
        tag.putInt(BINARY_VARIANT_KEY, binaryVariant);
        tag.putString(TERMINAL_PROFILE_KEY, terminalProfileId);
        tag.putString(EVENT_ID_KEY, eventId);
        tag.putLong(EVENT_START_KEY, eventStartGameTime);
        tag.putLong(EVENT_SEED_KEY, eventSeed);
        tag.putBoolean(EVENT_TRIGGERED_KEY, eventTriggered);
        tag.putBoolean(TERMINAL_APPLIED_KEY, terminalApplied);
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        save(tag);
        return tag;
    }

    public void writeUpdateTag(CompoundTag tag) {
        save(tag);
    }

    public void readUpdateTag(CompoundTag tag) {
        StellarEvolutionState loaded = fromTag(tag);
        this.copyFrom(loaded);
    }

    private void copyFrom(StellarEvolutionState loaded) {
        this.trackId = loaded.trackId;
        this.phaseId = loaded.phaseId;
        this.phaseIndex = loaded.phaseIndex;
        this.phaseStartGameTime = loaded.phaseStartGameTime;
        this.phaseDurationTicks = loaded.phaseDurationTicks;
        this.totalStartGameTime = loaded.totalStartGameTime;
        this.totalDurationTicks = loaded.totalDurationTicks;
        this.phaseProgress = loaded.phaseProgress;
        this.initialMass = loaded.initialMass;
        this.initialEnergy = loaded.initialEnergy;
        this.initialSize = loaded.initialSize;
        this.initialSurfaceClass = loaded.initialSurfaceClass;
        this.currentMass = loaded.currentMass;
        this.trackSeed = loaded.trackSeed;
        this.metallicityVariant = loaded.metallicityVariant;
        this.rotationVariant = loaded.rotationVariant;
        this.binaryVariant = loaded.binaryVariant;
        this.terminalProfileId = loaded.terminalProfileId;
        this.eventId = loaded.eventId;
        this.eventStartGameTime = loaded.eventStartGameTime;
        this.eventSeed = loaded.eventSeed;
        this.eventTriggered = loaded.eventTriggered;
        this.terminalApplied = loaded.terminalApplied;
        this.formatVersion = loaded.formatVersion;
        this.nodeId = loaded.nodeId;
        this.initialSolarMass = loaded.initialSolarMass;
        this.currentSolarMass = loaded.currentSolarMass;
        this.metallicityZ = loaded.metallicityZ;
        this.metallicityCoordinate = loaded.metallicityCoordinate;
        this.trackSnapshot = loaded.trackSnapshot;
        this.profileSnapshot = loaded.profileSnapshot;
        this.eventPlan = loaded.eventPlan;
        this.appliedEvents = new LinkedHashSet<>(loaded.appliedEvents);
        this.phaseDurations = loaded.phaseDurations;
        this.cachedTrackId = loaded.cachedTrackId;
        this.cachedDurationBudget = loaded.cachedDurationBudget;
        this.cachedScheduleStartIndex = loaded.cachedScheduleStartIndex;
        this.scheduleStartIndex = loaded.scheduleStartIndex;
    }

    private static long mixSeed(long seed, long salt) {
        long value = seed + salt * 0x9E3779B97F4A7C15L;
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
}
