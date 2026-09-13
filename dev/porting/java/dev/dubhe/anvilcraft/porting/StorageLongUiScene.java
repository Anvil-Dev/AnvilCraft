package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.saved.storage.HyperdimensionStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

public final class StorageLongUiScene {
    private static final BlockPos CORE = new BlockPos(20, 81, 0);
    private static final long COUNT = 2L * Integer.MAX_VALUE + 7;
    private static boolean requested;
    private static volatile boolean prepared;
    private static StorageScreen screen;
    private static boolean capturing;
    private static int stage;
    private static int frames;
    private static long started;

    public static void frame(Minecraft client) {
        if (!requested) {
            requested = true;
            started = System.nanoTime();
            client.getSingleplayerServer().execute(() -> prepare(client));
        }
        if (!prepared || client.level.getBlockEntity(CORE) == null) return;
        if (screen == null) {
            client.options.guiScale().set(2);
            client.resizeGui();
            screen = new StorageScreen(CORE) {
                @Override
                public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
                    int left = (int) field("left");
                    int top = (int) field("top");
                    int column = stage < 2 ? 2 : 1;
                    super.extractRenderState(graphics, left + 122 + column * 18, top + 26, partialTick);
                }
            };
            client.setScreen(screen);
        }
        if (client.screen != screen || capturing) return;
        if (System.nanoTime() - started > 120_000_000_000L) throw new IllegalStateException("大数量场景超时，阶段 " + stage);
        frames++;
        switch (stage) {
            case 0 -> {
                if (((Int2LongMap) field("counts")).containsValue(COUNT) && frames > 30) {
                    String tooltip = Component.translatable("screen.anvilcraft.storage.count", COUNT).getString();
                    if (!tooltip.contains(Long.toString(COUNT)) || tooltip.contains("screen.anvilcraft")) {
                        throw new IllegalStateException("精确数量提示未正确翻译");
                    }
                    if ((double) field("fullness") != 0) throw new IllegalStateException("超维存储不应产生有限容量占比");
                    capture(client, "unfolded", 1);
                }
            }
            case 1 -> {
                toggleFold();
                stage = 2;
                frames = 0;
            }
            case 2 -> {
                if (((Int2LongMap) field("foldedCounts")).containsValue(COUNT + 600) && frames > 20) capture(client, "folded", 3);
            }
            case 3 -> {
                screen.keyPressed(new KeyEvent(GLFW.GLFW_KEY_LEFT_SHIFT, 0, GLFW.GLFW_MOD_SHIFT));
                client.getSingleplayerServer().execute(() -> {
                    var level = client.getSingleplayerServer().overworld();
                    var core = (StorageBlockEntity) level.getBlockEntity(CORE);
                    var items = Storages.get().getOrCreate(core.getId(), HyperdimensionStorage.class).getItems();
                    try (Transaction transaction = Transaction.openRoot()) {
                        for (int index = 0; index < 3; index++) {
                            items.extract(ItemResource.of(Items.DIAMOND), Integer.MAX_VALUE, transaction);
                        }
                        transaction.commit();
                    }
                });
                stage = 4;
                frames = 0;
            }
            case 4 -> {
                var counts = (Int2LongMap) field("foldedCounts");
                if (counts.containsValue(600) && !counts.containsValue(COUNT + 600) && frames > 20) capture(client, "after-extract", 5);
            }
            case 5 -> {
                screen.keyReleased(new KeyEvent(GLFW.GLFW_KEY_LEFT_SHIFT, 0, 0));
                AnvilCraft.LOGGER.info("PORT_STORAGE_LONG_UI_PASSED: long count, folded total and preserved updates");
                client.stop();
            }
            default -> throw new IllegalStateException("未知大数量界面测试阶段");
        }
    }

    private static void prepare(Minecraft client) {
        var server = client.getSingleplayerServer();
        var level = server.overworld();
        var block = ModBlocks.HYPERDIMENSION_STORAGE_STATION.get();
        var state = block.defaultBlockState();
        for (var part : block.getParts()) {
            level.setBlock(CORE.offset(block.offsetFrom(state, part)), block.placedState(part, state),
                Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        UUID id = UUID.randomUUID();
        ((StorageBlockEntity) level.getBlockEntity(CORE)).setId(id);
        var items = Storages.get().getOrCreate(id, HyperdimensionStorage.class).getItems();
        items.set(0, ItemResource.of(Items.DIAMOND), Integer.MAX_VALUE);
        items.set(1, ItemResource.of(Items.DIAMOND), Integer.MAX_VALUE);
        items.set(2, ItemResource.of(Items.DIAMOND), 7);
        ItemStack variant = new ItemStack(Items.DIAMOND);
        variant.set(DataComponents.CUSTOM_NAME, Component.literal("variant"));
        items.set(3, ItemResource.of(variant), 600);
        items.set(4, ItemResource.of(Items.IRON_INGOT), 200);
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 20.5 83 3.5 180 20");
        prepared = true;
    }

    private static void toggleFold() {
        int x = (int) field("left") + 90;
        int y = (int) field("top") + 32;
        var event = new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0));
        screen.mouseClicked(event, false);
        screen.mouseReleased(event);
    }

    private static Object field(String name) {
        try {
            var field = StorageScreen.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(screen);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void capture(Minecraft client, String name, int next) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "storage-long-26.1-" + name + ".png", client.getMainRenderTarget(), 1, message ->
            client.execute(() -> {
                capturing = false;
                stage = next;
                frames = 0;
            }));
    }
}
