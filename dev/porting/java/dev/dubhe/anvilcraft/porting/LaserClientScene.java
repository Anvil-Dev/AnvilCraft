package dev.dubhe.anvilcraft.porting;

import dev.anvilcraft.lib.v2.rendering.cachedber.pipeline.CachedBlockEntityRenderingPipeline;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.cfa.interfaces.CelestialForgingAnvilInterfaceBlock;
import dev.dubhe.anvilcraft.block.entity.BaseLaserBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilLaserInterfaceBlockEntity;
import dev.dubhe.anvilcraft.block.laser.LensBlock;
import dev.dubhe.anvilcraft.block.state.LensType;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public final class LaserClientScene {
    private static final int HEIGHT_OFFSET = Integer.getInteger("anvilcraft.portLaserHeight", 0);
    private static int stage;
    private static long deadline;
    private static long next;
    private static volatile boolean ready;
    private static boolean capturing;

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 120000;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Laser scene " + stage);
        if (capturing || client.getOverlay() != null || System.currentTimeMillis() < next) return;
        if (stage == 0) {
            client.getSingleplayerServer().execute(() -> {
                var server = client.getSingleplayerServer();
                var level = server.overworld();
                for (int x = -9; x <= 9; x++) {
                    for (int y = 159; y <= 173; y++) {
                        for (int z = -2; z <= 5; z++) {
                            level.setBlock(new BlockPos(x, y + HEIGHT_OFFSET, z), z == -2 ? Blocks.BLACK_CONCRETE.defaultBlockState()
                                : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                        }
                    }
                }
                int[] strengths = {1, 4, 16, 64, 16};
                for (int row = 0; row < 5; row++) {
                    var pos = new BlockPos(-6, 162 + HEIGHT_OFFSET + row * 2, 0);
                    var state = ModBlocks.CELESTIAL_FORGING_ANVIL_LASER_INTERFACE.getDefaultState()
                        .setValue(CelestialForgingAnvilInterfaceBlock.FACING, Direction.EAST)
                        .setValue(CelestialForgingAnvilInterfaceBlock.ACTIVE, true);
                    level.setBlock(pos, state, Block.UPDATE_ALL);
                    var source = (CelestialForgingAnvilLaserInterfaceBlockEntity) level.getBlockEntity(pos);
                    source.setWormholeLaserOutput(strengths[row], row == 4);
                    if (row < 4) {
                        var lens = ModBlocks.LENS.getDefaultState().setValue(LensBlock.AXIS, Direction.Axis.X)
                            .setValue(LensBlock.TYPE, LensType.values()[row]);
                        level.setBlock(pos.east(2), lens, Block.UPDATE_ALL);
                    }
                    level.setBlock(new BlockPos(5, pos.getY(), 0), Blocks.BEDROCK.defaultBlockState(), Block.UPDATE_ALL);
                }
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set noon");
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "weather clear");
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "gamemode spectator @a");
                server.getCommands().performPrefixedCommand(
                    server.createCommandSourceStack(), "tp @a 0.5 " + (165 + HEIGHT_OFFSET) + " 18.5 180 0");
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tick unfreeze");
            });
            AnvilCraftClient.CONFIG.renderBloomEffect = false;
            client.options.hideGui = true;
            client.options.fov().set(60);
            client.setScreen(null);
            stage = 1;
            next = System.currentTimeMillis() + 5000;
            return;
        }
        if (stage == 1) {
            for (int row = 0; row < 5; row++) {
                var pos = new BlockPos(row == 0 || row == 4 ? -6 : -4, 162 + HEIGHT_OFFSET + row * 2, 0);
                if (!(client.level.getBlockEntity(pos) instanceof BaseLaserBlockEntity laser)
                    || laser.getIrradiateBlockPos() == null || laser.getLaserLevel() <= 0) return;
                if (laser.isEmittingGamma() != (row == 4)) throw new IllegalStateException("Client gamma row " + row);
            }
            client.getSingleplayerServer().execute(() -> {
                var server = client.getSingleplayerServer();
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tick freeze");
                ready = true;
            });
            stage = 2;
            next = System.currentTimeMillis() + 4000;
            return;
        }
        if (stage == 2 && ready) {
            if (client.player.getY() < 164 + HEIGHT_OFFSET) throw new IllegalStateException("Laser camera fell out of scene");
            capturing = true;
            Screenshot.grab(client.gameDirectory, captureName(Boolean.getBoolean("anvilcraft.portLaserNoFog")
                ? "laser-components-no-fog-26.1.png"
                : "laser-components-26.1.png"), client.getMainRenderTarget(), 1,
                message -> client.execute(() -> {
                    capturing = false;
                    stage = 3;
                }));
            return;
        }
        if (stage == 3) {
            AnvilCraftClient.CONFIG.renderBloomEffect = true;
            for (int row = 0; row < 5; row++) {
                for (int x : new int[]{-6, -4}) {
                    if (client.level.getBlockEntity(new BlockPos(x, 162 + HEIGHT_OFFSET + row * 2, 0))
                        instanceof BaseLaserBlockEntity laser) {
                        CachedBlockEntityRenderingPipeline.getInstance().update(laser, true);
                    }
                }
            }
            stage = 4;
            next = System.currentTimeMillis() + 1500;
            return;
        }
        if (stage == 4) {
            capturing = true;
            Screenshot.grab(client.gameDirectory, captureName(Boolean.getBoolean("anvilcraft.portLaserNoFog")
                ? "laser-components-bloom-no-fog-26.1.png"
                : "laser-components-bloom-26.1.png"), client.getMainRenderTarget(), 1,
                message -> client.execute(() -> {
                    capturing = false;
                    stage = 5;
                }));
            return;
        }
        if (stage == 5 && Boolean.getBoolean("anvilcraft.portLaserLifecycle")) {
            stage = 6;
            ready = false;
            client.reloadResourcePacks().thenRun(() -> client.execute(() -> {
                client.getWindow().setWindowed(960, 540);
                next = System.currentTimeMillis() + 2500;
                ready = true;
            }));
            return;
        }
        if (stage == 6 && ready) {
            if (client.getMainRenderTarget().width != 960 || client.getMainRenderTarget().height != 540) return;
            capturing = true;
            Screenshot.grab(client.gameDirectory, "laser-components-bloom-reload-26.1.png", client.getMainRenderTarget(), 1,
                message -> client.execute(() -> {
                    capturing = false;
                    stage = 7;
                }));
            return;
        }
        if (stage == 5 || stage == 7) {
            if (stage == 7) AnvilCraft.LOGGER.info("PORT_LASER_BLOOM_LIFECYCLE_PASSED: resource reload and 960x540 resize");
            AnvilCraft.LOGGER.info("PORT_LASER_CLIENT_PASSED: five live networked beams, lens variants, bloom off and on");
            client.stop();
            stage = 8;
        }
    }

    private static String captureName(String name) {
        return HEIGHT_OFFSET == 0 ? name : name.replace(".png", "-high.png");
    }

}
