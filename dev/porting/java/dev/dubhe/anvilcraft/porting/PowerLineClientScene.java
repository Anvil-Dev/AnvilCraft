package dev.dubhe.anvilcraft.porting;

import dev.anvilcraft.lib.v2.rendering.cachedber.pipeline.CachedBlockEntityRenderingPipeline;
import dev.anvilcraft.lib.v2.util.client.Line;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.PowerComponentInfo;
import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.api.power.SimplePowerGrid;
import dev.dubhe.anvilcraft.block.entity.BaseLaserBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CreativeLaserBlockEntity;
import dev.dubhe.anvilcraft.block.laser.CreativeLaserBlock;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.client.support.PowerGridSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Set;

public final class PowerLineClientScene {
    private static final boolean MIXED = Boolean.getBoolean("anvilcraft.portPowerLineMixed");
    private static final int HEIGHT = Integer.getInteger("anvilcraft.portPowerLineHeight", 0);
    private static final String[] CASES = {"plain", "bloom", "bloom-hidden", "plain-hidden", "disabled", "occluded", "reload"};
    private static int index;
    private static int stage;
    private static long deadline;
    private static long next;
    private static boolean capturing;
    private static volatile boolean reloaded;

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 180000;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Power line scene " + index + ":" + stage);
        if (capturing || client.getOverlay() != null || System.currentTimeMillis() < next) return;
        if (stage == 0) {
            client.getSingleplayerServer().execute(() -> {
                var server = client.getSingleplayerServer();
                var level = server.overworld();
                for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(-10, 158 + HEIGHT, -2), new BlockPos(10, 174 + HEIGHT, 4))) {
                    level.setBlockAndUpdate(pos, pos.getZ() == -2
                        ? Blocks.BLACK_CONCRETE.defaultBlockState() : Blocks.AIR.defaultBlockState());
                }
                if (MIXED) {
                    var emitter = new BlockPos(-6, 166 + HEIGHT, 0);
                    level.setBlockAndUpdate(emitter, ModBlocks.CREATIVE_LASER.getDefaultState()
                        .setValue(CreativeLaserBlock.FACING, Direction.EAST));
                    ((CreativeLaserBlockEntity) level.getBlockEntity(emitter)).setConfiguredLevel(4);
                    level.setBlockAndUpdate(new BlockPos(6, 166 + HEIGHT, 0), Blocks.BEDROCK.defaultBlockState());
                }
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set noon");
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "weather clear");
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "gamemode spectator @a");
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                    "tp @a 0.5 " + (166 + HEIGHT) + " 18.5 180 0");
                if (!MIXED) server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tick freeze");
            });
            client.options.fov().set(60);
            client.setScreen(null);
            stage = 1;
            next = System.currentTimeMillis() + 4000;
        } else if (stage == 1) {
            if (MIXED && (!(client.level.getBlockEntity(new BlockPos(-6, 166 + HEIGHT, 0)) instanceof BaseLaserBlockEntity laser)
                || laser.getIrradiateBlockPos() == null || laser.getLaserLevel() <= 0)) {
                return;
            }
            seed(client);
            configure(client);
        } else if (stage == 2) {
            if (index == 6 && (!reloaded || client.getWindow().getWidth() != 960)) return;
            capturing = true;
            String name = "power-lines-26.1-" + (MIXED ? "mixed-" : "") + CASES[index] + (HEIGHT == 0 ? "" : "-high") + ".png";
            Screenshot.grab(client.gameDirectory, name,
                client.getMainRenderTarget(), 1, message -> client.execute(() -> {
                    capturing = false;
                    AnvilCraft.LOGGER.info("PORT_POWER_LINES_CAPTURED: {}", CASES[index]);
                    if (++index == (MIXED ? 2 : CASES.length)) {
                        PowerGridSupport.clearAllGrid();
                        AnvilCraft.LOGGER.info(MIXED ? "PORT_POWER_LINES_MIXED_PASSED"
                            : "PORT_POWER_LINES_PASSED: 7 actual submit, toggle, occlusion and reload cases");
                        stage = 4;
                        client.stop();
                    } else {
                        stage = 3;
                    }
                }));
        } else if (stage == 3) {
            configure(client);
        }
    }

    private static void seed(Minecraft client) {
        PowerGridSupport.clearAllGrid();
        client.gui.getChat().clearMessages(true);
        BlockPos pos = new BlockPos(0, 166 + HEIGHT, 0);
        var grid = new SimplePowerGrid(73451, client.level.dimension().identifier().toString(), pos,
            List.of(new PowerComponentInfo(pos, 0, 0, 0, 0, 16, new AABB(pos).inflate(16), PowerComponentType.TRANSMITTER)), 0, 0, false);
        setLines(grid, Set.of(
            new Line(new Vec3(-6, 162 + HEIGHT, 0), new Vec3(6, 162 + HEIGHT, 0)),
            new Line(new Vec3(-6, 165 + HEIGHT, 0), new Vec3(6, 167 + HEIGHT, 0)),
            new Line(new Vec3(-4, 169 + HEIGHT, 0), new Vec3(4, 171 + HEIGHT, 0))));
        PowerGridSupport.getGridMap().put(grid.getId(), grid);
    }

    private static void setLines(SimplePowerGrid grid, Set<Line> lines) {
        try {
            var field = SimplePowerGrid.class.getDeclaredField("powerTransmitterLines");
            field.setAccessible(true);
            field.set(grid, lines);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void configure(Minecraft client) {
        AnvilCraftClient.CONFIG.renderBloomEffect = index != 0 && index != 3;
        AnvilCraftClient.CONFIG.renderPowerTransmitterLines = index != 4;
        if (MIXED && client.level.getBlockEntity(new BlockPos(-6, 166 + HEIGHT, 0)) instanceof BaseLaserBlockEntity laser) {
            CachedBlockEntityRenderingPipeline.getInstance().update(laser, true);
        }
        client.options.hideGui = index == 2 || index == 3;
        if (index == 5 || index == 6) {
            client.getSingleplayerServer().execute(() -> {
                var level = client.getSingleplayerServer().overworld();
                for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(-1, 158 + HEIGHT, 3), new BlockPos(1, 174 + HEIGHT, 3))) {
                    level.setBlockAndUpdate(pos, index == 5 ? Blocks.BLACK_CONCRETE.defaultBlockState() : Blocks.AIR.defaultBlockState());
                }
            });
        }
        if (index == 6) {
            reloaded = false;
            client.reloadResourcePacks().thenRun(() -> client.execute(() -> {
                GLFW.glfwSetWindowSize(client.getWindow().handle(), 960, 540);
                reloaded = true;
                next = System.currentTimeMillis() + 2000;
            }));
        }
        stage = 2;
        next = System.currentTimeMillis() + 2000;
    }
}
