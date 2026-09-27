package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.FilterMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

public final class FilterClientScene {
    private static int stage;
    private static long deadline;
    private static volatile boolean ready;
    private static volatile boolean synced;
    private static volatile @Nullable Throwable failure;

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 90000;
        if (failure != null || System.currentTimeMillis() > deadline) throw new IllegalStateException("Filter stage " + stage, failure);
        switch (stage) {
            case 0 -> {
                client.getSingleplayerServer().execute(() -> {
                    var server = client.getSingleplayerServer();
                    var player = server.getPlayerList().getPlayers().getFirst();
                    player.getInventory().clearContent();
                    player.setItemInHand(InteractionHand.OFF_HAND, ModItems.FILTER.asStack());
                    player.getInventory().setItem(3, ModItems.FILTER.asStack());
                    player.inventoryMenu.broadcastChanges();
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tick unfreeze");
                    ready = true;
                });
                stage = 1;
            }
            case 1 -> {
                if (!ready || client.screen != null || !client.player.getOffhandItem().is(ModItems.FILTER)) return;
                client.gameMode.useItem(client.player, InteractionHand.OFF_HAND);
                stage = 2;
            }
            case 2, 5 -> {
                if (!(client.player.containerMenu instanceof FilterMenu menu)) return;
                boolean offhand = stage == 2;
                var container = menu.getContainer();
                int expected = offhand ? Inventory.SLOT_OFFHAND : 3;
                check(container.getPosition() == expected,
                    "Menu position at stage " + stage + ": " + container.getPosition() + " expected " + expected);
                AnvilCraft.LOGGER.info("PORT_FILTER_MENU: stage={} slot={} clientIdentity={}", stage, expected,
                    container.stillValid(client.player));
                container.setItem(0, new ItemStack(offhand ? Items.DIAMOND : Items.EMERALD));
                container.setBlackList(offhand);
                container.setIncludeComponents(true);
                container.setChanged();
                container.sync();
                synced = false;
                stage = offhand ? 3 : 6;
            }
            case 3, 6 -> {
                if (!synced) {
                    boolean offhand = stage == 3;
                    client.getSingleplayerServer().execute(() -> {
                        try {
                            var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                            var off = player.getOffhandItem().get(ModComponents.FILTER_CONTENT);
                            var main = player.getInventory().getItem(3).get(ModComponents.FILTER_CONTENT);
                            if (off == null || !off.list().getFirst().is(Items.DIAMOND)) return;
                            if (!offhand && (main == null || !main.list().getFirst().is(Items.EMERALD))) return;
                            check(player.containerMenu instanceof FilterMenu menu && menu.stillValid(player),
                                "Server menu remains attached to the correct live item");
                            check(off.blackList() && off.includeComponents(), "Offhand filter options reach the server");
                            if (!offhand) check(!main.blackList() && main.includeComponents(), "Main hand remains independent");
                            synced = true;
                        } catch (Throwable error) {
                            failure = error;
                        }
                    });
                    return;
                }
                client.player.closeContainer();
                if (stage == 6) {
                    AnvilCraft.LOGGER.info("PORT_FILTER_CLIENT_PASSED: offhand and selected main-hand menu, "
                        + "content and option synchronization");
                    client.stop();
                    stage = 7;
                } else {
                    client.player.getInventory().setSelectedSlot(3);
                    stage = 4;
                }
            }
            case 4 -> {
                if (client.screen != null || !client.player.getMainHandItem().is(ModItems.FILTER)) return;
                client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
                stage = 5;
            }
            default -> {
            }
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
