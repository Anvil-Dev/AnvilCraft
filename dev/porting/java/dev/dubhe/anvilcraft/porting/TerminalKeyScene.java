package dev.dubhe.anvilcraft.porting;

import com.mojang.blaze3d.platform.InputConstants;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import dev.dubhe.anvilcraft.client.init.ModKeyMappings;
import dev.dubhe.anvilcraft.client.rpc.StorageTerminalClientStub;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.CompletableFuture;

public final class TerminalKeyScene {
    private static boolean started;
    private static volatile boolean prepared;
    private static volatile Throwable failure;
    private static InputConstants.Key originalKey;
    private static Screen expected;
    private static EditBox search;
    private static CompletableFuture<?> lateRequest;
    private static int stage;
    private static long nextAction;
    private static long deadline;
    private static boolean capturing;

    public static void frame(Minecraft client) {
        if (!started) {
            started = true;
            originalKey = ModKeyMappings.OPEN_TERMINAL.get().getKey();
            client.setScreen(null);
            nextAction = System.currentTimeMillis() + 1000;
            deadline = nextAction + 120000;
        }
        if (failure != null) throw new IllegalStateException("终端快捷键验证失败", failure);
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("终端快捷键超时：" + stage);
        if (capturing || System.currentTimeMillis() < nextAction || stage > 0 && !prepared) return;
        switch (stage) {
            case 0 -> {
                client.getSingleplayerServer().execute(() -> {
                    try {
                        var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                        var terminal = player.getInventory().getItem(0);
                        require(terminal.get(ModComponents.TERMINAL_BINDING).id().isPresent(), "前置绑定终端缺失");
                        player.setGameMode(GameType.SURVIVAL);
                        player.getAbilities().invulnerable = true;
                        player.onUpdateAbilities();
                        for (int i = 0; i < 36; i++) player.getInventory().setItem(i, ItemStack.EMPTY);
                        player.getInventory().setItem(8, new ItemStack(ModItems.HYPERDIMENSION_TERMINAL.get()));
                        player.getInventory().setItem(9, terminal);
                        player.getInventory().setSelectedSlot(5);
                        player.inventoryMenu.setCarried(ItemStack.EMPTY);
                        player.inventoryMenu.broadcastChanges();
                        prepared = true;
                    } catch (Throwable error) {
                        failure = error;
                    }
                });
                key(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_B));
                advance(1);
            }
            case 1 -> {
                if (client.player.hasInfiniteMaterials() || client.player.getInventory().getItem(8).isEmpty()) return;
                worldKey(GLFW.GLFW_REPEAT);
                require(client.screen == null, "按键重复事件不应触发开仓");
                worldKey(GLFW.GLFW_PRESS);
                advance(2);
            }
            case 2 -> {
                require(client.screen == null, "不得跳过排在前面的未绑定终端而打开后面的终端");
                client.getSingleplayerServer().execute(() -> {
                    var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                    player.getInventory().setItem(8, ItemStack.EMPTY);
                    player.inventoryMenu.broadcastChanges();
                });
                advance(3);
            }
            case 3 -> {
                if (!client.player.getInventory().getItem(8).isEmpty()) return;
                client.player.inventoryMenu.setCarried(new ItemStack(Items.DIRT));
                worldKey(GLFW.GLFW_PRESS);
                advance(4);
            }
            case 4 -> {
                require(client.screen == null, "指针持物时不能开仓");
                client.player.inventoryMenu.setCarried(ItemStack.EMPTY);
                worldKey(GLFW.GLFW_PRESS);
                worldKey(GLFW.GLFW_PRESS);
                advance(5);
            }
            case 5 -> {
                if (!ready(client)) return;
                capture(client, "world", 6);
            }
            case 6 -> {
                client.screen.onClose();
                advance(7);
            }
            case 7 -> {
                client.setScreen(new InventoryScreen(client.player));
                require(screenKey(client), "背包快捷键必须接管事件");
                advance(8);
            }
            case 8 -> {
                if (!ready(client)) return;
                client.screen.onClose();
                advance(9);
            }
            case 9 -> {
                var inventory = new InventoryScreen(client.player);
                client.setScreen(inventory);
                var book = (RecipeBookComponent<?>) inventory.children().stream()
                    .filter(child -> child instanceof RecipeBookComponent<?>).findFirst().orElseThrow();
                if (!book.isVisible()) {
                    var mouse = new MouseButtonEvent(inventory.getLeftPos() + 114, inventory.height / 2.0 - 13, new MouseButtonInfo(0, 0));
                    inventory.mouseClicked(mouse, false);
                    inventory.mouseReleased(mouse);
                }
                search = (EditBox) field(book, "searchBox");
                search.setValue("");
                search.setFocused(true);
                expected = inventory;
                screenKey(client);
                inventory.charTyped(new CharacterEvent('b'));
                advance(10);
            }
            case 10 -> {
                require(client.screen == expected && search.getValue().equals("b"), "配方书搜索被终端快捷键抢占");
                key(InputConstants.Type.MOUSE.getOrCreate(4));
                var event = new ScreenEvent.MouseButtonPressed.Pre(client.screen,
                    new MouseButtonEvent(0, 0, new MouseButtonInfo(4, 0)), false);
                NeoForge.EVENT_BUS.post(event);
                require(event.isCanceled(), "背包鼠标绑定未接管事件");
                advance(11);
            }
            case 11 -> {
                if (!ready(client)) return;
                client.screen.onClose();
                advance(12);
            }
            case 12 -> {
                key(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_B));
                expected = new Screen(Component.literal("Other screen")) {
                    @Override
                    public boolean isPauseScreen() {
                        return false;
                    }
                };
                client.setScreen(expected);
                require(!screenKey(client), "非背包界面不能被快捷键接管");
                client.setScreen(null);
                worldKey(GLFW.GLFW_PRESS);
                lateRequest = (CompletableFuture<?>) field(StorageTerminalClientStub.class, "pendingOpen");
                require(lateRequest != null, "未建立待返回的开仓请求");
                client.setScreen(expected);
                advance(13);
            }
            case 13 -> {
                if (!lateRequest.isDone() || field(StorageTerminalClientStub.class, "pendingOpen") != null) return;
                require(client.screen == expected, "迟到的开仓响应不应覆盖用户新界面");
                client.setScreen(null);
                client.getSingleplayerServer().execute(() -> client.getSingleplayerServer().getPlayerList().getPlayers().getFirst()
                    .setGameMode(GameType.CREATIVE));
                advance(14);
            }
            case 14 -> {
                if (!client.player.hasInfiniteMaterials()) return;
                var creative = new CreativeModeInventoryScreen(client.player, client.player.connection.enabledFeatures(), false);
                client.setScreen(creative);
                selectTab(creative, CreativeModeTabs.searchTab());
                search = (EditBox) field(creative, "searchBox");
                search.setValue("");
                search.setFocused(true);
                expected = creative;
                require(!screenKey(client), "创造搜索框输入不应被快捷键取消");
                creative.keyPressed(new KeyEvent(GLFW.GLFW_KEY_B, 0, 0));
                creative.charTyped(new CharacterEvent('b'));
                advance(15);
            }
            case 15 -> {
                require(client.screen == expected && search.getValue().equals("b"), "创造搜索输入应保留");
                selectTab((CreativeModeInventoryScreen) client.screen,
                    BuiltInRegistries.CREATIVE_MODE_TAB.getValue(CreativeModeTabs.INVENTORY));
                require(screenKey(client), "创造背包页快捷键未接管");
                advance(16);
            }
            case 16 -> {
                if (!ready(client)) return;
                client.screen.onClose();
                advance(17);
            }
            case 17 -> {
                client.getSingleplayerServer().execute(() -> {
                    var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                    var terminal = player.getInventory().getItem(9);
                    player.getInventory().setItem(9, ItemStack.EMPTY);
                    player.setItemSlot(EquipmentSlot.OFFHAND, terminal);
                    player.inventoryMenu.broadcastChanges();
                });
                advance(18);
            }
            case 18 -> {
                if (!client.player.getInventory().getItem(9).isEmpty() || client.player.getOffhandItem().isEmpty()) return;
                key(InputConstants.Type.MOUSE.getOrCreate(4));
                NeoForge.EVENT_BUS.post(new InputEvent.MouseButton.Post(new MouseButtonInfo(4, 0), GLFW.GLFW_PRESS));
                advance(19);
            }
            case 19 -> {
                if (!ready(client)) return;
                capture(client, "offhand", 20);
            }
            case 20 -> {
                key(originalKey);
                AnvilCraft.LOGGER.info(
                    "PORT_TERMINAL_KEY_SCENE_PASSED: world, inventory, creative, search, cursor, mouse, offhand, late response");
                client.stop();
            }
            default -> throw new IllegalStateException("Unknown key stage " + stage);
        }
    }

    private static boolean ready(Minecraft client) {
        if (!(client.screen instanceof StorageScreen screen) || !screen.canTransferRecipe()) return false;
        require(screen.getTransferMaterials().getOrDefault(ItemResource.of(Items.DIAMOND), 0L) == 8, "快捷键打开了错误目标");
        return true;
    }

    private static void key(InputConstants.Key key) {
        ModKeyMappings.OPEN_TERMINAL.get().setKey(key);
        KeyMapping.resetMapping();
    }

    private static void worldKey(int action) {
        NeoForge.EVENT_BUS.post(new InputEvent.Key(new KeyEvent(GLFW.GLFW_KEY_B, 0, 0), action));
    }

    private static boolean screenKey(Minecraft client) {
        var event = new ScreenEvent.KeyPressed.Pre(client.screen, new KeyEvent(GLFW.GLFW_KEY_B, 0, 0));
        NeoForge.EVENT_BUS.post(event);
        return event.isCanceled();
    }

    private static void selectTab(CreativeModeInventoryScreen screen, CreativeModeTab tab) {
        try {
            var method = CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", CreativeModeTab.class);
            method.setAccessible(true);
            method.invoke(screen, tab);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    private static Object field(Object owner, String name) {
        for (Class<?> type = owner instanceof Class<?> cls ? cls : owner.getClass(); type != null; type = type.getSuperclass()) {
            try {
                var field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(owner instanceof Class<?> ? null : owner);
            } catch (NoSuchFieldException ignored) {
                continue;
            } catch (IllegalAccessException error) {
                throw new IllegalStateException(error);
            }
        }
        throw new IllegalStateException(name);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static void advance(int next) {
        AnvilCraft.LOGGER.info("PORT_TERMINAL_KEY_STAGE: {} -> {}", stage, next);
        stage = next;
        nextAction = System.currentTimeMillis() + 750;
    }

    private static void capture(Minecraft client, String name, int next) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "terminal-key-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(next);
            }));
    }
}
