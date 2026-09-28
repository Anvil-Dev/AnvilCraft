package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.HypercubeBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public final class HypercubeClientScene {
    private static final BlockPos POS = new BlockPos(0, 162, 0);
    private static int stage;
    private static long deadline;
    private static long next;
    private static boolean capturing;

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 120000;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Hypercube stage " + stage);
        if (capturing || client.getOverlay() != null || System.currentTimeMillis() < next) return;
        switch (stage) {
            case 0 -> {
                client.getSingleplayerServer().execute(() -> {
                    var server = client.getSingleplayerServer();
                    var level = server.overworld();
                    for (int x = -3; x <= 3; x++) {
                        for (int y = 162; y <= 165; y++) {
                            level.setBlock(new BlockPos(x, y, -3), ((x + y) % 2 == 0 ? Blocks.WHITE_CONCRETE : Blocks.BLACK_CONCRETE)
                                .defaultBlockState(), Block.UPDATE_ALL);
                        }
                        for (int z = -3; z <= 2; z++) {
                            level.setBlockAndUpdate(new BlockPos(x, 161, z), Blocks.SMOOTH_STONE.defaultBlockState());
                        }
                    }
                    level.setBlockAndUpdate(POS, ModBlocks.HYPERCUBE.getDefaultState());
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "gamemode spectator @a");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 3.5 164 6.5 153.435 25");
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
                if (!(client.level.getBlockEntity(POS) instanceof HypercubeBlockEntity)) return;
                advance(9);
            }
            case 2 -> {
                client.getSingleplayerServer().execute(() -> client.getSingleplayerServer().overworld()
                    .setBlockAndUpdate(POS.north(), ModBlocks.HYPERCUBE.getDefaultState()));
                advance(3);
            }
            case 3 -> capture(client, "overlap", 4);
            case 4 -> {
                client.getSingleplayerServer().execute(() -> client.getSingleplayerServer().overworld()
                    .setBlockAndUpdate(POS.south(), Blocks.BLACK_CONCRETE.defaultBlockState()));
                advance(5);
            }
            case 5 -> capture(client, "occluded", 6);
            case 6 -> {
                client.setScreen(new Preview());
                advance(7);
            }
            case 7 -> capture(client, "item", 8);
            case 8 -> {
                AnvilCraft.LOGGER.info("PORT_HYPERCUBE_CLIENT_PASSED: standalone world render, overlap, occlusion and item model");
                client.stop();
                stage = 10;
            }
            case 9 -> capture(client, "single", 2);
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
        Screenshot.grab(client.gameDirectory, "hypercube-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(target);
            }));
    }

    private static final class Preview extends Screen {
        private Preview() {
            super(Component.literal("Hypercube"));
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, this.width, this.height, 0xFF303030);
            graphics.pose().pushMatrix();
            graphics.pose().translate(this.width / 2.0F - 32, this.height / 2.0F - 32);
            graphics.pose().scale(4, 4);
            graphics.item(ModBlocks.HYPERCUBE.asStack(), 0, 0);
            graphics.pose().popMatrix();
        }
    }
}
