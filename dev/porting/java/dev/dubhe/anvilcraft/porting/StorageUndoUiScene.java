package dev.dubhe.anvilcraft.porting;

import dev.anvilcraft.lib.v2.util.UnlimitedItemStack;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import dev.dubhe.anvilcraft.saved.storage.ShulkerContainerStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.lwjgl.glfw.GLFW;

import java.util.Map;

public final class StorageUndoUiScene {
    private static boolean requested;
    private static volatile boolean prepared;
    private static boolean capturing;
    private static int stage;
    private static int frames;

    public static void frame(Minecraft client, StorageScreen screen, BlockPos corePos) {
        if (!requested) {
            requested = true;
            ((EditBox) field(screen, "search")).setValue("");
            client.getSingleplayerServer().execute(() -> {
                var level = client.getSingleplayerServer().overworld();
                var core = (StorageBlockEntity) level.getBlockEntity(corePos);
                var items = Storages.get().getOrCreate(core.getId(), ShulkerContainerStorage.class).getItems();
                try (Transaction transaction = Transaction.openRoot()) {
                    for (int index = 0; index < items.size(); index++) {
                        var resource = items.getResource(index);
                        if (!resource.isEmpty()) items.extract(resource, Integer.MAX_VALUE, transaction);
                    }
                    items.insert(ItemResource.of(Items.GOLD_INGOT), 1, transaction);
                    transaction.commit();
                }
                var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                player.getInventory().clearContent();
                player.getInventory().setItem(9, ItemResource.of(Items.DIAMOND).toStack(4));
                player.getInventory().setItem(10, ItemResource.of(Items.IRON_INGOT).toStack(6));
                player.getInventory().setItem(18, ItemResource.of(Items.GOLD_INGOT).toStack(8));
                player.inventoryMenu.broadcastChanges();
                prepared = true;
            });
        }
        if (!prepared || capturing) return;
        frames++;
        int left = (int) field(screen, "left");
        int top = (int) field(screen, "top");
        switch (stage) {
            case 0 -> {
                if (client.player.getInventory().getItem(9).is(Items.DIAMOND) && frames > 20) {
                    screen.keyPressed(new KeyEvent(GLFW.GLFW_KEY_LEFT_SHIFT, 0, GLFW.GLFW_MOD_SHIFT));
                    screen.mouseClicked(event(left + 122, top + 148, GLFW.GLFW_MOD_SHIFT), false);
                    advance(1);
                }
            }
            case 1 -> {
                if (client.player.getInventory().getItem(9).isEmpty()) {
                    screen.mouseDragged(event(left + 140, top + 148, GLFW.GLFW_MOD_SHIFT), 18, 0);
                    advance(2);
                }
            }
            case 2 -> {
                if (client.player.getInventory().getItem(10).isEmpty()) {
                    screen.mouseReleased(event(left + 140, top + 148, GLFW.GLFW_MOD_SHIFT));
                    screen.keyReleased(new KeyEvent(GLFW.GLFW_KEY_LEFT_SHIFT, 0, 0));
                    advance(3);
                }
            }
            case 3 -> {
                if (frames > 10) {
                    screen.keyPressed(new KeyEvent(GLFW.GLFW_KEY_Z, 0, GLFW.GLFW_MOD_CONTROL));
                    advance(4);
                }
            }
            case 4 -> {
                if (count(client, ItemResource.of(Items.DIAMOND)) == 4 && count(client, ItemResource.of(Items.IRON_INGOT)) == 6
                    && stored(screen, ItemResource.of(Items.DIAMOND)) == 0 && stored(screen, ItemResource.of(Items.IRON_INGOT)) == 0) {
                    capture(client, "group", 5);
                }
            }
            case 5 -> {
                screen.mouseClicked(event(left + 286, top + 148, 0), false);
                screen.mouseReleased(event(left + 286, top + 148, 0));
                advance(6);
            }
            case 6 -> {
                if (count(client, ItemResource.of(Items.GOLD_INGOT)) == 0) {
                    screen.keyPressed(new KeyEvent(GLFW.GLFW_KEY_Z, 0, GLFW.GLFW_MOD_CONTROL));
                    advance(7);
                }
            }
            case 7 -> {
                if (count(client, ItemResource.of(Items.GOLD_INGOT)) == 8 && stored(screen, ItemResource.of(Items.GOLD_INGOT)) == 1) {
                    capture(client, "deposit", 8);
                }
            }
            case 8 -> {
                giveSameItems(client);
                advance(9);
            }
            case 9 -> {
                if (client.player.getInventory().getItem(10).getCount() == 5) {
                    screen.mouseClicked(event(left + 122, top + 148, GLFW.GLFW_MOD_ALT), false);
                    screen.mouseReleased(event(left + 122, top + 148, GLFW.GLFW_MOD_ALT));
                    advance(10);
                }
            }
            case 10 -> {
                if (count(client, ItemResource.of(Items.DIAMOND)) == 0 && stored(screen, ItemResource.of(Items.DIAMOND)) == 12) {
                    screen.keyPressed(new KeyEvent(GLFW.GLFW_KEY_Z, 0, GLFW.GLFW_MOD_CONTROL));
                    advance(11);
                }
            }
            case 11 -> {
                if (count(client, ItemResource.of(Items.DIAMOND)) == 12 && stored(screen, ItemResource.of(Items.DIAMOND)) == 0) {
                    giveSameItems(client);
                    advance(12);
                }
            }
            case 12 -> {
                if (client.player.getInventory().getItem(9).getCount() == 3
                    && client.player.getInventory().getItem(10).getCount() == 5
                    && System.currentTimeMillis() - (long) field(screen, "lastInventoryClickTime") >= 250) {
                    screen.keyPressed(new KeyEvent(GLFW.GLFW_KEY_LEFT_SHIFT, 0, GLFW.GLFW_MOD_SHIFT));
                    screen.mouseClicked(event(left + 122, top + 148, GLFW.GLFW_MOD_SHIFT), false);
                    screen.mouseReleased(event(left + 122, top + 148, GLFW.GLFW_MOD_SHIFT));
                    advance(13);
                }
            }
            case 13 -> {
                if (client.player.getInventory().getItem(9).isEmpty()) {
                    screen.mouseClicked(event(left + 122, top + 148, GLFW.GLFW_MOD_SHIFT), false);
                    screen.mouseReleased(event(left + 122, top + 148, GLFW.GLFW_MOD_SHIFT));
                    advance(14);
                }
            }
            case 14 -> {
                if (count(client, ItemResource.of(Items.DIAMOND)) == 0 && stored(screen, ItemResource.of(Items.DIAMOND)) == 20) {
                    screen.keyReleased(new KeyEvent(GLFW.GLFW_KEY_LEFT_SHIFT, 0, 0));
                    screen.keyPressed(new KeyEvent(GLFW.GLFW_KEY_Z, 0, GLFW.GLFW_MOD_CONTROL));
                    advance(15);
                }
            }
            case 15 -> {
                if (count(client, ItemResource.of(Items.DIAMOND)) == 17 && stored(screen, ItemResource.of(Items.DIAMOND)) == 3) {
                    screen.keyPressed(new KeyEvent(GLFW.GLFW_KEY_Z, 0, GLFW.GLFW_MOD_CONTROL));
                    advance(16);
                }
            }
            case 16 -> {
                if (count(client, ItemResource.of(Items.DIAMOND)) == 20 && stored(screen, ItemResource.of(Items.DIAMOND)) == 0) {
                    capture(client, "same", 17);
                }
            }
            case 17 -> {
                client.getSingleplayerServer().execute(() -> {
                    var level = client.getSingleplayerServer().overworld();
                    var core = (StorageBlockEntity) level.getBlockEntity(corePos);
                    var items = Storages.get().getOrCreate(core.getId(), ShulkerContainerStorage.class).getItems();
                    try (Transaction transaction = Transaction.openRoot()) {
                        items.insert(ItemResource.of(Items.DIAMOND), 128, transaction);
                        items.insert(ItemResource.of(Items.IRON_INGOT), 96, transaction);
                        transaction.commit();
                    }
                    var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                    player.getInventory().setItem(9, ItemResource.of(Items.GOLD_INGOT).toStack(2));
                    player.inventoryMenu.broadcastChanges();
                });
                advance(18);
            }
            case 18 -> {
                if (stored(screen, ItemResource.of(Items.DIAMOND)) == 128 && stored(screen, ItemResource.of(Items.IRON_INGOT)) == 96
                    && client.player.getInventory().getItem(9).getCount() == 2
                    && System.currentTimeMillis() - (long) field(screen, "lastInventoryClickTime") >= 250) {
                    var diamond = storagePoint(screen, ItemResource.of(Items.DIAMOND));
                    var iron = storagePoint(screen, ItemResource.of(Items.IRON_INGOT));
                    screen.keyPressed(new KeyEvent(GLFW.GLFW_KEY_LEFT_SHIFT, 0, GLFW.GLFW_MOD_SHIFT));
                    screen.mouseClicked(event(left + 122, top + 148, GLFW.GLFW_MOD_SHIFT), false);
                    screen.mouseDragged(event(diamond[0], diamond[1], GLFW.GLFW_MOD_SHIFT), 0, -60);
                    screen.mouseDragged(event(diamond[0], diamond[1], GLFW.GLFW_MOD_SHIFT), 1, 0);
                    screen.mouseDragged(event(iron[0], iron[1], GLFW.GLFW_MOD_SHIFT), -18, 0);
                    advance(19);
                }
            }
            case 19 -> {
                if (client.player.getInventory().getItem(9).isEmpty() && count(client, ItemResource.of(Items.IRON_INGOT)) == 70
                    && stored(screen, ItemResource.of(Items.IRON_INGOT)) == 32) {
                    if (count(client, ItemResource.of(Items.DIAMOND)) != 20 || stored(screen, ItemResource.of(Items.DIAMOND)) != 128) {
                        throw new IllegalStateException("取消选择的仓储槽被错误取出");
                    }
                    screen.mouseReleased(event(left + 122, top + 26, GLFW.GLFW_MOD_SHIFT));
                    screen.keyReleased(new KeyEvent(GLFW.GLFW_KEY_LEFT_SHIFT, 0, 0));
                    capture(client, "cross-drag", 20);
                }
            }
            case 20 -> {
                screen.keyPressed(new KeyEvent(GLFW.GLFW_KEY_Z, 0, GLFW.GLFW_MOD_CONTROL));
                advance(21);
            }
            case 21 -> {
                if (count(client, ItemResource.of(Items.GOLD_INGOT)) == 10 && stored(screen, ItemResource.of(Items.GOLD_INGOT)) == 1) {
                    AnvilCraft.LOGGER.info("PORT_STORAGE_UNDO_UI_PASSED: same-type, undo and cross-area Shift drag");
                    advance(22);
                }
            }
            case 22 -> {
                client.stop();
            }
            default -> throw new IllegalStateException("未知仓储撤销测试阶段");
        }
    }

    private static int count(Minecraft client, ItemResource resource) {
        int count = 0;
        for (int index = 0; index < Inventory.INVENTORY_SIZE; index++) {
            var stack = client.player.getInventory().getItem(index);
            if (!stack.isEmpty() && ItemResource.of(stack).equals(resource)) count += stack.getCount();
        }
        return count;
    }

    private static void giveSameItems(Minecraft client) {
        client.getSingleplayerServer().execute(() -> {
            var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
            player.getInventory().setItem(9, ItemResource.of(Items.DIAMOND).toStack(3));
            player.getInventory().setItem(10, ItemResource.of(Items.DIAMOND).toStack(5));
            player.inventoryMenu.broadcastChanges();
        });
    }

    private static int stored(StorageScreen screen, ItemResource resource) {
        int count = 0;
        var empty = (IntSet) field(screen, "emptySlots");
        for (var entry : ((Map<?, ?>) field(screen, "contents")).entrySet()) {
            var stack = (UnlimitedItemStack) entry.getValue();
            if (!empty.contains((Integer) entry.getKey()) && ItemResource.of(stack.toStack()).equals(resource)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static int[] storagePoint(StorageScreen screen, ItemResource resource) {
        var order = (IntList) field(screen, "displayOrder");
        var contents = (Map<?, ?>) field(screen, (boolean) field(screen, "nbtFolded") ? "foldedContents" : "contents");
        for (int index = 0; index < order.size(); index++) {
            var stack = (UnlimitedItemStack) contents.get(order.getInt(index));
            if (stack != null && ItemResource.of(stack.toStack()).equals(resource)) {
                return new int[]{(int) field(screen, "left") + 122 + index % 9 * 18,
                    (int) field(screen, "top") + 26 + index / 9 * 18};
            }
        }
        throw new IllegalStateException("仓储测试条目未显示");
    }

    private static MouseButtonEvent event(int x, int y, int modifiers) {
        return new MouseButtonEvent(x, y, new MouseButtonInfo(0, modifiers));
    }

    private static Object field(StorageScreen screen, String name) {
        try {
            var field = StorageScreen.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(screen);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void advance(int next) {
        AnvilCraft.LOGGER.info("PORT_STORAGE_UNDO_UI_STAGE: {} -> {}", stage, next);
        stage = next;
        frames = 0;
    }

    private static void capture(Minecraft client, String name, int next) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "storage-undo-26.1-" + name + ".png", client.getMainRenderTarget(), 1, message ->
            client.execute(() -> {
                capturing = false;
                advance(next);
            }));
    }
}
