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
        if (this.generation == manifest.generation() && this.generationSeed == manifest.activeSeed()) return;
        this.generation = manifest.generation();
        this.generationSeed = manifest.activeSeed();
        this.visualSeed = visualSeed(this.generationSeed);
        this.orbitEpochGameTime = gameTime;
        this.phase = Phase.ACTIVE;
        this.collapseStartedAt = -1L;
        this.nextGenerationSeed = manifest.nextSeed();
        this.collapseDamageIssued = false;
        this.generationRequestedByEntry = false;
        this.knownOverworldLikePlayers.clear();
        this.eclipseCalculatedAt = Long.MIN_VALUE;
        setDirty();
    }

    public int generation() {
        return this.generation;
    }

    public long generationSeed() {
        return this.generationSeed;
    }

    public long visualSeed() {
        return this.visualSeed;
    }

    private static long visualSeed(long seed) {
        long mixed = seed ^ 0xA24BAED4963EE407L;
        mixed ^= mixed >>> 29;
        mixed *= 0x9FB21C651E98DF25L;
        mixed ^= mixed >>> 32;
        return mixed;
    }

    public long orbitEpochGameTime() {
        return this.orbitEpochGameTime;
    }

    public Phase phase() {
        return this.phase;
    }

    public long collapseStartedAt() {
        return this.collapseStartedAt;
    }

    public long nextGenerationSeed() {
        return this.nextGenerationSeed;
    }

    public boolean collapseDamageIssued() {
        return this.collapseDamageIssued;
    }

    public int eclipseDarken(long gameTime, long dayTime) {
        if (this.eclipseCalculatedAt != gameTime) {
            this.eclipseCalculatedAt = gameTime;
            this.cachedEclipseDarken = OverworldLikeOrbitMath.additionalSkyDarken(
                gameTime,
                dayTime,
                this.orbitEpochGameTime,
                this.visualSeed
            );
        }
        return this.cachedEclipseDarken;
    }

    public boolean beginCollapse(long gameTime, long nextSeed) {
        if (this.phase != Phase.ACTIVE) return false;
        this.phase = Phase.COLLAPSING;
        this.collapseStartedAt = gameTime;
        this.nextGenerationSeed = nextSeed;
        this.collapseDamageIssued = false;
        this.generationRequestedByEntry = false;
        setDirty();
        return true;
    }

    public void markCollapseDamageIssued() {
        if (this.collapseDamageIssued) return;
        this.collapseDamageIssued = true;
        setDirty();
    }

    public void markResetPending() {
        if (this.phase == Phase.RESET_PENDING) return;
        this.phase = Phase.RESET_PENDING;
        setDirty();
    }

    public boolean requestGenerationByEntry() {
        if (this.phase != Phase.RESET_PENDING) return false;
        if (!this.generationRequestedByEntry) {
            this.generationRequestedByEntry = true;
            setDirty();
        }
        return true;
    }

    public boolean isGenerationRequestedByEntry() {
        return this.generationRequestedByEntry;
    }

    public void addPendingForcedRespawn(UUID playerId) {
        if (this.pendingForcedRespawns.add(playerId)) setDirty();
    }

    public boolean hasPendingForcedRespawn(UUID playerId) {
        return this.pendingForcedRespawns.contains(playerId);
    }

    public void markPlayerInOverworldLike(UUID playerId) {
        if (this.knownOverworldLikePlayers.add(playerId)) setDirty();
    }

    public void removeKnownOverworldLikePlayer(UUID playerId) {
        if (this.knownOverworldLikePlayers.remove(playerId)) setDirty();
    }

    public void enqueueKnownPlayersForForcedRespawn() {
        if (this.pendingForcedRespawns.addAll(this.knownOverworldLikePlayers)) setDirty();
    }

    public void removePendingForcedRespawn(UUID playerId) {
        if (this.pendingForcedRespawns.remove(playerId)) setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("generation", this.generation);
        tag.putLong("generationSeed", this.generationSeed);
        tag.putLong("visualSeed", this.visualSeed);
        tag.putLong("orbitEpochGameTime", this.orbitEpochGameTime);
        tag.putString("phase", this.phase.getSerializedName());
        tag.putLong("collapseStartedAt", this.collapseStartedAt);
        tag.putLong("nextGenerationSeed", this.nextGenerationSeed);
        tag.putBoolean("collapseDamageIssued", this.collapseDamageIssued);
        tag.putBoolean("generationRequestedByEntry", this.generationRequestedByEntry);
        ListTag pending = new ListTag();
        for (UUID playerId : this.pendingForcedRespawns) {
            CompoundTag player = new CompoundTag();
            player.putUUID("id", playerId);
            pending.add(player);
        }
        tag.put("pendingForcedRespawns", pending);
        ListTag known = new ListTag();
        for (UUID playerId : this.knownOverworldLikePlayers) {
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
