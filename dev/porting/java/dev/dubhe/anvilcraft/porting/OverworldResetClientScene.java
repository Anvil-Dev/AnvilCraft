package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import dev.dubhe.anvilcraft.client.support.OverworldLikeClientState;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.saved.OverworldLikeResetManifest;
import dev.dubhe.anvilcraft.saved.OverworldLikeWorldState;
import dev.dubhe.anvilcraft.saved.WormholeNetwork;
import dev.dubhe.anvilcraft.worldgen.OverworldLikeGenerationBootstrap;
import dev.dubhe.anvilcraft.worldgen.OverworldLikeResetManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;

import java.util.Set;
import java.util.UUID;

public final class OverworldResetClientScene {
    private static final BlockPos CENTER = new BlockPos(8, 80, 8);
    private static final BlockPos MARKER = new BlockPos(0, 299, 0);
    private static final UUID OFFLINE = new UUID(0x435, 0x777);
    private static final UUID WORMHOLE = new UUID(0x222, 0x777);
    private static volatile boolean busy;
    private static volatile RuntimeException failure;
    private static volatile int stage;
    private static ServerLevel oldLevel;
    private static long oldSeed;
    private static long nextSeed;
    private static int oldGeneration;
    private static boolean captured;
    private static boolean respawnRequested;
    private static long deadline;

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 300000;
        if (failure != null) throw failure;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Overworld reset timed out at " + stage);
        client.options.pauseOnLostFocus = false;
        client.options.hideGui = false;
        if (client.player.isDeadOrDying() && !respawnRequested) {
            respawnRequested = true;
            client.getConnection().send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
        }
        if (stage == 2 && !captured && OverworldLikeClientState.collapseProgress() >= 0.5F) {
            captured = true;
            Screenshot.grab(client.gameDirectory, "overworld-reset-flash-26.1.png", client.getMainRenderTarget(), 1,
                message -> AnvilCraft.LOGGER.info("PORT_OVERWORLD_COLLAPSE_CAPTURED"));
        }
        if (busy) return;
        if (stage == 0) {
            client.setScreen(null);
            server(client, () -> {
                var server = client.getSingleplayerServer();
                oldLevel = server.getLevel(CelestialTravelManager.OVERWORLD_LIKE_LEVEL);
                if (oldLevel == null) throw new IllegalStateException("Builtin overworld-like dimension missing");
                var manifest = OverworldLikeGenerationBootstrap.getManifest(server);
                oldSeed = oldLevel.getSeed();
                nextSeed = manifest.nextSeed();
                oldGeneration = manifest.generation();
                if (oldSeed != manifest.activeSeed() || oldSeed == server.overworld().getSeed()) {
                    throw new IllegalStateException("Dimension did not use the independent manifest seed");
                }
                oldLevel.setBlock(MARKER, Blocks.DIAMOND_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
                server.overworld().getWorldBorder().setSize(512);
                if (oldLevel.getWorldBorder().getSize() != 512) throw new IllegalStateException("Border did not follow overworld");
                var portal = CelestialTravelTests.machine(server.overworld(), CENTER,
                    CelestialTravelTests.travel(CelestialTravelManager.OVERWORLD_LIKE_LEVEL, MARKER.above()));
                var be = portal.findParentCfa();
                be.setLocked(true);
                be.getMaterialContainer().setItem(0, ModBlocks.RUBY_PRISM.asStack(16));
                var options = be.getClientVisibleOptions();
                int index = -1;
                for (int i = 0; i < options.size(); i++) {
                    if (options.get(i).megastructure().equals("planet_excavator")) index = i;
                }
                if (index < 0) throw new IllegalStateException("Excavator option missing");
                be.buildMegastructure(index);
                OverworldLikeWorldState.get(server).markPlayerInOverworldLike(OFFLINE);
                WormholeNetwork.get().register(WORMHOLE, oldLevel, MARKER);
                var player = server.getPlayerList().getPlayers().getFirst();
                player.setNoGravity(true);
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
                player.teleportTo(oldLevel, 0.5, 300, 0.5, Set.of(), 0, 0, false);
                player.setDeltaMovement(Vec3.ZERO);
                stage = 1;
                AnvilCraft.LOGGER.info("PORT_OVERWORLD_SEED: generation={}, active={}, next={}", oldGeneration, oldSeed, nextSeed);
            });
        } else if (stage == 1 && client.screen == null && client.getOverlay() == null
            && CelestialTravelManager.isOverworldLike(client.level.dimension())
            && client.level.getBlockState(MARKER).is(Blocks.DIAMOND_BLOCK)) {
            server(client, () -> {
                var server = client.getSingleplayerServer();
                var be = (CelestialForgingAnvilBlockEntity) server.overworld().getBlockEntity(CENTER);
                var laserPos = CENTER.east(2);
                server.overworld().setBlock(laserPos, ModBlocks.CELESTIAL_FORGING_ANVIL_LASER_INTERFACE.getDefaultState(),
                    Block.UPDATE_CLIENTS);
                var laser = (dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilLaserInterfaceBlockEntity)
                    server.overworld().getBlockEntity(laserPos);
                laser.onLaserReceived(1, true);
                be.getMegastructureManager().serverTick(be);
                if (OverworldLikeWorldState.get(server).phase() != OverworldLikeWorldState.Phase.COLLAPSING
                    || be.hasActiveMegastructure()) throw new IllegalStateException("Gamma excavation did not begin collapse");
                AnvilCraft.LOGGER.info("PORT_OVERWORLD_GAMMA_COLLAPSE_PASSED");
                if (OverworldLikeResetManager.getEntryDestination(server, CelestialTravelManager.OVERWORLD_LIKE_LEVEL) != null) {
                    throw new IllegalStateException("Collapsing generation accepted an entry");
                }
                if (!WormholeNetwork.get().getConnected(WORMHOLE, Level.OVERWORLD, BlockPos.ZERO).isEmpty()) {
                    throw new IllegalStateException("Collapse retained stale wormhole nodes");
                }
                stage = 2;
            });
        } else if (stage == 2) {
            server(client, () -> {
                var server = client.getSingleplayerServer();
                var state = OverworldLikeWorldState.get(server);
                if (state.phase() != OverworldLikeWorldState.Phase.RESET_PENDING) return;
                if (server.getLevel(CelestialTravelManager.OVERWORLD_LIKE_LEVEL) != oldLevel) {
                    throw new IllegalStateException("Generation replaced before a new entry request");
                }
                if (OverworldLikeResetManager.getEntryDestination(server, CelestialTravelManager.OVERWORLD_LIKE_LEVEL) != null) {
                    throw new IllegalStateException("Pending generation was entered before replacement");
                }
                stage = 3;
            });
        } else if (stage == 3) {
            server(client, () -> {
                var server = client.getSingleplayerServer();
                var state = OverworldLikeWorldState.get(server);
                if (state.phase() != OverworldLikeWorldState.Phase.ACTIVE || state.generation() != oldGeneration + 1) return;
                var replacement = server.getLevel(CelestialTravelManager.OVERWORLD_LIKE_LEVEL);
                if (replacement == oldLevel || replacement.getSeed() != nextSeed
                    || replacement.getBlockState(MARKER).is(Blocks.DIAMOND_BLOCK)) {
                    throw new IllegalStateException("Replacement retained old level, seed or terrain marker");
                }
                if (OverworldLikeResetManager.getEntryDestination(server, CelestialTravelManager.OVERWORLD_LIKE_LEVEL) != replacement) {
                    throw new IllegalStateException("Completed generation did not reopen the landing path");
                }
                if (!state.hasPendingForcedRespawn(OFFLINE)) throw new IllegalStateException("Offline evacuation was lost");
                WormholeNetwork.get().register(WORMHOLE, oldLevel, MARKER);
                if (!WormholeNetwork.get().getConnected(WORMHOLE, Level.OVERWORLD, BlockPos.ZERO).isEmpty()) {
                    throw new IllegalStateException("Closed generation re-registered a wormhole node");
                }
                server.overworld().getWorldBorder().setSize(768);
                if (replacement.getWorldBorder().getSize() != 768 || oldLevel.getWorldBorder().getSize() != 512) {
                    throw new IllegalStateException("Border listener was not transferred to the new level");
                }
                try {
                    if (!OverworldLikeResetManifest.read(server.getWorldPath(LevelResource.ROOT)).equals(
                        OverworldLikeGenerationBootstrap.getManifest(server))) {
                        throw new IllegalStateException("Manifest was not persisted");
                    }
                } catch (java.io.IOException exception) {
                    throw new IllegalStateException(exception);
                }
                stage = 4;
                AnvilCraft.LOGGER.info("PORT_OVERWORLD_REPLACED: generation={}, seed={}", state.generation(), replacement.getSeed());
            });
        } else if (stage == 4 && client.level.dimension().equals(Level.OVERWORLD)) {
            if (!captured || !respawnRequested) throw new IllegalStateException("Collapse flash/death/respawn were not observed");
            AnvilCraft.LOGGER.info("PORT_OVERWORLD_RESET_PASSED: death, respawn, entry lock, new terrain/seed, "
                + "offline state, border and wormholes");
            client.stop();
            stage = 5;
        }
    }

    private static void server(Minecraft client, Runnable action) {
        busy = true;
        client.getSingleplayerServer().execute(() -> {
            try {
                action.run();
            } catch (RuntimeException exception) {
                failure = exception;
            } finally {
                busy = false;
            }
        });
    }
}
