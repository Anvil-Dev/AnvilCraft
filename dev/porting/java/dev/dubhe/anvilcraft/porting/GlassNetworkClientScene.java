package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.FluidTankBlockEntity;
import dev.dubhe.anvilcraft.block.entity.fluid.GlassPipeBlockEntity;
import dev.dubhe.anvilcraft.block.fluid.PipeBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class GlassNetworkClientScene {
    private static final BlockPos TOP = new BlockPos(0, 85, 6);
    private static final BlockPos BOTTOM = new BlockPos(0, 81, 6);
    private static final List<BlockPos> PIPES = List.of(TOP.below(), TOP.below(2), TOP.below(3));
    private static int stage;
    private static int readyFrames;
    private static long deadline;
    private static volatile boolean prepared;
    private static volatile boolean cleared;
    private static boolean capturing;
    private static volatile @Nullable Throwable failure;

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 150000;
        if (failure != null || System.currentTimeMillis() > deadline) {
            throw new IllegalStateException("Glass network scene " + stage, failure);
        }
        if (capturing) return;
        client.options.hideGui = true;
        if (stage == 0) {
            stage = 1;
            client.getSingleplayerServer().execute(() -> {
                try {
                    var server = client.getSingleplayerServer();
                    var level = server.overworld();
                    level.setBlockAndUpdate(TOP, ModBlocks.FLUID_TANK.getDefaultState());
                    level.setBlockAndUpdate(BOTTOM, ModBlocks.FLUID_TANK.getDefaultState());
                    for (var pos : PIPES) {
                        level.setBlockAndUpdate(pos,
                            ModBlocks.GLASS_PIPE_STRAIGHT.getDefaultState().setValue(PipeBlock.AXIS, Direction.Axis.Y));
                    }
                    var tank = (FluidTankBlockEntity) level.getBlockEntity(TOP);
                    try (Transaction transaction = Transaction.openRoot()) {
                        if (tank.getFluidHandler().insert(FluidResource.of(Fluids.WATER), 16000, transaction) != 16000) {
                            throw new IllegalStateException("Fixture tank rejected water");
                        }
                        transaction.commit();
                    }
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 0 84 12 180 0");
                    server.getPlayerList().getPlayers().getFirst().setNoGravity(true);
                    prepared = true;
                } catch (Throwable exception) {
                    failure = exception;
                }
            });
            return;
        }
        if (!prepared) return;
        if (stage == 1) {
            if (client.screen != null || client.getOverlay() != null) return;
            if (!(client.level.getBlockEntity(TOP) instanceof FluidTankBlockEntity)) return;
            for (var pos : PIPES) {
                if (!(client.level.getBlockEntity(pos) instanceof GlassPipeBlockEntity)) return;
            }
            client.getSingleplayerServer().execute(() -> {
                var server = client.getSingleplayerServer();
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tick unfreeze");
            });
            stage = 2;
        }
        if (stage == 2) {
            for (var pos : PIPES) {
                var pipe = (GlassPipeBlockEntity) client.level.getBlockEntity(pos);
                if (pipe == null || !pipe.getDisplayFluid().is(Fluids.WATER)) return;
                if (!pipe.getDisplayDirections().containsAll(List.of(Direction.UP, Direction.DOWN))) {
                    throw new IllegalStateException("Live transfer has incomplete display directions");
                }
            }
            if (++readyFrames < 30) return;
            capturing = true;
            Screenshot.grab(client.gameDirectory, "glass-pipe-26.1-network.png", client.getMainRenderTarget(), 1,
                message -> client.execute(() -> {
                    capturing = false;
                    stage = 3;
                }));
            return;
        }
        if (stage == 3) {
            stage = 4;
            client.getSingleplayerServer().execute(() -> {
                try {
                    var level = client.getSingleplayerServer().overworld();
                    var top = ((FluidTankBlockEntity) level.getBlockEntity(TOP)).getFluidHandler();
                    var bottom = ((FluidTankBlockEntity) level.getBlockEntity(BOTTOM)).getFluidHandler();
                    long moved = bottom.getAmountAsLong(0);
                    if (moved <= 0 || top.getAmountAsLong(0) + moved != 16000) throw new IllegalStateException("Water was not conserved");
                    AnvilCraft.LOGGER.info("PORT_GLASS_NETWORK_TRANSFER: moved={}, total=16000", moved);
                    try (Transaction transaction = Transaction.openRoot()) {
                        top.extract(FluidResource.of(Fluids.WATER), Integer.MAX_VALUE, transaction);
                        bottom.extract(FluidResource.of(Fluids.WATER), Integer.MAX_VALUE, transaction);
                        transaction.commit();
                    }
                    cleared = true;
                } catch (Throwable exception) {
                    failure = exception;
                }
            });
        }
        if (stage == 4 && cleared) {
            for (var pos : PIPES) {
                if (!((GlassPipeBlockEntity) client.level.getBlockEntity(pos)).getDisplayFluid().isEmpty()) return;
            }
            AnvilCraft.LOGGER.info("PORT_GLASS_NETWORK_CLIENT_PASSED: scanner, gravity transfer, live display, conservation and expiry");
            client.stop();
            stage = 5;
        }
    }
}
