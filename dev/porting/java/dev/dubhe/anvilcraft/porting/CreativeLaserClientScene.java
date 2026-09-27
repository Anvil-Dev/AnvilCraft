package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.CreativeLaserBlockEntity;
import dev.dubhe.anvilcraft.block.laser.CreativeLaserBlock;
import dev.dubhe.anvilcraft.block.state.LensType;
import dev.dubhe.anvilcraft.client.gui.component.SliderWidget;
import dev.dubhe.anvilcraft.client.gui.component.TriStateButton;
import dev.dubhe.anvilcraft.client.gui.screen.CreativeLaserScreen;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

public final class CreativeLaserClientScene {
    private static final BlockPos POS = new BlockPos(0, 161, 4);
    private static int stage;
    private static long deadline;
    private static long next;
    private static long poll;
    private static boolean capturing;
    private static volatile boolean ready;
    private static volatile int serverLevel = Integer.MIN_VALUE;
    private static volatile LensType serverLens = LensType.NONE;
    private static volatile boolean serverGamma;
    private static int lensIndex = 1;
    private static CreativeLaserScreen screen;

    public static void frame(Minecraft client) {
        long now = System.currentTimeMillis();
        if (deadline == 0) deadline = now + 120000;
        if (now > deadline) throw new IllegalStateException("Creative laser stage " + stage);
        if (capturing || client.getOverlay() != null || now < next) return;
        if (ready && now >= poll) {
            poll = now + 100;
            client.getSingleplayerServer().execute(() -> {
                var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                var laser = (CreativeLaserBlockEntity) player.level().getBlockEntity(POS);
                if (laser == null) return;
                serverLevel = laser.getConfiguredLevel();
                serverLens = laser.getLensType();
                serverGamma = laser.isGamma();
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
                    server.overworld().setBlockAndUpdate(POS, ModBlocks.CREATIVE_LASER.getDefaultState()
                        .setValue(CreativeLaserBlock.FACING, Direction.NORTH));
                    server.overworld().setBlockAndUpdate(POS.north(4), Blocks.BEDROCK.defaultBlockState());
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tick unfreeze");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 0.5 162 7 180 45");
                    ready = true;
                });
                advance(1);
            }
            case 1 -> {
                if (!ready || !client.level.getBlockState(POS).is(ModBlocks.CREATIVE_LASER) || client.screen != null) return;
                client.player.setPos(0.5, 162, 7);
                client.player.setNoGravity(true);
                client.player.setYRot(180);
                client.player.setXRot(45);
                var result = client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(POS.getCenter(), Direction.UP, POS, false));
                AnvilCraft.LOGGER.info("PORT_CREATIVE_LASER_USE: {} hand={}", result, client.player.getMainHandItem());
                advance(2);
            }
            case 2 -> {
                if (!(client.screen instanceof CreativeLaserScreen current)) return;
                screen = current;
                if (!input().getValue().equals("16") || serverLevel != 16) return;
                check(slider().getMin() == 0 && slider().getMax() == 64, "Linear 0..64 slider");
                check(selected("lensButtons", 0) && selected("typeButtons", 0), "Initial lens and normal mode");
                capture(client, "default", 3);
            }
            case 3 -> {
                click(160, 37);
                advance(4);
            }
            case 4 -> {
                if (serverLevel != 64 || !input().getValue().equals("64")) return;
                capture(client, "max", 5);
            }
            case 5 -> {
                click(16, 37);
                advance(6);
            }
            case 6 -> {
                if (serverLevel != 0 || !input().getValue().equals("0")) return;
                input().setValue("99");
                advance(7);
            }
            case 7 -> {
                if (serverLevel != 64) return;
                check(slider().getValue() == 64, "Out-of-range two-digit input clamps the emitted strength");
                click(34, 37);
                advance(8);
            }
            case 8 -> {
                if (serverLevel != 63) return;
                click(16 + lensIndex * 18, 61);
                advance(9);
            }
            case 9 -> {
                if (serverLens != LensType.values()[lensIndex]) return;
                check(selected("lensButtons", lensIndex), "Lens button selection follows the clicked state");
                capture(client, serverLens.getSerializedName(), 10);
            }
            case 10 -> {
                lensIndex++;
                if (lensIndex < 4) {
                    advance(8);
                } else {
                    click(160, 61);
                    advance(11);
                }
            }
            case 11 -> {
                if (!serverGamma) return;
                check(selected("typeButtons", 1), "Gamma button selection");
                capture(client, "gamma", 12);
            }
            case 12 -> {
                screen.resize(screen.width, screen.height);
                check(input().getValue().equals("63") && selected("lensButtons", 3) && selected("typeButtons", 1),
                    "Resize retains level, lens and gamma controls");
                click(142, 61);
                input().setValue("");
                advance(13);
            }
            case 13 -> {
                if (serverGamma || serverLevel != 0) return;
                input().setValue("x");
                check(input().getValue().equals("0"), "Invalid input restores the current level");
                client.player.closeContainer();
                advance(14);
            }
            case 14 -> {
                if (client.screen != null) return;
                client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(POS.getCenter(), Direction.UP, POS, false));
                advance(15);
            }
            case 15 -> {
                if (!(client.screen instanceof CreativeLaserScreen current)) return;
                screen = current;
                if (!input().getValue().equals("0") || !selected("lensButtons", 3) || !selected("typeButtons", 0)) return;
                capture(client, "reopen", 16);
            }
            case 16 -> {
                client.player.closeContainer();
                client.setScreen(new ItemPreview());
                advance(17);
            }
            case 17 -> capture(client, "item", 18);
            case 18 -> {
                AnvilCraft.LOGGER.info("PORT_CREATIVE_LASER_CLIENT_PASSED: native interaction, input, "
                    + "lens/gamma controls, resize and reopen");
                client.player.closeContainer();
                client.stop();
                stage = 19;
            }
            default -> {
            }
        }
    }

    private static Object field(String name) {
        try {
            var field = CreativeLaserScreen.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(screen);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean selected(String name, int index) {
        var buttons = (List<TriStateButton>) field(name);
        return buttons.get(index).isSelected();
    }

    private static EditBox input() {
        return (EditBox) field("value");
    }

    private static SliderWidget slider() {
        return (SliderWidget) field("slider");
    }

    private static void click(int x, int y) {
        var event = new MouseButtonEvent((screen.width - 176) / 2.0 + x, (screen.height - 77) / 2 + y, new MouseButtonInfo(0, 0));
        screen.mouseClicked(event, false);
        screen.mouseReleased(event);
    }

    private static void advance(int value) {
        AnvilCraft.LOGGER.info("PORT_CREATIVE_LASER_STAGE: {} -> {}", stage, value);
        stage = value;
        next = System.currentTimeMillis() + 500;
    }

    private static void capture(Minecraft client, String name, int value) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "creative-laser-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(value);
            }));
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static final class ItemPreview extends Screen {
        private ItemPreview() {
            super(Component.literal("Creative laser item"));
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, this.width, this.height, 0xFF303030);
            graphics.pose().pushMatrix();
            graphics.pose().translate(this.width / 2.0F - 32, this.height / 2.0F - 32);
            graphics.pose().scale(4, 4);
            graphics.item(ModBlocks.CREATIVE_LASER.asStack(), 0, 0);
            graphics.pose().popMatrix();
        }
    }

}
