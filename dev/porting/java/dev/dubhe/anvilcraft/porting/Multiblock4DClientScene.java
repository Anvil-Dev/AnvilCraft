package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.block.entity.SpacetimeSupercomputerBlockEntity;
import dev.dubhe.anvilcraft.entity.FallingGiantAnvilEntity;
import dev.dubhe.anvilcraft.event.giantanvil.GiantAnvilLandingEventListener;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.entity.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

public final class Multiblock4DClientScene {
    private static final BlockPos CENTER = new BlockPos(0, 161, 0);
    private static final String[] CAMERAS = {
        "tp @a 0.5 162 6.5 180 25", "tp @a -5.5 162 0.5 -90 25",
        "tp @a 0.5 162 -5.5 0 25", "tp @a 6.5 162 0.5 90 25"
    };
    private static int stage;
    private static int face;
    private static long deadline;
    private static long next;
    private static boolean capturing;
    private static volatile boolean resultVerified;

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 120000;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("4D client stage " + stage);
        if (capturing || client.getOverlay() != null || System.currentTimeMillis() < next) return;
        switch (stage) {
            case 0 -> {
                client.getSingleplayerServer().execute(() -> {
                    var server = client.getSingleplayerServer();
                    var level = server.overworld();
                    for (int x = -1; x <= 1; x++) {
                        for (int z = -1; z <= 1; z++) {
                            level.setBlockAndUpdate(CENTER.offset(x, 0, z), Blocks.CRAFTING_TABLE.defaultBlockState());
                        }
                    }
                    level.setBlockAndUpdate(CENTER, ModBlocks.SPACETIME_SUPERCOMPUTER.getDefaultState());
                    fill(level, Blocks.DIRT);
                    land(level);
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "gamemode spectator @a");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), CAMERAS[0]);
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set noon");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "weather clear");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tick unfreeze");
                });
                client.options.hideGui = true;
                client.options.fov().set(60);
                client.setScreen(null);
                advance(1);
            }
            case 1 -> {
                if (!(client.level.getBlockEntity(CENTER) instanceof SpacetimeSupercomputerBlockEntity computer)
                    || computer.getProcessingProgress() != 1 || computer.getProcessingTotal() != 3) return;
                advance(2);
            }
            case 2 -> capture(client, "step1-face" + face, 3);
            case 3 -> {
                face++;
                if (face < CAMERAS.length) {
                    client.getSingleplayerServer().execute(() -> {
                        var server = client.getSingleplayerServer();
                        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), CAMERAS[face]);
                    });
                    advance(2);
                } else {
                    client.getSingleplayerServer().execute(() -> {
                        var server = client.getSingleplayerServer();
                        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), CAMERAS[0]);
                        fill(server.overworld(), Blocks.COBBLESTONE);
                        land(server.overworld());
                    });
                    advance(4);
                }
            }
            case 4 -> {
                var computer = (SpacetimeSupercomputerBlockEntity) client.level.getBlockEntity(CENTER);
                if (computer.getProcessingProgress() != 2) return;
                advance(5);
            }
            case 5 -> capture(client, "step2", 6);
            case 6 -> {
                client.getSingleplayerServer().execute(() -> {
                    var level = client.getSingleplayerServer().overworld();
                    fill(level, Blocks.OAK_PLANKS);
                    land(level);
                    int count = level.getEntitiesOfClass(ItemEntity.class, new AABB(CENTER).inflate(4)).stream()
                        .filter(e -> e.getItem().is(Items.DIAMOND)).mapToInt(e -> e.getItem().getCount()).sum();
                    if (count != 1) throw new IllegalStateException("4D result count " + count);
                    resultVerified = true;
                });
                advance(7);
            }
            case 7 -> {
                var computer = (SpacetimeSupercomputerBlockEntity) client.level.getBlockEntity(CENTER);
                if (!resultVerified || computer.getProcessingRecipe() != null || computer.getProcessingTotal() != 0) return;
                advance(8);
            }
            case 8 -> capture(client, "complete", 9);
            case 9 -> {
                AnvilCraft.LOGGER.info("PORT_4D_CLIENT_PASSED: four-sided progress, step transition, single result and completion clear");
                client.stop();
                stage = 10;
            }
            default -> {
            }
        }
    }

    private static void fill(ServerLevel level, Block block) {
        for (int x = -1; x <= 1; x++) {
            for (int y = -3; y <= -1; y++) {
                for (int z = -1; z <= 1; z++) {
                    level.setBlockAndUpdate(CENTER.offset(x, y, z), block.defaultBlockState());
                }
            }
        }
    }

    private static void land(ServerLevel level) {
        var entity = new FallingGiantAnvilEntity(ModEntities.FALLING_GIANT_ANVIL.get(), level);
        GiantAnvilLandingEventListener.handleMultiblock(new AnvilEvent.GiantOnLand(level, CENTER.above(2), entity, 1));
    }

    private static void advance(int value) {
        stage = value;
        next = System.currentTimeMillis() + 1000;
    }

    private static void capture(Minecraft client, String name, int value) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "multiblock-4d-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(value);
            }));
    }
}
