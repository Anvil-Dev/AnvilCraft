package dev.dubhe.anvilcraft.porting;

import dev.anvilcraft.lib.v2.wheel.api.WheelSelectionEffect;
import dev.anvilcraft.lib.v2.wheel.client.gui.component.WheelWidget;
import dev.anvilcraft.lib.v2.wheel.client.gui.screen.WheelScreen;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.HyperdimensionTerminalItem;
import dev.dubhe.anvilcraft.saved.setting.mode.BalanceMode;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Screenshot;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.UUID;

public final class TerminalBalanceScene {
    private static final BlockPos CORE = new BlockPos(20, 81, 0);
    private static boolean started;
    private static volatile boolean prepared;
    private static volatile Throwable failure;
    private static UUID storageId;
    private static int stage;
    private static long deadline;
    private static long nextAction;
    private static boolean capturing;

    public static void frame(Minecraft client) {
        if (!started) {
            started = true;
            deadline = System.currentTimeMillis() + 120000;
            client.options.guiScale().set(2);
            client.resizeGui();
            client.getSingleplayerServer().execute(() -> {
                try {
                    var server = client.getSingleplayerServer();
                    var level = server.overworld();
                    var block = ModBlocks.HYPERDIMENSION_STORAGE_STATION.get();
                    var state = block.defaultBlockState();
                    for (var part : block.getParts()) {
                        level.setBlock(CORE.offset(block.offsetFrom(state, part)), block.placedState(part, state),
                            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                    }
                    var core = (StorageBlockEntity) level.getBlockEntity(CORE);
                    core.setId(UUID.randomUUID());
                    storageId = core.getId();
                    var items = Storages.get().getOrCreate(storageId, core.getStorageType().clazz()).getItems();
                    try (Transaction transaction = Transaction.openRoot()) {
                        items.insert(ItemResource.of(Items.SNOWBALL), 20, transaction);
                        transaction.commit();
                    }
                    var player = server.getPlayerList().getPlayers().getFirst();
                    player.setGameMode(GameType.SURVIVAL);
                    player.setNoGravity(true);
                    player.getAbilities().invulnerable = true;
                    player.onUpdateAbilities();
                    for (int x = 9; x <= 11; x++) {
                        for (int z = 14; z <= 16; z++) {
                            level.setBlock(new BlockPos(x, 85, z), Blocks.BEDROCK.defaultBlockState(), Block.UPDATE_ALL);
                        }
                    }
                    for (int i = 0; i < 36; i++) player.getInventory().setItem(i, ItemStack.EMPTY);
                    var terminal = new ItemStack(ModItems.HYPERDIMENSION_TERMINAL.get());
                    HyperdimensionTerminalItem.bindToStation(player, terminal, core);
                    player.getInventory().setItem(0, terminal);
                    player.getInventory().setItem(1, new ItemStack(Items.SNOWBALL));
                    player.getInventory().setItem(9, new ItemStack(Items.STONE, 64));
                    player.getInventory().setItem(10, new ItemStack(Items.STONE, 10));
                    player.getInventory().setSelectedSlot(0);
                    player.inventoryMenu.broadcastChanges();
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 10.5 86 15.5");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tick unfreeze");
                    prepared = true;
                } catch (Throwable error) {
                    failure = error;
                }
            });
        }
        if (failure != null) throw new IllegalStateException("终端均衡客户端验证失败", failure);
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("终端均衡客户端超时：" + stage);
        client.getToastManager().clear();
        if (!prepared || capturing || System.currentTimeMillis() < nextAction) return;
        switch (stage) {
            case 0 -> {
                if (client.screen != null || client.player.hasInfiniteMaterials()
                    || !client.player.getMainHandItem().is(ModItems.HYPERDIMENSION_TERMINAL)) {
                    return;
                }
                AnvilCraft.LOGGER.info("PORT_BALANCE_PRESS_TIME: {}", client.level.getGameTime());
                key(GLFW.GLFW_PRESS);
                advance(1);
            }
            case 1 -> {
                if (!(client.screen instanceof WheelScreen)) return;
                point(client, 0);
                advance(2);
            }
            case 2 -> {
                var widget = (WheelWidget) field(client.screen, "wheelWidget");
                require(widget.getCurrentSectionIndex() == 0, "鼠标未命中智能模式扇区");
                require(field(widget, "selectionEffect") == WheelSelectionEffect.ANNULAR_SECTOR, "必须使用源版环形扇区高亮");
                capture(client, 3);
            }
            case 3 -> {
                key(GLFW.GLFW_RELEASE);
                advance(4);
            }
            case 4 -> {
                if (client.screen != null
                    || client.player.getInventory().getItem(0).get(ModComponents.TERMINAL_BALANCE_MODE) != BalanceMode.SMART) {
                    return;
                }
                if (!client.player.getInventory().getItem(10).isEmpty()) return;
                client.player.getInventory().setSelectedSlot(1);
                advance(5);
            }
            case 5 -> {
                client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
                advance(6);
            }
            case 6 -> {
                if (client.player.getMainHandItem().getCount() != 16) return;
                client.getSingleplayerServer().execute(() -> {
                    try {
                        require(count(Items.STONE) == 10 && count(Items.SNOWBALL) == 4, "自动存入或实际投掷后的补货数量错误");
                    } catch (Throwable error) {
                        failure = error;
                    }
                });
                client.player.getInventory().setSelectedSlot(0);
                advance(7);
            }
            case 7 -> {
                key(GLFW.GLFW_PRESS);
                advance(8);
            }
            case 8 -> {
                if (!(client.screen instanceof WheelScreen)) return;
                point(client, 2);
                advance(9);
            }
            case 9 -> {
                key(GLFW.GLFW_RELEASE);
                advance(10);
            }
            case 10 -> {
                if (client.player.getInventory().getItem(0).get(ModComponents.TERMINAL_BALANCE_MODE) != BalanceMode.OFF) return;
                client.getSingleplayerServer().execute(() -> {
                    var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                    player.getInventory().setItem(10, new ItemStack(Items.STONE, 10));
                    player.inventoryMenu.broadcastChanges();
                });
                advance(11);
                nextAction += 1500;
            }
            case 11 -> {
                require(client.player.getInventory().getItem(10).getCount() == 10, "关闭模式后仍在自动存入");
                AnvilCraft.LOGGER.info("PORT_TERMINAL_BALANCE_SCENE_PASSED: annular wheel, mode packet, deposit, thrown-item restock, off");
                client.stop();
            }
            default -> throw new IllegalStateException("Unknown balance stage " + stage);
        }
    }

    private static long count(net.minecraft.world.item.Item item) {
        var items = Storages.get().get(storageId).orElseThrow().getItems();
        long count = 0;
        for (int i = 0; i < items.size(); i++) {
            if (items.getResource(i).equals(ItemResource.of(item))) count += items.getAmountAsLong(i);
        }
        return count;
    }

    private static void key(int action) {
        NeoForge.EVENT_BUS.post(new InputEvent.Key(new KeyEvent(GLFW.GLFW_KEY_LEFT_ALT, 0, GLFW.GLFW_MOD_ALT), action));
    }

    private static void point(Minecraft client, int index) {
        var widget = field(client.screen, "wheelWidget");
        var section = (WheelWidget.WheelSection) ((List<?>) field(widget, "sections")).get(index);
        try {
            var window = client.getWindow();
            var x = MouseHandler.class.getDeclaredField("xpos");
            var y = MouseHandler.class.getDeclaredField("ypos");
            x.setAccessible(true);
            y.setAccessible(true);
            x.setDouble(client.mouseHandler, section.center().x * window.getScreenWidth() / window.getGuiScaledWidth());
            y.setDouble(client.mouseHandler, section.center().y * window.getScreenHeight() / window.getGuiScaledHeight());
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    private static Object field(Object owner, String name) {
        try {
            var field = owner.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(owner);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static void advance(int next) {
        AnvilCraft.LOGGER.info("PORT_TERMINAL_BALANCE_STAGE: {} -> {}", stage, next);
        stage = next;
        nextAction = System.currentTimeMillis() + 1000;
    }

    private static void capture(Minecraft client, int next) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "terminal-balance-26.1.png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(next);
            }));
    }
}
