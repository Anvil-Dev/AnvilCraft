package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.util.EnchantedGoldBlockPositions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class EnchantedGoldClientScene {
    private static final List<BlockPos> POSITIONS = List.of(new BlockPos(-2, 161, 5), new BlockPos(-1, 161, 5),
        new BlockPos(1, 161, 5), new BlockPos(3, 161, 5));
    private static int stage;
    private static boolean started;
    private static boolean requested;
    private static boolean capturing;
    private static boolean reloading;
    private static boolean effectsVerified;
    private static long effectsClearedAt;
    private static volatile boolean prepared;
    private static volatile boolean reloaded;
    private static volatile @Nullable Throwable failure;
    private static long deadline;
    private static long next;

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 180000;
        if (failure != null || System.currentTimeMillis() > deadline) {
            throw new IllegalStateException("Enchanted gold scene " + stage, failure);
        }
        if (capturing) return;
        if (!started) {
            started = true;
            client.getSingleplayerServer().execute(() -> {
                try {
                    var server = client.getSingleplayerServer();
                    var level = server.overworld();
                    for (var pos : BlockPos.betweenClosed(-6, 160, 2, 6, 160, 8)) {
                        level.setBlockAndUpdate(pos, Blocks.GRAY_CONCRETE.defaultBlockState());
                    }
                    for (var pos : POSITIONS) level.setBlockAndUpdate(pos, ModBlocks.ENCHANTED_GOLD_BLOCK.getDefaultState());
                    level.setBlockAndUpdate(new BlockPos(1, 161, 6), Blocks.STONE.defaultBlockState());
                    var player = server.getPlayerList().getPlayers().getFirst();
                    player.setGameMode(GameType.SURVIVAL);
                    player.setNoGravity(true);
                    player.setItemInHand(InteractionHand.MAIN_HAND, ModItems.ENCHANTED_GOLD_INGOT.asStack(64));
                    player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200));
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tick unfreeze");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set 6000");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 0 163 12 180 12");
                    prepared = true;
                } catch (Throwable error) {
                    failure = error;
                }
            });
            return;
        }
        if (!prepared || client.screen != null || client.getOverlay() != null) return;
        client.player.setNoGravity(true);
        client.player.setPos(0, 163, 12);
        client.player.setDeltaMovement(Vec3.ZERO);
        client.player.setYRot(180);
        client.player.setXRot(12);
        client.options.hideGui = true;
        client.options.fov().set(70);
        client.options.bobView().set(false);
        client.options.glintStrength().set(1.0);
        if (!effectsVerified) {
            if (!client.player.hasEffect(MobEffects.LUCK) || client.player.hasEffect(MobEffects.WEAKNESS)) return;
            effectsVerified = true;
            effectsClearedAt = System.currentTimeMillis() + 3000;
            client.getSingleplayerServer().execute(() -> {
                var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                player.setItemInHand(InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
                player.removeEffect(MobEffects.LUCK);
            });
        }
        if (client.player.hasEffect(MobEffects.LUCK) || System.currentTimeMillis() < effectsClearedAt) return;
        if (stage == 5) {
            EnchantedGoldJeiProbe.verify();
            AnvilCraft.LOGGER.info("PORT_ENCHANTED_GOLD_CLIENT_PASSED: live effects, block tracking, glint phases, occlusion and reload");
            client.stop();
            return;
        }
        if (!requested) {
            if (stage == 0 && !EnchantedGoldBlockPositions.getPositions().containsAll(POSITIONS)) return;
            requested = true;
            EnchantedGoldReferenceClock.install(stage == 2 ? 18000 : 6000);
            if (stage == 0) {
                EnchantedGoldBlockPositions.clear();
            } else {
                for (var pos : POSITIONS) EnchantedGoldBlockPositions.scanChunk(client.level.getChunkAt(pos));
            }
            if (stage == 3) {
                client.getSingleplayerServer().execute(() -> {
                    var level = client.getSingleplayerServer().overworld();
                    level.setBlockAndUpdate(POSITIONS.getFirst(), Blocks.STONE.defaultBlockState());
                });
            }
            if (stage == 4 && !reloading) {
                reloading = true;
                client.reloadResourcePacks().whenComplete((ignored, error) -> {
                    failure = error;
                    reloaded = true;
                });
            }
            next = System.currentTimeMillis() + 1600;
            return;
        }
        if (stage == 0) EnchantedGoldBlockPositions.clear();
        if (stage == 3 && (client.level.getBlockState(POSITIONS.getFirst()).is(ModBlocks.ENCHANTED_GOLD_BLOCK)
            || EnchantedGoldBlockPositions.getPositions().contains(POSITIONS.getFirst()))) {
            return;
        }
        if (stage == 4 && !reloaded) return;
        if (System.currentTimeMillis() < next) return;
        capturing = true;
        Screenshot.grab(client.gameDirectory, "enchanted-gold-26.1-" + stage + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                AnvilCraft.LOGGER.info("PORT_ENCHANTED_GOLD_CAPTURE: {}", stage);
                capturing = false;
                requested = false;
                stage++;
            }));
    }
}
