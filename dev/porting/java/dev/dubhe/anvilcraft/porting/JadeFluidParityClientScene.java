package dev.dubhe.anvilcraft.porting;

import com.google.gson.GsonBuilder;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.container.LargeFluidTankBlock;
import dev.dubhe.anvilcraft.block.entity.FluidTankBlockEntity;
import dev.dubhe.anvilcraft.block.entity.LargeFluidTankBlockEntity;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.util.TankUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.ui.IDisplayHelper;
import snownee.jade.api.view.ProgressView;
import snownee.jade.impl.Tooltip;
import snownee.jade.impl.WailaClientRegistration;
import snownee.jade.impl.ui.ProgressElement;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JadeFluidParityClientScene {
    private static final BlockPos POS = new BlockPos(0, 162, 0);
    private static final String[] CASES = {"single-empty", "single-250", "single-full", "single-infinite",
        "large-empty", "large-finite", "large-enhanced-finite", "large-mixed", "large-infinite"};
    private static final Map<String, List<Bar>> RESULTS = new LinkedHashMap<>();
    private static final Map<Long, String> FORMATS = new LinkedHashMap<>();
    private static List<Bar> bars = List.of();
    private static int index;
    private static int stage;
    private static long next;
    private static long deadline;
    private static long stable;
    private static boolean installed;
    private static boolean capturing;
    private static boolean observed;
    private static volatile int prepared = -1;

    private record Bar(String text, float progress) {
    }

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 240000;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Jade fluid stage " + index + ":" + stage);
        if (capturing || client.getOverlay() != null || System.currentTimeMillis() < next) return;
        if (!installed) {
            installed = true;
            WailaClientRegistration.instance().addTooltipCollectedCallback((box, accessor) -> {
                if (accessor instanceof BlockAccessor block && block.getPosition().distManhattan(POS) <= 2
                    && (block.getBlockState().is(ModBlocks.FLUID_TANK) || block.getBlockState().is(ModBlocks.LARGE_FLUID_TANK))) {
                    bars = readBars(box.getTooltip());
                    observed = true;
                }
            });
            WailaClientRegistration.instance().tooltipCollectedCallback.sort();
            for (long amount : new long[]{0, 1, 250, 999, 1000, 1575, 16000, 512000, 1000000, 12800000, 2147483647L}) {
                FORMATS.put(amount, IDisplayHelper.get().humanReadableNumber(amount, "B", true));
            }
            client.options.guiScale().set(2);
            client.resizeGui();
        }
        if (stage == 0) {
            stage = 1;
            next = System.currentTimeMillis() + 4000;
            observed = false;
            stable = 0;
            bars = List.of();
            client.setScreen(null);
            client.getSingleplayerServer().execute(() -> prepare(client));
        } else if (stage == 1) {
            if (prepared != index || !observed || !client.levelRenderer.isSectionCompiledAndVisible(POS)) return;
            if (stable == 0) stable = System.currentTimeMillis() + 2000;
            if (System.currentTimeMillis() < stable) return;
            RESULTS.put(CASES[index], List.copyOf(bars));
            AnvilCraft.LOGGER.info("PORT_JADE_FLUID_CASE: {} {}", CASES[index], bars);
            capturing = true;
            Screenshot.grab(client.gameDirectory, "jade-fluid-26.1-" + CASES[index] + ".png", client.getMainRenderTarget(), 1,
                message -> client.execute(() -> {
                    capturing = false;
                    if (++index == CASES.length) {
                        stage = 2;
                        save(client);
                    } else {
                        stage = 0;
                    }
                }));
        }
    }

    private static List<Bar> readBars(Tooltip tooltip) {
        List<Bar> result = new ArrayList<>();
        for (var line : tooltip.lines) {
            for (var element : line.elements()) {
                if (!(element instanceof ProgressElement progress)) continue;
                try {
                    var field = ProgressElement.class.getDeclaredField("view");
                    field.setAccessible(true);
                    ProgressView view = (ProgressView) field.get(progress);
                    result.add(new Bar(view.text.getString(), view.parts.stream().map(ProgressView.Part::progress).reduce(0F, Float::sum)));
                } catch (ReflectiveOperationException exception) {
                    throw new IllegalStateException(exception);
                }
            }
        }
        return result;
    }

    private static void prepare(Minecraft client) {
        var server = client.getSingleplayerServer();
        var level = server.overworld();
        for (BlockPos pos : BlockPos.betweenClosed(POS.offset(-5, -5, -5), POS.offset(5, 5, 5))) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        boolean large = index >= 4;
        final boolean enhanced = index == 3 || index >= 6;
        var block = large ? ModBlocks.LARGE_FLUID_TANK.get() : ModBlocks.FLUID_TANK.get();
        var state = block.defaultBlockState();
        if (large) state = state.setValue(LargeFluidTankBlock.HALF, Cube3x3PartHalf.MID_CENTER);
        level.setBlock(POS, state, Block.UPDATE_CLIENTS);
        block.setPlacedBy(level, POS, state, null, ItemStack.EMPTY);
        if (enhanced) buildMenger(level, POS, large ? 9 : 3);
        var entity = level.getBlockEntity(POS);
        ResourceHandler<FluidResource> handler;
        if (entity instanceof FluidTankBlockEntity tank) {
            if (enhanced) tank.onFormed();
            handler = tank.getFluidHandler();
        } else {
            var tank = (LargeFluidTankBlockEntity) entity;
            if (enhanced) tank.onFormed();
            handler = tank.getFluidHandler();
        }
        int water = switch (index) {
            case 1 -> 250;
            case 2, 5 -> 16000;
            case 3, 7, 8 -> 12800000;
            case 6 -> 1575;
            default -> 0;
        };
        int lava = index == 5 ? 1000 : index == 6 || index == 7 ? 64000 : index == 8 ? 12800000 : 0;
        try (Transaction tx = Transaction.openRoot()) {
            if (water > 0) handler.insert(FluidResource.of(Fluids.WATER), water, tx);
            if (lava > 0) handler.insert(FluidResource.of(Fluids.LAVA), lava, tx);
            tx.commit();
        }
        if (enhanced && !TankUtil.isMengerStructure(level, POS, large ? 9 : 3)) throw new IllegalStateException("Menger fixture invalid");
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 0.5 162 5.5 180 13");
        server.getPlayerList().getPlayers().forEach(player -> {
            player.setNoGravity(true);
            player.getInventory().clearContent();
            player.containerMenu.broadcastChanges();
        });
        prepared = index;
    }

    private static void buildMenger(ServerLevel level, BlockPos center, int size) {
        if (size == 1) {
            level.setBlock(center, ModBlocks.MENGER_SPONGE.getDefaultState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            return;
        }
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (TankUtil.isMengerPos(x, y, z)) {
                        buildMenger(level, center.offset(x * size / 3, y * size / 3, z * size / 3), size / 3);
                    }
                }
            }
        }
    }

    private static void save(Minecraft client) {
        try {
            Files.writeString(client.gameDirectory.toPath().resolve("jade-fluid-parity-26.1.json"),
                new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("cases", RESULTS, "formats", FORMATS)));
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
        AnvilCraft.LOGGER.info("PORT_JADE_FLUID_PARITY_PASSED: {} cases", RESULTS.size());
        client.stop();
    }
}
