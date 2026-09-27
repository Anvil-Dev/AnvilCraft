package dev.dubhe.anvilcraft.porting;

import dev.anvilcraft.lib.v2.registrum.util.CreativeVariantPickerRegistry;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.state.Color;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItemGroups;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.system.windows.User32;

import java.nio.file.Files;

public final class CreativeVariantClientScene {
    private static int stage;
    private static long deadline;
    private static long next;
    private static boolean capturing;
    private static volatile boolean prepared;
    private static CreativeModeInventoryScreen screen;
    private static int left;
    private static int top;
    private static int overlayX;
    private static int overlayY;

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 120000;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Creative variant stage " + stage);
        if (capturing || System.currentTimeMillis() < next) return;
        if (!client.isWindowActive()) {
            GLFW.glfwFocusWindow(client.getWindow().handle());
            return;
        }
        try {
            switch (stage) {
                case 0 -> {
                    AnvilCraft.CLIENT_CONFIG.creativeVariantPickerEnabled = true;
                    client.getSingleplayerServer().execute(() -> {
                        var server = client.getSingleplayerServer();
                        var player = server.getPlayerList().getPlayers().getFirst();
                        player.setGameMode(GameType.CREATIVE);
                        player.getInventory().clearContent();
                        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tick unfreeze");
                        prepared = true;
                    });
                    advance(1);
                }
                case 1 -> {
                    if (!prepared || !client.player.hasInfiniteMaterials()) return;
                    client.options.guiScale().set(2);
                    client.resizeGui();
                    var tab = ModItemGroups.ANVILCRAFT_BUILD_BLOCK.get();
                    tab.buildContents(new CreativeModeTab.ItemDisplayParameters(client.player.connection.enabledFeatures(), true,
                        client.level.registryAccess()));
                    long displayed = tab.getDisplayItems().stream().filter(stack -> ModBlocks.REINFORCED_CONCRETES.values()
                        .stream().anyMatch(block -> stack.is(block.asItem()))).count();
                    long searchable = tab.getSearchTabDisplayItems().stream().filter(stack -> ModBlocks.REINFORCED_CONCRETES.values()
                        .stream().anyMatch(block -> stack.is(block.asItem()))).count();
                    check(displayed == 1 && searchable == 16, "Category folding preserves every searchable color");
                    screen = new CreativeModeInventoryScreen(client.player, client.player.connection.enabledFeatures(), false);
                    client.setScreen(screen);
                    tab(tab);
                    screen.getMenu().items.clear();
                    screen.getMenu().items.add(ModBlocks.REINFORCED_CONCRETES.get(Color.WHITE).asStack());
                    screen.getMenu().scrollTo(0);
                    left = (Integer) field(screen, "leftPos");
                    top = (Integer) field(screen, "topPos");
                    advance(10);
                }
                case 2 -> {
                    Slot source = screen.getMenu().slots.getFirst();
                    click(left + source.x + 8, top + source.y + 8, 1, 0);
                    check(field(screen, "anvillib$variantOverlay") != null, "Right click opens native variant overlay");
                    overlayX = Math.clamp(left + source.x + 8 - 39, 0, screen.width - 78);
                    overlayY = top + source.y - 1 - 80;
                    if (overlayY < 0) overlayY = Math.clamp(top + source.y - 1 + 18, 0, screen.height - 80);
                    Files.writeString(client.gameDirectory.toPath().resolve("creative-variants-26.1.json"),
                        "{\"scale\":" + client.getWindow().getGuiScale() + ",\"overlayX\":" + overlayX
                            + ",\"overlayY\":" + overlayY + ",\"slotX\":" + (left + source.x)
                            + ",\"slotY\":" + (top + source.y) + "}");
                    advance(20);
                }
                case 10 -> capture(client, "indicator", 2);
                case 20 -> capture(client, "overlay", 3);
                case 3 -> {
                    click(overlayX + 3 + 18 + 8, overlayY + 3 + 18 + 8, 0, 0);
                    check(screen.getMenu().getCarried().is(ModBlocks.REINFORCED_CONCRETES.get(Color.values()[5]).asItem()),
                        "Overlay selects the source color at row two column two");
                    screen.getMenu().setCarried(ItemStack.EMPTY);
                    AnvilCraft.CLIENT_CONFIG.creativeVariantPickerEnabled = false;
                    advance(4);
                }
                case 4 -> {
                    check(field(screen, "anvillib$variantOverlay") == null, "Disabled picker closes stale overlay");
                    check(CreativeVariantPickerRegistry.createVariants(ModBlocks.REINFORCED_CONCRETES.get(Color.WHITE).asStack()).isEmpty(),
                        "Disabled configuration removes picker availability");
                    tab(BuiltInRegistries.CREATIVE_MODE_TAB.getValue(CreativeModeTabs.INVENTORY));
                    client.player.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 12));
                    client.player.getInventory().setItem(10, new ItemStack(Items.EMERALD, 7));
                    client.gameMode.handleCreativeModeItemAdd(client.player.getInventory().getItem(0), 36);
                    client.gameMode.handleCreativeModeItemAdd(client.player.getInventory().getItem(10), 10);
                    advance(5);
                }
                case 5 -> {
                    shift(client, true);
                    check(client.hasShiftDown(), "Owned window receives synchronous Shift press");
                    Slot trash = (Slot) field(screen, "destroyItemSlot");
                    var method = CreativeModeInventoryScreen.class.getDeclaredMethod("slotClicked", Slot.class, int.class,
                        int.class, ContainerInput.class);
                    method.setAccessible(true);
                    method.invoke(screen, trash, trash.index, 0, ContainerInput.PICKUP);
                    shift(client, false);
                    advance(6);
                }
                case 6 -> {
                    if (client.hasShiftDown()) return;
                    if (!client.player.getInventory().getItem(0).isEmpty() || !client.player.getInventory().getItem(10).isEmpty()) return;
                    AnvilCraft.LOGGER.info("PORT_CREATIVE_VARIANTS_CLIENT_PASSED: folded category/search, indicator, "
                        + "overlay selection/config close and Shift trash");
                    client.stop();
                    stage = 7;
                }
                default -> {
                }
            }
        } catch (ReflectiveOperationException | java.io.IOException error) {
            throw new IllegalStateException("Creative variant stage " + stage, error);
        }
    }

    private static Object field(Object owner, String name) throws ReflectiveOperationException {
        for (Class<?> type = owner.getClass(); type != null; type = type.getSuperclass()) {
            try {
                var field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(owner);
            } catch (NoSuchFieldException error) {
                if (type.getSuperclass() == null) throw error;
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static void tab(CreativeModeTab tab) throws ReflectiveOperationException {
        var method = CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", CreativeModeTab.class);
        method.setAccessible(true);
        method.invoke(screen, tab);
    }

    private static void click(int x, int y, int button, int modifiers) {
        var mouse = new MouseButtonEvent(x, y, new MouseButtonInfo(button, modifiers));
        screen.mouseClicked(mouse, false);
        screen.mouseReleased(mouse);
    }

    private static void shift(Minecraft client, boolean down) {
        long window = GLFWNativeWin32.glfwGetWin32Window(client.getWindow().handle());
        User32.SendMessage(null, window, down ? User32.WM_KEYDOWN : User32.WM_KEYUP, User32.VK_SHIFT,
            1 | (0x2AL << 16) | (down ? 0 : 0xC0000000L));
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static void advance(int value) {
        stage = value;
        next = System.currentTimeMillis() + 600;
    }

    private static void capture(Minecraft client, String name, int value) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "creative-variants-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(value);
            }));
    }
}
