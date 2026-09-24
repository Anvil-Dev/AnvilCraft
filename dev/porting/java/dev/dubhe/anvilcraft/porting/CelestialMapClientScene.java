package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.client.gui.component.CelestialMapsGuideWidget;
import dev.dubhe.anvilcraft.client.gui.screen.CelestialForgingAnvilScreen;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.Arrays;

public final class CelestialMapClientScene {
    private static final BlockPos POS = new BlockPos(8, 80, 8);
    private static int stage;
    private static boolean requested;
    private static volatile boolean ready;
    private static boolean capturing;
    private static boolean opened;
    private static long next;
    private static long deadline;
    private static final int[][] EXPECTED = {{0, 0, 0, 0}, {20, 30, 0, 0}, {20, 45, 50, 0},
        {10, 45, 50, 30}, {10, 46, 50, 30}, {10, 46, 49, 30}, {10, 46, 49, 29}, {11, 46, 49, 29}};

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 240000;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Map fixture timed out at " + stage);
        client.options.pauseOnLostFocus = false;
        client.options.guiScale().set(2);
        client.options.hideGui = false;
        if (!requested) {
            requested = true;
            client.getSingleplayerServer().execute(() -> {
                var server = client.getSingleplayerServer();
                var player = server.getPlayerList().getPlayers().getFirst();
                server.overworld().setBlock(POS, ModBlocks.CELESTIAL_FORGING_ANVIL.getDefaultState(), Block.UPDATE_CLIENTS);
                player.teleportTo(8.5, 81, 8.5);
                player.setNoGravity(true);
                player.getInventory().clearContent();
                Item[] items = {ModBlocks.CONFINED_TIME_ANVILON.asItem(), ModBlocks.CONFINED_SPACE_ANVILON.asItem(),
                    ModBlocks.CONFINED_MASS_ANVILON.asItem(), ModBlocks.CONFINED_ENERGY_ANVILON.asItem()};
                for (int i = 0; i < items.length; i++) player.getInventory().setItem(9 + i, new ItemStack(items[i], 64));
                ready = true;
            });
            next = System.currentTimeMillis() + 2000;
            return;
        }
        if (!ready || capturing || System.currentTimeMillis() < next || client.getOverlay() != null) return;
        if (!opened) {
            if (!(client.level.getBlockEntity(POS) instanceof CelestialForgingAnvilBlockEntity)) return;
            opened = true;
            client.getSingleplayerServer().execute(() -> {
                var server = client.getSingleplayerServer();
                var be = (CelestialForgingAnvilBlockEntity) server.overworld().getBlockEntity(POS);
                server.getPlayerList().getPlayers().getFirst().openMenu(be, POS);
            });
            next = System.currentTimeMillis() + 1500;
            return;
        }
        if (!(client.screen instanceof CelestialForgingAnvilScreen screen)) return;
        CelestialMapsGuideWidget guide = screen.children().stream().filter(CelestialMapsGuideWidget.class::isInstance)
            .map(CelestialMapsGuideWidget.class::cast).findFirst().orElseThrow();
        if (!guide.visible) throw new IllegalStateException("Initial/edited map must remain visible");
        int[] expected = EXPECTED[Math.min(stage, EXPECTED.length - 1)];
        int[] counts = new int[4];
        for (int i = 0; i < 4; i++) counts[i] = screen.getMenu().getSlot(i).getItem().getCount();
        if (!Arrays.equals(counts, expected)) throw new IllegalStateException("Map counts " + Arrays.toString(counts)
            + " expected " + Arrays.toString(expected));
        // Verify the authoritative menu after the real client packets have arrived.
        int checkedStage = stage;
        ready = false;
        client.getSingleplayerServer().execute(() -> {
            var menu = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst().containerMenu;
            for (int i = 0; i < 4; i++) {
                if (menu.getSlot(i).getItem().getCount() != expected[i]) {
                    throw new IllegalStateException("Server chart count mismatch at stage " + checkedStage);
                }
            }
            AnvilCraft.LOGGER.info("PORT_CFA_MAP_SERVER {}: {}", checkedStage, Arrays.toString(expected));
            ready = true;
        });
        if (stage == 0 || stage == 3 || stage == 8) {
            capturing = true;
            Screenshot.grab(client.gameDirectory, "cfa-map-26.1-" + stage + ".png", client.getMainRenderTarget(), 1,
                message -> client.execute(() -> capturing = false));
        }
        switch (stage) {
            case 0 -> click(screen, guide, 31, 38);
            case 1 -> click(screen, guide, 141, 23);
            case 2 -> click(screen, guide, 21, 118);
            case 3 -> scroll(screen, guide, 30, 30, 0, 1);
            case 4 -> scroll(screen, guide, 120, 30, -1, 0);
            case 5 -> scroll(screen, guide, 30, 120, 0, -1);
            case 6 -> scroll(screen, guide, 30, 120, 1, 0);
            case 7 -> {
                if (guide.isMouseOver(guide.getX() + 60, guide.getY() + 60)) {
                    throw new IllegalStateException("Text quadrant must not accept chart clicks");
                }
                screen.resize(screen.width, screen.height);
            }
            default -> {
                AnvilCraft.LOGGER.info("PORT_CFA_MAP_CAPTURED: chart quadrants, both wheel axes, resize, "
                    + "authoritative counts");
                client.stop();
            }
        }
        stage++;
        next = System.currentTimeMillis() + 1200;
    }

    private static void click(CelestialForgingAnvilScreen screen, CelestialMapsGuideWidget guide, int x, int y) {
        var event = new MouseButtonEvent(guide.getX() + x / 2.0, guide.getY() + y / 2.0, new MouseButtonInfo(0, 0));
        if (!screen.mouseClicked(event, false)) throw new IllegalStateException("Map click not consumed");
        screen.mouseReleased(event);
    }

    private static void scroll(CelestialForgingAnvilScreen screen, CelestialMapsGuideWidget guide, int x, int y,
                               double horizontal, double vertical) {
        if (!screen.mouseScrolled(guide.getX() + x / 2.0, guide.getY() + y / 2.0, horizontal, vertical)) {
            throw new IllegalStateException("Map wheel not consumed");
        }
    }
}
