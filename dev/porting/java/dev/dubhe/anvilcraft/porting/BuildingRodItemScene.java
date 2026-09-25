package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.renderer.item.StoredEnergyEmptyProperty;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.StoredEnergy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.GameType;

public final class BuildingRodItemScene {
    private static int stage;
    private static long next;
    private static long deadline;
    private static boolean capturing;
    private static volatile boolean prepared;
    private static volatile Throwable failure;

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 90000;
        if (failure != null || System.currentTimeMillis() > deadline) throw new IllegalStateException("Rod item scene: " + stage, failure);
        if (capturing || System.currentTimeMillis() < next) return;
        client.options.hideGui = false;
        switch (stage) {
            case 0 -> {
                client.getSingleplayerServer().execute(() -> {
                    try {
                        var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                        player.setGameMode(GameType.SURVIVAL);
                        player.setNoGravity(true);
                        player.getInventory().clearContent();
                        player.getInventory().setItem(0, ModItems.BUILDING_ROD.asStack());
                        player.inventoryMenu.broadcastChanges();
                        prepared = true;
                    } catch (Throwable error) {
                        failure = error;
                    }
                });
                advance(1);
            }
            case 1 -> {
                if (!prepared || !client.player.getInventory().getItem(0).is(ModItems.BUILDING_ROD)) return;
                check(client, true);
                client.setScreen(new InventoryScreen(client.player));
                advance(2);
            }
            case 2 -> {
                hover(client);
                advance(3);
            }
            case 3 -> capture(client, "empty", 4);
            case 4 -> {
                client.screen.onClose();
                client.getSingleplayerServer().execute(() -> {
                    var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                    player.getInventory().getItem(0).set(ModComponents.STORED_ENERGY, new StoredEnergy(8_000_000));
                    player.inventoryMenu.broadcastChanges();
                });
                advance(5);
            }
            case 5 -> {
                var energy = client.player.getInventory().getItem(0).getOrDefault(ModComponents.STORED_ENERGY, StoredEnergy.EMPTY);
                if (energy.value() == 0) return;
                check(client, false);
                client.setScreen(new InventoryScreen(client.player));
                advance(6);
            }
            case 6 -> {
                hover(client);
                advance(7);
            }
            case 7 -> capture(client, "charged", 8);
            case 8 -> {
                AnvilCraft.LOGGER.info("PORT_BUILDING_ROD_ITEM_PASSED");
                client.stop();
            }
            default -> throw new IllegalStateException("Unknown rod scene stage");
        }
    }

    private static void check(Minecraft client, boolean empty) {
        var stack = client.player.getInventory().getItem(0);
        if (StoredEnergyEmptyProperty.INSTANCE.get(stack, client.level, client.player, 0, ItemDisplayContext.GUI) != empty) {
            throw new IllegalStateException("Wrong empty-energy model branch");
        }
        var lines = stack.getTooltipLines(Item.TooltipContext.of(client.level), client.player, TooltipFlag.NORMAL);
        if (!(lines.get(1).getContents() instanceof TranslatableContents text)
            || !text.getKey().equals("tooltip.anvilcraft.property.stored_energy")) throw new IllegalStateException("Energy line moved");
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

    private static void advance(int target) {
        stage = target;
        next = System.currentTimeMillis() + 1000;
    }

    private static void capture(Minecraft client, String name, int target) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "building-rod-item-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(target);
            }));
    }
}
