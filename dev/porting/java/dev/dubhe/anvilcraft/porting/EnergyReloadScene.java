package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.renderer.item.EnergyWeaponFirstPersonRenderer;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.StoredEnergy;
import dev.dubhe.anvilcraft.item.weapon.EnergyWeaponReload;
import dev.dubhe.anvilcraft.network.EnergyWeaponReloadPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class EnergyReloadScene {
    private static boolean started;
    private static volatile boolean prepared;
    private static volatile Throwable failure;
    private static boolean capturing;
    private static int stage;
    private static int poseTick = -1;
    private static long next;
    private static long deadline;

    @SubscribeEvent
    public static void fixedPose(RenderFrameEvent.Pre event) {
        var client = Minecraft.getInstance();
        if (!started || poseTick < 0 || client.player == null) return;
        int slot = stage >= 9 ? 40 : 0;
        ItemStack weapon = client.player.getInventory().getItem(slot);
        ItemStack capacitor = poseTick < 24 ? ModItems.SUPER_CAPACITOR.asStack() : ModItems.SUPER_CAPACITOR_EMPTY.asStack();
        new EnergyWeaponReloadPacket(weapon, capacitor, slot, poseTick).handleOnClient(client.player);
    }

    public static void frame(Minecraft client) {
        if (!started) {
            started = true;
            client.screen.onClose();
            deadline = System.currentTimeMillis() + 150000;
        }
        if (failure != null || System.currentTimeMillis() > deadline) throw new IllegalStateException("换电客户端验证失败：" + stage, failure);
        if (capturing || System.currentTimeMillis() < next) return;
        client.getToastManager().clear();
        client.gui.getChat().clearMessages(false);
        switch (stage) {
            case 0 -> {
                client.getSingleplayerServer().execute(() -> {
                    try {
                        var server = client.getSingleplayerServer();
                        var player = server.getPlayerList().getPlayers().getFirst();
                        player.setGameMode(GameType.SURVIVAL);
                        player.setNoGravity(true);
                        player.getInventory().setSelectedSlot(0);
                        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                            player.getInventory().setItem(slot, ItemStack.EMPTY);
                        }
                        ItemStack weapon = ModItems.LASER_GUN.asStack();
                        weapon.set(ModComponents.STORED_ENERGY, StoredEnergy.EMPTY);
                        player.getInventory().setItem(0, weapon);
                        player.inventoryMenu.broadcastChanges();
                        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 100.5 100 100.5 180 0");
                        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tick unfreeze");
                        prepared = true;
                    } catch (Throwable error) {
                        failure = error;
                    }
                });
                advance(1, 1000);
            }
            case 1 -> {
                if (!prepared || !client.player.getInventory().getItem(0).is(ModItems.LASER_GUN)) return;
                client.player.getInventory().setSelectedSlot(0);
                client.getSingleplayerServer().execute(() -> {
                    var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                    player.getInventory().setItem(1, ModItems.SUPER_CAPACITOR.asStack());
                    player.inventoryMenu.broadcastChanges();
                });
                advance(2, 0);
            }
            case 2 -> {
                if (!EnergyWeaponReload.isReloading(client.player, client.player.getMainHandItem())) return;
                require(client.player.getMainHandItem().get(ModComponents.STORED_ENERGY).value() == 0, "开始换电不能提前充能");
                advance(3, 500);
            }
            case 3 -> capture(client, "live-before-charge", 4);
            case 4 -> {
                if (client.player.getMainHandItem().get(ModComponents.STORED_ENERGY).value() != 160000000) return;
                require(EnergyWeaponReload.isReloading(client.player, client.player.getMainHandItem()), "充能后应继续结束阶段动画");
                capture(client, "live-after-charge", 5);
            }
            case 5 -> {
                if (EnergyWeaponReload.isReloading(client.player, client.player.getMainHandItem())) return;
                require(client.player.getInventory().countItem(ModItems.SUPER_CAPACITOR_EMPTY.get()) == 1, "完成后应返还空电容");
                poseTick = 12;
                advance(6, 400);
            }
            case 6 -> capture(client, "pose-main-12", 7);
            case 7 -> {
                poseTick = 26;
                advance(8, 400);
            }
            case 8 -> {
                capture(client, "pose-main-26", 9);
                poseTick = -1;
                client.getSingleplayerServer().execute(() -> {
                    var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                    var weapon = player.getMainHandItem();
                    player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    player.setItemInHand(InteractionHand.OFF_HAND, weapon);
                    player.inventoryMenu.broadcastChanges();
                });
            }
            case 9 -> {
                if (!client.player.getOffhandItem().is(ModItems.LASER_GUN)) return;
                poseTick = 18;
                advance(10, 500);
            }
            case 10 -> capture(client, "pose-offhand-18", 11);
            case 11 -> {
                poseTick = -1;
                new EnergyWeaponReloadPacket(ItemStack.EMPTY, ItemStack.EMPTY, -1, 40).handleOnClient(client.player);
                client.options.keyUse.setDown(true);
                client.gameMode.useItem(client.player, InteractionHand.OFF_HAND);
                advance(12, 500);
            }
            case 12 -> {
                require(client.player.isUsingItem(), "换电结束后应能继续射击");
                capture(client, "offhand-firing", 13);
            }
            case 13 -> {
                client.options.keyUse.setDown(false);
                client.gameMode.releaseUsingItem(client.player);
                AnvilCraft.LOGGER.info("PORT_ENERGY_RELOAD_PASSED: network charge timing, return, main/offhand poses and resumed fire");
                client.stop();
            }
            default -> throw new IllegalStateException("Unknown reload stage " + stage);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static void advance(int value, long delay) {
        AnvilCraft.LOGGER.info("PORT_RELOAD_STAGE: {} -> {}", stage, value);
        stage = value;
        next = System.currentTimeMillis() + delay;
    }

    private static void capture(Minecraft client, String name, int nextStage) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "reload-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(nextStage, 0);
            }));
    }
}
