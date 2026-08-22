package dev.dubhe.anvilcraft.client.support;

import dev.dubhe.anvilcraft.saved.OverworldLikeWorldState;
import dev.dubhe.anvilcraft.worldgen.OverworldLikeOrbitMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

/** Client copy of the global overworld-like sky state. */
public final class OverworldLikeClientState {
    private static final float MAX_VISUAL_ECLIPSE_DARKEN = 0.72F;
    private static int generation = -1;
    private static long visualSeed;
    private static long orbitEpochGameTime;
    private static OverworldLikeWorldState.Phase phase = OverworldLikeWorldState.Phase.RESET_PENDING;
    private static long collapseStartedAt = -1L;

    private OverworldLikeClientState() {
    }

    public static void update(
        int generation,
        long visualSeed,
        long orbitEpochGameTime,
        OverworldLikeWorldState.Phase phase
    ) {
        OverworldLikeClientState.generation = generation;
        OverworldLikeClientState.visualSeed = visualSeed;
        OverworldLikeClientState.orbitEpochGameTime = orbitEpochGameTime;
        OverworldLikeClientState.phase = phase;
        if (phase != OverworldLikeWorldState.Phase.COLLAPSING) {
            collapseStartedAt = -1L;
        }
    }

    public static void clear() {
        generation = -1;
        visualSeed = 0L;
        orbitEpochGameTime = 0L;
        phase = OverworldLikeWorldState.Phase.RESET_PENDING;
        collapseStartedAt = -1L;
    }

    public static boolean isInitialized() {
        return generation >= 0;
    }

    public static long visualSeed() {
        return visualSeed;
    }

    public static long orbitEpochGameTime() {
        return orbitEpochGameTime;
    }

    public static OverworldLikeWorldState.Phase phase() {
        return phase;
    }

    public static float eclipseFactor(ClientLevel level) {
        if (!isInitialized() || phase == OverworldLikeWorldState.Phase.RESET_PENDING) return 0.0F;
        return OverworldLikeOrbitMath.eclipseFactor(
            level.getGameTime(),
            level.getDayTime(),
            orbitEpochGameTime,
            visualSeed
        );
    }

    public static float modifySkyDarken(ClientLevel level, float original) {
        return original * environmentColorMultiplier(level);
    }

    public static float environmentColorMultiplier(ClientLevel level) {
        return 1.0F - eclipseFactor(level) * MAX_VISUAL_ECLIPSE_DARKEN;
    }

    public static void beginCollapse(int elapsedTicks) {
        if (phase != OverworldLikeWorldState.Phase.COLLAPSING) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        collapseStartedAt = level.getGameTime() - Math.max(0, elapsedTicks);
    }

    public static float collapseProgress() {
        if (phase != OverworldLikeWorldState.Phase.COLLAPSING || collapseStartedAt < 0L) return 0.0F;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return 0.0F;
        return Math.clamp((level.getGameTime() - collapseStartedAt) / 40.0F, 0.0F, 1.0F);
    }
}
