package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.network.multiple.MultiphasePackets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class MultiphaseScene {
    private static int stage;
    private static long next;
    private static long deadline;
    private static boolean capturing;
    private static volatile boolean prepared;
    private static volatile Throwable failure;

    public static void frame(Minecraft client) {
        if (deadline == 0) {
            client.screen.onClose();
            deadline = System.currentTimeMillis() + 90000;
        }
        if (failure != null || System.currentTimeMillis() > deadline) {
            throw new IllegalStateException("多相客户端验证失败，阶段 " + stage, failure);
        }
        if (capturing || System.currentTimeMillis() < next) return;
        switch (stage) {
            case 0 -> {
                client.getSingleplayerServer().execute(() -> {
                    try {
                        var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                        player.setGameMode(GameType.SURVIVAL);
                        player.setNoGravity(true);
                        for (int slot = 0; slot < 36; slot++) player.getInventory().setItem(slot, ItemStack.EMPTY);
                        player.getInventory().setItem(0, ModItems.TRANSCENDENCE_RESONATOR.asStack());
                        player.getInventory().setItem(1, ModItems.TRANSCENDENCE_ANVIL_HAMMER.asStack());
                        player.getInventory().setItem(2, ModItems.TRANSCENDENCE_HEAVY_HALBERD.asStack());
                        player.getInventory().setItem(3, ModItems.TRANSCENDENCE_DRAGON_ROD.asStack());
                        player.inventoryMenu.broadcastChanges();
                        prepared = true;
                    } catch (Throwable error) {
                        failure = error;
                    }
                });
                advance(1);
            }
            case 1 -> {
                if (!prepared || !client.player.getInventory().getItem(0).is(ModItems.TRANSCENDENCE_RESONATOR)) return;
                for (int slot = 0; slot < 4; slot++) {
                    var stack = client.player.getInventory().getItem(slot);
                    require(stack.getItemName().getString().endsWith("-α"), "四种多相工具初始名称应包含 alpha 后缀");
                    require(stack.getRarity() == Rarity.EPIC, "多相工具必须同步史诗稀有度");
                }
                client.player.getInventory().setSelectedSlot(0);
                client.setScreen(new InventoryScreen(client.player));
                advance(2);
            }
            case 2 -> {
                hover(client);
                advance(3);
            }
            case 3 -> capture(client, "alpha", 4);
            case 4 -> {
                client.screen.onClose();
                ClientPacketDistributor.sendToServer(new MultiphasePackets.SwitchPhase());
                advance(5);
            }
            case 5 -> {
                var stack = client.player.getMainHandItem();
                if (stack.get(ModComponents.MULTIPHASE).activePhase() != 1) return;
                require(stack.getItemName().getString().endsWith("-β") && !stack.getItemName().getString().contains("-α"),
                    "网络切换应只显示 beta 后缀");
                var title = stack.getTooltipLines(Item.TooltipContext.of(client.level), client.player, TooltipFlag.NORMAL).getFirst();
                require(title.getStyle().getColor() != null && title.getStyle().getColor().getValue() == 0xFF55FF,
                    "提示标题必须恢复源分支的史诗紫色");
                client.setScreen(new InventoryScreen(client.player));
                advance(6);
            }
            case 6 -> {
                hover(client);
                advance(7);
            }
            case 7 -> capture(client, "beta", 8);
            case 8 -> {
                AnvilCraft.LOGGER.info("PORT_MULTIPHASE_PASSED: default names, rarity, server phase switch and tooltip");
                client.stop();
            }
            default -> throw new IllegalStateException("Unknown multiphase stage " + stage);
        }
    }

    private static void hover(Minecraft client) {
        var screen = (AbstractContainerScreen<?>) client.screen;
        var slot = screen.getMenu().slots.stream()
            .filter(candidate -> candidate.container == client.player.getInventory() && candidate.getContainerSlot() == 0)
            .findFirst().orElseThrow();
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

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static void advance(int value) {
        stage = value;
        next = System.currentTimeMillis() + 1000;
    }

    private static void capture(Minecraft client, String name, int nextStage) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "multiphase-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(nextStage);
            }));
    }
}
