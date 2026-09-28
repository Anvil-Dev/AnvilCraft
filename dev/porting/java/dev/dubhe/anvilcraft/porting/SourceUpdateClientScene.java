package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.PocketInventory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

public final class SourceUpdateClientScene {
    private static int stage;
    private static long deadline;
    private static long next;
    private static volatile boolean serverVerified;
    private static volatile boolean serverChecked;
    private static boolean capturing;

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 120000;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Source update client stage " + stage);
        if (capturing || client.getOverlay() != null || System.currentTimeMillis() < next) return;
        switch (stage) {
            case 0 -> {
                client.getSingleplayerServer().execute(() -> {
                    var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                    player.setGameMode(GameType.CREATIVE);
                    player.setItemSlot(EquipmentSlot.LEGS, ModItems.POCKETS_LEGGINGS.asStack());
                    var helmet = ModItems.WEATHERPROOF_SPACESUIT_HELMET.asStack();
                    helmet.set(ModComponents.NIGHT_VISION_ENABLED, false);
                    player.setItemSlot(EquipmentSlot.HEAD, helmet);
                    player.setItemInHand(InteractionHand.OFF_HAND, ModItems.ABNORMAL_AMULET.asStack());
                    PocketInventory.get(player).tick(player);
                    player.inventoryMenu.getSlot(46).set(new ItemStack(Items.DIAMOND, 3));
                    PocketInventory.get(player).syncChanges(player);
                    player.inventoryMenu.broadcastChanges();
                });
                client.options.guiScale().set(2);
                client.resizeGui();
                client.setScreen(null);
                advance(1);
            }
            case 1 -> {
                if (!client.player.getOffhandItem().is(ModItems.ABNORMAL_AMULET)
                    || !PocketInventory.get(client.player).getItem(0).is(Items.DIAMOND)) return;
                SourceUpdateTests.stew().finishUsingItem(client.level, client.player);
                require(!client.player.hasEffect(MobEffects.POISON) && client.player.hasEffect(MobEffects.REGENERATION),
                    "Client-side stew immunity preserves positive effects");
                require(client.player.addEffect(new MobEffectInstance(MobEffects.POISON, 100)), "Client food scope must end");
                client.player.removeEffect(MobEffects.POISON);
                var screen = new CreativeModeInventoryScreen(client.player, client.player.connection.enabledFeatures(), false);
                client.setScreen(screen);
                try {
                    var select = CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", CreativeModeTab.class);
                    select.setAccessible(true);
                    select.invoke(screen, BuiltInRegistries.CREATIVE_MODE_TAB.getValue(CreativeModeTabs.INVENTORY));
                } catch (ReflectiveOperationException exception) {
                    throw new IllegalStateException(exception);
                }
                advance(2);
            }
            case 2 -> {
                client.player.inventoryMenu.setCarried(new ItemStack(Items.EMERALD, 13));
                client.player.inventoryMenu.getSlot(7).set(ItemStack.EMPTY);
                client.getConnection().send(new ServerboundSetCreativeModeSlotPacket(7, ItemStack.EMPTY));
                advance(3);
            }
            case 3 -> {
                if (!client.player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.POCKETS_LEGGINGS)) return;
                require(marker(client), "Single-slot rejection cannot reset an unrelated client edit");
                client.player.inventoryMenu.getSlot(52).set(new ItemStack(Items.GOLD_INGOT, 7));
                client.getConnection().send(new ServerboundSetCreativeModeSlotPacket(52, new ItemStack(Items.GOLD_INGOT, 7)));
                advance(4);
            }
            case 4 -> {
                if (!client.player.inventoryMenu.getSlot(52).getItem().isEmpty()) return;
                require(marker(client), "Inactive-slot correction cannot resync the whole inventory");
                client.getSingleplayerServer().execute(() -> {
                    var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                    serverVerified = player.inventoryMenu.getCarried().isEmpty()
                        && player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.POCKETS_LEGGINGS)
                        && PocketInventory.get(player).getItem(0).getCount() == 3
                        && PocketInventory.get(player).getItem(6).isEmpty();
                    serverChecked = true;
                });
                advance(5);
            }
            case 5 -> {
                if (!serverChecked) return;
                require(serverVerified, "Server inventory, pockets and cursor remain authoritative");
                capturing = true;
                Screenshot.grab(client.gameDirectory, "source-e0dedd6-client.png", client.getMainRenderTarget(), 1,
                    message -> client.execute(() -> {
                        capturing = false;
                        advance(6);
                    }));
            }
            case 6 -> {
                AnvilCraft.LOGGER.info("PORT_SOURCE_UPDATE_CLIENT_PASSED: client food immunity and isolated creative slot corrections");
                stage = 7;
                client.stop();
            }
            default -> {
            }
        }
    }

    private static boolean marker(Minecraft client) {
        var stack = client.player.inventoryMenu.getCarried();
        return stack.is(Items.EMERALD) && stack.getCount() == 13;
    }

    private static void advance(int target) {
        stage = target;
        next = System.currentTimeMillis() + 1200;
    }

    private static void require(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
}
