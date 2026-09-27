package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.amulet.AmuletManager;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

public final class AmuletClientScene {
    private static int stage;
    private static boolean started;
    private static boolean requested;
    private static boolean capturing;
    private static boolean validationRequested;
    private static volatile boolean ready;
    private static volatile boolean supplied;
    private static volatile boolean validated;
    private static volatile boolean reloaded;
    private static volatile @Nullable Throwable failure;
    private static long deadline;
    private static long next;

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 180000;
        if (failure != null || System.currentTimeMillis() > deadline) {
            throw new IllegalStateException("Amulet client stage " + stage, failure);
        }
        if (capturing) return;
        if (!started) {
            started = true;
            client.getSingleplayerServer().execute(() -> {
                try {
                    var server = client.getSingleplayerServer();
                    var level = server.overworld();
                    for (var pos : BlockPos.betweenClosed(-4, 79, -4, 4, 79, 4)) {
                        level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
                    }
                    var player = server.getPlayerList().getPlayers().getFirst();
                    player.setGameMode(GameType.SURVIVAL);
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set 6000");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 0.5 80 0.5 0 0");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tick unfreeze");
                    ready = true;
                } catch (Throwable error) {
                    failure = error;
                }
            });
            return;
        }
        if (!ready || client.screen != null || client.getOverlay() != null) return;
        client.options.keyShift.setDown(stage == 2);
        client.options.hideGui = false;
        client.options.guiScale().set(2);
        client.getToastManager().clear();
        client.gui.getChat().clearMessages(false);
        if (stage == 5) {
            if (!requested) {
                requested = true;
                var server = client.getSingleplayerServer();
                server.execute(() -> {
                    var player = server.getPlayerList().getPlayers().getFirst();
                    var before = AmuletManager.get(player.registryAccess());
                    var reload = server.reloadResources(server.getPackRepository().getSelectedIds());
                    reload.whenComplete((ignored, error) -> server.execute(() -> {
                        failure = error;
                        if (error == null && before == AmuletManager.get(player.registryAccess())) {
                            failure = new IllegalStateException("Reload did not clear the amulet manager");
                        }
                        reloaded = true;
                    }));
                });
            }
            if (!reloaded) return;
            AnvilCraft.LOGGER.info("PORT_AMULET_CLIENT_PASSED: live player ticks, effects, crouch, attributes, removal and server reload");
            client.stop();
            return;
        }
        if (!requested) {
            requested = true;
            supplied = false;
            validated = false;
            validationRequested = false;
            client.getSingleplayerServer().execute(() -> {
                var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                player.setItemInHand(InteractionHand.MAIN_HAND, switch (stage) {
                    case 0 -> ModItems.GEM_AMULET.asStack();
                    case 1, 2 -> ModItems.FEATHER_AMULET.asStack();
                    case 3 -> ModItems.ANVIL_AMULET.asStack();
                    default -> ItemStack.EMPTY;
                });
                player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                player.containerMenu.broadcastChanges();
                supplied = true;
            });
            next = System.currentTimeMillis() + 1500;
            return;
        }
        if (!supplied || System.currentTimeMillis() < next) return;
        if (stage == 0 && (!client.player.hasEffect(MobEffects.HASTE) || !client.player.hasEffect(MobEffects.STRENGTH))) return;
        if (stage == 1 && !client.player.hasEffect(MobEffects.SLOW_FALLING)) return;
        if (stage == 2 && (!client.player.isCrouching() || client.player.hasEffect(MobEffects.SLOW_FALLING))) return;
        if (stage == 3) {
            if (!client.player.getMainHandItem().is(ModItems.ANVIL_AMULET)) return;
            var ctx = new dev.dubhe.anvilcraft.api.amulet.ctx.AmuletEffectContext();
            AmuletManager.get(client.level.registryAccess()).trigger(client.player, ctx);
            if (!ctx.getOrDefault(dev.dubhe.anvilcraft.init.item.ModAmuletEffectContextKeys.IGNORE_GRAVITY, false)) {
                throw new IllegalStateException("Client-side gravity query did not resolve the synced amulet");
            }
        }
        if (stage == 4 && !client.player.getMainHandItem().isEmpty()) return;
        if (!validated) {
            if (validationRequested) return;
            validationRequested = true;
            client.getSingleplayerServer().execute(() -> {
                try {
                    var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                    if (stage == 2 && (!player.isCrouching() || player.hasEffect(MobEffects.SLOW_FALLING))) {
                        throw new IllegalStateException("Server crouch immunity disagrees with client");
                    }
                    if (stage == 3 && (player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) != 1
                        || player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 100)))) {
                        throw new IllegalStateException("Anvil amulet accepted levitation");
                    }
                    if (stage == 4 && player.getAttribute(Attributes.KNOCKBACK_RESISTANCE)
                        .hasModifier(AnvilCraft.of("anvil_amulet_knockback_resistance"))) {
                        throw new IllegalStateException("Unequipped modifier remained");
                    }
                    validated = true;
                } catch (Throwable error) {
                    failure = error;
                }
            });
            return;
        }
        capturing = true;
        Screenshot.grab(client.gameDirectory, "amulet-refactor-26.1-" + stage + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                AnvilCraft.LOGGER.info("PORT_AMULET_CLIENT_STAGE: {}", stage);
                stage++;
                requested = false;
                capturing = false;
            }));
    }
}
