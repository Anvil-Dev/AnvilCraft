package dev.dubhe.anvilcraft.saved;

import dev.dubhe.anvilcraft.worldgen.OverworldLikeGenerationBootstrap;
import dev.dubhe.anvilcraft.worldgen.OverworldLikeOrbitMath;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Persistent runtime state shared by every overworld-like landing portal. */
public class OverworldLikeWorldState extends SavedData {
    private static final String DATA_ID = "anvilcraft_overworld_like_world_state";

    private int generation = -1;
    private long generationSeed;
    private long visualSeed;
    private long orbitEpochGameTime;
    private Phase phase = Phase.ACTIVE;
    private long collapseStartedAt = -1L;
    private long nextGenerationSeed;
    private boolean collapseDamageIssued;
    private boolean generationRequestedByEntry;
    private final Set<UUID> pendingForcedRespawns = new HashSet<>();
    private final Set<UUID> knownOverworldLikePlayers = new HashSet<>();
    private long eclipseCalculatedAt = Long.MIN_VALUE;
    private int cachedEclipseDarken;

    public static OverworldLikeWorldState get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        DimensionDataStorage storage = overworld.getDataStorage();
        OverworldLikeWorldState state = storage.computeIfAbsent(
            new SavedData.Factory<>(OverworldLikeWorldState::new, OverworldLikeWorldState::load),
            DATA_ID
        );
        state.synchronizeGeneration(OverworldLikeGenerationBootstrap.getManifest(server), overworld.getGameTime());
        return state;
    }

    private static OverworldLikeWorldState load(CompoundTag tag, HolderLookup.Provider registries) {
        OverworldLikeWorldState state = new OverworldLikeWorldState();
        state.generation = tag.getInt("generation");
        state.generationSeed = tag.getLong("generationSeed");
        state.visualSeed = tag.getLong("visualSeed");
        state.orbitEpochGameTime = tag.getLong("orbitEpochGameTime");
        state.phase = Phase.fromName(tag.getString("phase"));
        state.collapseStartedAt = tag.contains("collapseStartedAt") ? tag.getLong("collapseStartedAt") : -1L;
        state.nextGenerationSeed = tag.getLong("nextGenerationSeed");
        state.collapseDamageIssued = tag.getBoolean("collapseDamageIssued");
        state.generationRequestedByEntry = tag.getBoolean("generationRequestedByEntry");
        ListTag pending = tag.getList("pendingForcedRespawns", Tag.TAG_COMPOUND);
        for (int index = 0; index < pending.size(); index++) {
            CompoundTag player = pending.getCompound(index);
            if (player.hasUUID("id")) {
                state.pendingForcedRespawns.add(player.getUUID("id"));
            }
        }
        ListTag known = tag.getList("knownOverworldLikePlayers", Tag.TAG_COMPOUND);
        for (int index = 0; index < known.size(); index++) {
            CompoundTag player = known.getCompound(index);
            if (player.hasUUID("id")) {
                state.knownOverworldLikePlayers.add(player.getUUID("id"));
            }
        }
        return state;
    }

    private void synchronizeGeneration(OverworldLikeResetManifest manifest, long gameTime) {
        if (generation == manifest.generation() && generationSeed == manifest.activeSeed()) return;
        generation = manifest.generation();
        generationSeed = manifest.activeSeed();
        visualSeed = visualSeed(generationSeed);
        orbitEpochGameTime = gameTime;
        phase = Phase.ACTIVE;
        collapseStartedAt = -1L;
        nextGenerationSeed = manifest.nextSeed();
        collapseDamageIssued = false;
        generationRequestedByEntry = false;
        knownOverworldLikePlayers.clear();
        eclipseCalculatedAt = Long.MIN_VALUE;
        setDirty();
    }

    public int generation() {
        return generation;
    }

    public long generationSeed() {
        return generationSeed;
    }

    public long visualSeed() {
        return visualSeed;
    }

    private static long visualSeed(long seed) {
        long mixed = seed ^ 0xA24BAED4963EE407L;
        mixed ^= mixed >>> 29;
        mixed *= 0x9FB21C651E98DF25L;
        mixed ^= mixed >>> 32;
        return mixed;
    }

    public long orbitEpochGameTime() {
        return orbitEpochGameTime;
    }

    public Phase phase() {
        return phase;
    }

    public long collapseStartedAt() {
        return collapseStartedAt;
    }

    public long nextGenerationSeed() {
        return nextGenerationSeed;
    }

    public boolean collapseDamageIssued() {
        return collapseDamageIssued;
    }

    public int eclipseDarken(long gameTime, long dayTime) {
        if (eclipseCalculatedAt != gameTime) {
            eclipseCalculatedAt = gameTime;
            cachedEclipseDarken = OverworldLikeOrbitMath.additionalSkyDarken(
                gameTime,
                dayTime,
                orbitEpochGameTime,
                visualSeed
            );
        }
        return cachedEclipseDarken;
    }

    public boolean beginCollapse(long gameTime, long nextSeed) {
        if (phase != Phase.ACTIVE) return false;
        phase = Phase.COLLAPSING;
        collapseStartedAt = gameTime;
        nextGenerationSeed = nextSeed;
        collapseDamageIssued = false;
        generationRequestedByEntry = false;
        setDirty();
        return true;
    }

    public void markCollapseDamageIssued() {
        if (collapseDamageIssued) return;
        collapseDamageIssued = true;
        setDirty();
    }

    public void markResetPending() {
        if (phase == Phase.RESET_PENDING) return;
        phase = Phase.RESET_PENDING;
        setDirty();
    }

    public boolean requestGenerationByEntry() {
        if (phase != Phase.RESET_PENDING) return false;
        if (!generationRequestedByEntry) {
            generationRequestedByEntry = true;
            setDirty();
        }
        return true;
    }

    public boolean isGenerationRequestedByEntry() {
        return generationRequestedByEntry;
    }

    public void addPendingForcedRespawn(UUID playerId) {
        if (pendingForcedRespawns.add(playerId)) setDirty();
    }

    public boolean hasPendingForcedRespawn(UUID playerId) {
        return pendingForcedRespawns.contains(playerId);
    }

    public void markPlayerInOverworldLike(UUID playerId) {
        if (knownOverworldLikePlayers.add(playerId)) setDirty();
    }

    public void removeKnownOverworldLikePlayer(UUID playerId) {
        if (knownOverworldLikePlayers.remove(playerId)) setDirty();
    }

    public void enqueueKnownPlayersForForcedRespawn() {
        if (pendingForcedRespawns.addAll(knownOverworldLikePlayers)) setDirty();
    }

    public void removePendingForcedRespawn(UUID playerId) {
        if (pendingForcedRespawns.remove(playerId)) setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("generation", generation);
        tag.putLong("generationSeed", generationSeed);
        tag.putLong("visualSeed", visualSeed);
        tag.putLong("orbitEpochGameTime", orbitEpochGameTime);
        tag.putString("phase", phase.getSerializedName());
        tag.putLong("collapseStartedAt", collapseStartedAt);
        tag.putLong("nextGenerationSeed", nextGenerationSeed);
        tag.putBoolean("collapseDamageIssued", collapseDamageIssued);
        tag.putBoolean("generationRequestedByEntry", generationRequestedByEntry);
        ListTag pending = new ListTag();
        for (UUID playerId : pendingForcedRespawns) {
            CompoundTag player = new CompoundTag();
            player.putUUID("id", playerId);
            pending.add(player);
        }
        tag.put("pendingForcedRespawns", pending);
        ListTag known = new ListTag();
        for (UUID playerId : knownOverworldLikePlayers) {
            CompoundTag player = new CompoundTag();
            player.putUUID("id", playerId);
            known.add(player);
        }
        tag.put("knownOverworldLikePlayers", known);
        return tag;
    }

    @Getter
    public enum Phase {
        ACTIVE("active"),
        COLLAPSING("collapsing"),
        RESET_PENDING("reset_pending");

        private final String serializedName;

        Phase(String serializedName) {
            this.serializedName = serializedName;
        }

        public static Phase fromName(String name) {
            for (Phase value : values()) {
                if (value.serializedName.equals(name)) return value;
            }
            return ACTIVE;
        }

        public static Phase fromOrdinal(int ordinal) {
            Phase[] values = values();
            return ordinal >= 0 && ordinal < values.length ? values[ordinal] : ACTIVE;
        }
    }
}
