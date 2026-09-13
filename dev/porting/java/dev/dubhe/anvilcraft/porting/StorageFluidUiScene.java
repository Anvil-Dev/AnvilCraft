package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.StoragePortManager;
import dev.dubhe.anvilcraft.block.entity.StorageFluidPortBlockEntity;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import dev.dubhe.anvilcraft.client.rpc.SettingClientStub;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import dev.dubhe.anvilcraft.saved.storage.ShulkerContainerStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import dev.dubhe.anvilcraft.saved.storage.category.FluidCategory;
import dev.dubhe.anvilcraft.saved.storage.category.store.CategoryMode;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 通过界面事件和真实 RPC 验证流体交互；不控制桌面鼠标或键盘。 */
public final class StorageFluidUiScene {
    private static final BlockPos CORE = new BlockPos(20, 81, 0);
    private static boolean requested;
    private static volatile boolean prepared;
    private static volatile boolean serverCursorBucket;
    private static TestScreen screen;
    private static int stage;
    private static int frames;
    private static boolean capturing;
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
            screen = new TestScreen();
            client.setScreen(screen);
        }
        if (System.nanoTime() - started > 120_000_000_000L) throw new IllegalStateException("流体界面场景超时，阶段 " + stage);
        if (client.screen != screen || capturing) return;
        frames++;
        if (frames % 10 == 0) {
            client.getSingleplayerServer().execute(() -> {
                if (client.getSingleplayerServer().getPlayerList().getPlayers().isEmpty()) return;
                final var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                serverCursorBucket = player.inventoryMenu.getCarried().is(Items.BUCKET);
            });
        }
        switch (stage) {
            case 0 -> {
                if (order().size() == 3 && amount() == 250 && frames > 30) capture(client, "initial", 1);
            }
            case 1 -> {
                clickFluid(0, 0);
                advance(2);
            }
            case 2 -> notice(client, StorageServerStub.FluidNotice.BUCKET_MISSING, "bucket-missing", 3);
            case 3 -> {
                giveBucket(client);
                advance(4);
            }
            case 4 -> {
                if (client.player.getInventory().getItem(12).is(Items.BUCKET)) {
                    click(left() + 176, top() + 148, 0, 0);
                    advance(5);
                }
            }
            case 5 -> {
                if (carried().is(Items.BUCKET) && serverCursorBucket) {
                    clickFluid(0, 0);
                    advance(6);
                }
            }
            case 6 -> notice(client, StorageServerStub.FluidNotice.NOT_ENOUGH, "not-enough", 7);
            case 7 -> {
                client.getSingleplayerServer().execute(() -> {
                    final var port = (StorageFluidPortBlockEntity) client.getSingleplayerServer().overworld().getBlockEntity(CORE.west(2));
                    port.getTank().set(0, FluidResource.of(Fluids.WATER), 2000);
                });
                advance(8);
            }
            case 8 -> {
                if (amount() == 2000 && !pending()) {
                    clickFluid(0, 0);
                    advance(9);
                }
            }
            case 9 -> {
                if (carried().is(Items.WATER_BUCKET) && amount() == 1000 && !pending() && frames > 15) {
                    capture(client, "filled", 10);
                }
            }
            case 10 -> {
                clickFluid(1, 0);
                advance(11);
            }
            case 11 -> {
                if (carried().isEmpty() && amount() == 1000 && order().size() == 4 && !pending() && frames > 15) {
                    capture(client, "stored-bucket", 12);
                }
            }
            case 12 -> {
                click(left() + 90, top() + 32, 0, 0);
                advance(13);
            }
            case 13 -> {
                if ((boolean) field("nbtFolded") && waterSlot() >= 0 && !pending()) {
                    categories(client, CategoryMode.BLOCKLIST, false);
                    advance(14);
                }
            }
            case 14 -> {
                if (order().size() == 2 && waterSlot() < 0 && frames > 15) capture(client, "filtered", 15);
            }
            case 15 -> {
                categories(client, CategoryMode.UNLIMITED, true);
                advance(16);
            }
            case 16 -> {
                if (waterSlot() >= 0 && order().size() == 2 && frames > 15) {
                    giveBucket(client);
                    advance(17);
                }
            }
            case 17 -> {
                if (client.player.getInventory().getItem(12).is(Items.BUCKET)) {
                    screen.keyPressed(new KeyEvent(GLFW.GLFW_KEY_LEFT_SHIFT, 0, GLFW.GLFW_MOD_SHIFT));
                    clickFluid(0, GLFW.GLFW_MOD_SHIFT);
                    advance(18);
                }
            }
            case 18 -> {
                if (amount() == 0 && waterSlot() >= 0 && !pending() && frames > 15) capture(client, "zero-held-order", 19);
            }
            case 19 -> {
                screen.keyReleased(new KeyEvent(GLFW.GLFW_KEY_LEFT_SHIFT, 0, 0));
                advance(20);
            }
            case 20 -> {
                if (waterSlot() < 0 && order().size() == 1 && !pending()) {
                    AnvilCraft.LOGGER.info("PORT_STORAGE_FLUID_UI_PASSED: notices, cursor buckets, right click, folding, "
                        + "categories, search and held order");
                    client.stop();
                }
            }
            default -> throw new IllegalStateException("未知流体界面测试阶段");
        }
    }

    private static void prepare(Minecraft client) {
        final var server = client.getSingleplayerServer();
        final var level = server.overworld();
        final var block = ModBlocks.SHULKER_CONTAINER.get();
        final var state = block.defaultBlockState();
        for (var part : block.getParts()) {
            level.setBlock(CORE.offset(block.offsetFrom(state, part)), block.placedState(part, state),
                Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        UUID id = UUID.randomUUID();
        ((StorageBlockEntity) level.getBlockEntity(CORE)).setId(id);
        for (int index = 0; index < 2; index++) {
            BlockPos pos = CORE.west(2 + index);
            level.setBlock(pos, ModBlocks.STORAGE_FLUID_PORT.getDefaultState(), Block.UPDATE_ALL);
            final var port = (StorageFluidPortBlockEntity) level.getBlockEntity(pos);
            port.getTank().set(0, FluidResource.of(index == 0 ? Fluids.WATER : ModFluids.HONEY.get()), index == 0 ? 250 : 1575);
            port.tickServer();
        }
        final var items = Storages.get().getOrCreate(id, ShulkerContainerStorage.class).getItems();
        try (Transaction transaction = Transaction.openRoot()) {
            items.insert(ItemResource.of(Items.DIAMOND), 16, transaction);
            transaction.commit();
        }
        final var player = server.getPlayerList().getPlayers().getFirst();
        player.getInventory().clearContent();
        player.inventoryMenu.setCarried(ItemStack.EMPTY);
        player.inventoryMenu.broadcastChanges();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 20.5 83 3.5 180 20");
        prepared = true;
    }

    private static void giveBucket(Minecraft client) {
        client.getSingleplayerServer().execute(() -> {
            final var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
            player.getInventory().setItem(12, new ItemStack(Items.BUCKET));
            player.inventoryMenu.broadcastChanges();
        });
    }

    private static void categories(Minecraft client, CategoryMode mode, boolean search) {
        final var listed = new ArrayList<>(SettingClientStub.setting().listed());
        listed.stream().filter(entry -> entry.getCategory() instanceof FluidCategory).forEach(entry -> entry.changeMode(mode));
        SettingClientStub.update(listed).thenRunAsync(() -> {
            if (search) ((EditBox) field("search")).setValue(waterEntry().icon().getHoverName().getString());
            else invokeReorder();
        }, client);
    }

    private static void notice(Minecraft client, StorageServerStub.FluidNotice expected, String image, int next) {
        if (((Component) field("flyoutMessage")).getString().equals(expected.text().getString()) && !pending()) {
            setField("flyoutTimer", 10);
            if (frames > 10) capture(client, image, next);
        }
    }

    private static boolean pending() {
        return (boolean) field("interactionPending");
    }

    private static ItemStack carried() {
        return (ItemStack) field("carried");
    }

    private static IntList order() {
        return (IntList) field("displayOrder");
    }

    private static StorageServerStub.@Nullable FluidEntry waterEntry() {
        for (Object value : (List<?>) field("fluids")) {
            final var entry = (StorageServerStub.FluidEntry) value;
            if (entry.icon().getFluid() == Fluids.WATER) return entry;
        }
        return null;
    }

    private static int amount() {
        final var water = waterEntry();
        return water == null ? -1 : water.amount();
    }

    private static int waterSlot() {
        final var fluids = (List<?>) field("fluids");
        for (int index = 0; index < order().size(); index++) {
            int slot = order().getInt(index) - StoragePortManager.FLUID_SLOT_BASE;
            if (slot >= 0 && slot < fluids.size()
                && ((StorageServerStub.FluidEntry) fluids.get(slot)).icon().getFluid() == Fluids.WATER) return index;
        }
        return -1;
    }

    private static int left() {
        return (int) field("left");
    }

    private static int top() {
        return (int) field("top");
    }

    private static void clickFluid(int button, int modifiers) {
        int index = waterSlot();
        if (index < 0) throw new IllegalStateException("流体槽未显示");
        click(left() + 122 + index % 9 * 18, top() + 26 + index / 9 * 18, button, modifiers);
    }

    private static void click(int x, int y, int button, int modifiers) {
        final var event = new MouseButtonEvent(x, y, new MouseButtonInfo(button, modifiers));
        screen.mouseClicked(event, false);
        screen.mouseReleased(event);
    }

    private static Object field(String name) {
        try {
            Field field = StorageScreen.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(screen);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void setField(String name, Object value) {
        try {
            Field field = StorageScreen.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(screen, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void invokeReorder() {
        try {
            final var method = StorageScreen.class.getDeclaredMethod("reorder", boolean.class);
            method.setAccessible(true);
            method.invoke(screen, false);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void advance(int next) {
        AnvilCraft.LOGGER.info("PORT_STORAGE_FLUID_UI_STAGE: {} -> {}", stage, next);
        stage = next;
        frames = 0;
    }

    private static void capture(Minecraft client, String name, int next) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "storage-fluid-ui-26.1-" + name + ".png", client.getMainRenderTarget(), 1, message ->
            client.execute(() -> {
                capturing = false;
                advance(next);
            }));
    }

    private static final class TestScreen extends StorageScreen {
        private TestScreen() {
            super(CORE);
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            int index = waterSlot();
            int x = index < 0 ? 0 : left() + 122 + index % 9 * 18;
            int y = index < 0 ? 0 : top() + 26 + index / 9 * 18;
            super.extractRenderState(graphics, x, y, partialTick);
        }
    }
}
