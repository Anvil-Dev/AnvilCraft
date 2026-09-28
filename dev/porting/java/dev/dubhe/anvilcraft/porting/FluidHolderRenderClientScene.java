package dev.dubhe.anvilcraft.porting;

import com.google.gson.GsonBuilder;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidStack;

import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

public final class FluidHolderRenderClientScene {
    private static final BlockPos POS = new BlockPos(0, 162, 0);
    private static final String[] CASES = {"port-one", "port-milk", "port-gas", "drain-one", "drain-gas",
        "column-water", "column-lava", "column-milk", "collector-one", "collector-half", "fish-one", "fish-milk"};
    private static final Map<String, Object> RESULTS = new LinkedHashMap<>();
    private static int index;
    private static int stage;
    private static long next;
    private static long deadline;
    private static boolean capturing;
    private static Block expected;

    public static void frame(Minecraft client) {
        if (deadline == 0) {
            deadline = System.currentTimeMillis() + 180000;
            client.options.hideGui = true;
        }
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Fluid holder scene " + index + ":" + stage);
        if (capturing || client.getOverlay() != null || System.currentTimeMillis() < next) return;
        if (stage == 0) {
            stage = 1;
            next = System.currentTimeMillis() + 4000;
            client.setScreen(null);
            expected = index < 3 ? ModBlocks.STORAGE_FLUID_PORT.get() : index < 8 ? ModBlocks.DRAIN.get()
                : index < 10 ? ModBlocks.EXP_COLLECTOR.get() : ModBlocks.FISH_TANK.get();
            client.getSingleplayerServer().execute(() -> {
                var server = client.getSingleplayerServer();
                var level = server.overworld();
                level.setBlockAndUpdate(POS, Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(POS, expected.defaultBlockState());
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 0.5 162 5.5 180 24");
                server.getPlayerList().getPlayers().forEach(player -> player.setNoGravity(true));
            });
        } else if (stage == 1) {
            if (!client.level.getBlockState(POS).is(expected) || client.level.getBlockEntity(POS) == null) return;
            var fluid = fluid();
            int column = index >= 5 && index <= 7 ? POS.getY() - 3 : Integer.MIN_VALUE;
            FluidHolderRenderProbe.configure(client.level.getBlockEntity(POS), fluid, column);
            RESULTS.put(CASES[index], FluidHolderRenderProbe.capture(client, POS, fluid, column));
            stage = 2;
            next = System.currentTimeMillis() + 250;
        } else if (stage == 2) {
            capturing = true;
            Screenshot.grab(client.gameDirectory, "fluid-holders-26.1-" + CASES[index] + ".png", client.getMainRenderTarget(), 1,
                message -> client.execute(() -> {
                    capturing = false;
                    if (++index == CASES.length) save(client);
                    else stage = 0;
                }));
        }
    }

    private static FluidStack fluid() {
        var fluid = index == 1 || index == 7 || index == 11 ? NeoForgeMod.MILK.get()
            : index == 2 || index == 4 ? ModFluids.HYDROGEN.get() : index == 6 ? Fluids.LAVA
            : index == 8 || index == 9 ? ModFluids.EXP_FLUID.get() : Fluids.WATER;
        int amount = index == 0 || index == 3 || index == 8 || index == 10 ? 1
            : index < 3 ? 32000 : index == 9 ? 2000 : 500;
        return new FluidStack(fluid, amount);
    }

    private static void save(Minecraft client) {
        stage = 3;
        try {
            Files.writeString(client.gameDirectory.toPath().resolve("fluid-holders-26.1.json"),
                new GsonBuilder().setPrettyPrinting().create().toJson(RESULTS));
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
        AnvilCraft.LOGGER.info("PORT_FLUID_HOLDERS_PASSED: {} cases", RESULTS.size());
        client.stop();
    }
}
