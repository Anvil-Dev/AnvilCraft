package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.fluid.GlassPipeBlockEntity;
import dev.dubhe.anvilcraft.block.fluid.PipeBlock;
import dev.dubhe.anvilcraft.building.BuildingCommit;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;

public final class GlassVisualParityScene {
    private static final List<BlockPos> POSITIONS = List.of(new BlockPos(-2, 161, 6), new BlockPos(0, 161, 6), new BlockPos(2, 161, 6));
    private static final float[] ALPHAS = {0, 0.25F, 0.5F, 1, 1.0F / 255};
    private static int stage;
    private static boolean started;
    private static boolean requested;
    private static boolean capturing;
    private static volatile boolean prepared;
    private static volatile boolean updated;
    private static volatile @Nullable Throwable failure;
    private static long deadline;
    private static long captureAfter;

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 180000;
        if (failure != null || System.currentTimeMillis() > deadline) throw new IllegalStateException("Glass parity " + stage, failure);
        if (capturing) return;
        if (!started) {
            started = true;
            client.getSingleplayerServer().execute(() -> {
                try {
                    var server = client.getSingleplayerServer();
                    var level = server.overworld();
                    for (var pos : BlockPos.betweenClosed(-5, 159, 3, 5, 165, 4)) {
                        level.setBlockAndUpdate(pos, Blocks.GRAY_CONCRETE.defaultBlockState());
                    }
                    var straight = ModBlocks.GLASS_PIPE_STRAIGHT.getDefaultState();
                    var corner = ModBlocks.GLASS_PIPE_CORNER.getDefaultState()
                        .setValue(PipeBlock.CORNER_ENDED, PipeBlock.CornerEnded.fromDirections(Direction.NORTH, Direction.UP));
                    var node = ModBlocks.GLASS_PIPE_NODE.getDefaultState();
                    for (var direction : Direction.values()) {
                        node = node.setValue(PipeBlock.getPropertyForDirection(direction), PipeBlock.NodePipe.PIPE);
                    }
                    var states = List.of(straight, corner, node);
                    BuildingCommit.quietly(level, () -> {
                        for (int i = 0; i < states.size(); i++) {
                            BuildingCommit.set(level, POSITIONS.get(i), states.get(i));
                        }
                    });
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set 6000");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 0 162 12 180 8");
                    server.getPlayerList().getPlayers().getFirst().setNoGravity(true);
                    prepared = true;
                } catch (Throwable exception) {
                    failure = exception;
                }
            });
            return;
        }
        if (!prepared || client.screen != null || client.getOverlay() != null) return;
        for (var pos : POSITIONS) {
            if (!(client.level.getBlockEntity(pos) instanceof GlassPipeBlockEntity)) return;
        }
        client.options.hideGui = true;
        client.options.fov().set(70);
        client.options.bobView().set(false);
        client.player.setPos(0, 162, 12);
        client.player.setDeltaMovement(Vec3.ZERO);
        client.player.setYRot(180);
        client.player.setXRot(8);
        if (!requested) {
            requested = true;
            updated = false;
            client.getSingleplayerServer().execute(() -> {
                try {
                    var level = client.getSingleplayerServer().overworld();
                    for (int i = 0; i < POSITIONS.size(); i++) {
                        var pipe = (GlassPipeBlockEntity) level.getBlockEntity(POSITIONS.get(i));
                        Set<Direction> directions = i == 0 ? Set.of(Direction.WEST, Direction.EAST)
                            : i == 1 ? Set.of(Direction.NORTH, Direction.UP) : Set.of(Direction.values());
                        if (stage < ALPHAS.length) {
                            pipe.setGasDisplay(new FluidStack(Fluids.WATER, 1), directions, ALPHAS[stage]);
                        } else {
                            pipe.clearGasDisplay();
                            pipe.showFluid(new FluidStack(i == 1 ? Fluids.LAVA : Fluids.WATER, 1), directions);
                        }
                    }
                    updated = true;
                } catch (Throwable exception) {
                    failure = exception;
                }
            });
            captureAfter = System.currentTimeMillis() + 1500;
            return;
        }
        if (!updated || System.currentTimeMillis() < captureAfter) return;
        for (var pos : POSITIONS) {
            var pipe = (GlassPipeBlockEntity) client.level.getBlockEntity(pos);
            if (stage < ALPHAS.length && (!pipe.isShowingGas() || pipe.getGasAlpha() != ALPHAS[stage])) return;
        }
        capturing = true;
        Screenshot.grab(client.gameDirectory, "glass-parity-26.1-" + stage + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                AnvilCraft.LOGGER.info("PORT_GLASS_PARITY_CAPTURED: stage={}", stage);
                capturing = false;
                requested = false;
                if (++stage > ALPHAS.length) {
                    AnvilCraft.LOGGER.info("PORT_GLASS_PARITY_PASSED");
                    client.stop();
                }
            }));
    }
}
