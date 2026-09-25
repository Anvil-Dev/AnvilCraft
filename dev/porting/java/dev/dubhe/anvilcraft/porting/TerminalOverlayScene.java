package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.support.TerminalRemoteOverlay;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.lwjgl.glfw.GLFW;

public final class TerminalOverlayScene {
    private static boolean started;
    private static volatile boolean prepared;
    private static volatile Throwable failure;
    private static AbstractContainerScreen<?> screen;
    private static Slot hovered;
    private static int stage;
    private static long nextAction;
    private static long deadline;
    private static boolean capturing;

    public static void frame(Minecraft client) {
        if (!started) {
            started = true;
            client.screen.onClose();
            nextAction = System.currentTimeMillis() + 1000;
            deadline = nextAction + 150000;
        }
        if (failure != null) throw new IllegalStateException("终端浮窗验证失败", failure);
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("终端浮窗超时：" + stage);
        if (capturing || System.currentTimeMillis() < nextAction) return;
        switch (stage) {
            case 0 -> {
                client.getSingleplayerServer().execute(() -> {
                    try {
                        var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                        var terminal = player.getInventory().getItem(0);
                        final var target = terminal.get(ModComponents.TERMINAL_BINDING).id().orElseThrow();
                        player.setGameMode(GameType.SURVIVAL);
                        player.getAbilities().invulnerable = true;
                        player.onUpdateAbilities();
                        for (int i = 0; i < 36; i++) player.getInventory().setItem(i, ItemStack.EMPTY);
                        player.getInventory().setItem(9, terminal);
                        player.getInventory().setSelectedSlot(5);
                        player.inventoryMenu.setCarried(ItemStack.EMPTY);
                        player.inventoryMenu.broadcastChanges();
                        var items = Storages.get().get(target).orElseThrow().getItems();
                        try (Transaction transaction = Transaction.openRoot()) {
                            for (int i = 0; i < items.size(); i++) {
                                if (!items.getResource(i).isEmpty()) items.extract(i, items.getResource(i), Integer.MAX_VALUE, transaction);
                            }
                            for (int i = 0; i < 300; i++) {
                                var item = switch (i % 3) {
                                    case 0 -> Items.DIAMOND;
                                    case 1 -> Items.IRON_INGOT;
                                    default -> Items.STONE;
                                };
                                var stack = new ItemStack(item);
                                stack.set(DataComponents.CUSTOM_NAME, Component.literal("Port " + String.format("%03d", i)));
                                require(items.insert(ItemResource.of(stack), 70, transaction) == 70, "测试库存写入失败");
                            }
                            transaction.commit();
                        }
                        prepared = true;
                    } catch (Throwable error) {
                        failure = error;
                    }
                });
                advance(1);
            }
            case 1 -> {
                if (!prepared || client.player.hasInfiniteMaterials() || client.player.getInventory().getItem(9).isEmpty()) return;
                screen = new InventoryScreen(client.player);
                client.setScreen(screen);
                hover(client, terminalSlot());
                advance(2);
            }
            case 2 -> {
                if (order().size() != 300) return;
                require(!TerminalRemoteOverlay.hasSelection(), "悬停不应自动选择物品");
                require(!press(0, 0, false), "未滚动时应放行拿起终端");
                capture(client, "4", 3);
            }
            case 3 -> {
                scroll();
                require((int) field("cursor") == 0, "第一次滚动应选中首格");
                for (int i = 0; i < 17; i++) scroll();
                require((int) field("cursor") == 17, "滚轮应跨页选择");
                key(GLFW.GLFW_KEY_LEFT_ALT);
                advance(4);
            }
            case 4 -> {
                require((int) field("sizeMode") == 1, "Alt 未切到五行");
                capture(client, "5", 5);
            }
            case 5 -> {
                key(GLFW.GLFW_KEY_LEFT_ALT);
                advance(6);
            }
            case 6 -> {
                require((int) field("sizeMode") == 2, "Alt 未切到六行");
                capture(client, "6", 7);
            }
            case 7 -> {
                key(GLFW.GLFW_KEY_TAB);
                for (char c : "Port 299".toCharArray()) character(c);
                advance(8);
            }
            case 8 -> {
                if (order().size() != 1) return;
                require(((EditBox) field("searchBox")).isFocused(), "Tab 应聚焦搜索");
                capture(client, "search", 9);
            }
            case 9 -> {
                require(press(1, 0, true), "已选择物品时右键应被接管");
                advance(10);
            }
            case 10 -> {
                if (screen.getMenu().getCarried().isEmpty()) return;
                require(screen.getMenu().getCarried().getHoverName().getString().equals("Port 299")
                    && screen.getMenu().getCarried().getCount() == 1, "跨页文字搜索或右键单件取出错误");
                require(press(0, 0, true), "指针持物时左键不能换走终端");
                require(press(1, 0, true), "右键存入未接管");
                advance(11);
            }
            case 11 -> {
                if (!screen.getMenu().getCarried().isEmpty()) return;
                require(press(0, GLFW.GLFW_MOD_SHIFT, true), "Shift 取物未接管");
                advance(12);
            }
            case 12 -> {
                long count = 0;
                for (int i = 0; i < 36; i++) {
                    var stack = client.player.getInventory().getItem(i);
                    if (stack.getHoverName().getString().equals("Port 299")) count += stack.getCount();
                }
                if (count != 64) return;
                require(screen.getMenu().getCarried().isEmpty(), "Shift 取出不应占指针");
                ((EditBox) field("searchBox")).setValue("#minecraft:planks");
                advance(13);
            }
            case 13 -> {
                if (!order().isEmpty()) return;
                ((EditBox) field("searchBox")).setValue("@minecraft");
                advance(14);
            }
            case 14 -> {
                if (order().size() != 300) return;
                key(GLFW.GLFW_KEY_ESCAPE);
                require(!TerminalRemoteOverlay.isHovering() && client.screen == screen, "Esc 应只关闭浮窗");
                advance(15);
            }
            case 15 -> {
                require(!TerminalRemoteOverlay.isHovering(), "Esc 后继续悬停不能重开");
                hover(client, screen.getMenu().getSlot(10));
                advance(16);
            }
            case 16 -> {
                hover(client, terminalSlot());
                advance(17);
            }
            case 17 -> {
                if (order().size() != 300) return;
                require(!TerminalRemoteOverlay.hasSelection(), "重新悬停应重置选择");
                client.screen.onClose();
                client.getSingleplayerServer().execute(() -> client.getSingleplayerServer().getPlayerList().getPlayers().getFirst()
                    .setGameMode(GameType.CREATIVE));
                advance(18);
            }
            case 18 -> {
                if (!client.player.hasInfiniteMaterials()) return;
                var creative = new CreativeModeInventoryScreen(client.player, client.player.connection.enabledFeatures(), false);
                client.setScreen(creative);
                try {
                    var method = CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", CreativeModeTab.class);
                    method.setAccessible(true);
                    method.invoke(creative, BuiltInRegistries.CREATIVE_MODE_TAB.getValue(CreativeModeTabs.INVENTORY));
                } catch (ReflectiveOperationException error) {
                    throw new IllegalStateException(error);
                }
                screen = creative;
                hover(client, terminalSlot());
                advance(19);
            }
            case 19 -> {
                if (order().size() != 300) return;
                scroll();
                require(press(1, 0, true), "创造背包取物未接管");
                advance(20);
            }
            case 20 -> {
                if (screen.getMenu().getCarried().isEmpty()) return;
                require(screen.getMenu().getCarried().getCount() == 1, "创造背包指针镜像错误");
                require(press(1, 0, true), "创造背包存入未接管");
                advance(21);
            }
            case 21 -> {
                if (!screen.getMenu().getCarried().isEmpty()) return;
                capture(client, "creative", 22);
            }
            case 22 -> {
                AnvilCraft.LOGGER.info("PORT_TERMINAL_OVERLAY_PASSED: sizes, scroll, paged search, take, insert, shift, escape, creative");
                client.stop();
            }
            default -> throw new IllegalStateException("Unknown overlay stage " + stage);
        }
    }

    private static Slot terminalSlot() {
        return screen.getMenu().slots.stream()
            .filter(slot -> slot.getItem().is(ModItems.HYPERDIMENSION_TERMINAL)).findFirst().orElseThrow();
    }

    private static void hover(Minecraft client, Slot slot) {
        hovered = slot;
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

    private static boolean press(int button, int modifiers, boolean execute) {
        var mouse = new MouseButtonEvent(screen.getLeftPos() + hovered.x + 8, screen.getTopPos() + hovered.y + 8,
            new MouseButtonInfo(button, modifiers));
        var event = new ScreenEvent.MouseButtonPressed.Pre(screen, mouse, false);
        NeoForge.EVENT_BUS.post(event);
        if (execute && !event.isCanceled()) screen.mouseClicked(mouse, false);
        var release = new ScreenEvent.MouseButtonReleased.Pre(screen, mouse);
        NeoForge.EVENT_BUS.post(release);
        if (execute && !release.isCanceled()) screen.mouseReleased(mouse);
        return event.isCanceled();
    }

    private static void scroll() {
        var event = new ScreenEvent.MouseScrolled.Pre(screen, screen.getLeftPos() + hovered.x + 8,
            screen.getTopPos() + hovered.y + 8, 0, -1);
        NeoForge.EVENT_BUS.post(event);
        require(event.isCanceled(), "浮窗未接管滚轮");
    }

    private static void key(int key) {
        var event = new ScreenEvent.KeyPressed.Pre(screen, new KeyEvent(key, 0, 0));
        NeoForge.EVENT_BUS.post(event);
        require(event.isCanceled(), "浮窗未接管按键 " + key);
    }

    private static void character(char character) {
        var event = new ScreenEvent.CharacterTyped.Pre(screen, new CharacterEvent(character));
        NeoForge.EVENT_BUS.post(event);
        require(event.isCanceled(), "浮窗未接管输入字符");
    }

    private static IntList order() {
        return (IntList) field("order");
    }

    private static Object field(String name) {
        try {
            var field = TerminalRemoteOverlay.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(null);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static void advance(int next) {
        AnvilCraft.LOGGER.info("PORT_TERMINAL_OVERLAY_STAGE: {} -> {}", stage, next);
        stage = next;
        nextAction = System.currentTimeMillis() + 750;
    }

    private static void capture(Minecraft client, String name, int next) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "terminal-overlay-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(next);
            }));
    }
}
