package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import dev.dubhe.anvilcraft.saved.storage.CraftingStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class StorageCraftingDragScene {
    private static boolean started;
    private static volatile boolean prepared;
    private static volatile Throwable failure;
    private static StorageScreen screen;
    private static int stage;
    private static int frames;
    private static boolean capturing;
    private static boolean pickupTesting;

    public static void frame(Minecraft client, BlockPos corePos, StorageScreen current) {
        if (pickupTesting) {
            StorageCraftingPickupScene.frame(client, corePos, current);
            return;
        }
        screen = current;
        if (!started) {
            started = true;
            client.getSingleplayerServer().execute(() -> {
                try {
                    var server = client.getSingleplayerServer();
                    var core = (StorageBlockEntity) server.overworld().getBlockEntity(corePos);
                    Storages.get().get(core.getId()).orElseThrow().setCrafting(CraftingStorage.EMPTY.withLastOpened(true));
                    var player = server.getPlayerList().getPlayers().getFirst();
                    player.containerMenu.setCarried(ItemStack.EMPTY);
                    player.getInventory().setItem(9, new ItemStack(Items.STONE, 10));
                    player.getInventory().setItem(10, ItemStack.EMPTY);
                    player.getInventory().setItem(11, ItemStack.EMPTY);
                    player.getInventory().setItem(12, new ItemStack(Items.OAK_LOG, 5));
                    player.getInventory().setItem(13, ItemStack.EMPTY);
                    player.inventoryMenu.broadcastChanges();
                    prepared = true;
                } catch (Throwable error) {
                    failure = error;
                }
            });
        }
        if (failure != null) throw new IllegalStateException("混合拖拽界面验证失败", failure);
        if (!prepared || capturing) return;
        if (++frames > 900) throw new IllegalStateException("混合拖拽界面超时：" + stage);
        if (frames < 35 || (boolean) field("interactionPending")) return;
        switch (stage) {
            case 0 -> {
                invoke("refreshCrafting");
                advance(1);
            }
            case 1 -> {
                click(122, 148, 0);
                advance(2);
            }
            case 2 -> {
                check(carried().is(Items.STONE) && carried().getCount() == 10, "背包拾取必须同步指针");
                screen.mouseClicked(event(15, 170, 0), false);
                screen.mouseDragged(event(16, 170, 0), 1, 0);
                screen.mouseDragged(event(140, 148, 0), 124, -22);
                screen.mouseDragged(event(51, 206, 0), -89, 58);
                check((int) invoke("getQuickCraftRemaining") == 1, "跨区预览必须统一计算余数");
                check(state().craftingInput().get(0).isEmpty() && client.player.getInventory().getItem(10).isEmpty(),
                    "预览不能提前修改真实槽位");
                advance(10);
            }
            case 10 -> capture(client, "preview", 3);
            case 3 -> {
                screen.mouseReleased(event(51, 206, 0));
                advance(4);
            }
            case 4 -> {
                check(state().craftingInput().get(0).getCount() == 3 && state().craftingInput().get(8).getCount() == 3
                    && client.player.getInventory().getItem(10).getCount() == 3 && carried().getCount() == 1,
                    "松开后合成区、背包与指针数量必须等于预览");
                click(158, 148, 0);
                advance(5);
            }
            case 5 -> {
                check(carried().isEmpty(), "剩余指针物品应能正常放回背包");
                click(176, 148, 0);
                advance(6);
            }
            case 6 -> {
                check(carried().is(Items.OAK_LOG) && carried().getCount() == 5, "右键拖拽准备失败");
                screen.mouseClicked(event(194, 148, 1), false);
                screen.mouseDragged(event(195, 148, 1), 1, 0);
                screen.mouseDragged(event(33, 170, 1), -162, 22);
                screen.mouseDragged(event(33, 206, 1), 0, 36);
                screen.mouseReleased(event(33, 206, 1));
                advance(7);
            }
            case 7 -> {
                check(carried().getCount() == 2 && state().craftingInput().get(1).getCount() == 1
                    && state().craftingInput().get(7).getCount() == 1 && client.player.getInventory().getItem(13).getCount() == 1,
                    "从背包开始的右键拖拽应每槽只放一个");
                screen.mouseClicked(event(194, 148, 2), false);
                screen.mouseDragged(event(195, 148, 2), 1, 0);
                screen.mouseDragged(event(33, 170, 2), -162, 22);
                screen.mouseDragged(event(15, 138, 2), -18, -32);
                check((int) invoke("getQuickCraftRemaining") == 2, "创造中键预览不能扣减指针");
                screen.mouseReleased(event(33, 170, 2));
                advance(8);
            }
            case 8 -> {
                check(carried().getCount() == 2 && state().craftingInput().get(1).getCount() == 64
                    && client.player.getInventory().getItem(13).getCount() == 64 && state().stonecutterInput().isEmpty(),
                    "中键应填满有效目标，且不能把原木塞进切石槽");
                capture(client, "completed", 9);
            }
            case 9 -> {
                AnvilCraft.LOGGER.info(
                    "PORT_CRAFTING_DRAG_SCENE_PASSED: mixed preview, release, right drag, creative clone, slot validation");
                if (Boolean.getBoolean("anvilcraft.portCraftingPickupScene")) pickupTesting = true;
                else client.stop();
            }
            default -> throw new IllegalStateException("Unknown stage " + stage);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static CraftingStorage state() {
        return (CraftingStorage) field("crafting");
    }

    private static ItemStack carried() {
        return (ItemStack) field("carried");
    }

    private static Object field(String name) {
        try {
            var field = StorageScreen.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(screen);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    private static Object invoke(String name) {
        try {
            var method = StorageScreen.class.getDeclaredMethod(name);
            method.setAccessible(true);
            return method.invoke(screen);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    private static MouseButtonEvent event(int x, int y, int button) {
        return new MouseButtonEvent((int) field("left") + x, (int) field("top") + y, new MouseButtonInfo(button, 0));
    }

    private static void click(int x, int y, int button) {
        screen.mouseClicked(event(x, y, button), false);
        screen.mouseReleased(event(x, y, button));
    }

    private static void advance(int next) {
        AnvilCraft.LOGGER.info("PORT_CRAFTING_DRAG_STAGE: {} -> {}", stage, next);
        stage = next;
        frames = 0;
    }

    private static void capture(Minecraft client, String name, int next) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "crafting-drag-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(next);
            }));
    }
}
