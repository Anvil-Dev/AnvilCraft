package dev.dubhe.anvilcraft.block.entity.celestial;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 恒星演化的可持久化状态机。
 *
 * <p>时钟使用世界绝对游戏刻，因此区块重载、客户端迟到数据包和服务器暂停都能
 * 重新收敛。阶段视觉数据由轨道采样，不会写回玩法用的 {@link StarData}。</p>
 */
public final class StellarEvolutionState {
    private static final int FORMAT_VERSION = 3;
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

    /** 与运行快照一同保存的阶段时长。 */
    private List<Integer> phaseDurations = List.of();
    private int scheduleStartIndex;
    private transient String cachedTrackId = "";
    private transient int cachedDurationBudget = -1;
    private transient int cachedScheduleStartIndex = -1;

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
        if (track.definition().massAnvils() != initialMass) {
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

    @Nullable
    public StellarTrack trackSnapshot() {
        return this.trackSnapshot;
    }

    public String nodeId() {
        return this.nodeId;
    }

    public double initialSolarMass() {
        return this.initialSolarMass;
    }

    public double currentSolarMass() {
        return this.currentSolarMass;
    }

    public double metallicityZ() {
        return this.metallicityZ;
    }

    public double metallicityCoordinate() {
        return this.metallicityCoordinate;
    }

    public List<StellarScheduledEvent> eventPlan() {
        return this.eventPlan;
    }

    public Set<String> appliedEvents() {
        return Set.copyOf(this.appliedEvents);
    }

    public StellarEventProfile eventProfile(String id) {
        StellarEventProfile profile = this.profileSnapshot.get(id);
        if (profile != null) return profile;
        throw new IllegalStateException("Missing frozen event profile: " + id);
    }

    @Nullable
    public StellarEventProfile findEventProfile(String id) {
        return this.profileSnapshot.get(id);
    }

    private StellarTrack effectiveTrack(StellarTrack track) {
        return this.trackSnapshot == null ? track : this.trackSnapshot;
    }

    private void captureProfiles(StellarTrack track) {
        Map<String, StellarEventProfile> profiles = new LinkedHashMap<>();
        for (PhaseNode node : track.phaseNodes()) {
            if (node.hasEventProfile()) {
                profiles.put(node.eventProfileId(), StellarTrackLibrary.eventProfile(node.eventProfileId()));
            }
        }
        this.profileSnapshot = Map.copyOf(profiles);
    }

    private void createEventPlan(StellarTrack track) {
        List<StellarScheduledEvent> events = new ArrayList<>();
        int offset = 0;
        for (int index = this.scheduleStartIndex; index < track.phaseNodes().size(); index++) {
            PhaseNode node = track.phaseNodes().get(index);
            int duration = this.durationAt(index);
            if (node.hasEventProfile()) {
                int pulses = node.dynamics().pulses();
                StellarEventProfile profile = this.eventProfile(node.eventProfileId());
                for (int pulse = 0; pulse < pulses; pulse++) {
                    int start = offset + duration * pulse / pulses;
                    int end = offset + duration * (pulse + 1) / pulses;
                    int shock = start + Math.min(end - start - 1,
                        Math.round((end - start) * profile.shockBreakoutTick() / (float) profile.totalTicks()));
                    events.add(new StellarScheduledEvent(this.trackId + ":" + node.nodeId() + ":" + pulse,
                        node.nodeId(), node.eventProfileId(), start, end, shock, pulse,
                        mixSeed(this.trackSeed, index * 31L + pulse), node.dynamics().eventPolicy()));
                }
            }
            offset += duration;
        }
        this.eventPlan = List.copyOf(events);
    }

    public List<StellarScheduledEvent> dueEvents(long gameTime) {
        if (this.terminalApplied) return List.of();
        long elapsed = this.elapsedTicks(gameTime);
        return this.eventPlan.stream().filter(event -> elapsed >= event.shockOffset()
            && !this.appliedEvents.contains(event.instanceId())).toList();
    }

    public void markEventApplied(StellarScheduledEvent event) {
        this.appliedEvents.add(event.instanceId());
    }

    @Nullable
    private StellarScheduledEvent eventAt(double elapsed) {
        for (StellarScheduledEvent event : this.eventPlan) {
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

    /** 从 NBT 恢复完整的运行快照。 */
    public static StellarEvolutionState fromTag(CompoundTag tag) {
        tag = migrateResultStage(tag);
        StellarEvolutionState state = new StellarEvolutionState();
        state.trackId = tag.getStringOr(TRACK_ID_KEY, "");
        state.phaseId = tag.getStringOr(PHASE_ID_KEY, "");
        state.phaseIndex = Math.max(0, tag.getIntOr(PHASE_INDEX_KEY, 0));
        state.scheduleStartIndex = tag.contains(SCHEDULE_START_INDEX_KEY)
            ? Math.max(0, tag.getIntOr(SCHEDULE_START_INDEX_KEY, 0))
            : state.phaseIndex;
        state.phaseStartGameTime = tag.getLongOr(PHASE_START_KEY, 0L);
        state.phaseDurationTicks = Math.max(1, tag.getIntOr(PHASE_DURATION_KEY, 0));
        state.totalStartGameTime = tag.getLongOr(TOTAL_START_KEY, 0L);
        state.totalDurationTicks = Math.max(1, tag.getIntOr(TOTAL_DURATION_KEY, 0));
        float loadedProgress = tag.getFloatOr(PHASE_PROGRESS_KEY, 0.0F);
        state.phaseProgress = Float.isFinite(loadedProgress) ? Math.clamp(loadedProgress, 0.0f, 1.0f) : 0.0f;
        state.initialMass = tag.getIntOr(INITIAL_MASS_KEY, 0);
        state.initialEnergy = tag.getIntOr(INITIAL_ENERGY_KEY, 0);
        state.initialSize = tag.getIntOr(INITIAL_SIZE_KEY, 0);
        state.initialSurfaceClass = tag.getStringOr(INITIAL_SURFACE_CLASS_KEY, "");
        state.currentMass = tag.contains(CURRENT_MASS_KEY) ? tag.getIntOr(CURRENT_MASS_KEY, 0) : state.initialMass;
        state.trackSeed = tag.getLongOr(TRACK_SEED_KEY, 0L);
        state.metallicityVariant = tag.getIntOr(METALLICITY_VARIANT_KEY, 0);
        state.rotationVariant = tag.getIntOr(ROTATION_VARIANT_KEY, 0);
        state.binaryVariant = tag.getIntOr(BINARY_VARIANT_KEY, 0);
        state.terminalProfileId = tag.getStringOr(TERMINAL_PROFILE_KEY, "");
        state.eventId = tag.getStringOr(EVENT_ID_KEY, "");
        state.eventStartGameTime = tag.contains(EVENT_START_KEY) ? tag.getLongOr(EVENT_START_KEY, 0L) : -1L;
        state.eventSeed = tag.getLongOr(EVENT_SEED_KEY, 0L);
        state.eventTriggered = tag.getBooleanOr(EVENT_TRIGGERED_KEY, false);
        state.terminalApplied = tag.getBooleanOr(TERMINAL_APPLIED_KEY, false);
        if (tag.getIntOr("stellarFormatVersion", 0) != FORMAT_VERSION) {
            throw new IllegalArgumentException("Unsupported stellar state version");
        }
        state.nodeId = tag.getStringOr("stellarNodeId", "");
        state.initialSolarMass = tag.contains("stellarInitialSolarMass") ? tag.getDoubleOr("stellarInitialSolarMass", 0.0)
            : state.initialMass > 0 ? CelestialMassTable.at(state.initialMass).solarMass() : 0;
        state.currentSolarMass = tag.contains("stellarCurrentSolarMass")
            ? tag.getDoubleOr("stellarCurrentSolarMass", 0.0) : state.initialSolarMass;
        if (!tag.contains(METALLICITY_VARIANT_KEY)) state.metallicityVariant = StellarTrackLibrary.variant(state.trackSeed, 0) & 3;
        state.metallicityCoordinate = tag.contains("stellarMetallicityCoordinate")
            ? tag.getDoubleOr("stellarMetallicityCoordinate", 0.0) : state.metallicityVariant / 1.5 - 1;
        state.metallicityZ = tag.contains("stellarMetallicityZ") ? tag.getDoubleOr("stellarMetallicityZ", 0.0)
            : StellarMetallicity.DEFAULT.at(state.metallicityCoordinate);
        if (!Double.isFinite(state.metallicityZ) || state.metallicityZ <= 0
            || !Double.isFinite(state.metallicityCoordinate) || Math.abs(state.metallicityCoordinate) > 1) {
            throw new IllegalArgumentException("Invalid saved stellar metallicity");
        }
        if (tag.contains("stellarTrackSnapshot")) {
            state.trackSnapshot = StellarTrack.CODEC.parse(NbtOps.INSTANCE, tag.getCompoundOrEmpty("stellarTrackSnapshot")).getOrThrow();
            state.trackSnapshot.validate();
            state.profileSnapshot = Codec.unboundedMap(Codec.STRING, StellarEventProfile.CODEC)
                .parse(NbtOps.INSTANCE, tag.getCompoundOrEmpty("stellarProfileSnapshot")).getOrThrow();
            state.phaseDurations = java.util.Arrays.stream(tag.getIntArray("stellarPhaseDurations").orElseGet(() -> new int[0]))
                .boxed().toList();
            state.cachedTrackId = state.trackId;
            state.cachedDurationBudget = state.totalDurationTicks;
            state.cachedScheduleStartIndex = state.scheduleStartIndex;
            state.eventPlan = StellarScheduledEvent.CODEC.listOf().parse(NbtOps.INSTANCE,
                tag.get("stellarEventPlan")).getOrThrow();
            state.appliedEvents = new LinkedHashSet<>(Codec.STRING.listOf().parse(NbtOps.INSTANCE,
                tag.get("stellarAppliedEvents")).getOrThrow());
            state.validateSnapshot();
        } else {
            throw new IllegalArgumentException("Missing active stellar snapshot");
        }
        return state;
    }

    /** 修正格式 2 末尾的结果节点，只删除额外时长，不重算真实阶段或事件。 */
    private static CompoundTag migrateResultStage(CompoundTag original) {
        if (original.getIntOr("stellarFormatVersion", 0) != 2) return original;
        CompoundTag tag = original.copy();
        tag.putInt("stellarFormatVersion", 3);
        CompoundTag snapshot = tag.getCompoundOrEmpty("stellarTrackSnapshot");
        CompoundTag definition = snapshot.getCompoundOrEmpty("definition");
        definition.putInt("version", 3);
        String outcome = definition.getCompoundOrEmpty("terminal").getStringOr("kind", "");
        ListTag nodes = snapshot.getList("phaseNodes").filter(list -> list.stream().allMatch(CompoundTag.class::isInstance))
            .orElseGet(ListTag::new);
        int last = nodes.size() - 1;
        if (last < 0 || outcome.equals("keep") || !nodes.getCompoundOrEmpty(last).getStringOr("phaseId", "").equals(outcome)) return tag;
        int start = tag.getIntOr(SCHEDULE_START_INDEX_KEY, 0);
        int[] durations = tag.getIntArray("stellarPhaseDurations").orElseGet(() -> new int[0]);
        if (last < 1 || start >= last || durations.length != nodes.size() - start) {
            throw new IllegalArgumentException("Invalid result-stage migration timeline");
        }
        nodes.remove(last);
        int[] remaining = java.util.Arrays.copyOf(durations, durations.length - 1);
        tag.putIntArray("stellarPhaseDurations", remaining);
        tag.putInt(TOTAL_DURATION_KEY, java.util.Arrays.stream(remaining).sum());
        if (tag.getIntOr(PHASE_INDEX_KEY, 0) >= last) {
            CompoundTag finalPhase = nodes.getCompoundOrEmpty(last - 1);
            int finalDuration = remaining[remaining.length - 1];
            tag.putInt(PHASE_INDEX_KEY, last - 1);
            tag.putString(PHASE_ID_KEY, finalPhase.getStringOr("phaseId", ""));
            tag.putString("stellarNodeId", finalPhase.getStringOr("nodeId", ""));
            tag.putLong(PHASE_START_KEY, tag.getLongOr(TOTAL_START_KEY, 0L) + tag.getIntOr(TOTAL_DURATION_KEY, 0) - finalDuration);
            tag.putInt(PHASE_DURATION_KEY, finalDuration);
            tag.putFloat(PHASE_PROGRESS_KEY, 1);
        }
        return tag;
    }

    /** 绑定轨道并重建阶段时长缓存。 */
    public void attachTrack(StellarTrack track) {
        track = this.effectiveTrack(track);
        if (this.trackSnapshot == null) {
            this.trackSnapshot = track;
            this.captureProfiles(track);
        }
        if (!track.trackId().equals(this.trackId) && !this.trackId.isEmpty()) return;
        this.trackId = track.trackId();
        this.scheduleStartIndex = Math.clamp(this.scheduleStartIndex, 0, track.phaseNodes().size() - 1);
        if (!track.trackId().equals(this.cachedTrackId)
            || this.cachedDurationBudget != this.totalDurationTicks
            || this.cachedScheduleStartIndex != this.scheduleStartIndex
            || this.phaseDurations.isEmpty()) {
            this.phaseDurations = durationsFrom(track, this.scheduleStartIndex, this.totalDurationTicks);
            this.totalDurationTicks = this.phaseDurations.stream().mapToInt(Integer::intValue).sum();
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

    /** 按节点权重分配总预算，每次脉冲至少保留一刻。 */
    private static List<Integer> durationsFrom(StellarTrack track, int startIndex, int budget) {
        return allocateDurations(track.phaseNodes().subList(startIndex, track.phaseNodes().size()), budget);
    }

    private static List<Integer> allocateDurations(List<PhaseNode> nodes, int budget) {
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
        if (this.trackSnapshot == null) return;
        if (!this.trackSnapshot.trackId().equals(this.trackId)
            || this.scheduleStartIndex < 0 || this.phaseIndex < this.scheduleStartIndex
            || this.phaseIndex >= this.trackSnapshot.phaseNodes().size()
            || this.phaseDurations.size() != this.trackSnapshot.phaseNodes().size() - this.scheduleStartIndex
            || this.phaseDurations.stream().anyMatch(value -> value < 1)
            || this.phaseDurations.stream().mapToLong(Integer::longValue).sum() != this.totalDurationTicks) {
            throw new IllegalArgumentException("Invalid saved stellar timeline");
        }
        if (!this.trackSnapshot.phaseNodes().get(this.phaseIndex).nodeId().equals(this.nodeId)) {
            throw new IllegalArgumentException("Saved stellar node ID/index disagree");
        }
        if (this.trackSnapshot.definition().massAnvils() != this.initialMass
            || this.initialSolarMass != CelestialMassTable.at(this.initialMass).solarMass()
            || !Double.isFinite(this.currentSolarMass) || this.currentSolarMass < 0 || this.currentSolarMass > this.initialSolarMass) {
            throw new IllegalArgumentException("Invalid saved stellar mass");
        }
        for (PhaseNode node : this.trackSnapshot.phaseNodes()) {
            if (node.hasEventProfile() && !this.profileSnapshot.containsKey(node.eventProfileId())) {
                throw new IllegalArgumentException("Missing saved event profile");
            }
        }
        for (Map.Entry<String, StellarEventProfile> entry : this.profileSnapshot.entrySet()) {
            if (!entry.getKey().equals(entry.getValue().profileId())) throw new IllegalArgumentException("Mismatched saved event profile");
        }
        Set<String> ids = new LinkedHashSet<>();
        for (StellarScheduledEvent event : this.eventPlan) {
            if (!ids.add(event.instanceId()) || !this.profileSnapshot.containsKey(event.profileId())
                || event.endOffset() > this.totalDurationTicks) throw new IllegalArgumentException("Invalid saved stellar event");
            this.trackSnapshot.nodeIndex(event.nodeId());
        }
        if (!ids.containsAll(this.appliedEvents)) throw new IllegalArgumentException("Unknown completed stellar event");
        List<StellarScheduledEvent> savedPlan = this.eventPlan;
        this.createEventPlan(this.trackSnapshot);
        if (!this.eventPlan.equals(savedPlan)) throw new IllegalArgumentException("Saved stellar event plan disagrees with snapshot");
    }

    private int durationAt(int absoluteIndex) {
        int relative = absoluteIndex - this.scheduleStartIndex;
        if (relative < 0 || relative >= this.phaseDurations.size()) return Math.max(1, this.phaseDurationTicks);
        return Math.max(1, this.phaseDurations.get(relative));
    }

    /** 推进到绝对游戏刻，返回是否发生了阶段边界。 */
    public boolean update(long gameTime, StellarTrack track) {
        track = this.effectiveTrack(track);
        this.attachTrack(track);
        boolean changed = false;
        if (track.phaseNodes().isEmpty() || this.isComplete()) return false;
        while (this.phaseIndex < track.phaseNodes().size() - 1
            && gameTime >= this.phaseStartGameTime + this.phaseDurationTicks) {
            this.phaseStartGameTime += this.phaseDurationTicks;
            this.phaseIndex++;
            this.phaseDurationTicks = this.durationAt(this.phaseIndex);
            this.phaseId = track.phaseNodes().get(this.phaseIndex).phaseId().getSerializedName();
            changed = true;
        }
        this.nodeId = track.phaseNodes().get(this.phaseIndex).nodeId();
        this.updateProgress(gameTime);
        StellarVisualState visual = this.structuralVisualState(track, gameTime, 0);
        double remnant = track.definition().terminal().solarMass();
        float initialEnvelope = track.phaseNodes().get(this.scheduleStartIndex).dynamics().points().getFirst().envelope();
        this.currentSolarMass = Math.clamp(remnant + (this.initialSolarMass - remnant)
            * Math.clamp(visual.envelopeOpacity() / Math.max(0.001f, initialEnvelope), 0, 1), remnant, this.initialSolarMass);
        StellarScheduledEvent event = this.eventAt(this.elapsedTicks(gameTime));
        this.eventId = event == null ? "" : event.instanceId();
        this.eventStartGameTime = event == null ? -1 : this.totalStartGameTime + event.startOffset();
        this.eventSeed = event == null ? 0 : event.seed();
        this.eventTriggered = event != null && this.appliedEvents.contains(event.instanceId());
        return changed;
    }

    private void updateProgress(long gameTime) {
        if (this.phaseDurationTicks <= 0) {
            this.phaseProgress = 1.0f;
        } else {
            this.phaseProgress = Math.clamp(
                (float) (gameTime - this.phaseStartGameTime) / this.phaseDurationTicks,
                0.0f,
                1.0f
            );
        }
    }

    public boolean isActive() {
        return !this.trackId.isEmpty() && !this.terminalApplied;
    }

    public boolean isComplete() {
        if (this.terminalApplied) return true;
        if (this.phaseDurations.isEmpty()) return false;
        return this.phaseIndex >= this.scheduleStartIndex && this.phaseIndex >= this.phaseCount() - 1
            && this.phaseProgress >= 1.0f;
    }

    private int phaseCount() {
        return this.scheduleStartIndex + this.phaseDurations.size();
    }

    public String trackId() {
        return this.trackId;
    }

    public String phaseId() {
        return this.phaseId;
    }

    public int phaseIndex() {
        return this.phaseIndex;
    }

    public int scheduleStartIndex() {
        return this.scheduleStartIndex;
    }

    public long phaseStartGameTime() {
        return this.phaseStartGameTime;
    }

    public int phaseDurationTicks() {
        return this.phaseDurationTicks;
    }

    public List<Integer> phaseDurations(StellarTrack track) {
        this.attachTrack(track);
        return List.copyOf(this.phaseDurations);
    }

    public PhaseNode currentNode(StellarTrack track) {
        track = this.effectiveTrack(track);
        int index = Math.clamp(this.phaseIndex, 0, track.phaseNodes().size() - 1);
        return track.phaseNodes().get(index);
    }

    public float envelopeFraction(StellarTrack track) {
        return this.currentNode(track).envelopeFraction();
    }

    public float massLossBudget(StellarTrack track) {
        return this.currentNode(track).massLossBudget();
    }

    public long totalStartGameTime() {
        return this.totalStartGameTime;
    }

    public int totalDurationTicks() {
        return this.totalDurationTicks;
    }

    public float phaseProgress() {
        return this.phaseProgress;
    }

    /** 按绝对时间计算客户端当前阶段进度。 */
    public float phaseProgressAt(StellarTrack track, long gameTime, float partialTick) {
        track = this.effectiveTrack(track);
        this.attachTrack(track);
        return this.sampleAtAbsoluteTime(track, gameTime, partialTick).progress();
    }

    /** 按绝对时间返回客户端当前阶段 ID。 */
    public StellarEvolutionPhase phaseAt(StellarTrack track, long gameTime, float partialTick) {
        track = this.effectiveTrack(track);
        this.attachTrack(track);
        return this.sampleAtAbsoluteTime(track, gameTime, partialTick).node().phaseId();
    }

    public float totalProgress(long gameTime, float partialTick) {
        if (this.totalDurationTicks <= 0) return 1.0f;
        float frame = frameFraction(partialTick);
        return Math.clamp(
            (float) (gameTime - this.totalStartGameTime + frame) / this.totalDurationTicks,
            0.0f,
            1.0f
        );
    }

    public float progressAt(long gameTime, float partialTick) {
        return this.totalProgress(gameTime, partialTick);
    }

    public long elapsedTicks(long gameTime) {
        return Math.max(0L, gameTime - this.totalStartGameTime);
    }

    public int initialMass() {
        return this.initialMass;
    }

    public int initialEnergy() {
        return this.initialEnergy;
    }

    public int initialSize() {
        return this.initialSize;
    }

    public String initialSurfaceClass() {
        return this.initialSurfaceClass;
    }

    public int currentMass() {
        return this.currentMass;
    }

    public long trackSeed() {
        return this.trackSeed;
    }

    public int metallicityVariant() {
        return this.metallicityVariant;
    }

    public int rotationVariant() {
        return this.rotationVariant;
    }

    public int binaryVariant() {
        return this.binaryVariant;
    }

    public String terminalProfileId() {
        return this.terminalProfileId;
    }

    public String eventId() {
        return this.eventId;
    }

    public long eventStartGameTime() {
        return this.eventStartGameTime;
    }

    public long eventSeed() {
        return this.eventSeed;
    }

    public boolean eventTriggered() {
        return this.eventTriggered;
    }

    public boolean terminalApplied() {
        return this.terminalApplied;
    }

    public boolean isRunning() {
        return this.isActive();
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
        track = this.effectiveTrack(track);
        return this.phaseIndex >= 0 && this.phaseIndex < track.phaseNodes().size()
            && track.phaseNodes().get(this.phaseIndex).hasEventProfile();
    }

    public String currentEventProfileId(StellarTrack track) {
        if (this.hasVisualEvent(track)) return track.phaseNodes().get(this.phaseIndex).eventProfileId();
        return this.terminalProfileId;
    }

    /** 按绝对时间返回当前视觉事件，供客户端在网络心跳之间及时显示事件层。 */
    public String eventProfileAt(StellarTrack track, long gameTime, float partialTick) {
        track = this.effectiveTrack(track);
        this.attachTrack(track);
        TimedPhaseSample timed = this.sampleAtAbsoluteTime(track, gameTime, partialTick);
        return timed.node().hasEventProfile() ? timed.node().eventProfileId() : "";
    }

    /**
     * 从当前节点采样视觉快照。
     *
     * <p>节点数值是本阶段的目标状态，因此插值起点取上一节点：阶段内部完成过渡，
     * 阶段结束时正好到达本节点。脉动按阶段进度叠加在事件核心之后。</p>
     */
    public StellarVisualState visualState(StellarTrack track, long gameTime, float partialTick) {
        track = this.effectiveTrack(track);
        this.attachTrack(track);
        if (track.phaseNodes().isEmpty()) return StellarVisualState.DEFAULT;
        TimedPhaseSample timed = this.sampleAtAbsoluteTime(track, gameTime, partialTick);
        PhaseNode node = timed.node();
        float progress = timed.progress();
        StellarVisualState state = node.dynamics().sample(node, timed.previousNode(), progress);
        String profileId = node.hasEventProfile() ? node.eventProfileId() : "";
        if (!profileId.isBlank()) {
            StellarEventProfile profile = this.eventProfile(profileId);
            float eventProgress = this.eventProgressAt(gameTime, frameFraction(partialTick));
            int surfaceColor = state.surfaceColor();
            state = applyEvent(state, profile, eventProgress);
            state = state.withSurfaceColor(surfaceColor);
        }
        return state.withVisualRadius(state.radiusAt(progress)).withWind(node.dynamics().wind(), progress);
    }

    /**
     * 返回只包含轨道结构半径的快照。事件核心收缩、抛射壳和周期脉动留给恒星本体，
     * 束星环使用此方法即可避免跟随快速的表面效果抖动。
     */
    public StellarVisualState structuralVisualState(StellarTrack track, long gameTime, float partialTick) {
        track = this.effectiveTrack(track);
        this.attachTrack(track);
        if (track.phaseNodes().isEmpty()) return StellarVisualState.DEFAULT;
        TimedPhaseSample timed = this.sampleAtAbsoluteTime(track, gameTime, partialTick);
        return timed.node().dynamics().sample(timed.node(), timed.previousNode(), timed.progress())
            .withWind(timed.node().dynamics().wind(), timed.progress());
    }

    private static float frameFraction(float partialTick) {
        return Float.isFinite(partialTick) ? Math.clamp(partialTick, 0.0f, 1.0f) : 0.0f;
    }

    private float eventProgressAt(long gameTime, float partialTick) {
        StellarScheduledEvent event = this.eventAt(gameTime - this.totalStartGameTime + partialTick);
        return event == null ? 0 : event.progress(gameTime - this.totalStartGameTime + partialTick);
    }

    /**
     * 按绝对时间定位视觉阶段，不依赖客户端最近一次收到的 phaseIndex。
     * 这样网络同步仍可保持低频，渲染却能在阶段边界连续前进。
     */
    private TimedPhaseSample sampleAtAbsoluteTime(StellarTrack track, long gameTime, float partialTick) {
        track = this.effectiveTrack(track);
        long elapsed = Math.max(0L, gameTime - this.totalStartGameTime);
        long clampedElapsed = Math.min((long) this.totalDurationTicks, elapsed);
        float frame = Float.isFinite(partialTick) ? Math.clamp(partialTick, 0.0f, 1.0f) : 0.0f;
        long cursor = 0L;
        int lastRelative = Math.max(0, this.phaseDurations.size() - 1);
        for (int relative = 0; relative < this.phaseDurations.size(); relative++) {
            int absolute = this.scheduleStartIndex + relative;
            int duration = this.durationAt(absolute);
            long end = cursor + duration;
            if (clampedElapsed < end || relative == lastRelative) {
                float progress = Math.clamp((float) (clampedElapsed - cursor + frame) / duration, 0.0f, 1.0f);
                int index = Math.clamp(absolute, 0, track.phaseNodes().size() - 1);
                PhaseNode node = track.phaseNodes().get(index);
                PhaseNode previous = track.phaseNodes().get(Math.max(this.scheduleStartIndex, index - 1));
                PhaseNode next = index + 1 < track.phaseNodes().size()
                    ? track.phaseNodes().get(index + 1) : node;
                return new TimedPhaseSample(index, cursor, duration, progress, previous, node, next);
            }
            cursor = end;
        }
        int last = Math.max(0, track.phaseNodes().size() - 1);
        PhaseNode node = track.phaseNodes().get(last);
        PhaseNode previous = track.phaseNodes().get(Math.max(this.scheduleStartIndex, last - 1));
        return new TimedPhaseSample(last, cursor, this.durationAt(last), 1.0f, previous, node, node);
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

    /** 当前事件在其实际轨道窗口内的归一化进度。 */
    public float eventProgress(
        StellarTrack track,
        long gameTime,
        float partialTick,
        StellarEventProfile profile
    ) {
        this.attachTrack(track);
        TimedPhaseSample timed = this.sampleAtAbsoluteTime(track, gameTime, partialTick);
        String profileId = timed.node().hasEventProfile() ? timed.node().eventProfileId() : "";
        if (profileId.isBlank() || !profileId.equalsIgnoreCase(profile.profileId())) return 0.0f;
        return this.eventProgressAt(gameTime, frameFraction(partialTick));
    }

    /** 返回终局 profile 的冲击突破绝对游戏刻，用于音乐和客户端预告对齐。 */
    public long terminalShockGameTime(StellarTrack track) {
        track = this.effectiveTrack(track);
        this.attachTrack(track);
        for (StellarScheduledEvent event : this.eventPlan) {
            if (event.policy().destructive()) return this.totalStartGameTime + event.shockOffset();
        }
        return this.totalStartGameTime + this.totalDurationTicks;
    }

    public void save(CompoundTag tag) {
        tag.putInt("stellarFormatVersion", FORMAT_VERSION);
        tag.putString("stellarNodeId", this.nodeId);
        tag.putDouble("stellarInitialSolarMass", this.initialSolarMass);
        tag.putDouble("stellarCurrentSolarMass", this.currentSolarMass);
        tag.putDouble("stellarMetallicityZ",
            this.metallicityZ > 0 ? this.metallicityZ : StellarMetallicity.DEFAULT.at(this.metallicityCoordinate));
        tag.putDouble("stellarMetallicityCoordinate", this.metallicityCoordinate);
        if (this.trackSnapshot != null) {
            tag.put("stellarTrackSnapshot", StellarTrack.CODEC.encodeStart(NbtOps.INSTANCE, this.trackSnapshot).getOrThrow());
            tag.put("stellarProfileSnapshot", Codec.unboundedMap(Codec.STRING, StellarEventProfile.CODEC)
                .encodeStart(NbtOps.INSTANCE, this.profileSnapshot).getOrThrow());
            tag.putIntArray("stellarPhaseDurations", this.phaseDurations.stream().mapToInt(Integer::intValue).toArray());
            tag.put("stellarEventPlan", StellarScheduledEvent.CODEC.listOf().encodeStart(NbtOps.INSTANCE, this.eventPlan).getOrThrow());
            tag.put("stellarAppliedEvents", Codec.STRING.listOf()
                .encodeStart(NbtOps.INSTANCE, List.copyOf(this.appliedEvents)).getOrThrow());
        }
        tag.putString(TRACK_ID_KEY, this.trackId);
        tag.putString(PHASE_ID_KEY, this.phaseId);
        tag.putInt(PHASE_INDEX_KEY, this.phaseIndex);
        tag.putLong(PHASE_START_KEY, this.phaseStartGameTime);
        tag.putInt(PHASE_DURATION_KEY, this.phaseDurationTicks);
        tag.putLong(TOTAL_START_KEY, this.totalStartGameTime);
        tag.putInt(TOTAL_DURATION_KEY, this.totalDurationTicks);
        tag.putFloat(PHASE_PROGRESS_KEY, this.phaseProgress);
        tag.putInt(SCHEDULE_START_INDEX_KEY, this.scheduleStartIndex);
        tag.putInt(INITIAL_MASS_KEY, this.initialMass);
        tag.putInt(INITIAL_ENERGY_KEY, this.initialEnergy);
        tag.putInt(INITIAL_SIZE_KEY, this.initialSize);
        tag.putString(INITIAL_SURFACE_CLASS_KEY, this.initialSurfaceClass);
        tag.putInt(CURRENT_MASS_KEY, this.currentMass);
        tag.putLong(TRACK_SEED_KEY, this.trackSeed);
        tag.putInt(METALLICITY_VARIANT_KEY, this.metallicityVariant);
        tag.putInt(ROTATION_VARIANT_KEY, this.rotationVariant);
        tag.putInt(BINARY_VARIANT_KEY, this.binaryVariant);
        tag.putString(TERMINAL_PROFILE_KEY, this.terminalProfileId);
        tag.putString(EVENT_ID_KEY, this.eventId);
        tag.putLong(EVENT_START_KEY, this.eventStartGameTime);
        tag.putLong(EVENT_SEED_KEY, this.eventSeed);
        tag.putBoolean(EVENT_TRIGGERED_KEY, this.eventTriggered);
        tag.putBoolean(TERMINAL_APPLIED_KEY, this.terminalApplied);
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        this.save(tag);
        return tag;
    }

    public void writeUpdateTag(CompoundTag tag) {
        this.save(tag);
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
