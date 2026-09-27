package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.CreativeGeneratorBlockEntity;
import dev.dubhe.anvilcraft.client.gui.component.SliderWidget;
import dev.dubhe.anvilcraft.client.gui.screen.SliderScreen;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;

public final class CreativeGeneratorClientScene {
    private static final BlockPos POS = new BlockPos(0, 161, 4);
    private static int stage;
    private static long deadline;
    private static long next;
    private static long poll;
    private static long diagnosis;
    private static boolean capturing;
    private static volatile boolean ready;
    private static volatile int serverPower = Integer.MIN_VALUE;
    private static volatile String serverState = "pending";
    private static SliderScreen screen;

    public static void frame(Minecraft client) {
        long now = System.currentTimeMillis();
        if (deadline == 0) deadline = now + 120000;
        if (now > deadline) throw new IllegalStateException("Creative generator stage " + stage);
        if (capturing || client.getOverlay() != null || now < next) return;
        if (ready && now >= poll) {
            poll = now + 100;
            client.getSingleplayerServer().execute(() -> {
                var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                serverPower = ((CreativeGeneratorBlockEntity) player.level().getBlockEntity(POS)).getPower();
                serverState = player.containerMenu.getClass().getSimpleName() + " " + player.position()
                    + " spectator=" + player.isSpectator() + " hand=" + player.getMainHandItem();
            });
        }
        switch (stage) {
            case 0 -> {
                client.options.guiScale().set(2);
                client.resizeGui();
                client.getSingleplayerServer().execute(() -> {
                    var server = client.getSingleplayerServer();
                    var player = server.getPlayerList().getPlayers().getFirst();
                    player.getInventory().clearContent();
                    player.setPos(0.5, 162, 7);
                    player.setNoGravity(true);
                    player.inventoryMenu.broadcastChanges();
                    server.overworld().setBlockAndUpdate(POS, ModBlocks.CREATIVE_GENERATOR.getDefaultState());
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tick unfreeze");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 0.5 162 7 180 45");
                    ready = true;
                });
                advance(1);
            }
            case 1 -> {
                if (!ready || !client.level.getBlockState(POS).is(ModBlocks.CREATIVE_GENERATOR) || client.screen != null) return;
                client.player.setPos(0.5, 162, 7);
                client.player.setNoGravity(true);
                client.player.setYRot(180);
                client.player.setXRot(45);
                var result = client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(POS.getCenter(), Direction.UP, POS, false));
                AnvilCraft.LOGGER.info("PORT_GENERATOR_USE: {} hand={}", result, client.player.getMainHandItem());
                advance(2);
            }
            case 2 -> {
                if (now >= diagnosis) {
                    diagnosis = now + 10000;
                    AnvilCraft.LOGGER.info("PORT_GENERATOR_DIAG: screen={} serverPower={} state={}",
                        client.screen == null ? "null" : client.screen.getClass().getSimpleName(), serverPower, serverState);
                    if (client.screen instanceof SliderScreen current) {
                        screen = current;
                        AnvilCraft.LOGGER.info("PORT_GENERATOR_VALUE: {}", input().getValue());
                    }
                }
                if (!(client.screen instanceof SliderScreen current)) return;
                screen = current;
                if (!input().getValue().equals("8192") || serverPower != 8192) return;
                check(slider().getMin() == -17 && slider().getMax() == 17, "Logarithmic slider includes both 65536 endpoints");
                capture(client, "default", 3);
            }
            case 3 -> {
                click(160, 51);
                advance(4);
            }
            case 4 -> {
                if (!input().getValue().equals("65536") || serverPower != 65536) return;
                capture(client, "max", 5);
            }
            case 5 -> {
                click(16, 51);
                advance(6);
            }
            case 6 -> {
                if (!input().getValue().equals("-65536") || serverPower != -65536) return;
                screen.resize(screen.width, screen.height);
                check(input().getValue().equals("-65536"), "Resize retains the selected signed value");
                capture(client, "min", 7);
            }
            case 7 -> {
                input().setValue("100000");
                advance(8);
            }
            case 8 -> {
                if (serverPower != 65536) return;
                check(slider().getValue() == 65536, "Text input clamps before positioning and network submission");
                input().setValue("12345");
                advance(9);
            }
            case 9 -> {
                if (serverPower != 12345) return;
                input().setValue("-");
                advance(10);
            }
            case 10 -> {
                check(serverPower == 12345, "Partial negative text does not change the server setting");
                input().setValue("-12345");
                advance(11);
            }
            case 11 -> {
                if (serverPower != -12345) return;
                AnvilCraft.LOGGER.info("PORT_CREATIVE_GENERATOR_CLIENT_PASSED: default, endpoints, resize, "
                    + "typed values and native menu sync");
                client.player.closeContainer();
                client.stop();
                stage = 12;
            }
            default -> {
            }
        }
    }

    private static Object field(String name) {
        try {
            var field = SliderScreen.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(screen);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    private static EditBox input() {
        return (EditBox) field("value");
    }

    private static SliderWidget slider() {
        return (SliderWidget) field("sliderWidget");
    }

    private static void click(int x, int y) {
        var event = new MouseButtonEvent((screen.width - 176) / 2.0 + x, (screen.height - 77) / 2 + y, new MouseButtonInfo(0, 0));
        screen.mouseClicked(event, false);
        screen.mouseReleased(event);
    }

    private static void advance(int value) {
        AnvilCraft.LOGGER.info("PORT_GENERATOR_STAGE: {} -> {}", stage, value);
        stage = value;
        next = System.currentTimeMillis() + 500;
    }

    private static void capture(Minecraft client, String name, int value) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "creative-generator-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(value);
            }));
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
