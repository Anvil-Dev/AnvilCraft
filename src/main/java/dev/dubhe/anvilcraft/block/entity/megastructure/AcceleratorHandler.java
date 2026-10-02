package dev.dubhe.anvilcraft.block.entity.megastructure;

import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyClass;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyData;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyMatcher;
import dev.dubhe.anvilcraft.block.entity.celestial.PlanetaryResourceSet;
import dev.dubhe.anvilcraft.block.entity.celestial.StarData;
import dev.dubhe.anvilcraft.block.entity.celestial.StellarEventProfile;
import dev.dubhe.anvilcraft.block.entity.celestial.StellarEvolutionPhase;
import dev.dubhe.anvilcraft.block.entity.celestial.StellarEvolutionState;
import dev.dubhe.anvilcraft.block.entity.celestial.StellarNodeDynamics;
import dev.dubhe.anvilcraft.block.entity.celestial.StellarScheduledEvent;
import dev.dubhe.anvilcraft.block.entity.celestial.StellarTerminal;
import dev.dubhe.anvilcraft.block.entity.celestial.StellarTrack;
import dev.dubhe.anvilcraft.block.entity.celestial.StellarTrackLibrary;
import dev.dubhe.anvilcraft.block.entity.celestial.StellarVisualState;
import dev.dubhe.anvilcraft.init.ModMegastructures;
import dev.dubhe.anvilcraft.network.QuenchedOutMusicPacket;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 恒星演化加速器。
 *
 * <p>演化由 {@link StellarEvolutionState} 的绝对游戏刻和 {@link StellarTrack} 节点推进。
 * 演化中间阶段绝不会改写 StarData 的玩法字段。</p>
 */
public class AcceleratorHandler extends BaseMegastructureHandler {
    /** 供巨构资格判断使用的粗阶段。 */
    @Getter
    private int stage;
    @Getter
    private int ticksRemaining;
    @Getter
    private int ticksTotal;
    private int originalMass;
    private int originalEnergy;
    private int originalSize;
    private boolean dysonDestroyed;
    private long dysonDestroyTick = -1L;
    /** 增幅器断开时记录暂停起点，恢复后平移时间轴。 */
    private long pausedSinceGameTime = -1L;

    @Nullable
    private StellarEvolutionState evolutionState;
    @Nullable
    private StellarTrack evolutionTrack;

    /** 提前 70.75 秒播放，让约 71.77 秒曲目的最后一秒覆盖爆发起点并自然结束。 */
    private static final int QUENCHED_FULL_PLAY_TICKS = 1440;
    private static final int QUENCHED_EXPLOSION_LEAD_TICKS = 1415;
    private static final float SUPERNOVA_SHAKE_RADIUS = 32.0f;
    /**
     * 视觉缩放的软饱和膝点，取发现阶段可能出现的最大离散尺寸。
     *
     * <p>膝点以下的缩放完全等于物理比例，因此静态天体和演化起始帧不受影响。</p>
     */
    private static final float VISUAL_SCALE_KNEE = CelestialBodyData.bodyScaleForSize(64);
    /** 视觉缩放上限：膨胀峰值最多是最大可搜索天体的两倍。 */
    private static final float VISUAL_SCALE_CEILING = VISUAL_SCALE_KNEE * 2.0f;

    private String quenchedEventId = "";
    private boolean quenchedScheduled;
    private long quenchedStartTick = -1L;
    private boolean quenchedStarted;
    private boolean quenchedCanceled;
    private boolean quenchedSupernovaFired;

    @Override
    public String name() {
        return "stellar_evolution_accelerator";
    }

    /** 轨道或事件状态仍在运行时视为活动。 */
    public boolean isActive() {
        return this.evolutionState != null && this.evolutionState.isActive();
    }

    public boolean isPaused() {
        return this.pausedSinceGameTime >= 0L;
    }

    @Nullable
    public StellarEvolutionState getEvolutionState() {
        return this.evolutionState;
    }

    @Nullable
    public StellarTrack getEvolutionTrack() {
        return this.evolutionTrack;
    }

    /** 当前物理阶段 ID，空闲时返回空字符串。 */
    public String getPhaseId() {
        return this.evolutionState == null || !this.isActive() ? "" : this.evolutionState.phaseId();
    }

    public String getPhaseId(CelestialForgingAnvilBlockEntity be) {
        if (this.evolutionState == null || !this.isActive() || this.evolutionTrack == null || be.getLevel() == null) {
            return this.getPhaseId();
        }
        return this.evolutionState.phaseAt(this.evolutionTrack, this.clockTime(be), 0.0f).getSerializedName();
    }

    /** 当前阶段进度，始终钳制在 0..1。 */
    public float getPhaseProgress() {
        return this.evolutionState == null || !this.isActive() ? 0.0f : this.evolutionState.phaseProgress();
    }

    /** 客户端按绝对游戏时间重建阶段进度，不依赖 20 tick 心跳。 */
    public float getPhaseProgress(CelestialForgingAnvilBlockEntity be, float partialTick) {
        if (this.evolutionState == null || !this.isActive() || be.getLevel() == null) return 0.0f;
        if (this.evolutionTrack == null) return this.evolutionState.phaseProgress();
        return this.evolutionState.phaseProgressAt(this.evolutionTrack, this.clockTime(be), this.visualPartialTick(partialTick));
    }

    /** 客户端按绝对游戏时间计算剩余刻数。 */
    public int getTicksRemaining(CelestialForgingAnvilBlockEntity be) {
        if (this.evolutionState == null || be.getLevel() == null) return this.ticksRemaining;
        long elapsed = Math.max(0L, this.clockTime(be) - this.evolutionState.totalStartGameTime());
        return Math.max(0, this.evolutionState.totalDurationTicks()
            - (int) Math.min(Integer.MAX_VALUE, elapsed));
    }

    /** 总演化进度，供客户端 UI 使用。 */
    public float getTotalProgress(CelestialForgingAnvilBlockEntity be, float partialTick) {
        if (this.evolutionState == null || be.getLevel() == null) return 0.0f;
        return this.evolutionState.totalProgress(this.clockTime(be), this.visualPartialTick(partialTick));
    }

    public String getTerminalProfileId() {
        return this.evolutionState == null ? "" : this.evolutionState.terminalProfileId();
    }

    public String getTerminalProfileId(CelestialForgingAnvilBlockEntity be) {
        return this.getTerminalProfileId();
    }

    /** 显示当前轨道的终局。 */
    public String getTerminalOutcomeId() {
        return this.evolutionTrack == null ? "" : this.evolutionTrack.definition().terminal().kind().id();
    }

    public String getTerminalOutcomeId(CelestialForgingAnvilBlockEntity be) {
        return this.getTerminalOutcomeId();
    }

    /** 根据绝对游戏刻采样视觉快照；非演化状态由调用方回退到 StarData。 */
    @Nullable
    public StellarVisualState getVisualState(CelestialForgingAnvilBlockEntity be, float partialTick) {
        return this.sampleVisualState(be, partialTick, true);
    }

    /**
     * 返回不含周期脉动的结构快照。束星环、巨构和中心高度只使用这个值，
     * 从而不会随着恒星表面的快速小幅呼吸上下抖动。
     */
    @Nullable
    public StellarVisualState getStructuralVisualState(CelestialForgingAnvilBlockEntity be, float partialTick) {
        return this.sampleVisualState(be, partialTick, false);
    }

    private @Nullable StellarVisualState sampleVisualState(
        CelestialForgingAnvilBlockEntity be,
        float partialTick,
        boolean includePulsation
    ) {
        if (this.evolutionState == null || this.evolutionTrack == null || be.getLevel() == null) return null;
        if (!this.evolutionState.isActive() && this.evolutionTrack.definition().terminal().kind() != StellarTerminal.Kind.KEEP) {
            return null;
        }
        float frameFraction = this.visualPartialTick(partialTick);
        long visualTime = this.clockTime(be);
        StellarVisualState sampled = includePulsation
            ? this.evolutionState.visualState(this.evolutionTrack, visualTime, frameFraction)
            : this.evolutionState.structuralVisualState(this.evolutionTrack, visualTime, frameFraction);
        return this.blendInitialSurfaceColor(be, sampled, visualTime);
    }

    /** 将轨道半径换算为当前恒星的结构渲染缩放，供束星环和中心共用。 */
    public float getVisualBodyScale(CelestialForgingAnvilBlockEntity be, float partialTick) {
        return this.calculateVisualBodyScale(be, partialTick, false);
    }

    /** 将含脉动的半径换算为本体渲染缩放；不供束星环使用。 */
    public float getPulsatingVisualBodyScale(CelestialForgingAnvilBlockEntity be, float partialTick) {
        return this.calculateVisualBodyScale(be, partialTick, true);
    }

    /** 返回本次演化终点在服务端 StarData 中使用的原始视觉缩放。 */
    public float getTerminalVisualBodyScale(CelestialForgingAnvilBlockEntity be) {
        if (!(be.getCelestialBodyData() instanceof StarData star)) return 0.01f;
        if (this.evolutionState == null) return star.bodyScale();
        return this.terminalVisualBodyScale(star);
    }

    private float calculateVisualBodyScale(
        CelestialForgingAnvilBlockEntity be,
        float partialTick,
        boolean includePulsation
    ) {
        if (!(be.getCelestialBodyData() instanceof StarData star)) return 0.0f;
        StellarVisualState visual = includePulsation
            ? this.getVisualState(be, partialTick)
            : this.getStructuralVisualState(be, partialTick);
        if (visual == null || this.evolutionState == null || this.evolutionTrack == null) return star.bodyScale();
        int index = Math.clamp(this.evolutionState.scheduleStartIndex(), 0, this.evolutionTrack.phaseNodes().size() - 1);
        var initialNode = this.evolutionTrack.phaseNodes().get(index);
        float baseRadius = initialNode.dynamics().points().getFirst().radius();
        float startScale = this.evolutionState.initialSize() > 0
            ? CelestialBodyData.bodyScaleForSize(this.evolutionState.initialSize())
            : star.bodyScale();
        startScale = Math.max(0.01f, startScale);
        float physicalRatio = visual.radius() / Math.max(0.01f, baseRadius);
        float mappedRatio = mapRadiusRatio(physicalRatio, startScale);

        // 在最后一个物理阶段结束时衔接残骸尺寸，终局本身不占用阶段时长。
        float terminalScale = this.terminalVisualBodyScale(star);
        float totalProgress = this.evolutionState.totalProgress(this.clockTime(be), this.visualPartialTick(partialTick));
        float finalNodeStart = this.finalNodeStartProgress();
        float endpointStart = finalNodeStart + (1.0f - finalNodeStart) * 0.9f;
        float endpointRange = Math.max(0.001f, 1.0f - endpointStart);
        float endpointProgress = smoothstep(Math.clamp(
            (totalProgress - endpointStart) / endpointRange,
            0.0f,
            1.0f
        ));
        mappedRatio = lerp(mappedRatio, terminalScale / startScale, endpointProgress);
        return Math.clamp(startScale * mappedRatio, 0.01f, 100.0f);
    }

    private float finalNodeStartProgress() {
        if (this.evolutionState == null || this.evolutionTrack == null || this.evolutionState.totalDurationTicks() <= 0) return 0.94f;
        List<Integer> durations = this.evolutionState.phaseDurations(this.evolutionTrack);
        if (durations.isEmpty()) return 0.94f;
        int finalRelative = durations.size() - 1;
        long elapsedBeforeFinal = 0L;
        for (int index = 0; index < finalRelative; index++) elapsedBeforeFinal += durations.get(index);
        return Math.clamp(
            elapsedBeforeFinal / (float) Math.max(1, this.evolutionState.totalDurationTicks()),
            0.0f,
            1.0f
        );
    }

    /**
     * 把轨道的物理半径比映射成视觉半径比。
     *
     * <p>膨胀方向在超过“可搜索最大天体”后转为软饱和：轨道数据里 AGB/红超巨星能达到
     * 起始半径的五到十余倍，而起始缩放本身还有十几倍差距，直接相乘会得到上百格宽的
     * 恒星。软上限保留连续单调的膨胀过程和远大于任何可搜索恒星的量级差，同时把峰值
     * 收在最大可搜索天体的两倍以内。收缩方向保留原有的可见下限。</p>
     */
    private static float mapRadiusRatio(float ratio, float startScale) {
        if (!Float.isFinite(ratio)) return 1.0f;
        float safeRatio = Math.max(0.01f, ratio);
        if (safeRatio <= 1.0f) return compressSmallRadius(safeRatio);
        float safeStart = Math.max(0.01f, startScale);
        return softLimitBodyScale(safeStart * safeRatio) / safeStart;
    }

    /**
     * 对超过 {@link #VISUAL_SCALE_KNEE} 的视觉缩放做软饱和。
     *
     * <p>膝点以下完全保留物理比例（含发现时可能达到的最大离散尺寸），
     * 膝点以上以指数方式逼近 {@link #VISUAL_SCALE_CEILING}，导数连续且始终单调递增。</p>
     */
    private static float softLimitBodyScale(float bodyScale) {
        if (bodyScale <= VISUAL_SCALE_KNEE) return bodyScale;
        float headroom = VISUAL_SCALE_CEILING - VISUAL_SCALE_KNEE;
        float excess = (bodyScale - VISUAL_SCALE_KNEE) / headroom;
        return VISUAL_SCALE_KNEE + headroom * (1.0f - (float) Math.exp(-excess));
    }

    private static float compressSmallRadius(float ratio) {
        if (!Float.isFinite(ratio)) return 1.0f;
        float safeRatio = Math.max(0.01f, ratio);
        if (safeRatio >= 1.0f) return safeRatio;
        final float minimum = 0.24f;
        final float curve = 8.0f;
        float logarithmic = (float) (Math.log1p(safeRatio * curve) / Math.log1p(curve));
        return minimum + (1.0f - minimum) * Math.clamp(logarithmic, 0.0f, 1.0f);
    }

    private static float lerp(float from, float to, float progress) {
        return from + (to - from) * progress;
    }

    private static float smoothstep(float value) {
        float t = Math.clamp(value, 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }

    private float terminalVisualBodyScale(StarData star) {
        if (star.bodyClass() == CelestialBodyClass.WHITE_DWARF
            || star.bodyClass() == CelestialBodyClass.NEUTRON_STAR
            || star.bodyClass() == CelestialBodyClass.BLACK_HOLE) {
            return star.bodyScale();
        }
        if (this.evolutionTrack == null) return star.bodyScale();
        return switch (this.getTerminalOutcomeId()) {
            case "white_dwarf" -> CelestialBodyData.bodyScaleForSize(this.evolutionTrack.definition().terminal().size());
            case "neutron_star" -> 0.8f;
            case "black_hole" -> 1.5f;
            case "keep" -> star.bodyScale();
            default -> 0.01f;
        };
    }

    private static int packColor(StarData star) {
        int red = Math.clamp(star.colorR(), 0, 255);
        int green = Math.clamp(star.colorG(), 0, 255);
        int blue = Math.clamp(star.colorB(), 0, 255);
        return (red << 16) | (green << 8) | blue;
    }

    private StellarVisualState blendInitialSurfaceColor(
        CelestialForgingAnvilBlockEntity be,
        StellarVisualState sampled,
        long gameTime
    ) {
        if (!(be.getCelestialBodyData() instanceof StarData star) || this.evolutionState == null) return sampled;
        int originalColor = packColor(star);
        int transitionTicks = Math.min(40, Math.max(1, this.evolutionState.totalDurationTicks() / 20));
        long elapsed = this.evolutionState.elapsedTicks(gameTime);
        if (elapsed >= transitionTicks) return sampled;
        float progress = smoothstep(elapsed / (float) transitionTicks);
        return sampled.withSurfaceColor(StellarVisualState.interpolateColor(
            originalColor,
            sampled.surfaceColor(),
            progress
        ));
    }

    /** 当前事件 profile，供渲染器选择独立核心/抛射物层。 */
    @Nullable
    public StellarEventProfile getCurrentEventProfile(CelestialForgingAnvilBlockEntity be) {
        if (this.evolutionState == null || !this.evolutionState.isActive() || this.evolutionTrack == null) return null;
        String id = this.evolutionState.eventProfileAt(this.evolutionTrack, this.clockTime(be), 0.0f);
        if (id == null || id.isBlank()) return null;
        return this.evolutionState.eventProfile(id);
    }

    @Nullable
    public StellarEventProfile getEventProfile(String id) {
        return this.evolutionState == null ? StellarTrackLibrary.eventProfiles().get(id) : this.evolutionState.findEventProfile(id);
    }

    /** 捕获的是已冻结的运行，不在复制或重新捕获时抽样。 */
    public void captureSnapshot(CelestialForgingAnvilBlockEntity be, CompoundTag snapshot) {
        if (be.getLevel() == null) return;
        if (this.evolutionState == null) return;
        CompoundTag saved = new CompoundTag();
        this.saveState(saved);
        snapshot.put("stellarEvolution", saved);
        snapshot.putLong("stellarSnapshotGameTime", this.clockTime(be));
    }

    public void restoreSnapshot(CelestialForgingAnvilBlockEntity be, CompoundTag snapshot) {
        if (be.getLevel() == null || be.getLevel().isClientSide()) return;
        this.onClear(be);
        if (!snapshot.contains("stellarEvolution")) return;
        this.loadState(snapshot.getCompoundOrEmpty("stellarEvolution"));
        if (this.evolutionState == null) return;
        long now = be.getLevel().getGameTime();
        long delta = now - snapshot.getLongOr("stellarSnapshotGameTime", 0L);
        this.evolutionState.shiftTimeline(delta);
        if (this.dysonDestroyTick >= 0) this.dysonDestroyTick += delta;
        this.pausedSinceGameTime = this.pausedSinceGameTime >= 0 || !be.isAmplifierPresent() ? now : -1;
        this.quenchedScheduled = false;
        this.quenchedStarted = false;
        if (this.evolutionState.isActive()) this.scheduleQuenchedOut(be);
        this.syncProgress(now);
    }

    /** 当前事件的 0..1 进度。 */
    public float getEventProgress(CelestialForgingAnvilBlockEntity be, float partialTick) {
        StellarEventProfile profile = this.getCurrentEventProfile(be);
        if (profile == null || this.evolutionState == null || be.getLevel() == null) return 0.0f;
        if (this.evolutionTrack == null) return this.evolutionState.phaseProgress();
        return this.evolutionState.eventProgress(
            this.evolutionTrack,
            this.clockTime(be),
            this.visualPartialTick(partialTick),
            profile
        );
    }

    @Override
    public boolean isAuxiliaryActive(CelestialForgingAnvilBlockEntity be) {
        return this.isActive();
    }

    @Override
    public void serverTick(CelestialForgingAnvilBlockEntity be) {
        if (be.getLevel() == null || be.getLevel().isClientSide()) return;
        if (this.evolutionState == null || !this.evolutionState.isActive() || this.evolutionTrack == null) return;

        long gameTime = be.getLevel().getGameTime();
        if (!be.isAmplifierPresent() && !this.evolutionState.isComplete()) {
            this.cancelQuenchedOutMusic(be);
            if (this.pausedSinceGameTime < 0L) {
                this.pausedSinceGameTime = gameTime;
                this.syncToClient(be);
            }
            return;
        }
        if (this.pausedSinceGameTime >= 0L) {
            long pausedTicks = Math.max(0L, gameTime - this.pausedSinceGameTime);
            this.evolutionState.shiftTimeline(pausedTicks);
            if (this.dysonDestroyTick >= 0L) this.dysonDestroyTick += pausedTicks;
            this.pausedSinceGameTime = -1L;
            this.syncToClient(be);
        }
        this.tickQuenchedOutMusic(be);
        this.syncProgress(gameTime);

        if (this.isDysonSphereBuilt(be) && !this.dysonDestroyed && this.dysonDestroyTick < 0L) {
            long remaining = Math.max(20L, (long) this.ticksRemaining);
            this.dysonDestroyTick = gameTime + Math.max(1L, remaining / 2L);
        }
        if (!this.dysonDestroyed && this.dysonDestroyTick >= 0L && gameTime >= this.dysonDestroyTick) {
            this.destroyDysonSphere(be);
        }

        boolean phaseChanged = this.evolutionState.update(gameTime, this.evolutionTrack);
        for (StellarScheduledEvent event : this.evolutionState.dueEvents(gameTime)) {
            this.evolutionState.markEventApplied(event);
            if (event.instanceId().equals(this.quenchedEventId)) this.quenchedSupernovaFired = true;
            if (event.policy().destructive()) this.triggerDestructiveEvent(be, event.profileId(), event.seed());
        }
        if (this.evolutionState.isComplete() && !this.evolutionState.terminalApplied()) {
            this.completeEvolution(be);
        } else {
            this.syncProgress(gameTime);
            if (phaseChanged || gameTime % 20L == 0L) this.syncToClient(be);
        }
    }

    @Override
    public void onBuild(CelestialForgingAnvilBlockEntity be) {
        if (be.getLevel() == null || be.getLevel().isClientSide()) return;
        if (this.isActive() || !(be.getCelestialBodyData() instanceof StarData star)) return;
        int mass = be.getStellarMass();
        StellarTrack track = StellarTrackLibrary.select(mass, star.bodyClass(), star.specialRedDwarf(), be.getBodySeed());
        if (track == null) return;
        this.originalMass = mass;
        this.originalEnergy = star.energy();
        this.originalSize = star.size();
        this.dysonDestroyed = false;
        this.dysonDestroyTick = -1L;
        this.pausedSinceGameTime = -1L;
        this.quenchedSupernovaFired = false;
        long evolutionSeed = be.getLevel().getRandom().nextLong();
        this.evolutionState = StellarEvolutionState.beginNew(track, star.bodyClass(), this.originalMass, this.originalEnergy,
            this.originalSize, evolutionSeed, be.getLevel().getGameTime(), this.evolutionBudget(be, star.bodyClass()));
        this.evolutionTrack = this.evolutionState.trackSnapshot();
        this.syncProgress(be.getLevel().getGameTime());
        this.scheduleQuenchedOut(be);
        this.syncToClient(be);
    }

    /** 根据天体图中的年龄和温度计算轨道总时长。 */
    private int evolutionBudget(CelestialForgingAnvilBlockEntity be, CelestialBodyClass cls) {
        int ageX = CelestialBodyMatcher.toX(be.getAgeAnvilCount());
        int energyY = CelestialBodyMatcher.toY(this.originalEnergy);
        int main = cls.isMainSequence() ? CelestialBodyMatcher.countPixelsRightInAgeTemp(ageX, energyY) * 2400 : 0;
        int giant = CelestialBodyMatcher.countPixelsDownInAgeTempSp(ageX, energyY);
        int giantTotal = CelestialBodyMatcher.countTotalColoredPixelsInAgeTempSpColumn(ageX, energyY);
        int giantTicks = giantTotal <= 0 ? 1 : Math.max(1, Math.round(2400.0f * giant / giantTotal));
        if (cls == CelestialBodyClass.M_MAIN) {
            return Math.max(2400, main + 2400);
        }
        return Math.max(20, main + giantTicks + 10);
    }

    @Nullable
    private StellarScheduledEvent nextQuenchedOutEvent() {
        if (this.evolutionState == null) return null;
        var applied = this.evolutionState.appliedEvents();
        for (StellarScheduledEvent event : this.evolutionState.eventPlan()) {
            if (!applied.contains(event.instanceId())
                && (event.policy().destructive() || event.policy() == StellarNodeDynamics.EventPolicy.PPISN)) {
                return event;
            }
        }
        return null;
    }

    private void scheduleQuenchedOut(CelestialForgingAnvilBlockEntity be) {
        this.quenchedScheduled = false;
        this.quenchedStartTick = -1L;
        this.quenchedStarted = false;
        this.quenchedCanceled = false;
        this.quenchedSupernovaFired = false;
        StellarScheduledEvent event = this.nextQuenchedOutEvent();
        this.quenchedEventId = event == null ? "" : event.instanceId();
        if (event == null || this.evolutionState == null || be.getLevel() == null) return;
        long shockTime = this.evolutionState.totalStartGameTime() + event.shockOffset();
        if (shockTime - this.clockTime(be) < QUENCHED_FULL_PLAY_TICKS) return;
        this.quenchedScheduled = true;
        this.quenchedStartTick = shockTime - QUENCHED_EXPLOSION_LEAD_TICKS;
    }

    private void cancelQuenchedOutMusic(CelestialForgingAnvilBlockEntity be) {
        if (this.quenchedStarted && !this.quenchedSupernovaFired) this.sendQuenchedOutMusic(be, false);
        this.quenchedStarted = false;
        this.quenchedScheduled = false;
        this.quenchedCanceled = true;
    }

    private void tickQuenchedOutMusic(CelestialForgingAnvilBlockEntity be) {
        if (!this.isActive() || be.getLevel() == null || this.evolutionState == null) return;
        StellarScheduledEvent event = this.nextQuenchedOutEvent();
        if (event == null) return;
        if (this.quenchedCanceled || !event.instanceId().equals(this.quenchedEventId)) this.scheduleQuenchedOut(be);
        long gameTime = be.getLevel().getGameTime();
        if (this.quenchedScheduled && !this.quenchedStarted && gameTime >= this.quenchedStartTick
            && gameTime < this.evolutionState.totalStartGameTime() + event.shockOffset()) {
            this.quenchedScheduled = false;
            this.quenchedStarted = true;
            this.sendQuenchedOutMusic(be, true);
        }
    }

    private void sendQuenchedOutMusic(CelestialForgingAnvilBlockEntity be, boolean start) {
        if (!(be.getLevel() instanceof ServerLevel serverLevel)) return;
        PacketDistributor.sendToPlayersTrackingChunk(
            serverLevel,
            ChunkPos.containing(be.getBlockPos()),
            new QuenchedOutMusicPacket(be.getBlockPos(), start)
        );
    }

    private boolean isDysonSphereBuilt(CelestialForgingAnvilBlockEntity be) {
        return ModMegastructures.DYSON_SPHERE_SMALL.getId().equals(be.getActiveMegastructureId())
            || ModMegastructures.DYSON_SPHERE_LARGE.getId().equals(be.getActiveMegastructureId());
    }

    private void triggerDestructiveEvent(CelestialForgingAnvilBlockEntity be, String profileId, long eventSeed) {
        /// 触发闪光、震动和方块爆炸，残骸在演化结束时生成。
        be.startSupernovaFlash(profileId, eventSeed);
        if (be.getLevel() instanceof ServerLevel serverLevel) {
            PacketDistributor.sendToPlayersTrackingChunk(
                serverLevel,
                ChunkPos.containing(be.getBlockPos()),
                dev.dubhe.anvilcraft.network.ScreenShakePacket.of(
                    new net.minecraft.world.phys.Vec3(
                        be.getBlockPos().getX() + 0.5,
                        be.getBodyCenterWorldY(),
                        be.getBlockPos().getZ() + 0.5
                    ),
                    SUPERNOVA_SHAKE_RADIUS,
                    dev.dubhe.anvilcraft.network.ScreenShakePacket.ShakeType.SUPERNOVA
                )
            );
        }
        /// profile 只改变渲染核心、抛射物和发光节奏。
        be.getLevel().explode(
            null,
            be.getBlockPos().getX() + 0.5,
            be.getBodyCenterWorldY(),
            be.getBlockPos().getZ() + 0.5,
            10.0f,
            Level.ExplosionInteraction.BLOCK
        );
        be.getMegastructureManager().clearOtherMegastructures(be);
        this.quenchedSupernovaFired = true;
        this.syncToClient(be);
    }

    /** 终局与爆炸独立分派，并在保存、重复 tick 和恢复后保持幂等。 */
    private void completeEvolution(CelestialForgingAnvilBlockEntity be) {
        if (this.evolutionState == null || this.evolutionState.terminalApplied() || this.evolutionTrack == null) return;
        this.evolutionState.markTerminalApplied();
        StellarTerminal terminal = this.evolutionTrack.definition().terminal();
        be.getMegastructureManager().clearOtherMegastructures(be);
        switch (terminal.kind()) {
            case WHITE_DWARF -> this.createWhiteDwarfRemnant(be);
            case NEUTRON_STAR -> this.createNeutronStarRemnant(be);
            case BLACK_HOLE -> this.createBlackHoleRemnant(be);
            case NONE -> {
                be.setCelestialBodyData(null);
                be.setPlanetaryResourceSet(null);
                be.setStellarMass(0);
            }
            default -> {
                // KEEP 的冷却外观继续从快照采样。
            }
        }
        this.finishAccelerator(be);
        this.syncToClient(be);
    }

    private void createWhiteDwarfRemnant(CelestialForgingAnvilBlockEntity be) {
        if (!(be.getCelestialBodyData() instanceof StarData star)) return;
        if (this.evolutionTrack == null) return;
        StellarTerminal terminal = this.evolutionTrack.definition().terminal();
        int wdSpaceAnvil = terminal.size();
        int wdMassAnvil = terminal.massAnvils();
        int wdEnergy = 47;
        int[] rgb = CelestialBodyMatcher.getStarColor(wdEnergy);
        int newMag = Math.min(star.magneticFieldStrength() + 1, 5);
        int newRotation = Math.min(star.rotationSpeed() + 1, 5);
        be.setAgeAnvilCount(be.getAgeAnvilCount() + 1);
        be.setStellarMass(wdMassAnvil);
        be.setCelestialBodyData(new StarData(
            CelestialBodyClass.WHITE_DWARF,
            wdSpaceAnvil,
            rgb[0],
            rgb[1],
            rgb[2],
            star.axialTilt(),
            newRotation,
            newMag,
            wdEnergy,
            star.bodyUuid()
        ));
        be.setPlanetaryResourceSet(new PlanetaryResourceSet());
    }

    private void createNeutronStarRemnant(CelestialForgingAnvilBlockEntity be) {
        if (!(be.getCelestialBodyData() instanceof StarData star)) return;
        if (this.evolutionTrack == null) return;
        int neutronMass = this.evolutionTrack.definition().terminal().massAnvils();
        int newMag = Math.min(star.magneticFieldStrength() + 2, 6);
        int newRotation = Math.min(star.rotationSpeed() + 2, 5);
        be.setAgeAnvilCount(be.getAgeAnvilCount() + 1);
        be.setStellarMass(neutronMass);
        be.setCelestialBodyData(new StarData(
            CelestialBodyClass.NEUTRON_STAR,
            1,
            255,
            255,
            255,
            star.axialTilt(),
            newRotation,
            newMag,
            64,
            star.bodyUuid()
        ));
        be.setPlanetaryResourceSet(new PlanetaryResourceSet());
    }

    private void createBlackHoleRemnant(CelestialForgingAnvilBlockEntity be) {
        if (!(be.getCelestialBodyData() instanceof StarData star)) return;
        if (this.evolutionTrack == null) return;
        int bhMass = this.evolutionTrack.definition().terminal().massAnvils();
        int newMag = Math.min(star.magneticFieldStrength() + 2, 6);
        be.setAgeAnvilCount(be.getAgeAnvilCount() + 1);
        be.setStellarMass(bhMass);
        be.setCelestialBodyData(new StarData(
            CelestialBodyClass.BLACK_HOLE,
            1,
            0,
            0,
            0,
            star.axialTilt(),
            1,
            newMag,
            64,
            star.bodyUuid()
        ));
        be.setPlanetaryResourceSet(new PlanetaryResourceSet());
    }

    private void finishAccelerator(CelestialForgingAnvilBlockEntity be) {
        if (this.quenchedStarted && !this.quenchedSupernovaFired) this.sendQuenchedOutMusic(be, false);
        this.stage = 0;
        this.ticksRemaining = 0;
        this.ticksTotal = 0;
        this.dysonDestroyed = false;
        this.dysonDestroyTick = -1L;
        this.pausedSinceGameTime = -1L;
        this.quenchedScheduled = false;
        this.quenchedStartTick = -1L;
        this.quenchedStarted = false;
        this.quenchedCanceled = false;
    }

    private void destroyDysonSphere(CelestialForgingAnvilBlockEntity be) {
        if (this.dysonDestroyed) return;
        this.dysonDestroyed = true;
        be.getMegastructureManager().clearMegastructure(be);
        be.setChanged();
        be.getLevel().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
    }

    private void syncToClient(CelestialForgingAnvilBlockEntity be) {
        be.setChanged();
        be.getLevel().sendBlockUpdated(
            be.getBlockPos(),
            be.getBlockState(),
            be.getBlockState(),
            Block.UPDATE_CLIENTS
        );
    }

    private void syncProgress(long gameTime) {
        if (this.evolutionState == null || !this.evolutionState.isActive() || this.evolutionTrack == null) {
            this.stage = 0;
            this.ticksRemaining = 0;
            this.ticksTotal = 0;
            return;
        }
        this.stage = StellarEvolutionPhase.fromIdOrDefault(this.evolutionState.phaseId()).acceleratorStage();
        this.ticksTotal = this.evolutionState.totalDurationTicks();
        long elapsed = Math.max(0L, gameTime - this.evolutionState.totalStartGameTime());
        this.ticksRemaining = Math.max(0, this.ticksTotal - (int) Math.min(Integer.MAX_VALUE, elapsed));
    }

    private float visualPartialTick(float partialTick) {
        return this.isPaused() || !Float.isFinite(partialTick) ? 0 : Math.clamp(partialTick, 0, 1);
    }

    /** 返回暂停期间冻结的视觉时钟；恢复后回到世界绝对游戏刻。 */
    private long clockTime(CelestialForgingAnvilBlockEntity be) {
        if (be.getLevel() == null) return 0L;
        long now = be.getLevel().getGameTime();
        return this.pausedSinceGameTime < 0L ? now : Math.min(now, this.pausedSinceGameTime);
    }

    @Override
    public void onClear(CelestialForgingAnvilBlockEntity be) {
        this.stage = 0;
        this.ticksRemaining = 0;
        this.ticksTotal = 0;
        this.evolutionState = null;
        this.evolutionTrack = null;
        this.dysonDestroyed = false;
        this.dysonDestroyTick = -1L;
        this.pausedSinceGameTime = -1L;
        if (this.quenchedSupernovaFired) {
            this.quenchedSupernovaFired = false;
        } else if (this.quenchedStarted) {
            this.sendQuenchedOutMusic(be, false);
        }
        this.quenchedScheduled = false;
        this.quenchedStartTick = -1L;
        this.quenchedStarted = false;
        this.quenchedCanceled = false;
    }

    @Override
    public void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        CompoundTag tag = new CompoundTag();
        this.saveState(tag);
        output.store(tag);
    }

    @Override
    public void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        this.loadState(input.read(com.mojang.serialization.MapCodec.assumeMapUnsafe(CompoundTag.CODEC)).orElseGet(CompoundTag::new));
    }

    private void saveState(CompoundTag tag) {
        tag.putInt("acceleratorStage", this.stage);
        tag.putInt("acceleratorTicksRemaining", this.ticksRemaining);
        tag.putInt("acceleratorTicksTotal", this.ticksTotal);
        tag.putInt("acceleratorOriginalMass", this.originalMass);
        tag.putInt("acceleratorOriginalEnergy", this.originalEnergy);
        tag.putInt("acceleratorOriginalSize", this.originalSize);
        tag.putBoolean("acceleratorDysonDestroyed", this.dysonDestroyed);
        tag.putLong("acceleratorDysonDestroyTick", this.dysonDestroyTick);
        tag.putLong("acceleratorPausedSinceGameTime", this.pausedSinceGameTime);
        tag.putBoolean("quenchedScheduled", this.quenchedScheduled);
        tag.putLong("quenchedStartTick", this.quenchedStartTick);
        tag.putBoolean("quenchedStarted", this.quenchedStarted);
        tag.putBoolean("quenchedCanceled", this.quenchedCanceled);
        tag.putBoolean("quenchedSupernovaFired", this.quenchedSupernovaFired);
        if (this.evolutionState != null) this.evolutionState.save(tag);
    }

    private void loadState(CompoundTag tag) {
        this.stage = tag.getIntOr("acceleratorStage", 0);
        this.ticksRemaining = tag.getIntOr("acceleratorTicksRemaining", 0);
        this.ticksTotal = tag.getIntOr("acceleratorTicksTotal", 0);
        this.originalMass = tag.getIntOr("acceleratorOriginalMass", 0);
        this.originalEnergy = tag.getIntOr("acceleratorOriginalEnergy", 0);
        this.originalSize = tag.getIntOr("acceleratorOriginalSize", 0);
        this.dysonDestroyed = tag.getBooleanOr("acceleratorDysonDestroyed", false);
        this.dysonDestroyTick = tag.getLongOr("acceleratorDysonDestroyTick", 0L);
        this.pausedSinceGameTime = tag.contains("acceleratorPausedSinceGameTime")
            ? tag.getLongOr("acceleratorPausedSinceGameTime", 0L) : -1L;
        this.quenchedEventId = "";
        this.quenchedScheduled = tag.getBooleanOr("quenchedScheduled", false);
        this.quenchedStartTick = tag.getLongOr("quenchedStartTick", 0L);
        this.quenchedStarted = tag.getBooleanOr("quenchedStarted", false);
        this.quenchedCanceled = tag.getBooleanOr("quenchedCanceled", false);
        this.quenchedSupernovaFired = tag.getBooleanOr("quenchedSupernovaFired", false);
        this.evolutionState = hasSnapshot(tag) ? StellarEvolutionState.fromTag(tag) : null;
        this.evolutionTrack = this.evolutionState == null ? null : this.evolutionState.trackSnapshot();
        if (this.evolutionState != null) {
            this.originalMass = this.evolutionState.initialMass();
            this.originalEnergy = this.evolutionState.initialEnergy();
            this.originalSize = this.evolutionState.initialSize();
            StellarScheduledEvent event = this.nextQuenchedOutEvent();
            if (this.quenchedScheduled && !this.quenchedCanceled && event != null
                && this.quenchedStartTick == this.evolutionState.totalStartGameTime() + event.shockOffset()
                    - QUENCHED_EXPLOSION_LEAD_TICKS) {
                this.quenchedEventId = event.instanceId();
            }
        }
        if (this.evolutionState == null) {
            this.stage = 0;
            this.ticksRemaining = 0;
            this.ticksTotal = 0;
            this.dysonDestroyed = false;
            this.dysonDestroyTick = -1L;
            this.pausedSinceGameTime = -1L;
            this.quenchedScheduled = false;
            this.quenchedStartTick = -1L;
            this.quenchedStarted = false;
            this.quenchedCanceled = false;
            this.quenchedSupernovaFired = false;
        }
    }

    private static boolean hasSnapshot(CompoundTag tag) {
        int version = tag.getIntOr("stellarFormatVersion", 0);
        return (version == 2 || version == 3) && tag.contains(StellarEvolutionState.TRACK_ID_KEY);
    }

    @Override
    public void writeUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("acceleratorStage", this.stage);
        tag.putInt("acceleratorTicksRemaining", this.ticksRemaining);
        tag.putInt("acceleratorTicksTotal", this.ticksTotal);
        tag.putLong("acceleratorPausedSinceGameTime", this.pausedSinceGameTime);
        if (this.evolutionState != null) this.evolutionState.writeUpdateTag(tag);
    }

    @Override
    public void readUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        this.stage = tag.getIntOr("acceleratorStage", 0);
        this.ticksRemaining = tag.getIntOr("acceleratorTicksRemaining", 0);
        this.ticksTotal = tag.getIntOr("acceleratorTicksTotal", 0);
        this.pausedSinceGameTime = tag.contains("acceleratorPausedSinceGameTime")
            ? tag.getLongOr("acceleratorPausedSinceGameTime", 0L) : -1L;
        if (hasSnapshot(tag)) {
            this.evolutionState = StellarEvolutionState.fromTag(tag);
            this.evolutionTrack = this.evolutionState.trackSnapshot();
            this.originalMass = this.evolutionState.initialMass();
            this.originalEnergy = this.evolutionState.initialEnergy();
            this.originalSize = this.evolutionState.initialSize();
        } else {
            this.evolutionState = null;
            this.evolutionTrack = null;
            this.stage = 0;
            this.ticksRemaining = 0;
            this.ticksTotal = 0;
            this.pausedSinceGameTime = -1L;
        }
    }
}
