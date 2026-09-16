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
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.List;
import java.util.UUID;

public final class StorageCraftingPanelScene {
    private static boolean started;
    private static volatile boolean prepared;
    private static volatile Throwable failure;
    private static UUID id;
    private static StorageScreen screen;
    private static int stage;
    private static int frames;
    private static boolean capturing;
    private static boolean supplied;
    private static boolean dragTesting;

    public static void frame(Minecraft client, BlockPos corePos) {
        if (dragTesting) {
            StorageCraftingDragScene.frame(client, corePos, screen);
            return;
        }
        if (!started) {
            started = true;
            client.getSingleplayerServer().execute(() -> {
                try {
                    var server = client.getSingleplayerServer();
                    var core = (StorageBlockEntity) server.overworld().getBlockEntity(corePos);
                    id = core.getId();
                    var storage = Storages.get().get(id).orElseThrow();
                    storage.setCrafting(CraftingStorage.EMPTY);
                    var unlocked = dev.dubhe.anvilcraft.saved.storage.BaseStorage.class.getDeclaredField("craftingUnlocked");
                    unlocked.setAccessible(true);
                    unlocked.setBoolean(storage, false);
                    var player = server.getPlayerList().getPlayers().getFirst();
                    player.containerMenu.setCarried(ItemStack.EMPTY);
                    player.getInventory().setItem(9, new ItemStack(Items.OAK_LOG, 4));
                    player.getInventory().setItem(10, new ItemStack(Items.STONE, 8));
                    player.getInventory().setItem(12, ItemStack.EMPTY);
                    player.inventoryMenu.broadcastChanges();
                    prepared = true;
                } catch (Throwable error) {
                    failure = error;
                }
            });
        }
        if (failure != null) throw new IllegalStateException("合成面板验证失败", failure);
        if (!prepared || capturing) return;
        if (screen == null) {
            screen = new StorageScreen(corePos);
            client.setScreen(screen);
        }
        if (++frames > 1200) throw new IllegalStateException("合成面板阶段超时：" + stage);
        if (frames < 30 || flag("interactionPending")) return;
        switch (stage) {
            case 0 -> {
                click(287, 204, 0, 0);
                advance(1);
            }
            case 1 -> {
                if (flag("craftingMode")) throw new IllegalStateException("没有材料时不能解锁");
                capture(client, "locked", 2);
            }
            case 2 -> {
                if (!supplied) {
                    supplied = true;
                    client.getSingleplayerServer().execute(() -> {
                        try (Transaction tx = Transaction.openRoot()) {
                            var items = Storages.get().get(id).orElseThrow().getItems();
                            items.insert(ItemResource.of(Items.CRAFTING_TABLE), 1, tx);
                            items.insert(ItemResource.of(Items.STONECUTTER), 1, tx);
                            tx.commit();
                        }
                    });
                    frames = 0;
                    return;
                }
                click(287, 204, 0, 0);
                advance(3);
            }
            case 3 -> {
                if (!flag("craftingMode") || !flag("craftingLoaded")) return;
                click(122, 148, 0, 0);
                advance(4);
            }
            case 4 -> {
                if (!carried().is(Items.OAK_LOG)) throw new IllegalStateException("背包取物失败");
                click(51, 206, 0, 0);
                advance(5);
            }
            case 5 -> {
                if (!carried().isEmpty() || !state().craftingInput().get(8).is(Items.OAK_LOG)) return;
                if (!((ItemStack) field("craftingResult")).is(Items.OAK_PLANKS)) throw new IllegalStateException("本地合成预览错误");
                capture(client, "grid", 6);
            }
            case 6 -> {
                click(91, 206, 0, 0);
                advance(7);
            }
            case 7 -> {
                if (!carried().is(Items.OAK_PLANKS) || carried().getCount() != 4
                    || state().craftingInput().get(8).getCount() != 3) throw new IllegalStateException("结果点击未正确消耗一次输入");
                click(176, 148, 0, 0);
                advance(8);
            }
            case 8 -> {
                if (!carried().isEmpty()) return;
                click(51, 206, 0, 1);
                advance(9);
            }
            case 9 -> {
                if (!state().craftingInput().get(8).isEmpty() || !carried().isEmpty()
                    || !((ItemStack) field("craftingResult")).isEmpty()) throw new IllegalStateException("Shift 移出后的状态不完整");
                click(140, 148, 0, 0);
                advance(10);
            }
            case 10 -> {
                if (!carried().is(Items.STONE)) throw new IllegalStateException("切石原料未取出");
                click(15, 138, 0, 0);
                advance(11);
            }
            case 11 -> {
                if (((List<?>) field("stonecutterRecipes")).isEmpty()) return;
                if (((List<?>) field("stonecutterRecipes")).size() > 6) {
                    int left = (int) field("left");
                    int top = (int) field("top");
                    screen.mouseClicked(new MouseButtonEvent(left + 97, top + 123, new MouseButtonInfo(0, 0)), false);
                    screen.mouseDragged(new MouseButtonEvent(left + 97, top + 152, new MouseButtonInfo(0, 0)), 0, 29);
                    if ((int) field("recipeHead") == 0) throw new IllegalStateException("切石滚动条未响应拖动");
                    screen.mouseReleased(new MouseButtonEvent(left + 97, top + 152, new MouseButtonInfo(0, 0)));
                    screen.mouseScrolled(left + 45, top + 125, 0, 20);
                    while ((int) field("recipeHead") != 0) screen.mouseScrolled(left + 45, top + 125, 0, 1);
                }
                click(83, 129, 0, 0);
                advance(12);
            }
            case 12 -> {
                if (state().stonecutterSelected() != 2) throw new IllegalStateException("切石选择未同步");
                click(81, 188, 0, 0);
                click(94, 188, 0, 0);
                advance(16);
            }
            case 16 -> capture(client, "stonecutter", 13);
            case 13 -> {
                click(68, 188, 0, 0);
                advance(14);
            }
            case 14 -> {
                if (!state().stonecutterInput().isEmpty()) return;
                screen.onClose();
                screen = new StorageScreen(corePos);
                client.setScreen(screen);
                advance(15);
            }
            case 15 -> {
                if (!flag("craftingMode") || !flag("craftingLoaded")) return;
                if (!state().autoFill() || !state().toStorage()) throw new IllegalStateException("关闭重开丢失选项");
                AnvilCraft.LOGGER.info(
                    "PORT_CRAFTING_PANEL_SCENE_PASSED: unlock, inventory, grid, result, shift, stonecutter, options, reopen");
                if (Boolean.getBoolean("anvilcraft.portCraftingDragScene")) dragTesting = true;
                else client.stop();
            }
            default -> throw new IllegalStateException("Unknown stage " + stage);
        }
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

    private static boolean flag(String name) {
        return (boolean) field(name);
    }

    private static CraftingStorage state() {
        return (CraftingStorage) field("crafting");
    }

    private static ItemStack carried() {
        return (ItemStack) field("carried");
    }

    private static void click(int x, int y, int button, int modifiers) {
        var event = new MouseButtonEvent((int) field("left") + x, (int) field("top") + y, new MouseButtonInfo(button, modifiers));
        screen.mouseClicked(event, false);
        screen.mouseReleased(event);
    }

    private static void advance(int next) {
        AnvilCraft.LOGGER.info("PORT_CRAFTING_PANEL_STAGE: {} -> {}", stage, next);
        stage = next;
        frames = 0;
    }

    private static void capture(Minecraft client, String name, int next) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "crafting-panel-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(next);
            }));
    }
}
