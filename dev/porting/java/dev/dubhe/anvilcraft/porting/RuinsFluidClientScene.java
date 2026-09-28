package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.RuinsBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.item.block.RuinsBlockItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.List;

public final class RuinsFluidClientScene {
    static final BlockPos PAIR = new BlockPos(-3, 162, -12);
    private static int stage;
    private static long next;
    private static long deadline;
    private static boolean capturing;

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 120000;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Ruins fluid stage " + stage);
        if (stage > 0) {
            client.player.setYRot(180);
            client.player.setXRot(30);
            client.player.yRotO = 180;
            client.player.xRotO = 30;
        }
        if (capturing || client.getOverlay() != null || System.currentTimeMillis() < next) return;
        switch (stage) {
            case 0 -> {
                client.getSingleplayerServer().execute(() -> {
                    var server = client.getSingleplayerServer();
                    var level = server.overworld();
                    for (int x = -5; x <= 5; x++) {
                        for (int z = -7; z <= 6; z++) {
                            level.setBlockAndUpdate(new BlockPos(x, 161, z), Blocks.SMOOTH_STONE.defaultBlockState());
                        }
                    }
                    List<BlockState> states = List.of(Blocks.WATER.defaultBlockState(),
                        Blocks.OAK_SLAB.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true),
                        Blocks.LAVA.defaultBlockState(), ModFluids.OIL.get().defaultFluidState().createLegacyBlock());
                    for (int row = 0; row < states.size(); row++) {
                        basin(level, new BlockPos(-3, 162, row * 3 - 5), states.get(row), false);
                        basin(level, new BlockPos(3, 162, row * 3 - 5), states.get(row), true);
                    }
                    for (int x : new int[]{-17, -16, 15, 16}) {
                        basin(level, new BlockPos(x, 162, -10 - (x & 1) * 3), Blocks.WATER.defaultBlockState(), true);
                    }
                    for (int x = -1; x <= 2; x++) {
                        for (int z = -1; z <= 1; z++) {
                            var pos = PAIR.offset(x, 0, z);
                            level.setBlockAndUpdate(pos.below(), Blocks.SMOOTH_STONE.defaultBlockState());
                            level.setBlockAndUpdate(pos, x == -1 || x == 2 || z != 0
                                ? Blocks.GLASS.defaultBlockState() : Blocks.WATER.defaultBlockState());
                        }
                    }
                    RuinsBlockItem.convert(level, PAIR);
                    RuinsBlockItem.convert(level, PAIR.east());
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 0.5 167 11.5 180 30");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set noon");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "weather clear");
                    server.getPlayerList().getPlayers().getFirst().setNoGravity(true);
                });
                client.options.guiScale().set(2);
                client.options.fov().set(60);
                client.options.hideGui = true;
                client.resizeGui();
                client.setScreen(null);
                advance(1, 5000);
            }
            case 1 -> {
                if (!(client.level.getBlockEntity(new BlockPos(3, 162, 4)) instanceof RuinsBlockEntity)) return;
                RuinsFluidProbe.verify(client);
                capture(client, "day", 2);
            }
            case 2 -> {
                client.getSingleplayerServer().execute(() -> {
                    var server = client.getSingleplayerServer();
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set midnight");
                });
                advance(3, 2500);
            }
            case 3 -> capture(client, "night", 4);
            case 4 -> {
                AnvilCraft.LOGGER.info("PORT_RUINS_FLUID_CLIENT_PASSED: water, waterlogged slab, lava and oil day/night");
                stage = 5;
                client.stop();
            }
            default -> {
            }
        }
    }

    private static void basin(ServerLevel level, BlockPos pos, BlockState state, boolean ruins) {
        level.setBlockAndUpdate(pos.below(), Blocks.SMOOTH_STONE.defaultBlockState());
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            level.setBlockAndUpdate(pos.relative(direction), Blocks.GLASS.defaultBlockState());
        }
        level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        if (ruins) RuinsBlockItem.convert(level, pos);
    }

    private static void advance(int target, int delay) {
        stage = target;
        next = System.currentTimeMillis() + delay;
    }

    private static void capture(Minecraft client, String name, int target) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "ruins-fluid-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(target, 300);
            }));
    }
}
