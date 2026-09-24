package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.CelestialBackGateBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilPortalBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

public final class CelestialTravelClientScene {
    private static final BlockPos CENTER = new BlockPos(8, 80, 8);
    private static final BlockPos DESTINATION = new BlockPos(160, 100, 160);
    private static final ResourceKey<Level> VOID = ResourceKey.create(Registries.DIMENSION, AnvilCraft.of("port_travel_void"));
    private static boolean requested;
    private static volatile boolean ready;
    private static volatile RuntimeException failure;
    private static boolean capturing;
    private static int stage;
    private static long next;
    private static long deadline;

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 240000;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Player travel timed out at " + stage);
        if (failure != null) throw failure;
        client.options.pauseOnLostFocus = false;
        client.options.hideGui = true;
        client.options.fov().set(70);
        if (!requested) {
            requested = true;
            client.setScreen(null);
            server(client, () -> {
                var server = client.getSingleplayerServer();
                if (server.getLevel(VOID) == null) throw new IllegalStateException("Integrated server did not load fixture dimension");
                var level = server.overworld();
                for (int x = -4; x <= 4; x++) {
                    for (int z = -6; z <= 4; z++) {
                        level.setBlock(CENTER.offset(x, -1, z), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
                CelestialTravelTests.machine(level, CENTER, CelestialTravelTests.travel(VOID, DESTINATION));
                var player = server.getPlayerList().getPlayers().getFirst();
                player.setNoGravity(true);
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
                player.teleportTo(level, 8.5, 80, 2.5, Set.<Relative>of(), 0, 0, false);
                player.setDeltaMovement(Vec3.ZERO);
            });
            return;
        }
        if (!ready || capturing || System.currentTimeMillis() < next || client.getOverlay() != null) return;
        switch (stage) {
            case 0 -> {
                if (!(client.level.getBlockEntity(CENTER.north(2)) instanceof CelestialForgingAnvilPortalBlockEntity)) return;
                stage = 1;
                server(client, () -> {
                    var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                    player.teleportTo(player.level(), 8.5, 80, 6.5, Set.<Relative>of(), 0, 0, false);
                    player.setDeltaMovement(Vec3.ZERO);
                });
            }
            case 1 -> {
                if (!client.level.dimension().equals(VOID)) return;
                if (client.player.position().distanceTo(Vec3.atBottomCenterOf(DESTINATION.north())) > 0.1) {
                    return;
                }
                AnvilCraft.LOGGER.info("PORT_TRAVEL_PLAYER_OUTBOUND: dimension={}, position={}",
                    client.level.dimension(), client.player.position());
                stage = 2;
                server(client, () -> {
                    var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                    var gate = player.level().getBlockEntity(DESTINATION.above());
                    if (!(gate instanceof CelestialBackGateBlockEntity returning)
                        || !CENTER.north(2).equals(returning.getReturnPortalPos())) {
                        throw new IllegalStateException("Player landing did not create a linked return gate");
                    }
                    player.teleportTo(player.level(), 160.5, 100, 156.5, Set.<Relative>of(), 0, 0, false);
                    player.setDeltaMovement(Vec3.ZERO);
                });
                next = System.currentTimeMillis() + 1500;
            }
            case 2 -> capture(client, "return-gate", 3);
            case 3 -> server(client, () -> {
                var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                if (player.isOnPortalCooldown()) return;
                player.teleportTo(player.level(), 160.5, 100, 160.5, Set.<Relative>of(), 180, 0, false);
                player.setDeltaMovement(Vec3.ZERO);
                stage = 4;
            });
            case 4 -> {
                if (!client.level.dimension().equals(Level.OVERWORLD)) return;
                if (client.player.position().distanceTo(new Vec3(8.5, 80, 5.5)) > 0.1) {
                    return;
                }
                AnvilCraft.LOGGER.info("PORT_TRAVEL_PLAYER_RETURNED: dimension={}, position={}",
                    client.level.dimension(), client.player.position());
                stage = 5;
                server(client, () -> {
                    var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                    player.teleportTo(player.level(), 8.5, 80, 2.5, Set.<Relative>of(), 0, 0, false);
                    player.setDeltaMovement(Vec3.ZERO);
                });
                next = System.currentTimeMillis() + 1000;
            }
            case 5 -> capture(client, "source-gate", 6);
            default -> {
                client.options.hideGui = false;
                AnvilCraft.LOGGER.info("PORT_TRAVEL_PLAYER_PASSED: live CFA landing, generated gate and cross-dimension return");
                client.stop();
            }
        }
    }

    private static void server(Minecraft client, Runnable action) {
        ready = false;
        client.getSingleplayerServer().execute(() -> {
            try {
                action.run();
            } catch (RuntimeException exception) {
                failure = exception;
            } finally {
                ready = true;
            }
        });
    }

    private static void capture(Minecraft client, String name, int nextStage) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "celestial-travel-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                stage = nextStage;
            }));
    }
}
