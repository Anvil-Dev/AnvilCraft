package dev.dubhe.anvilcraft.porting;

import dev.anvilcraft.lib.v2.wheel.client.gui.screen.WheelScreen;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.component.PocketEffectLayout;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.PocketInventory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.system.windows.User32;

import java.util.concurrent.CompletableFuture;

public final class PocketScene {
    private static int stage;
    private static long next;
    private static long deadline;
    private static boolean capturing;
    private static volatile boolean prepared;
    private static volatile Throwable failure;
    private static long keyboardWindow;
    private static CompletableFuture<Void> verification;

    public static void frame(Minecraft client) {
        if (deadline == 0) {
            client.screen.onClose();
            deadline = System.currentTimeMillis() + 180000;
        }
        if (failure != null || System.currentTimeMillis() > deadline) {
            releaseKeys();
            throw new IllegalStateException("口袋客户端验证失败，阶段 " + stage, failure);
        }
        if (capturing || System.currentTimeMillis() < next) return;
        client.getToastManager().clear();
        client.gui.getChat().clearMessages(false);
        switch (stage) {
            case 0 -> {
                server(client, () -> {
                    var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                    player.setGameMode(GameType.SURVIVAL);
                    player.removeAllEffects();
                    player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 100000, 0, false, false));
                    player.addEffect(new MobEffectInstance(MobEffects.SPEED, 100000, 0, false, false));
                    player.addEffect(new MobEffectInstance(MobEffects.HASTE, 100000, 0, false, false));
                    for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                        player.getInventory().setItem(slot, ItemStack.EMPTY);
                    }
                    player.getInventory().setItem(0, new ItemStack(Items.APPLE, 5));
                    player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STONE, 3));
                    configure(client, false);
                    player.inventoryMenu.broadcastChanges();
                    var server = client.getSingleplayerServer();
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tick unfreeze");
                    prepared = true;
                });
                advance(1);
            }
            case 1 -> {
                if (!prepared || PocketInventory.capacity(client.player) != 6 || !pockets(client).getItem(0).is(Items.DIAMOND)) return;
                client.player.getInventory().setSelectedSlot(0);
                client.player.getRecipeBook().setOpen(RecipeBookType.CRAFTING, false);
                client.setScreen(new InventoryScreen(client.player));
                advance(2);
            }
            case 2 -> {
                checkLayout(client, 6, false);
                capture(client, "survival-six", 3);
            }
            case 3 -> {
                clickSlot(client, 7);
                require(screen(client).getMenu().getCarried().isEmpty(), "点击非空口袋的护腿不应取下护腿");
                clickSlot(client, 46);
                advance(4);
            }
            case 4 -> {
                if (!screen(client).getMenu().getCarried().is(Items.DIAMOND)) return;
                clickSlot(client, 46);
                advance(5);
            }
            case 5 -> {
                if (!screen(client).getMenu().getCarried().isEmpty() || pockets(client).getItem(0).getCount() != 3) return;
                server(client, () -> configure(client, true));
                advance(6);
            }
            case 6 -> {
                if (PocketInventory.capacity(client.player) != 12 || !pockets(client).getItem(11).is(Items.EMERALD)) return;
                checkLayout(client, 12, false);
                capture(client, "survival-twelve", 7);
            }
            case 7 -> {
                var screen = screen(client);
                click(client, screen.getLeftPos() + 114, screen.height / 2 - 13);
                advance(8);
            }
            case 8 -> capture(client, "recipe-wide", 9);
            case 9 -> {
                client.options.guiScale().set(3);
                client.resizeGui();
                advance(10);
            }
            case 10 -> {
                require(PocketEffectLayout.isHiddenByRecipeBook(screen(client))
                    && PocketEffectLayout.areas(screen(client), 3).isEmpty(), "窄屏配方书覆盖背包时必须隐藏效果及其 JEI 占位");
                capture(client, "recipe-narrow", 11);
            }
            case 11 -> {
                client.options.guiScale().set(2);
                client.resizeGui();
                client.screen.onClose();
                server(client, () -> client.getSingleplayerServer().getPlayerList().getPlayers().getFirst().setGameMode(GameType.CREATIVE));
                advance(12);
            }
            case 12 -> {
                if (!client.player.isCreative()) return;
                var creative = new CreativeModeInventoryScreen(client.player, client.player.connection.enabledFeatures(), false);
                client.setScreen(creative);
                try {
                    var select = CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", CreativeModeTab.class);
                    select.setAccessible(true);
                    select.invoke(creative, BuiltInRegistries.CREATIVE_MODE_TAB.getValue(CreativeModeTabs.INVENTORY));
                } catch (ReflectiveOperationException error) {
                    throw new IllegalStateException(error);
                }
                advance(13);
            }
            case 13 -> {
                checkLayout(client, 12, true);
                capture(client, "creative-twelve", 14);
            }
            case 14 -> {
                clickSlot(client, 7);
                require(screen(client).getMenu().getCarried().isEmpty(), "创造背包也必须锁定非空口袋的护腿");
                clickSlot(client, 57);
                advance(15);
            }
            case 15 -> {
                if (!screen(client).getMenu().getCarried().is(Items.EMERALD)) return;
                clickSlot(client, 57);
                advance(16);
            }
            case 16 -> {
                if (!screen(client).getMenu().getCarried().isEmpty() || pockets(client).getItem(11).getCount() != 5) return;
                verification = new CompletableFuture<>();
                server(client, () -> {
                    var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                    require(PocketInventory.get(player).getItem(11).getCount() == 5, "创造口袋操作必须同步真实服务端库存");
                    player.setGameMode(GameType.SURVIVAL);
                    verification.complete(null);
                });
                client.screen.onClose();
                advance(17);
            }
            case 17 -> {
                if (!verification.isDone() || client.player.isCreative()) return;
                GLFW.glfwFocusWindow(client.getWindow().handle());
                advance(18);
            }
            case 18 -> {
                if (GLFW.glfwGetWindowAttrib(client.getWindow().handle(), GLFW.GLFW_FOCUSED) != GLFW.GLFW_TRUE) return;
                keyboardWindow = GLFWNativeWin32.glfwGetWin32Window(client.getWindow().handle());
                require(User32.PostMessage(null, keyboardWindow, User32.WM_KEYDOWN, User32.VK_CONTROL, 0x001D0001L),
                    "无法向测试窗口发送 Ctrl");
                require(User32.PostMessage(null, keyboardWindow, User32.WM_KEYDOWN, 0x46, 0x00210001L),
                    "无法向测试窗口发送 F");
                advance(19);
            }
            case 19 -> {
                if (!(client.screen instanceof WheelScreen)) return;
                TerminalBalanceScene.point(client, 0);
                advance(20);
            }
            case 20 -> capture(client, "wheel-twelve", 21);
            case 21 -> {
                releaseKeys();
                advance(22);
            }
            case 22 -> {
                if (client.screen != null || !client.player.getOffhandItem().is(Items.DIAMOND)) return;
                require(pockets(client).getItem(0).is(Items.STONE) && pockets(client).getItem(0).getCount() == 3
                    && client.player.getMainHandItem().is(Items.APPLE) && client.player.getMainHandItem().getCount() == 5,
                    "Ctrl+F 应交换指定口袋与副手，并消费原版 F 交换输入");
                AnvilCraft.LOGGER.info(
                    "PORT_POCKETS_PASSED: six/twelve slots, locks, survival/creative clicks, layouts, window Ctrl+F swap");
                client.stop();
            }
            default -> throw new IllegalStateException("Unknown pocket stage " + stage);
        }
    }

    private static void configure(Minecraft client, boolean advanced) {
        var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
        PocketInventory.get(player).clearContent();
        player.setItemSlot(EquipmentSlot.LEGS,
            advanced ? ModItems.WEATHERPROOF_SPACESUIT_LEGGINGS.asStack() : ModItems.POCKETS_LEGGINGS.asStack());
        PocketInventory.get(player).tick(player);
        player.inventoryMenu.getSlot(46).set(new ItemStack(Items.DIAMOND, 3));
        if (advanced) player.inventoryMenu.getSlot(57).set(new ItemStack(Items.EMERALD, 5));
        PocketInventory.get(player).syncChanges(player);
        player.inventoryMenu.broadcastChanges();
    }

    private static PocketInventory pockets(Minecraft client) {
        return PocketInventory.get(client.player);
    }

    private static AbstractContainerScreen<?> screen(Minecraft client) {
        return (AbstractContainerScreen<?>) client.screen;
    }

    private static void checkLayout(Minecraft client, int capacity, boolean creative) {
        var screen = screen(client);
        require(java.util.stream.IntStream.range(46, 58).filter(index -> screen.getMenu().getSlot(index).isActive()).count() == capacity,
            "有效口袋槽数量不匹配");
        AnvilCraft.LOGGER.info("PORT_POCKET_LAYOUT: capacity={} creative={} left={} top={} width={} height={}",
            capacity, creative, screen.getLeftPos(), screen.getTopPos(), screen.getImageWidth(), screen.getImageHeight());
        var areas = PocketEffectLayout.areas(screen, PocketEffectLayout.visibleEffects().size());
        require(areas.size() == 3 && areas.stream()
                .allMatch(area -> area.getY() + area.getHeight() <= screen.getTopPos() - (creative ? 28 : 0)),
            "药水效果必须完整放在背包/创造标签上方");
    }

    private static void clickSlot(Minecraft client, int index) {
        var screen = screen(client);
        var slot = screen.getMenu().getSlot(index);
        click(client, screen.getLeftPos() + slot.x + 8, screen.getTopPos() + slot.y + 8);
    }

    private static void click(Minecraft client, int x, int y) {
        var screen = screen(client);
        point(client, x, y);
        var mouse = new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0));
        var press = new ScreenEvent.MouseButtonPressed.Pre(screen, mouse, false);
        NeoForge.EVENT_BUS.post(press);
        if (!press.isCanceled()) screen.mouseClicked(mouse, false);
        var release = new ScreenEvent.MouseButtonReleased.Pre(screen, mouse);
        NeoForge.EVENT_BUS.post(release);
        if (!release.isCanceled()) screen.mouseReleased(mouse);
    }

    private static void point(Minecraft client, int x, int y) {
        try {
            var window = client.getWindow();
            var mx = MouseHandler.class.getDeclaredField("xpos");
            var my = MouseHandler.class.getDeclaredField("ypos");
            mx.setAccessible(true);
            my.setAccessible(true);
            mx.setDouble(client.mouseHandler, (double) x * window.getScreenWidth() / window.getGuiScaledWidth());
            my.setDouble(client.mouseHandler, (double) y * window.getScreenHeight() / window.getGuiScaledHeight());
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    private static void server(Minecraft client, Runnable action) {
        client.getSingleplayerServer().execute(() -> {
            try {
                action.run();
            } catch (Throwable error) {
                failure = error;
            }
        });
    }

    private static void releaseKeys() {
        if (keyboardWindow != 0) {
            User32.PostMessage(null, keyboardWindow, User32.WM_KEYUP, User32.VK_CONTROL, 0xC01D0001L);
            User32.PostMessage(null, keyboardWindow, User32.WM_KEYUP, 0x46, 0xC0210001L);
            keyboardWindow = 0;
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static void advance(int value) {
        AnvilCraft.LOGGER.info("PORT_POCKET_STAGE: {} -> {}", stage, value);
        stage = value;
        if (Minecraft.getInstance().screen instanceof AbstractContainerScreen<?>) point(Minecraft.getInstance(), 0, 0);
        next = System.currentTimeMillis() + 800;
    }

    private static void capture(Minecraft client, String name, int next) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "pockets-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(next);
            }));
    }
}
