package dev.dubhe.anvilcraft.porting;

import com.google.gson.GsonBuilder;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModFoodItems;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CursedAppleClientScene {
    private static final String[] CASES = {"overworld-nether", "negative-coordinates", "nether-overworld", "end-respawn", "creative"};
    private static final List<Map<String, Object>> RESULTS = new ArrayList<>();
    private static int index;
    private static int stage;
    private static long deadline;
    private static long next;
    private static volatile Map<String, Object> result;
    private static volatile Throwable failure;

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 240000;
        if (failure != null) throw new IllegalStateException("Apple case " + CASES[index], failure);
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Apple scene " + index + ":" + stage);
        if (client.getOverlay() != null || System.currentTimeMillis() < next) return;
        if (stage == 0) {
            stage = 1;
            result = null;
            client.setScreen(null);
            client.getSingleplayerServer().execute(() -> {
                try {
                    var server = client.getSingleplayerServer();
                    var player = server.getPlayerList().getPlayers().getFirst();
                    perform(player, index);
                } catch (Throwable exception) {
                    failure = exception;
                }
            });
        } else if (stage == 1 && result != null && client.level.dimension().identifier().toString().equals(result.get("to"))) {
            RESULTS.add(result);
            AnvilCraft.LOGGER.info("PORT_CURSED_APPLE_CASE: {} {}", CASES[index], result);
            if (++index == CASES.length) {
                try {
                    Files.writeString(client.gameDirectory.toPath().resolve("cursed-apple-26.1.json"),
                        new GsonBuilder().setPrettyPrinting().create().toJson(RESULTS));
                } catch (java.io.IOException exception) {
                    throw new IllegalStateException(exception);
                }
                AnvilCraft.LOGGER.info("PORT_CURSED_APPLE_PASSED: 5 real-player dimension/consumption cases");
                client.stop();
                stage = 2;
            } else {
                stage = 0;
                next = System.currentTimeMillis() + 1000;
            }
        }
    }

    private static void perform(ServerPlayer player, int test) {
        var server = player.level().getServer();
        ServerLevel source = server.getLevel(test == 2 ? Level.NETHER : test == 3 ? Level.END : Level.OVERWORLD);
        ServerLevel target = server.getLevel(test == 0 || test == 1 || test == 4 ? Level.NETHER : Level.OVERWORLD);
        if (source == null || target == null) throw new IllegalStateException("Missing vanilla dimension");
        int x = test == 1 ? -1 : target.dimension() == Level.NETHER ? 20 : 160;
        int floor = target.dimension() == Level.NETHER ? 100 : 200;
        platform(target, new BlockPos(x, floor, x));
        double origin = test == 1 ? -14.5 : test == 2 ? 20.5 : test == 3 ? 0.5 : 160.5;
        player.teleportTo(source, origin, 180, origin, Set.of(), 37, 11, true);
        if (test == 3) respawn(player);
        player.setGameMode(test == 4 ? GameType.CREATIVE : GameType.SURVIVAL);
        player.getAbilities().invulnerable = true;
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        player.setNoGravity(true);
        player.getFoodData().setFoodLevel(test == 4 ? 20 : 10);
        player.getFoodData().setSaturation(0);
        var stack = new ItemStack(ModFoodItems.CURSED_GOLDEN_APPLE.get(), 2);
        if (!stack.get(DataComponents.FOOD).canAlwaysEat()) throw new IllegalStateException("Apple is not always edible");
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var remaining = stack.finishUsingItem(source, player);
        player.setItemInHand(InteractionHand.MAIN_HAND, remaining);
        player.stopUsingItem();
        if (player.level() != target || player.getBlockX() != x || player.getBlockZ() != x || player.getBlockY() != floor + 1) {
            throw new IllegalStateException("Unexpected landing " + player.level().dimension() + " " + player.position());
        }
        if (remaining.getCount() != (test == 4 ? 2 : 1) || player.getFoodData().getFoodLevel() != (test == 4 ? 20 : 14)) {
            throw new IllegalStateException("Consumption or food value mismatch");
        }
        result = Map.of("case", CASES[test], "from", source.dimension().identifier().toString(),
            "to", target.dimension().identifier().toString(), "x", player.getBlockX(), "y", player.getBlockY(),
            "z", player.getBlockZ(), "remaining", remaining.getCount(), "food", player.getFoodData().getFoodLevel());
    }

    private static void respawn(ServerPlayer player) {
        player.setRespawnPosition(new ServerPlayer.RespawnConfig(new net.minecraft.world.level.storage.LevelData.RespawnData(
            net.minecraft.core.GlobalPos.of(Level.OVERWORLD, new BlockPos(160, 201, 160)), 90, 0), true), false);
    }

    private static void platform(ServerLevel level, BlockPos floor) {
        int top = Math.min(level.getMaxY(), level.getMinY() + level.getLogicalHeight() - 1);
        for (int x = -1; x <= 2; x++) {
            level.setBlock(floor.offset(x, 0, 0), Blocks.STONE.defaultBlockState(), 2);
            for (int y = floor.getY() + 1; y <= top; y++) {
                level.setBlock(new BlockPos(floor.getX() + x, y, floor.getZ()), Blocks.AIR.defaultBlockState(), 2);
            }
        }
    }
}
