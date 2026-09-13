package dev.dubhe.anvilcraft.porting;

import dev.anvilcraft.lib.v2.util.UnlimitedItemStack;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import dev.dubhe.anvilcraft.saved.storage.ShulkerContainerStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
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
                AnvilCraft.LOGGER.info("PORT_STORAGE_UNDO_UI_PASSED: grouped Shift drag and deposit Ctrl+Z");
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
