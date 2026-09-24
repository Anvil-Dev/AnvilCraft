package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import dev.dubhe.anvilcraft.client.rpc.SettingClientStub;
import dev.dubhe.anvilcraft.saved.storage.CraftingStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Screenshot;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class StorageMenuScene {
    private static boolean started;
    private static boolean jeiTesting;
    private static int foregrounds;
    private static volatile boolean prepared;
    private static volatile Throwable failure;
    private static StorageScreen screen;
    private static int stage;
    private static long nextAction;
    private static boolean capturing;

    @SubscribeEvent
    public static void foreground(ContainerScreenEvent.Render.Foreground event) {
        if (event.getContainerScreen() == screen) foregrounds++;
    }

    public static void frame(Minecraft client, BlockPos corePos) {
        if (jeiTesting) {
            StorageJeiScene.frame(client, corePos);
            return;
        }
        if (!started) {
            started = true;
            SettingClientStub.updateFlipped(false);
            client.getSingleplayerServer().execute(() -> {
                try {
                    var server = client.getSingleplayerServer();
                    var core = (StorageBlockEntity) server.overworld().getBlockEntity(corePos);
                    var storage = Storages.get().get(core.getId()).orElseThrow();
                    storage.setCrafting(CraftingStorage.EMPTY);
                    var items = storage.getItems();
                    try (Transaction transaction = Transaction.openRoot()) {
                        for (int i = 0; i < items.size(); i++) {
                            if (!items.getResource(i).isEmpty()) items.extract(i, items.getResource(i), Integer.MAX_VALUE, transaction);
                        }
                        items.insert(ItemResource.of(Items.STICK), 5, transaction);
                        transaction.commit();
                    }
                    var player = server.getPlayerList().getPlayers().getFirst();
                    for (int i = 0; i < 36; i++) player.getInventory().setItem(i, ItemStack.EMPTY);
                    player.getInventory().setItem(9, new ItemStack(Items.STONE, 3));
                    player.getInventory().setItem(1, new ItemStack(Items.DIAMOND, 2));
                    player.containerMenu.setCarried(ItemStack.EMPTY);
                    player.inventoryMenu.broadcastChanges();
                    prepared = true;
                } catch (Throwable error) {
                    failure = error;
                }
            });
        }
        if (failure != null) throw new IllegalStateException("仓储客户端容器验证失败", failure);
        if (!prepared || capturing || System.currentTimeMillis() < nextAction) return;
        switch (stage) {
            case 0 -> {
                if (!client.player.inventoryMenu.getCarried().isEmpty()
                    || !client.player.getInventory().getItem(9).is(Items.STONE)) return;
                screen = new StorageScreen(corePos);
                client.setScreen(screen);
                advance(1);
            }
            case 1 -> {
                require(foregrounds > 0, "容器前景事件缺失，JEI 列表将无法绘制");
                require(screen.getMenu().getSourcePos().equals(corePos), "扩展菜单必须指向当前仓储");
                require(client.player.containerMenu == client.player.inventoryMenu, "界面不应替换原版同步容器");
                try {
                    var method = StorageScreen.class.getDeclaredMethod("slotClicked", net.minecraft.world.inventory.Slot.class,
                        int.class, int.class, net.minecraft.world.inventory.ContainerInput.class);
                    method.setAccessible(true);
                    method.invoke(screen, screen.getMenu().getSlot(9), 9, 0, net.minecraft.world.inventory.ContainerInput.PICKUP);
                } catch (ReflectiveOperationException exception) {
                    throw new IllegalStateException(exception);
                }
                advance(11);
            }
            case 11 -> {
                require(client.player.inventoryMenu.getCarried().isEmpty() && client.player.getInventory().getItem(9).getCount() == 3,
                    "Extension slot callback sent a duplicate vanilla inventory click");
                AnvilCraft.LOGGER.info("PORT_STORAGE_EXTENSION_CLICK_PASSED");
                mouse(client, 122, 148);
                require(screen.getHoveredSlot().index == 9 && screen.getHoveredSlot().getItem().is(Items.STONE), "原版扩展悬停槽错误");
                screen.keyPressed(new KeyEvent(GLFW.GLFW_KEY_2, 0, 0));
                advance(2);
            }
            case 2 -> {
                require(client.player.getInventory().getItem(9).is(Items.DIAMOND)
                    && client.player.getInventory().getItem(1).getCount() == 3, "数字键交换被吞掉或重复执行");
                mouse(client, 122, 148);
                screen.keyPressed(new KeyEvent(GLFW.GLFW_KEY_Q, 0, 0));
                advance(3);
            }
            case 3 -> {
                require(client.player.getInventory().getItem(9).getCount() == 1, "背包 Q 应只丢一个");
                mouse(client, 122, 26);
                screen.keyPressed(new KeyEvent(GLFW.GLFW_KEY_Q, 0, 0));
                advance(4);
            }
            case 4 -> {
                var item = screen.getItemUnderMouse(screen.getLeftPos() + 122, screen.getTopPos() + 26);
                require(item != null && item.is(Items.STICK) && item.getCount() == 4, "仓储 Q 被父类吞掉或重复处理");
                click(285, 6);
                advance(5);
            }
            case 5 -> {
                require(screen.getMenu().getSlot(9).x == 8, "翻转未同步扩展菜单槽位");
                mouse(client, 16, 148);
                require(screen.getHoveredSlot().index == 9, "翻转后的扩展悬停不匹配");
                capture(client);
                advance(6);
            }
            case 6 -> {
                click(16, 148);
                advance(7);
            }
            case 7 -> {
                require(screen.getMenu().getCarried().is(Items.DIAMOND), "菜单指针与实际点击不同步");
                screen.onClose();
                advance(8);
            }
            case 8 -> {
                require(client.screen == null && client.player.containerMenu == client.player.inventoryMenu,
                    "关闭后应恢复正常背包上下文");
                require(client.player.inventoryMenu.getCarried().isEmpty(), "关闭必须归还指针");
                int count = 0;
                for (int i = 0; i < 36; i++) {
                    var stack = client.player.getInventory().getItem(i);
                    if (stack.is(Items.DIAMOND)) count += stack.getCount();
                }
                require(count == 1, "关闭归还不能复制或丢失物品");
                AnvilCraft.LOGGER.info("PORT_STORAGE_MENU_SCENE_PASSED: slot bridge, number key, Q, flip, cursor, close");
                if (Boolean.getBoolean("anvilcraft.portStorageJeiScene")) jeiTesting = true;
                else client.stop();
            }
            default -> throw new IllegalStateException("Unknown menu stage " + stage);
        }
    }

    private static void mouse(Minecraft client, int x, int y) {
        try {
            var window = client.getWindow();
            var horizontal = MouseHandler.class.getDeclaredField("xpos");
            var vertical = MouseHandler.class.getDeclaredField("ypos");
            horizontal.setAccessible(true);
            vertical.setAccessible(true);
            horizontal.setDouble(client.mouseHandler,
                (screen.getLeftPos() + x) * (double) window.getScreenWidth() / window.getGuiScaledWidth());
            vertical.setDouble(client.mouseHandler,
                (screen.getTopPos() + y) * (double) window.getScreenHeight() / window.getGuiScaledHeight());
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    private static void click(int x, int y) {
        var event = new MouseButtonEvent(screen.getLeftPos() + x, screen.getTopPos() + y, new MouseButtonInfo(0, 0));
        screen.mouseClicked(event, false);
        screen.mouseReleased(event);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static void advance(int next) {
        AnvilCraft.LOGGER.info("PORT_STORAGE_MENU_STAGE: {} -> {}", stage, next);
        stage = next;
        nextAction = System.currentTimeMillis() + 750;
    }

    private static void capture(Minecraft client) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "storage-menu-26.1.png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> capturing = false));
    }
}
