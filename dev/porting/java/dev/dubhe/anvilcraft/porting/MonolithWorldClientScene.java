package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.GiantMonolithCoreBlock;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.config.AnvilCraftClientConfig.MunLightingQuality;
import dev.dubhe.anvilcraft.event.TheMonolithEventListener;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.worldgen.TheMonolith;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;

public final class MonolithWorldClientScene {
    private static int stage;
    private static long next;
    private static long deadline;
    private static volatile boolean ready;
    private static volatile @Nullable RuntimeException failure;
    private static @Nullable BlockPos core;
    private static @Nullable BlockPos touch;

    public static void frame(Minecraft client) {
        if (failure != null) throw failure;
        if (deadline == 0) deadline = System.currentTimeMillis() + 240000;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Monolith world scene timed out at " + stage);
        client.options.pauseOnLostFocus = false;
        if (stage == 0) {
            stage = 1;
            client.setScreen(null);
            AnvilCraft.CLIENT_CONFIG.munLightingQuality = MunLightingQuality.OFF;
            server(client, () -> {
                var server = Objects.requireNonNull(client.getSingleplayerServer());
                var moon = Objects.requireNonNull(server.getLevel(CelestialTravelManager.MUN_LEVEL));
                var state = TheMonolith.State.get(moon);
                var data = (CompoundTag) TheMonolith.State.CODEC.encodeStart(NbtOps.INSTANCE, state).getOrThrow();
                int[] box = data.getIntArray("BoundingBox").orElseThrow();
                check(box.length == 6, "New world did not generate its Moon monolith");
                TheMonolith.ensureGenerated(moon);
                check(TheMonolith.State.CODEC.encodeStart(NbtOps.INSTANCE, state).getOrThrow().equals(data), "Monolith generated twice");
                var overworld = (CompoundTag) TheMonolith.State.CODEC.encodeStart(NbtOps.INSTANCE,
                    TheMonolith.State.get(server.overworld())).getOrThrow();
                if (Boolean.getBoolean("anvilcraft.portMonolithNormalWorldScene")) {
                    int[] small = overworld.getIntArray("BoundingBox").orElseThrow();
                    check(small.length == 6, "Normal Overworld did not generate its small monolith");
                    for (int x = small[0] - 1; x <= small[3] + 1; x++) {
                        for (int z = small[2] - 1; z <= small[5] + 1; z++) {
                            var ground = server.overworld().getBlockState(new BlockPos(x, small[1] - 1, z));
                            check(!ground.isAir() && ground.getFluidState().isEmpty(), "Small monolith lacks dry prepared ground");
                        }
                    }
                    AnvilCraft.LOGGER.info("PORT_MONOLITH_SMALL_WORLD_GENERATED: {}", java.util.Arrays.toString(small));
                } else {
                    check(!overworld.contains("BoundingBox"), "Flat Overworld received an automatic monolith");
                }
                for (var pos : BlockPos.betweenClosed(new BlockPos(box[0], box[1], box[2]), new BlockPos(box[3], box[4], box[5]))) {
                    var block = moon.getBlockState(pos);
                    if (block.is(ModBlocks.GIANT_MONOLITH_CORE.get())
                        && block.getValue(GiantMonolithCoreBlock.HALF) == Cube3x3PartHalf.MID_CENTER) core = pos.immutable();
                    if (touch == null && block.is(ModBlocks.MONOLITH.get())) touch = pos.immutable();
                }
                final var target = Objects.requireNonNull(core);
                var player = server.getPlayerList().getPlayers().getFirst();
                player.setNoGravity(true);
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
                player.teleportTo(moon, target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 4.5,
                    Set.<Relative>of(), 180, 0, false);
                AnvilCraft.LOGGER.info("PORT_MONOLITH_WORLD_GENERATED: core={}, box={}", core, java.util.Arrays.toString(box));
            });
            next = System.currentTimeMillis() + 6500;
            return;
        }
        if (!ready || System.currentTimeMillis() < next || client.level == null
            || client.screen != null || client.getOverlay() != null) return;
        if (stage == 1 && client.level.dimension().equals(CelestialTravelManager.MUN_LEVEL)) {
            stage = 2;
            server(client, () -> {
                var server = Objects.requireNonNull(client.getSingleplayerServer());
                var player = server.getPlayerList().getPlayers().getFirst();
                var progress = (Map<?, ?>) field("PROGRESS");
                var nearby = (Map<?, ?>) progress.get(player);
                check(nearby != null && (Integer) nearby.get(Objects.requireNonNull(core)) == 101, "Proximity hint did not complete once");
                click(player);
                click(player);
                check(player.level().dimension().equals(CelestialTravelManager.MUN_LEVEL), "Same-tick second hand caused a return");
                var touches = touches();
                touches.put(player, player.level().getGameTime() - 61);
                click(player);
                check(touches.get(player) == player.level().getGameTime(), "Expired confirmation did not restart");
                for (int x = 296; x <= 304; x++) {
                    for (int z = 296; z <= 304; z++) {
                        server.overworld().setBlock(new BlockPos(x, 99, z), Blocks.STONE.defaultBlockState(), 2);
                        for (int y = 100; y <= 103; y++) {
                            server.overworld().setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                }
                player.setRespawnPosition(new ServerPlayer.RespawnConfig(
                    new LevelData.RespawnData(GlobalPos.of(Level.OVERWORLD, new BlockPos(300, 100, 300)), 0, 0), true), false);
                touches.put(player, player.level().getGameTime() - 60);
                click(player);
                check(player.level().dimension().equals(Level.OVERWORLD), "Three-second boundary did not return to respawn");
                check(player.getDeltaMovement().lengthSqr() == 0 && player.fallDistance == 0, "Return retained fall motion");
                check(!touches.containsKey(player) && !progress.containsKey(player), "Dimension leave retained interaction state");
            });
        } else if (stage == 2 && client.level.dimension().equals(Level.OVERWORLD)) {
            check(client.player != null && client.player.position().distanceTo(new Vec3(300.5, 100.1, 300.5)) < 1,
                "Client did not arrive at the configured respawn point");
            AnvilCraft.LOGGER.info("PORT_MONOLITH_WORLD_PASSED: automatic generation, POI hint, confirmation window and respawn return");
            stage = 3;
            client.stop();
        }
    }

    private static void click(ServerPlayer player) {
        var pos = Objects.requireNonNull(touch);
        var event = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos,
            new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false));
        TheMonolithEventListener.onRightClickBlock(event);
        check(event.isCanceled(), "Monolith touch did not consume the interaction");
    }

    @SuppressWarnings("unchecked")
    private static Map<ServerPlayer, Long> touches() {
        return (Map<ServerPlayer, Long>) field("RETURN_TOUCHES");
    }

    private static Object field(String name) {
        try {
            var field = TheMonolithEventListener.class.getDeclaredField(name);
            field.setAccessible(true);
            return Objects.requireNonNull(field.get(null));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void server(Minecraft client, Runnable action) {
        ready = false;
        Objects.requireNonNull(client.getSingleplayerServer()).execute(() -> {
            try {
                action.run();
                ready = true;
            } catch (RuntimeException exception) {
                failure = exception;
            }
        });
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
