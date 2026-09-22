package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.support.AmuletSelectorSupport;
import dev.dubhe.anvilcraft.init.item.ModAmulets;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.BoxContents;
import dev.dubhe.anvilcraft.item.property.component.Comrades;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.GameType;

public final class AmuletRegistryScene {
    private static boolean started;
    private static volatile boolean prepared;
    private static volatile Throwable failure;
    private static boolean capturing;
    private static int stage;
    private static long next;
    private static long deadline;

    public static void frame(Minecraft client) {
        if (!started) {
            started = true;
            client.screen.onClose();
            next = System.currentTimeMillis() + 1000;
            deadline = next + 90000;
        }
        if (failure != null) throw new IllegalStateException("护符客户端验证失败", failure);
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("护符客户端验证超时：" + stage);
        if (capturing || System.currentTimeMillis() < next) return;
        switch (stage) {
            case 0 -> {
                client.getSingleplayerServer().execute(() -> {
                    try {
                        var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                        player.setGameMode(GameType.SURVIVAL);
                        player.getAbilities().invulnerable = true;
                        player.onUpdateAbilities();
                        for (int slot = 0; slot < 36; slot++) player.getInventory().setItem(slot, ItemStack.EMPTY);
                        player.getInventory().setItem(0, ModItems.COMRADE_AMULET.asStack());
                        var cat = ModItems.CAT_AMULET.asStack();
                        cat.set(ModComponents.AMULET_WEIGHT, 9);
                        var contents = BoxContents.EMPTY.mutable();
                        contents.tryInsert(cat);
                        var box = ModItems.AMULET_BOX.asStack();
                        box.set(ModComponents.BOX_CONTENTS, contents.immutable());
                        player.getInventory().setItem(1, box);
                        player.inventoryMenu.broadcastChanges();
                        prepared = true;
                    } catch (Throwable error) {
                        failure = error;
                    }
                });
                advance(1);
            }
            case 1 -> {
                if (!prepared || !client.player.getInventory().getItem(0).is(ModItems.COMRADE_AMULET.get())) return;
                client.player.getInventory().setSelectedSlot(0);
                client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
                advance(2);
            }
            case 2 -> {
                ItemStack signed = client.player.getMainHandItem();
                if (!signed.getOrDefault(ModComponents.COMRADES, Comrades.EMPTY).contains(client.player.getUUID())) return;
                require(ModAmulets.COMRADE.getKey().equals(signed.get(ModComponents.AMULET)), "签名后注册身份必须保留");
                var lines = signed.getTooltipLines(Item.TooltipContext.of(client.level), client.player, TooltipFlag.NORMAL).stream()
                    .map(Component::getString).toList();
                require(lines.stream().anyMatch(line -> line.contains("· ") && line.contains(client.player.getDisplayName().getString())),
                    "签名提示必须显示在线玩家名称和翻译后的条目");
                client.setScreen(new InventoryScreen(client.player));
                advance(3);
            }
            case 3 -> {
                hover(client, 0);
                advance(4);
            }
            case 4 -> capture(client, "signature", 5);
            case 5 -> {
                var box = client.player.getInventory().getItem(1);
                var contents = box.getOrDefault(ModComponents.BOX_CONTENTS, BoxContents.EMPTY);
                require(contents.usage() == 9 && AmuletSelectorSupport.Layout.layout(contents) == AmuletSelectorSupport.Layout.BIG_AMULET_1,
                    "护符盒布局必须使用同步的重量组件");
                hover(client, 1);
                advance(6);
            }
            case 6 -> capture(client, "weight", 7);
            case 7 -> {
                client.screen.onClose();
                client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
                advance(8);
            }
            case 8 -> {
                require(client.player.getMainHandItem().get(ModComponents.COMRADES).players().size() == 1,
                    "重复右键不得产生重复签名");
                AnvilCraft.LOGGER.info(
                    "PORT_AMULET_REGISTRY_PASSED: registered identity, signing, profile tooltip, component-driven box layout");
                client.stop();
            }
            default -> throw new IllegalStateException("Unknown amulet stage " + stage);
        }
    }

    private static void hover(Minecraft client, int inventorySlot) {
        var screen = (AbstractContainerScreen<?>) client.screen;
        var slot = screen.getMenu().slots.stream()
            .filter(candidate -> candidate.container == client.player.getInventory() && candidate.getContainerSlot() == inventorySlot)
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
        next = System.currentTimeMillis() + 750;
    }

    private static void capture(Minecraft client, String name, int next) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "amulet-registry-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(next);
            }));
    }
}
