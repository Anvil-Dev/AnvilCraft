package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.storage.HyperdimensionStorageStationBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public final class StorageStationClientScene {
    private static final BlockPos POS = new BlockPos(0, 162, 0);
    private static int stage;
    private static long deadline;
    private static long next;
    private static boolean capturing;

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 120000;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Storage station stage " + stage);
        if (capturing || client.getOverlay() != null || System.currentTimeMillis() < next) return;
        switch (stage) {
            case 0 -> {
                client.getSingleplayerServer().execute(() -> {
                    var server = client.getSingleplayerServer();
                    var level = server.overworld();
                    for (int x = -5; x <= 5; x++) {
                        for (int z = -5; z <= 7; z++) {
                            level.setBlockAndUpdate(new BlockPos(x, 161, z), Blocks.SMOOTH_STONE.defaultBlockState());
                        }
                    }
                    var block = ModBlocks.HYPERDIMENSION_STORAGE_STATION.get();
                    var state = block.defaultBlockState();
                    for (var part : block.getParts()) {
                        var partState = block.placedState(part, state);
                        level.setBlock(POS.offset(block.offsetFrom(state, part)), partState,
                            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                        int expected = switch (part) {
                            case BOTTOM_CENTER, MID_N, MID_E, MID_S, MID_W, MID_CENTER, TOP_CENTER -> 2;
                            case BOTTOM_WN, BOTTOM_EN, BOTTOM_ES, BOTTOM_WS, TOP_WN, TOP_EN, TOP_ES, TOP_WS -> 8;
                            default -> 6;
                        };
                        if (partState.getLightEmission() != expected) throw new IllegalStateException("Part lighting " + part);
                    }
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "gamemode spectator @a");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 0.5 163 7.5 180 0");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set noon");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "weather clear");
                });
                client.options.hideGui = true;
                client.options.fov().set(60);
                client.options.guiScale().set(2);
                client.resizeGui();
                client.setScreen(null);
                advance(1);
            }
            case 1 -> {
                if (!(client.level.getBlockEntity(POS) instanceof HyperdimensionStorageStationBlockEntity)) return;
                advance(9);
            }
            case 2 -> {
                client.getSingleplayerServer().execute(() -> {
                    var server = client.getSingleplayerServer();
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 5.5 165 7.5 144.462 20");
                });
                advance(3);
            }
            case 3 -> capture(client, "corner", 4);
            case 4 -> {
                client.getSingleplayerServer().execute(() -> {
                    var server = client.getSingleplayerServer();
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set midnight");
                });
                advance(5);
            }
            case 5 -> capture(client, "night", 6);
            case 6 -> {
                client.setScreen(new Preview());
                advance(7);
            }
            case 7 -> capture(client, "item", 8);
            case 8 -> {
                AnvilCraft.LOGGER.info("PORT_STORAGE_STATION_CLIENT_PASSED: "
                    + "day/night multipart render, part light emission and full item model");
                client.stop();
                stage = 10;
            }
            case 9 -> capture(client, "front", 2);
            default -> {
            }
        }
    }

    private static void advance(int value) {
        stage = value;
        next = System.currentTimeMillis() + 2500;
    }

    private static void capture(Minecraft client, String name, int target) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "storage-station-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(target);
            }));
    }

    private static final class Preview extends Screen {
        private Preview() {
            super(Component.literal("Storage station"));
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, this.width, this.height, 0xFF303030);
            graphics.pose().pushMatrix();
            graphics.pose().translate(this.width / 2.0F - 32, this.height / 2.0F - 32);
            graphics.pose().scale(4, 4);
            graphics.item(ModBlocks.HYPERDIMENSION_STORAGE_STATION.asStack(), 0, 0);
            graphics.pose().popMatrix();
        }
    }
}
