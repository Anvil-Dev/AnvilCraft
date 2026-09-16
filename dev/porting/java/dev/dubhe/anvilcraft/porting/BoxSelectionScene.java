package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class BoxSelectionScene {
    private static boolean started;
    private static boolean originalInverted;
    private static volatile boolean prepared;
    private static volatile Throwable failure;
    private static InventoryScreen screen;
    private static int stage;
    private static long nextAction;
    private static boolean capturing;
    private static int hoverSlot;

    public static void frame(Minecraft client) {
        if (!started) {
            started = true;
            originalInverted = AnvilCraftClient.CONFIG.invertOverrideAction;
            AnvilCraftClient.CONFIG.invertOverrideAction = false;
            client.setScreen(null);
            client.getSingleplayerServer().execute(() -> {
                try {
                    var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                    player.setGameMode(GameType.SURVIVAL);
                    player.getAbilities().invulnerable = true;
                    player.onUpdateAbilities();
                    for (int i = 0; i < 36; i++) player.getInventory().setItem(i, ItemStack.EMPTY);
                    player.getInventory().setItem(9, BoxSelectionTests.pillBox());
                    player.getInventory().setItem(11, BoxSelectionTests.amuletBox());
                    player.getInventory().setItem(13, BoxSelectionTests.pillBox());
                    player.getInventory().setItem(16, BoxSelectionTests.pillBox());
                    player.inventoryMenu.setCarried(ItemStack.EMPTY);
                    player.inventoryMenu.broadcastChanges();
                    prepared = true;
                } catch (Throwable error) {
                    failure = error;
                }
            });
        }
        if (failure != null) throw new IllegalStateException("盒子选中同步验证失败", failure);
        if (!prepared || capturing || System.currentTimeMillis() < nextAction) return;
        switch (stage) {
            case 0 -> {
                if (client.player.hasInfiniteMaterials() || client.player.getInventory().getItem(9)
                    .get(ModComponents.PILL_BOX_CONTENTS).pills().size() != 2) {
                    return;
                }
                screen = new InventoryScreen(client.player);
                client.setScreen(screen);
                hover(client, 9);
                advance(1);
            }
            case 1 -> {
                scroll();
                scroll();
                advance(2);
            }
            case 2 -> {
                require(client.player.getInventory().getItem(9).get(ModComponents.PILL_BOX_CONTENTS).index() == 1,
                    "药盒高亮未稳定在第二堆");
                capture(client, "pill", 3);
            }
            case 3 -> {
                click(client, 9, 1);
                advance(4);
            }
            case 4 -> {
                require(carried().getHoverName().getString().equals("Second pill") && carried().getCount() == 7,
                    "服务端取出的药片与高亮不一致");
                click(client, 10, 0);
                hover(client, 13);
                advance(5);
            }
            case 5 -> {
                scroll();
                scroll();
                hover(client, 14);
                advance(6);
            }
            case 6 -> {
                require(client.player.getInventory().getItem(13).get(ModComponents.PILL_BOX_CONTENTS).index() == -1,
                    "移开鼠标未重置药盒高亮");
                hover(client, 13);
                advance(7);
            }
            case 7 -> {
                click(client, 13, 1);
                advance(8);
            }
            case 8 -> {
                require(carried().getHoverName().getString().equals("First pill") && carried().getCount() == 3,
                    "重新悬停未滚动时服务端仍使用旧索引");
                click(client, 14, 0);
                hover(client, 11);
                advance(9);
            }
            case 9 -> {
                scroll();
                advance(10);
            }
            case 10 -> {
                var old = client.player.getInventory().getItem(11);
                require(old.get(ModComponents.BOX_CONTENTS).selection() == 1, "护符盒高亮未同步");
                client.player.getInventory().setItem(11, old.copy());
                advance(11);
            }
            case 11 -> {
                scroll();
                require(client.player.getInventory().getItem(11).get(ModComponents.BOX_CONTENTS).selection() == 0,
                    "等值新栈替换后选择器仍写入旧实例");
                scroll();
                advance(12);
            }
            case 12 -> capture(client, "amulet", 13);
            case 13 -> {
                click(client, 11, 1);
                advance(14);
            }
            case 14 -> {
                require(carried().getHoverName().getString().equals("Second totem"), "护符盒取出与高亮不一致");
                click(client, 15, 0);
                hover(client, 16);
                advance(15);
            }
            case 15 -> {
                scroll();
                scroll();
                advance(16);
            }
            case 16 -> {
                screen.onClose();
                advance(17);
            }
            case 17 -> {
                screen = new InventoryScreen(client.player);
                client.setScreen(screen);
                hover(client, 16);
                advance(18);
            }
            case 18 -> {
                require(client.player.getInventory().getItem(16).get(ModComponents.PILL_BOX_CONTENTS).index() == -1,
                    "重新打开药盒应显示未选择状态");
                click(client, 16, 1);
                advance(19);
            }
            case 19 -> {
                require(carried().getHoverName().getString().equals("First pill"), "关闭再打开后服务端仍使用旧高亮");
                AnvilCraftClient.CONFIG.invertOverrideAction = originalInverted;
                AnvilCraft.LOGGER.info("PORT_BOX_SELECTION_SCENE_PASSED: scroll, take, hover reset, replacement, reopen");
                client.stop();
            }
            default -> throw new IllegalStateException("Unknown selection stage " + stage);
        }
    }

    private static ItemStack carried() {
        return screen.getMenu().getCarried();
    }

    private static void hover(Minecraft client, int index) {
        hoverSlot = index;
        var slot = screen.getMenu().getSlot(index);
        try {
            var window = client.getWindow();
            var x = MouseHandler.class.getDeclaredField("xpos");
            var y = MouseHandler.class.getDeclaredField("ypos");
            x.setAccessible(true);
            y.setAccessible(true);
            x.setDouble(client.mouseHandler, (screen.getLeftPos() + slot.x + 8.0) * window.getScreenWidth() / window.getGuiScaledWidth());
            y.setDouble(client.mouseHandler, (screen.getTopPos() + slot.y + 8.0) * window.getScreenHeight() / window.getGuiScaledHeight());
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    private static void scroll() {
        var slot = screen.getMenu().getSlot(hoverSlot);
        var event = new ScreenEvent.MouseScrolled.Pre(screen, screen.getLeftPos() + slot.x + 8, screen.getTopPos() + slot.y + 8, 0, -1);
        NeoForge.EVENT_BUS.post(event);
        require(event.isCanceled(), "真实滚轮事件未被盒子选择器接管");
    }

    private static void click(Minecraft client, int index, int button) {
        hover(client, index);
        var slot = screen.getMenu().getSlot(index);
        var event = new MouseButtonEvent(screen.getLeftPos() + slot.x + 8, screen.getTopPos() + slot.y + 8, new MouseButtonInfo(button, 0));
        screen.mouseClicked(event, false);
        screen.mouseReleased(event);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static void advance(int next) {
        AnvilCraft.LOGGER.info("PORT_BOX_SELECTION_STAGE: {} -> {}", stage, next);
        stage = next;
        nextAction = System.currentTimeMillis() + 750;
    }

    private static void capture(Minecraft client, String name, int next) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "box-selection-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(next);
            }));
    }
}
