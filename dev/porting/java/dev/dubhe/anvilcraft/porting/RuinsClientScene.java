package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.RuinsBlockEntity;
import dev.dubhe.anvilcraft.client.gui.screen.RuinsScreen;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.item.block.RuinsBlockItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

public final class RuinsClientScene {
    private static final BlockPos POS = new BlockPos(0, 162, 0);
    private static int stage;
    private static long next;
    private static long deadline;
    private static boolean capturing;
    private static volatile boolean configured;
    private static volatile boolean destroyed;

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 150000;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Ruins foundation stage " + stage);
        if (capturing || client.getOverlay() != null || System.currentTimeMillis() < next) return;
        switch (stage) {
            case 0 -> {
                client.getSingleplayerServer().execute(() -> {
                    var server = client.getSingleplayerServer();
                    var level = server.overworld();
                    for (int x = -5; x <= 5; x++) {
                        for (int z = -4; z <= 6; z++) {
                            level.setBlockAndUpdate(new BlockPos(x, 161, z), Blocks.SMOOTH_STONE.defaultBlockState());
                        }
                    }
                    level.setBlockAndUpdate(POS, Blocks.STONE.defaultBlockState());
                    var palette = new net.minecraft.world.level.block.Block[]{Blocks.GRASS_BLOCK, Blocks.OAK_SLAB, Blocks.CHEST};
                    for (int row = 0; row < palette.length; row++) {
                        var first = new BlockPos(-3, 162, row * 2 - 2);
                        var second = new BlockPos(3, 162, row * 2 - 2);
                        level.setBlockAndUpdate(first, palette[row].defaultBlockState());
                        level.setBlockAndUpdate(second, palette[row].defaultBlockState());
                        RuinsBlockItem.convert(level, second);
                    }
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "gamemode creative @a");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 0.5 163 3.5 180 20");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set noon");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "weather clear");
                    var player = server.getPlayerList().getPlayers().getFirst();
                    player.setNoGravity(true);
                    player.setItemInHand(InteractionHand.MAIN_HAND, ModBlocks.RUINS_BLOCK.asStack());
                    player.containerMenu.broadcastChanges();
                });
                client.options.guiScale().set(2);
                client.options.fov().set(70);
                client.options.hideGui = true;
                client.resizeGui();
                client.setScreen(null);
                advance(1);
            }
            case 1 -> {
                if (!client.player.getMainHandItem().is(ModBlocks.RUINS_BLOCK.asItem())
                    || client.player.distanceToSqr(0.5, 163, 3.5) > 1) return;
                advance(2);
                use(client);
            }
            case 2 -> {
                if (!(client.level.getBlockEntity(POS) instanceof RuinsBlockEntity ruins)
                    || !ruins.getDisplayState().is(Blocks.STONE)) return;
                capture(client, "world", 3);
            }
            case 3 -> {
                client.options.keyShift.setDown(true);
                advance(4);
            }
            case 4 -> {
                if (!client.player.isShiftKeyDown()) return;
                advance(5);
                use(client);
            }
            case 5 -> {
                if (!(client.screen instanceof RuinsScreen)) return;
                client.options.keyShift.setDown(false);
                input(client).setValue("missing:loot");
                if (save(client).active) throw new IllegalStateException("Invalid loot ID accepted by editor");
                advance(6);
            }
            case 6 -> capture(client, "invalid", 7);
            case 7 -> {
                input(client).setValue("minecraft:blocks/diamond_block");
                if (!save(client).active) throw new IllegalStateException("Valid loot ID rejected by editor");
                click(client, client.screen.width / 2.0, client.screen.height / 2.0 + 54);
                advance(8);
            }
            case 8 -> capture(client, "editor", 9);
            case 9 -> {
                var button = save(client);
                advance(10);
                click(client, button.getX() + 20, button.getY() + 10);
            }
            case 10 -> {
                if (!configured) {
                    client.getSingleplayerServer().execute(() -> {
                        var ruins = (RuinsBlockEntity) client.getSingleplayerServer().overworld().getBlockEntity(POS);
                        configured = ruins.isFragile() && ruins.getDropsId().equals("minecraft:blocks/diamond_block");
                    });
                    next = System.currentTimeMillis() + 250;
                    return;
                }
                if (client.screen != null || client.player.isShiftKeyDown()) return;
                advance(11);
                use(client);
            }
            case 11 -> {
                if (!destroyed) {
                    client.getSingleplayerServer().execute(() -> {
                        var level = client.getSingleplayerServer().overworld();
                        int count = level.getEntitiesOfClass(ItemEntity.class, new AABB(POS).inflate(2)).stream()
                            .map(ItemEntity::getItem).filter(stack -> stack.is(Items.DIAMOND_BLOCK))
                            .mapToInt(stack -> stack.getCount()).sum();
                        destroyed = level.getBlockState(POS).isAir() && count == 1;
                    });
                    next = System.currentTimeMillis() + 250;
                    return;
                }
                client.getSingleplayerServer().execute(() -> {
                    var server = client.getSingleplayerServer();
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 0.5 165 7.5 180 30");
                });
                advance(12);
            }
            case 12 -> capture(client, "world-wide", 13);
            case 13 -> {
                client.setScreen(new ItemPreview());
                advance(14);
            }
            case 14 -> capture(client, "item", 15);
            case 15 -> {
                AnvilCraft.LOGGER.info(
                    "PORT_RUINS_FOUNDATION_CLIENT_PASSED: creative conversion, editor validation/save, fragile loot and item");
                stage = 16;
                client.stop();
            }
            default -> {
            }
        }
    }

    private static void use(Minecraft client) {
        client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND,
            new BlockHitResult(POS.getCenter(), Direction.SOUTH, POS, false));
    }

    private static EditBox input(Minecraft client) {
        return (EditBox) field(client.screen, "input");
    }

    private static Button save(Minecraft client) {
        return (Button) field(client.screen, "save");
    }

    private static Object field(Object instance, String name) {
        try {
            var field = instance.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(instance);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void click(Minecraft client, double x, double y) {
        var screen = client.screen;
        var event = new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0));
        screen.mouseClicked(event, false);
        screen.mouseReleased(event);
    }

    private static void advance(int target) {
        stage = target;
        next = System.currentTimeMillis() + 2300;
    }

    private static void capture(Minecraft client, String name, int target) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "ruins-foundation-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(target);
            }));
    }

    private static final class ItemPreview extends Screen {
        private ItemPreview() {
            super(Component.literal("Ruins"));
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, this.width, this.height, 0xFF303030);
            graphics.pose().pushMatrix();
            graphics.pose().translate(this.width / 2.0F - 32, this.height / 2.0F - 32);
            graphics.pose().scale(4, 4);
            graphics.item(ModBlocks.RUINS_BLOCK.asStack(), 0, 0);
            graphics.pose().popMatrix();
        }
    }
}
