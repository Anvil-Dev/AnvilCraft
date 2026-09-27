package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.tool.HeavyHalberdItem;
import dev.dubhe.anvilcraft.item.tool.HeavyHalberdMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class CrabClawClientScene {
    private static final String[] CASES = {"stone", "slab", "panel", "fence", "pipe", "valve", "torch", "trident",
        "throwing", "halberd_spear", "halberd_throwing", "cfa", "cfa_plain", "halberd_trident", "halberd_mace", "halberd_sword"};
    private static boolean started;
    private static boolean freezeRequested;
    private static volatile boolean frozen;
    private static volatile long settledAt;
    private static boolean requested;
    private static boolean capturing;
    private static boolean backgroundRequested;
    private static boolean backgroundCaptured;
    private static boolean foregroundReady;
    private static boolean reloading;
    private static volatile boolean prepared;
    private static volatile boolean supplied;
    private static volatile boolean reloaded;
    private static volatile @Nullable Throwable failure;
    private static int stage;
    private static long deadline;
    private static long next;

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 240000;
        if (failure != null || System.currentTimeMillis() > deadline) throw new IllegalStateException("Crab scene " + stage, failure);
        if (capturing) return;
        if (!started) {
            started = true;
            client.getSingleplayerServer().execute(() -> {
                try {
                    var server = client.getSingleplayerServer();
                    var level = server.overworld();
                    for (var pos : BlockPos.betweenClosed(-8, 179, 5, 8, 192, 6)) {
                        level.setBlockAndUpdate(pos, Blocks.LIME_CONCRETE.defaultBlockState());
                    }
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set 6000");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 0 184 0 0 0");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tick unfreeze");
                    server.getPlayerList().getPlayers().getFirst().setNoGravity(true);
                    settledAt = System.currentTimeMillis() + 4000;
                    prepared = true;
                } catch (Throwable error) {
                    failure = error;
                }
            });
            return;
        }
        if (!prepared || client.screen != null || client.getOverlay() != null) return;
        if (!frozen) {
            if (System.currentTimeMillis() < settledAt) return;
            if (!freezeRequested) {
                freezeRequested = true;
                client.getSingleplayerServer().execute(() -> {
                    var server = client.getSingleplayerServer();
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tick freeze");
                    frozen = true;
                });
            }
            return;
        }
        client.level.setTimeFromServer(500);
        client.level.clockManager().handleUpdates(500, java.util.Map.of(
            client.level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.WORLD_CLOCK)
                .getOrThrow(net.minecraft.world.clock.WorldClocks.OVERWORLD),
            new net.minecraft.world.clock.ClockNetworkState(6000, 0, 0)));
        client.level.environmentAttributes().invalidateTickCache();
        client.options.hideGui = false;
        client.options.guiScale().set(2);
        client.getToastManager().clear();
        client.options.fov().set(70);
        client.options.bobView().set(false);
        client.options.fovEffectScale().set(0.0);
        client.player.setPos(0, 184, 0);
        client.player.setDeltaMovement(Vec3.ZERO);
        client.player.setYRot(0);
        client.player.setXRot(0);
        client.gui.getChat().clearMessages(false);
        if (stage == CASES.length * 2) {
            if (!reloading) {
                reloading = true;
                client.reloadResourcePacks().whenComplete((value, error) -> {
                    failure = error;
                    reloaded = true;
                });
            }
            if (!reloaded) return;
            CrabClawRenderProbe.verify(client);
            AnvilCraft.LOGGER.info("PORT_CRAB_CLAW_PASSED: 32 hand captures, model selection, transforms and reload");
            client.stop();
            return;
        }
        int index = stage % CASES.length;
        client.options.keyUse.setDown(index == 8 || index == 10);
        var arm = stage < CASES.length ? HumanoidArm.RIGHT : HumanoidArm.LEFT;
        client.player.setMainArm(arm);
        if (!requested) {
            requested = true;
            supplied = false;
            client.getSingleplayerServer().execute(() -> {
                try {
                    var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                    player.stopUsingItem();
                    player.setMainArm(arm);
                    player.setItemInHand(InteractionHand.MAIN_HAND, item(index));
                    player.setItemInHand(InteractionHand.OFF_HAND, index == 12 ? ItemStack.EMPTY : ModItems.CRAB_CLAW.asStack());
                    if (index == 9) HeavyHalberdItem.setMode(player, InteractionHand.MAIN_HAND, HeavyHalberdMode.SPEAR);
                    if (index == 14) HeavyHalberdItem.setMode(player, InteractionHand.MAIN_HAND, HeavyHalberdMode.MACE);
                    if (index == 15) HeavyHalberdItem.setMode(player, InteractionHand.MAIN_HAND, HeavyHalberdMode.SWORD);
                    player.containerMenu.broadcastChanges();
                    if (index == 8 || index == 10) player.startUsingItem(InteractionHand.MAIN_HAND);
                    var data = player.getEntityData().packDirty();
                    if (data != null) player.connection.send(new ClientboundSetEntityDataPacket(player.getId(), data));
                    supplied = true;
                } catch (Throwable error) {
                    failure = error;
                }
            });
            client.player.stopUsingItem();
            next = System.currentTimeMillis() + 1800;
            return;
        }
        if (!supplied || System.currentTimeMillis() < next || !client.player.getMainHandItem().is(item(index).getItem())) return;
        if ((index == 8 || index == 10) && !client.player.isUsingItem()) return;
        String label = (arm == HumanoidArm.RIGHT ? "right-" : "left-") + CASES[index];
        if (!backgroundRequested) {
            backgroundRequested = true;
            return;
        }
        if (!backgroundCaptured) {
            capturing = true;
            Screenshot.grab(client.gameDirectory, "crab-26.1-" + label + "-background.png", client.getMainRenderTarget(), 1,
                message -> client.execute(() -> {
                    backgroundCaptured = true;
                    capturing = false;
                }));
            return;
        }
        if (!foregroundReady) {
            foregroundReady = true;
            return;
        }
        if (stage == 0) CrabClawRenderProbe.verify(client);
        if (index == 8 || index == 10) CrabClawRenderProbe.verifyThrowing(client);
        capturing = true;
        Screenshot.grab(client.gameDirectory, "crab-26.1-" + label + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                AnvilCraft.LOGGER.info("PORT_CRAB_CAPTURE: {}", label);
                capturing = false;
                requested = false;
                backgroundRequested = false;
                backgroundCaptured = false;
                foregroundReady = false;
                stage++;
            }));
    }

    public static boolean hidingHands() {
        return backgroundRequested && !foregroundReady;
    }

    private static ItemStack item(int index) {
        return switch (index) {
            case 0 -> new ItemStack(Items.STONE);
            case 1 -> new ItemStack(Items.STONE_SLAB);
            case 2 -> new ItemStack(Items.STONE_PRESSURE_PLATE);
            case 3 -> new ItemStack(Items.OAK_FENCE);
            case 4 -> ModItems.PIPE.asStack();
            case 5 -> ModItems.CHECK_VALVE.asStack();
            case 6 -> new ItemStack(Items.TORCH);
            case 7, 8 -> new ItemStack(Items.TRIDENT);
            case 9, 10, 13, 14, 15 -> ModItems.FROST_METAL_HEAVY_HALBERD.asStack();
            default -> ModBlocks.CELESTIAL_FORGING_ANVIL.asStack();
        };
    }
}
