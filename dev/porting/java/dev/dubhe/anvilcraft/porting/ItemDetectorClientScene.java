package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.ItemDetectorBlockEntity;
import dev.dubhe.anvilcraft.client.gui.component.SwitchableButton;
import dev.dubhe.anvilcraft.client.gui.screen.ItemDetectorScreen;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;

public final class ItemDetectorClientScene {
    private static final BlockPos POS = new BlockPos(8, 81, 8);
    private static int stage;
    private static long deadline;
    private static long next;
    private static volatile boolean prepared;
    private static volatile boolean serverInverted;
    private static boolean opened;
    private static boolean capturing;

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 120000;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Detector scene " + stage);
        if (capturing || System.currentTimeMillis() < next) return;
        client.options.guiScale().set(2);
        client.getToastManager().clear();
        client.gui.getChat().clearMessages(false);
        if (stage == 0) {
            client.getSingleplayerServer().execute(() -> {
                var server = client.getSingleplayerServer();
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tick unfreeze");
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 8.5 81 10.5");
                server.getPlayerList().getPlayers().getFirst().setNoGravity(true);
                server.overworld().setBlockAndUpdate(POS, ModBlocks.ITEM_DETECTOR.getDefaultState());
                detector(client).setOutputInvert(true);
                serverInverted = true;
                prepared = true;
            });
            advance(1);
            return;
        }
        if (!prepared) return;
        if (stage == 1 && !opened) {
            if (!(client.level.getBlockEntity(POS) instanceof ItemDetectorBlockEntity)) return;
            opened = true;
            open(client);
            next = System.currentTimeMillis() + 800;
            return;
        }
        if (stage == 6) {
            open(client);
            advance(7);
            return;
        }
        if (!(client.screen instanceof ItemDetectorScreen screen)) return;
        var button = button(screen);
        switch (stage) {
            case 1 -> {
                if (!screen.getMenu().getBlockEntity().isOutputInvert() || button.getCurrent() != 1) return;
                capture(client, "initial-on", 2);
            }
            case 2 -> {
                click(screen, button, 0);
                client.getSingleplayerServer().execute(() -> serverInverted = detector(client).isOutputInvert());
                advance(3);
            }
            case 3 -> {
                client.getSingleplayerServer().execute(() -> serverInverted = detector(client).isOutputInvert());
                if (serverInverted || screen.getMenu().getBlockEntity().isOutputInvert() || button.getCurrent() != 0) return;
                capture(client, "clicked-off", 4);
            }
            case 4 -> {
                client.getSingleplayerServer().execute(() -> detector(client).setOutputInvert(true));
                advance(5);
            }
            case 5 -> {
                if (!screen.getMenu().getBlockEntity().isOutputInvert() || button.getCurrent() != 1) return;
                screen.onClose();
                advance(6);
            }
            case 7 -> {
                if (button.getCurrent() != 1) return;
                serverInverted = true;
                click(screen, button, 1);
                advance(8);
            }
            case 8 -> {
                client.getSingleplayerServer().execute(() -> serverInverted = detector(client).isOutputInvert());
                if (serverInverted || screen.getMenu().getBlockEntity().isOutputInvert() || button.getCurrent() != 0) return;
                AnvilCraft.LOGGER.info("PORT_DETECTOR_UI_PASSED: initial sync, left/right click, server update and reopen");
                client.stop();
            }
            default -> throw new IllegalStateException("Unknown detector stage " + stage);
        }
    }

    private static void open(Minecraft client) {
        client.getSingleplayerServer().execute(() -> {
            var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
            player.openMenu(detector(client), POS);
        });
    }

    private static ItemDetectorBlockEntity detector(Minecraft client) {
        return (ItemDetectorBlockEntity) client.getSingleplayerServer().overworld().getBlockEntity(POS);
    }

    private static SwitchableButton button(ItemDetectorScreen screen) {
        try {
            var field = ItemDetectorScreen.class.getDeclaredField("outputModeButton");
            field.setAccessible(true);
            return (SwitchableButton) field.get(screen);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    private static void click(ItemDetectorScreen screen, SwitchableButton button, int mouseButton) {
        var event = new MouseButtonEvent(button.getX() + 4, button.getY() + 4, new MouseButtonInfo(mouseButton, 0));
        screen.mouseClicked(event, false);
        screen.mouseReleased(event);
    }

    private static void advance(int value) {
        stage = value;
        next = System.currentTimeMillis() + 800;
    }

    private static void capture(Minecraft client, String name, int nextStage) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "detector-ui-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(nextStage);
            }));
    }
}
