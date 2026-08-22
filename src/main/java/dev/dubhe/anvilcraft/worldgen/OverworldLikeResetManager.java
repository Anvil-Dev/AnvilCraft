package dev.dubhe.anvilcraft.worldgen;

import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelData;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import dev.dubhe.anvilcraft.block.entity.celestial.SpecialCelestialBodyData;
import dev.dubhe.anvilcraft.init.entity.ModDamageTypes;
import dev.dubhe.anvilcraft.network.OverworldLikeCollapsePacket;
import dev.dubhe.anvilcraft.network.OverworldLikeSkyStatePacket;
import dev.dubhe.anvilcraft.saved.OverworldLikeWorldState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import javax.annotation.Nullable;

/** Coordinates collapse, player evacuation, reset locking, and orbital-lighting access. */
public final class OverworldLikeResetManager {
    public static final int COLLAPSE_DELAY_TICKS = 40;
    private static final Map<MinecraftServer, Set<UUID>> QUEUED_DIRECT_ENTRIES = new WeakHashMap<>();

    private OverworldLikeResetManager() {
    }

    public static boolean beginCollapse(CelestialForgingAnvilBlockEntity source) {
        if (!(source.getLevel() instanceof ServerLevel sourceLevel)) return false;
        if (!(source.getCelestialBodyData() instanceof SpecialCelestialBodyData special)) return false;
        if (!special.canBeShattered()) return false;
        CelestialTravelData landing = special.landing();
        if (landing == null || !CelestialTravelManager.OVERWORLD_LIKE_DIMENSION.equals(landing.dimension())) return false;
        if (source.getActiveMegastructureOption() == null
            || !"planet_excavator".equals(source.getActiveMegastructureOption().megastructure())) {
            return false;
        }

        MinecraftServer server = sourceLevel.getServer();
        OverworldLikeWorldState state = OverworldLikeWorldState.get(server);
        if (state.phase() != OverworldLikeWorldState.Phase.ACTIVE) return false;
        long nextSeed = OverworldLikeGenerationBootstrap.getManifest(server).nextSeed();
        if (!OverworldLikeGenerationBootstrap.requestReset(server, nextSeed)) return false;
        if (!state.beginCollapse(sourceLevel.getGameTime(), nextSeed)) return false;

        state.enqueueKnownPlayersForForcedRespawn();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (CelestialTravelManager.isOverworldLike(player.level().dimension())) {
                state.addPendingForcedRespawn(player.getUUID());
            }
        }
        syncStateToAll(server, state);
        ServerLevel overworldLike = server.getLevel(CelestialTravelManager.OVERWORLD_LIKE_LEVEL);
        if (overworldLike != null) {
            PacketDistributor.sendToPlayersInDimension(overworldLike, new OverworldLikeCollapsePacket(0));
        }
        return true;
    }

    @Nullable
    public static ServerLevel getEntryDestination(MinecraftServer server, ResourceKey<Level> destination) {
        if (!CelestialTravelManager.isOverworldLike(destination)) return server.getLevel(destination);
        OverworldLikeWorldState state = OverworldLikeWorldState.get(server);
        if (state.phase() == OverworldLikeWorldState.Phase.RESET_PENDING) {
            state.requestGenerationByEntry();
            return null;
        }
        if (state.phase() != OverworldLikeWorldState.Phase.ACTIVE
            || OverworldLikeGenerationBootstrap.getManifest(server).resetPending()) {
            return null;
        }
        return server.getLevel(destination);
    }

    public static int modifySkyDarken(Level level, int original) {
        if (!(level instanceof ServerLevel serverLevel) || !CelestialTravelManager.isOverworldLike(level.dimension())) {
            return original;
        }
        OverworldLikeWorldState state = OverworldLikeWorldState.get(serverLevel.getServer());
        if (state.phase() == OverworldLikeWorldState.Phase.RESET_PENDING) return original;
        int eclipseDarken = state.eclipseDarken(level.getGameTime(), level.getDayTime());
        return Math.min(15, original + eclipseDarken);
    }

    public static void tick(MinecraftServer server) {
        OverworldLikeWorldState state = OverworldLikeWorldState.get(server);
        if (state.phase() == OverworldLikeWorldState.Phase.COLLAPSING) {
            ServerLevel overworldLike = server.getLevel(CelestialTravelManager.OVERWORLD_LIKE_LEVEL);
            long gameTime = overworldLike == null ? server.overworld().getGameTime() : overworldLike.getGameTime();
            long elapsed = Math.max(0L, gameTime - state.collapseStartedAt());
            if (elapsed >= COLLAPSE_DELAY_TICKS && !state.collapseDamageIssued()) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    if (!CelestialTravelManager.isOverworldLike(player.level().dimension())) continue;
                    state.addPendingForcedRespawn(player.getUUID());
                    player.hurt(ModDamageTypes.planetaryCollapse(player.level()), Float.MAX_VALUE);
                }
                state.markCollapseDamageIssued();
            }
            if (state.collapseDamageIssued()) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    if (!CelestialTravelManager.isOverworldLike(player.level().dimension())) continue;
                    state.addPendingForcedRespawn(player.getUUID());
                    if (player.isAlive()) forceReturn(player, state);
                }
            }
            if (allOnlinePlayersOutsideOverworldLike(server)) {
                state.markResetPending();
                syncStateToAll(server, state);
            }
        }

        if (state.phase() == OverworldLikeWorldState.Phase.RESET_PENDING
            && state.isGenerationRequestedByEntry()
            && OverworldLikeGenerationBootstrap.activatePendingGeneration(server)) {
            OverworldLikeWorldState activeState = OverworldLikeWorldState.get(server);
            syncStateToAll(server, activeState);
            moveQueuedDirectEntries(server);
        }
    }

    public static void onPlayerLoggedIn(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        OverworldLikeWorldState state = OverworldLikeWorldState.get(server);
        if (state.hasPendingForcedRespawn(player.getUUID())
            || state.phase() != OverworldLikeWorldState.Phase.ACTIVE
            && CelestialTravelManager.isOverworldLike(player.level().dimension())) {
            state.addPendingForcedRespawn(player.getUUID());
            forceReturn(player, state);
        } else if (CelestialTravelManager.isOverworldLike(player.level().dimension())) {
            state.markPlayerInOverworldLike(player.getUUID());
        }
        syncTo(player, state);
    }

    public static void onPlayerRespawn(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        OverworldLikeWorldState state = OverworldLikeWorldState.get(server);
        if (state.hasPendingForcedRespawn(player.getUUID())
            || state.phase() != OverworldLikeWorldState.Phase.ACTIVE
            && CelestialTravelManager.isOverworldLike(player.level().dimension())) {
            state.addPendingForcedRespawn(player.getUUID());
            forceReturn(player, state);
        } else if (CelestialTravelManager.isOverworldLike(player.level().dimension())) {
            state.markPlayerInOverworldLike(player.getUUID());
        }
        syncTo(player, state);
    }

    public static void onPlayerChangedDimension(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        OverworldLikeWorldState state = OverworldLikeWorldState.get(server);
        boolean isInOverworldLike = CelestialTravelManager.isOverworldLike(player.level().dimension());
        if (!isInOverworldLike) {
            state.removeKnownOverworldLikePlayer(player.getUUID());
            state.removePendingForcedRespawn(player.getUUID());
        } else if (state.hasPendingForcedRespawn(player.getUUID())
            || state.phase() == OverworldLikeWorldState.Phase.COLLAPSING) {
            state.addPendingForcedRespawn(player.getUUID());
            forceReturn(player, state);
        } else if (state.phase() == OverworldLikeWorldState.Phase.RESET_PENDING) {
            forceReturn(player, state);
            queueDirectEntry(server, player.getUUID());
            state.requestGenerationByEntry();
            return;
        } else {
            state.markPlayerInOverworldLike(player.getUUID());
        }
        syncTo(player, state);
    }

    private static void forceReturn(ServerPlayer player, OverworldLikeWorldState state) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        ServerLevel overworld = server.overworld();
        BlockPos fallback = overworld.getSharedSpawnPos();
        BlockPos landing = CelestialTravelManager.findSafeLandingPos(overworld, fallback);
        if (landing == null) landing = fallback;
        state.removePendingForcedRespawn(player.getUUID());
        state.removeKnownOverworldLikePlayer(player.getUUID());
        player.teleportTo(
            overworld,
            landing.getX() + 0.5D,
            landing.getY(),
            landing.getZ() + 0.5D,
            player.getYRot(),
            player.getXRot()
        );
    }

    private static boolean allOnlinePlayersOutsideOverworldLike(MinecraftServer server) {
        return server.getPlayerList().getPlayers().stream()
            .noneMatch(player -> CelestialTravelManager.isOverworldLike(player.level().dimension()));
    }

    private static void queueDirectEntry(MinecraftServer server, UUID playerId) {
        QUEUED_DIRECT_ENTRIES.computeIfAbsent(server, ignored -> new HashSet<>()).add(playerId);
    }

    private static void moveQueuedDirectEntries(MinecraftServer server) {
        Set<UUID> queued = QUEUED_DIRECT_ENTRIES.remove(server);
        if (queued == null || queued.isEmpty()) return;
        ServerLevel destination = server.getLevel(CelestialTravelManager.OVERWORLD_LIKE_LEVEL);
        if (destination == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!queued.contains(player.getUUID()) || !player.isAlive()) continue;
            BlockPos fallback = destination.getSharedSpawnPos();
            BlockPos landing = CelestialTravelManager.findSafeLandingPos(destination, fallback);
            if (landing == null) landing = fallback;
            player.teleportTo(
                destination,
                landing.getX() + 0.5D,
                landing.getY(),
                landing.getZ() + 0.5D,
                player.getYRot(),
                player.getXRot()
            );
        }
    }

    private static void syncStateToAll(MinecraftServer server, OverworldLikeWorldState state) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncTo(player, state);
        }
    }

    private static void syncTo(ServerPlayer player, OverworldLikeWorldState state) {
        PacketDistributor.sendToPlayer(
            player,
            new OverworldLikeSkyStatePacket(
                state.generation(),
                state.visualSeed(),
                state.orbitEpochGameTime(),
                state.phase()
            )
        );
        if (state.phase() == OverworldLikeWorldState.Phase.COLLAPSING
            && CelestialTravelManager.isOverworldLike(player.level().dimension())) {
            long elapsed = Math.max(0L, player.level().getGameTime() - state.collapseStartedAt());
            PacketDistributor.sendToPlayer(player, new OverworldLikeCollapsePacket((int) Math.min(Integer.MAX_VALUE, elapsed)));
        }
    }
}
