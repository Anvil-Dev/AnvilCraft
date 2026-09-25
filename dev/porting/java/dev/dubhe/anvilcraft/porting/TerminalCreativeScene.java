package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import dev.dubhe.anvilcraft.util.ItemResourceHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class TerminalCreativeScene {
    private static boolean started;
    private static volatile boolean prepared;
    private static volatile Throwable failure;
    private static boolean originalInverted;
    private static CreativeModeInventoryScreen screen;
    private static UUID target;
    private static int sounds;
    private static int stage;
    private static long nextAction;
    private static long deadline;
    private static boolean capturing;
    private static CompletableFuture<Void> verification;

    @SubscribeEvent
    public static void sound(PlaySoundEvent event) {
        if (started && event.getOriginalSound().getIdentifier().equals(SoundEvents.ENDERMAN_TELEPORT.location())) sounds++;
    }

    public static void frame(Minecraft client) {
        if (!started) {
            started = true;
            originalInverted = AnvilCraftClient.CONFIG.invertOverrideAction;
            AnvilCraftClient.CONFIG.invertOverrideAction = false;
            client.screen.onClose();
            nextAction = System.currentTimeMillis() + 1000;
            deadline = nextAction + 150000;
        }
        if (failure != null) throw new IllegalStateException("创造终端验证失败", failure);
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("创造终端超时：" + stage);
        if (capturing || System.currentTimeMillis() < nextAction) return;
        switch (stage) {
            case 0 -> {
                client.getSingleplayerServer().execute(() -> {
                    try {
                        var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                        var terminal = player.getInventory().getItem(0).copy();
                        target = terminal.get(ModComponents.TERMINAL_BINDING).id().orElseThrow();
                        player.setGameMode(GameType.CREATIVE);
                        for (int i = 0; i < 36; i++) player.getInventory().setItem(i, ItemStack.EMPTY);
                        player.getInventory().setItem(9, terminal);
                        player.getInventory().setItem(12, new ItemStack(Items.STICK, 5));
                        player.inventoryMenu.setCarried(ItemStack.EMPTY);
                        player.inventoryMenu.broadcastChanges();
                        var items = Storages.get().get(target).orElseThrow().getItems();
                        try (Transaction transaction = Transaction.openRoot()) {
                            for (int i = 0; i < items.size(); i++) {
                                if (!items.getResource(i).isEmpty()) items.extract(i, items.getResource(i), Integer.MAX_VALUE, transaction);
                            }
                            items.insert(ItemResource.of(Items.STONE), 130, transaction);
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
                if (!prepared || !client.player.isCreative() || client.player.getInventory().getItem(9).isEmpty()) return;
                screen = new CreativeModeInventoryScreen(client.player, client.player.connection.enabledFeatures(), false);
                client.setScreen(screen);
                tab(BuiltInRegistries.CREATIVE_MODE_TAB.getValue(CreativeModeTabs.INVENTORY));
                require(!click(client, slot(client, 9), 0, true), "未选择时应允许原版拿起终端");
                advance(2);
            }
            case 2 -> {
                require(carried().is(ModItems.HYPERDIMENSION_TERMINAL), "原版未将终端放入指针");
                require(click(client, slot(client, 10), 1, true), "空槽右键未接管");
                require(click(client, slot(client, 10), 1, true), "在途重复点击应被吞掉");
                var escape = new ScreenEvent.KeyPressed.Pre(screen, new KeyEvent(GLFW.GLFW_KEY_ESCAPE, 0, 0));
                NeoForge.EVENT_BUS.post(escape);
                require(escape.isCanceled(), "等待事务时不能提前关闭创造菜单");
                advance(3);
            }
            case 3 -> {
                if (!ready(client, 10, new ItemStack(Items.STONE, 64), 66, 1)) return;
                require(carried().is(ModItems.HYPERDIMENSION_TERMINAL), "取物应保留指针终端");
                capture(client, "take", 4);
            }
            case 4 -> {
                require(click(client, slot(client, 10), 1, true), "默认右键存入未接管");
                advance(5);
            }
            case 5 -> {
                if (!ready(client, 10, ItemStack.EMPTY, 130, 2)) return;
                AnvilCraftClient.CONFIG.invertOverrideAction = true;
                require(click(client, slot(client, 10), 1, true), "反转后取出仍应使用右键");
                advance(6);
            }
            case 6 -> {
                if (!ready(client, 10, new ItemStack(Items.STONE, 64), 66, 3)) return;
                require(click(client, slot(client, 10), 0, true), "反转后的左键存入未接管");
                advance(7);
            }
            case 7 -> {
                if (!ready(client, 10, ItemStack.EMPTY, 130, 4)) return;
                AnvilCraftClient.CONFIG.invertOverrideAction = false;
                advance(8);
            }
            case 8 -> {
                require(!click(client, slot(client, 12), 0, true), "默认左键交换必须交给原版");
                require(carried().is(Items.STICK) && carried().getCount() == 5, "交换未保留原槽位物品");
                require(!click(client, slot(client, 14), 0, true), "普通物品放下不应被终端拦截");
                require(carried().isEmpty(), "普通物品未放下");
                require(!click(client, slot(client, 12), 0, true), "交换后的终端应能再次拿起");
                advance(9);
            }
            case 9 -> {
                require(click(client, slot(client, 40), 1, true), "副手空槽取出未接管");
                advance(10);
            }
            case 10 -> {
                if (!ready(client, 40, new ItemStack(Items.STONE, 64), 66, 5)) return;
                require(click(client, slot(client, 40), 1, true), "副手存入未接管");
                advance(11);
            }
            case 11 -> {
                if (!ready(client, 40, ItemStack.EMPTY, 130, 6)) return;
                tab(CreativeModeTabs.searchTab());
                var ghost = screen.getMenu().slots.stream()
                    .filter(slot -> slot.container != client.player.getInventory()).findFirst().orElseThrow();
                require(!click(client, ghost, 1, false), "创造物品列表不能作为真实背包取放目标");
                require(click(client, slot(client, 0), 1, true), "普通创造标签页快捷栏取出未接管");
                advance(12);
            }
            case 12 -> {
                if (!ready(client, 0, new ItemStack(Items.STONE, 64), 66, 7)) return;
                require(click(client, slot(client, 0), 1, true), "普通创造标签页快捷栏存入未接管");
                advance(13);
            }
            case 13 -> {
                if (!ready(client, 0, ItemStack.EMPTY, 130, 8)) return;
                tab(BuiltInRegistries.CREATIVE_MODE_TAB.getValue(CreativeModeTabs.INVENTORY));
                verification = new CompletableFuture<>();
                client.getSingleplayerServer().execute(() -> {
                    var items = Storages.get().get(target).orElseThrow().getItems();
                    try (Transaction transaction = Transaction.openRoot()) {
                        items.extract(ItemResource.of(Items.STONE), 130, transaction);
                        transaction.commit();
                    }
                    verification.complete(null);
                });
                stage = 14;
            }
            case 14 -> {
                if (!verification.isDone()) return;
                require(click(client, slot(client, 16), 1, true), "空仓取物应由服务端决定原版回退");
                advance(15);
            }
            case 15 -> {
                if (!carried().isEmpty() || !client.player.getInventory().getItem(16).is(ModItems.HYPERDIMENSION_TERMINAL)) return;
                if (!ready(client, 16, client.player.getInventory().getItem(16).copy(), 0, 8)) return;
                capture(client, "fallback", 16);
            }
            case 16 -> {
                AnvilCraftClient.CONFIG.invertOverrideAction = originalInverted;
                AnvilCraft.LOGGER.info(
                    "PORT_TERMINAL_CREATIVE_PASSED: inventory, offhand, hotbar, inverse, fallback, duplicate, native swap, audio");
                client.stop();
            }
            default -> throw new IllegalStateException("Unknown creative stage " + stage);
        }
    }

    private static boolean ready(Minecraft client, int index, ItemStack expected, long stored, int soundCount) {
        if (!ItemStack.matches(client.player.getInventory().getItem(index), expected) || sounds < soundCount) return false;
        require(sounds == soundCount, "一次操作出现重复音效");
        if (verification == null) {
            verification = new CompletableFuture<>();
            client.getSingleplayerServer().execute(() -> {
                try {
                    var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                    require(ItemResourceHelper.matchesNetworkStack(player.getInventory().getItem(index), expected, player.registryAccess()),
                        "客户端显示与服务端背包不一致：slot=" + index + " server=" + player.getInventory().getItem(index)
                            + " expected=" + expected);
                    var items = Storages.get().get(target).orElseThrow().getItems();
                    long amount = 0;
                    for (int i = 0; i < items.size(); i++) {
                        if (items.getResource(i).equals(ItemResource.of(Items.STONE))) amount += items.getAmountAsLong(i);
                    }
                    require(amount == stored, "仓储数量不守恒或重复取放");
                    verification.complete(null);
                } catch (Throwable error) {
                    verification.completeExceptionally(error);
                }
            });
            return false;
        }
        if (!verification.isDone()) return false;
        verification.join();
        return true;
    }

    private static ItemStack carried() {
        return screen.getMenu().getCarried();
    }

    private static Slot slot(Minecraft client, int inventoryIndex) {
        return screen.getMenu().slots.stream().filter(slot -> slot.container == client.player.getInventory()
            && slot.getSlotIndex() == inventoryIndex).findFirst().orElseThrow();
    }

    private static boolean click(Minecraft client, Slot slot, int button, boolean execute) {
        int x = screen.getLeftPos() + slot.x + 8;
        int y = screen.getTopPos() + slot.y + 8;
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
        var mouse = new MouseButtonEvent(x, y, new MouseButtonInfo(button, 0));
        var event = new ScreenEvent.MouseButtonPressed.Pre(screen, mouse, false);
        NeoForge.EVENT_BUS.post(event);
        if (execute && !event.isCanceled()) screen.mouseClicked(mouse, false);
        var release = new ScreenEvent.MouseButtonReleased.Pre(screen, mouse);
        NeoForge.EVENT_BUS.post(release);
        if (execute && !release.isCanceled()) screen.mouseReleased(mouse);
        return event.isCanceled();
    }

    private static void tab(CreativeModeTab tab) {
        try {
            var method = CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", CreativeModeTab.class);
            method.setAccessible(true);
            method.invoke(screen, tab);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static void advance(int next) {
        AnvilCraft.LOGGER.info("PORT_TERMINAL_CREATIVE_STAGE: {} -> {}", stage, next);
        stage = next;
        verification = null;
        nextAction = System.currentTimeMillis() + 750;
    }

    private static void capture(Minecraft client, String name, int next) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "terminal-creative-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(next);
            }));
    }
}
