package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.TerminalSessions;
import dev.dubhe.anvilcraft.client.renderer.item.decoration.TerminalInsertionDecoration;
import dev.dubhe.anvilcraft.client.rpc.TerminalReachabilityCache;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.List;
import java.util.UUID;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class TerminalPresentationScene {
    private static boolean started;
    private static volatile boolean prepared;
    private static volatile Throwable failure;
    private static AbstractContainerScreen<?> screen;
    private static UUID local;
    private static UUID shulker;
    private static int stage;
    private static long nextAction;
    private static long deadline;
    private static boolean capturing;
    private static int probe = -1;
    private static boolean probed;

    public static void frame(Minecraft client) {
        if (!started) {
            started = true;
            client.screen.onClose();
            local = TerminalSessions.localTerminalId(client.player.getUUID());
            shulker = TerminalSessions.shulkerTerminalId(client.player.getUUID());
            nextAction = System.currentTimeMillis() + 1000;
            deadline = nextAction + 120000;
        }
        if (failure != null) throw new IllegalStateException("终端提示与装饰验证失败", failure);
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("终端提示与装饰超时：" + stage);
        if (capturing || System.currentTimeMillis() < nextAction) return;
        switch (stage) {
            case 0 -> {
                client.getSingleplayerServer().execute(() -> {
                    try {
                        var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                        var terminal = player.getInventory().getItem(0).copy();
                        require(terminal.get(ModComponents.TERMINAL_BINDING).id().isPresent(), "前置绑定终端缺失");
                        player.setGameMode(GameType.SURVIVAL);
                        player.getAbilities().invulnerable = true;
                        player.onUpdateAbilities();
                        for (int i = 0; i < 36; i++) player.getInventory().setItem(i, ItemStack.EMPTY);
                        player.getInventory().setItem(9, terminal);
                        player.getInventory().setItem(10, new ItemStack(ModItems.LOCAL_TERMINAL.get()));
                        player.getInventory().setItem(11, new ItemStack(ModItems.SHULKER_TERMINAL.get()));
                        player.getInventory().setItem(12, new ItemStack(ModItems.HYPERDIMENSION_TERMINAL.get()));
                        player.inventoryMenu.setCarried(ItemStack.EMPTY);
                        player.inventoryMenu.broadcastChanges();
                        player.openMenu(new SimpleMenuProvider((id, inventory, owner) ->
                            ChestMenu.threeRows(id, inventory, new SimpleContainer(27)), Component.literal("Terminal presentation check")));
                        player.containerMenu.setCarried(new ItemStack(Items.STONE, 32));
                        player.containerMenu.broadcastChanges();
                        prepared = true;
                    } catch (Throwable error) {
                        failure = error;
                    }
                });
                advance(1);
            }
            case 1 -> {
                if (!prepared || !(client.screen instanceof AbstractContainerScreen<?> current)
                    || current.getMenu().containerId == 0 || current.getMenu().getCarried().isEmpty()) return;
                screen = current;
                moveMouse(client, 0, 0);
                checkTooltips(client);
                require(client.player.inventoryMenu.getCarried().isEmpty(), "场景必须保留独立的背包菜单空指针");
                requestProbe(0);
                advance(2);
            }
            case 2 -> {
                if (!probed || !Boolean.TRUE.equals(TerminalReachabilityCache.getReachability(local))
                    || !Boolean.TRUE.equals(TerminalReachabilityCache.getReachability(shulker))) return;
                requestProbe(1);
                advance(3);
            }
            case 3 -> {
                if (!probed) return;
                capture(client, "reachable", 4);
            }
            case 4 -> {
                var server = client.getSingleplayerServer();
                server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 100.5 82 2.5"));
                advance(5);
                nextAction += 2000;
            }
            case 5 -> {
                if (!Boolean.FALSE.equals(TerminalReachabilityCache.getReachability(local))
                    || !Boolean.FALSE.equals(TerminalReachabilityCache.getReachability(shulker))) return;
                requestProbe(2);
                advance(6);
            }
            case 6 -> {
                if (!probed) return;
                capture(client, "unreachable", 7);
            }
            case 7 -> {
                client.getSingleplayerServer().execute(() -> {
                    var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                    player.containerMenu.setCarried(ItemStack.EMPTY);
                    player.containerMenu.broadcastChanges();
                });
                advance(8);
            }
            case 8 -> {
                if (!screen.getMenu().getCarried().isEmpty()) return;
                requestProbe(3);
                var slot = screen.getMenu().getSlot(27);
                moveMouse(client, screen.getLeftPos() + slot.x + 8, screen.getTopPos() + slot.y + 8);
                advance(9);
            }
            case 9 -> {
                if (!probed) return;
                capture(client, "tooltip", 10);
            }
            case 10 -> {
                AnvilCraft.LOGGER.info(
                    "PORT_TERMINAL_PRESENTATION_PASSED: native tooltips, hidden binding, active menu, ghost, reachability");
                client.stop();
            }
            default -> throw new IllegalStateException("Unknown presentation stage " + stage);
        }
    }

    @SubscribeEvent
    public static void render(ScreenEvent.Render.Post event) {
        if (probe < 0 || event.getScreen() != screen) return;
        var client = Minecraft.getInstance();
        int current = probe;
        probe = -1;
        try {
            if (current == 0) TerminalReachabilityCache.clear();
            var decoration = new TerminalInsertionDecoration();
            for (int i = 27; i <= 30; i++) {
                var slot = screen.getMenu().getSlot(i);
                boolean rendered = decoration.render(event.getGuiGraphics(), client.font, slot.getItem(),
                    screen.getLeftPos() + slot.x, screen.getTopPos() + slot.y);
                boolean expected = current != 3 && (i == 27 || current == 1 && i != 30);
                require(rendered == expected, "错误的终端装饰状态：模式 " + current + " 槽位 " + i);
            }
            var terminal = screen.getMenu().getSlot(27).getItem();
            require(!decoration.render(event.getGuiGraphics(), client.font, terminal.copy(), 0, 0), "JEI/复制展示栈不能显示存入提示");
            ItemStack oldCarried = screen.getMenu().getCarried();
            screen.getMenu().setCarried(terminal.copy());
            try {
                require(!decoration.render(event.getGuiGraphics(), client.font, terminal, 0, 0), "捏着等值终端时不能提示放回自身");
            } finally {
                screen.getMenu().setCarried(oldCarried);
            }
            probed = true;
        } catch (Throwable error) {
            failure = error;
        }
    }

    private static void checkTooltips(Minecraft client) {
        ItemStack bound = screen.getMenu().getSlot(27).getItem();
        ItemStack unbound = screen.getMenu().getSlot(30).getItem();
        String boundText = I18n.get("item.anvilcraft.hyperdimension_terminal.bound");
        String unboundText = I18n.get("item.anvilcraft.hyperdimension_terminal.unbound");
        require(tooltip(client, bound, TooltipFlag.NORMAL).contains(boundText), "原版提示链没有已绑定状态");
        require(tooltip(client, unbound, TooltipFlag.NORMAL).contains(unboundText), "原版提示链没有未绑定状态");
        var hidden = bound.copy();
        hidden.set(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT.withHidden(ModComponents.TERMINAL_BINDING, true));
        require(!tooltip(client, hidden, TooltipFlag.NORMAL).contains(boundText), "隐藏绑定组件后仍输出状态");
        for (String name : List.of("local_terminal", "shulker_terminal", "hyperdimension_terminal")) {
            var stack = switch (name) {
                case "local_terminal" -> screen.getMenu().getSlot(28).getItem();
                case "shulker_terminal" -> screen.getMenu().getSlot(29).getItem();
                default -> bound;
            };
            String key = "tooltip.anvilcraft.item." + name;
            require(!I18n.get(key).equals(key) && tooltip(client, stack, TooltipFlag.NORMAL).contains(I18n.get(key)),
                "缺少终端简要说明：" + name);
        }
        var shift = new TooltipFlag() {
            @Override
            public boolean isAdvanced() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return false;
            }

            @Override
            public boolean hasShiftDown() {
                return true;
            }
        };
        String detailed = tooltip(client, screen.getMenu().getSlot(29).getItem(), shift);
        require(detailed.contains("64 blocks") && !detailed.contains("all Shulker Boxes"), "潜影说明没有匹配实际连接目标");
    }

    private static String tooltip(Minecraft client, ItemStack stack, TooltipFlag flag) {
        return stack.getTooltipLines(Item.TooltipContext.of(client.level), client.player, flag).stream()
            .map(Component::getString).collect(java.util.stream.Collectors.joining("\n"));
    }

    private static void requestProbe(int value) {
        probed = false;
        probe = value;
    }

    private static void moveMouse(Minecraft client, int mouseX, int mouseY) {
        try {
            var window = client.getWindow();
            var x = MouseHandler.class.getDeclaredField("xpos");
            var y = MouseHandler.class.getDeclaredField("ypos");
            x.setAccessible(true);
            y.setAccessible(true);
            x.setDouble(client.mouseHandler, (double) mouseX * window.getScreenWidth() / window.getGuiScaledWidth());
            y.setDouble(client.mouseHandler, (double) mouseY * window.getScreenHeight() / window.getGuiScaledHeight());
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static void advance(int next) {
        AnvilCraft.LOGGER.info("PORT_TERMINAL_PRESENTATION_STAGE: {} -> {}", stage, next);
        stage = next;
        nextAction = System.currentTimeMillis() + 750;
    }

    private static void capture(Minecraft client, String name, int next) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "terminal-presentation-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(next);
            }));
    }
}
