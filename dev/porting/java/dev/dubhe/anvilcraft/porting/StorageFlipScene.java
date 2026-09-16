package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.client.gui.screen.CategorySettingsScreen;
import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import dev.dubhe.anvilcraft.client.rpc.SettingClientStub;
import dev.dubhe.anvilcraft.saved.setting.PlayerSetting;
import dev.dubhe.anvilcraft.saved.setting.PlayerSettings;
import dev.dubhe.anvilcraft.saved.storage.CraftingStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class StorageFlipScene {
    private static boolean started;
    private static boolean executing;
    private static int categories;
    private static volatile boolean prepared;
    private static volatile Throwable failure;
    private static int stage;
    private static long nextAction;
    private static boolean capturing;

    public static void frame(Minecraft client, BlockPos corePos) {
        if (executing) return;
        executing = true;
        try {
            runFrame(client, corePos);
        } finally {
            executing = false;
        }
    }

    private static void runFrame(Minecraft client, BlockPos corePos) {
        if (!started) {
            started = true;
            client.getSingleplayerServer().execute(() -> {
                try {
                    var server = client.getSingleplayerServer();
                    var core = (StorageBlockEntity) server.overworld().getBlockEntity(corePos);
                    Storages.get().get(core.getId()).orElseThrow().setCrafting(CraftingStorage.EMPTY);
                    var player = server.getPlayerList().getPlayers().getFirst();
                    PlayerSettings.getSetting(server.registryAccess(), player.getUUID()).storage().setFlipped(false);
                    for (int i = 0; i < 36; i++) player.getInventory().setItem(i, ItemStack.EMPTY);
                    player.getInventory().setItem(9, new ItemStack(Items.STICK, 5));
                    player.inventoryMenu.broadcastChanges();
                    prepared = true;
                } catch (Throwable error) {
                    failure = error;
                }
            });
        }
        if (failure != null) throw new IllegalStateException("布局翻转验证失败", failure);
        if (!prepared || capturing || System.currentTimeMillis() < nextAction) return;
        Screen screen = client.screen;
        if (screen instanceof StorageScreen && (boolean) field(screen, "interactionPending")) return;
        switch (stage) {
            case 0 -> {
                categories = SettingClientStub.listed().size();
                SettingClientStub.storage().setFlipped(false);
                client.setScreen(new StorageScreen(corePos));
                advance(1);
            }
            case 1 -> {
                click(screen, 285, 6);
                advance(2);
            }
            case 2 -> {
                require((boolean) field(screen, "flipped"), "普通页未翻转");
                capture(client, "normal", 3);
            }
            case 3 -> {
                click(screen, 122, 148);
                advance(4);
            }
            case 4 -> {
                var carried = (ItemStack) field(screen, "carried");
                require(carried.is(Items.STICK) && carried.getCount() == 5, "翻转背包点击未取出原槽物品");
                click(screen, 140, 148);
                advance(5);
            }
            case 5 -> {
                require(client.player.getInventory().getItem(10).getCount() == 5, "翻转背包放入位置错误");
                click(screen, 287, 204);
                advance(6);
            }
            case 6 -> {
                require((boolean) field(screen, "craftingMode"), "翻转合成按钮未切换模式");
                click(screen, 140, 148);
                advance(7);
            }
            case 7 -> {
                click(screen, 51, 206);
                advance(8);
            }
            case 8 -> {
                var crafting = (CraftingStorage) field(screen, "crafting");
                require(crafting.craftingInput().get(8).is(Items.STICK)
                    && crafting.craftingInput().get(8).getCount() == 5, "翻转合成槽放入位置错误");
                require(((ItemStack) field(screen, "carried")).isEmpty(), "合成槽放入后指针残留");
                capture(client, "crafting", 9);
            }
            case 9 -> {
                click(screen, 15, 79);
                advance(10);
            }
            case 10 -> {
                require(screen instanceof CategorySettingsScreen, "翻转分类设置按钮位置错误");
                require((boolean) field(screen, "flipped"), "分类设置页未翻转");
                require(((PlayerSetting) field(screen, "draftSetting")).storage().isFlipped(), "草稿丢失翻转状态");
                require(((PlayerSetting) field(screen, "draftSetting")).listed().size() == categories, "打开分类页不能改变分类列表");
                capture(client, "categories", 11);
            }
            case 11 -> {
                click(screen, 287, 149);
                advance(12);
            }
            case 12 -> {
                require(screen instanceof StorageScreen && (boolean) field(screen, "flipped"), "确认分类后翻转状态丢失");
                client.getSingleplayerServer().execute(() -> {
                    try {
                        var server = client.getSingleplayerServer();
                        var player = server.getPlayerList().getPlayers().getFirst();
                        require(PlayerSettings.getSetting(server.registryAccess(), player.getUUID()).storage().isFlipped(),
                            "服务端未保存翻转状态");
                    } catch (Throwable error) {
                        failure = error;
                    }
                });
                click(screen, 285, 6);
                advance(13);
            }
            case 13 -> {
                require(!(boolean) field(screen, "flipped"), "再次点击未恢复布局");
                client.setScreen(new StorageScreen(corePos));
                advance(14);
            }
            case 14 -> {
                require(!(boolean) field(screen, "flipped"), "重新打开后设置未同步");
                var state = (CraftingStorage) field(screen, "crafting");
                require(state.craftingInput().get(8).getCount() == 5, "切换布局不能改变合成物品");
                AnvilCraft.LOGGER.info("PORT_STORAGE_FLIP_SCENE_PASSED: inventory, crafting, categories, persistence, restore");
                client.stop();
            }
            default -> throw new IllegalStateException("Unknown flip stage " + stage);
        }
    }

    private static Object field(Screen screen, String name) {
        try {
            var field = screen.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(screen);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    private static void click(Screen screen, int x, int y) {
        if ((boolean) field(screen, "flipped")) x = x < 106 ? x + 194 : x - 106;
        var event = new MouseButtonEvent((int) field(screen, "left") + x, (int) field(screen, "top") + y, new MouseButtonInfo(0, 0));
        screen.mouseClicked(event, false);
        screen.mouseReleased(event);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static void advance(int next) {
        AnvilCraft.LOGGER.info("PORT_STORAGE_FLIP_STAGE: {} -> {}", stage, next);
        stage = next;
        nextAction = System.currentTimeMillis() + 750;
    }

    private static void capture(Minecraft client, String name, int next) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "storage-flip-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(next);
            }));
    }
}
