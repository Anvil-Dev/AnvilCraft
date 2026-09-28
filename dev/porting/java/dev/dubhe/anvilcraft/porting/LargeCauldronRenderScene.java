package dev.dubhe.anvilcraft.porting;

import com.google.gson.GsonBuilder;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LargeCauldronRenderScene {
    private static final BlockPos POS = new BlockPos(0, 162, 0);
    private static final String[] CASES = {
        "dry-items", "water-items", "layers", "milk", "oil-fire", "half-fire", "empty-fire", "hidden-fire"
    };
    private static final List<net.minecraft.world.item.Item> INPUTS = List.of(
        Items.STONE, Items.GRANITE, Items.DIORITE, Items.ANDESITE, Items.DIRT, Items.COBBLESTONE, Items.OAK_PLANKS, Items.SAND);
    private static final Map<String, Object> RESULTS = new LinkedHashMap<>();
    private static int index;
    private static int stage;
    private static long next;
    private static long deadline;
    private static boolean capturing;

    public static void frame(Minecraft client) {
        if (deadline == 0) {
            deadline = System.currentTimeMillis() + 180000;
            client.options.hideGui = true;
        }
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Large cauldron stage " + index + ":" + stage);
        if (capturing || client.getOverlay() != null || System.currentTimeMillis() < next) return;
        if (stage == 0) {
            stage = 1;
            next = System.currentTimeMillis() + 4000;
            client.setScreen(null);
            client.getSingleplayerServer().execute(() -> {
                var server = client.getSingleplayerServer();
                var level = server.overworld();
                var bottom = POS.below();
                for (var pos : BlockPos.betweenClosed(bottom.offset(-1, 0, -1), bottom.offset(1, 2, 1))) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                }
                var state = ModBlocks.LARGE_CAULDRON.getDefaultState();
                level.setBlockAndUpdate(bottom, state);
                ModBlocks.LARGE_CAULDRON.get().setPlacedBy(level, bottom, state, null, ModBlocks.LARGE_CAULDRON.asStack());
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 0.5 167 7.5 180 38");
                server.getPlayerList().getPlayers().forEach(player -> player.setNoGravity(true));
            });
        } else if (stage == 1) {
            if (!(client.level.getBlockEntity(POS) instanceof LargeCauldronBlockEntity tank)) return;
            configure(tank);
            LargeCauldronRenderProbe.hideFire = index == 7;
            RESULTS.put(CASES[index], LargeCauldronRenderProbe.capture(client, tank));
            AnvilCraft.LOGGER.info("PORT_LARGE_CAULDRON_RENDER_CASE: {}", CASES[index]);
            stage = 2;
            next = System.currentTimeMillis() + 250;
        } else if (stage == 2) {
            LargeCauldronRenderProbe.clock(300);
            capturing = true;
            Screenshot.grab(client.gameDirectory, "large-cauldron-26.1-" + CASES[index] + ".png", client.getMainRenderTarget(), 1,
                message -> client.execute(() -> {
                    capturing = false;
                    if (++index == CASES.length) save(client);
                    else stage = 0;
                }));
        }
    }

    private static void configure(LargeCauldronBlockEntity tank) {
        List<FluidStack> fluids = new ArrayList<>();
        if (index == 1 || index == 3 || index == 4 || index == 7) {
            var fluid = index == 1 ? Fluids.WATER : index == 3 ? NeoForgeMod.MILK.get() : ModFluids.OIL.get();
            for (int i = 0; i < 8; i++) fluids.add(new FluidStack(fluid, 64000));
        } else if (index == 2) {
            for (var fluid : List.of(Fluids.WATER, NeoForgeMod.MILK.get(), Fluids.LAVA, ModFluids.HYDROGEN.get())) {
                fluids.add(new FluidStack(fluid, 64000));
            }
        } else if (index == 5) {
            for (int i = 0; i < 3; i++) fluids.add(new FluidStack(Fluids.WATER, 64000));
            fluids.add(new FluidStack(ModFluids.OIL.get(), 64000));
        }
        tank.getFluids().setFluids(fluids);
        for (int slot = 0; slot < tank.getInputHandler().size(); slot++) {
            tank.getInputHandler().setStackInSlot(slot, net.minecraft.world.item.ItemStack.EMPTY);
        }
        for (int slot = 0; slot < tank.getInputHandler().size(); slot++) {
            tank.getInputHandler().setStackInSlot(slot, index < 2 ? new net.minecraft.world.item.ItemStack(INPUTS.get(slot), slot + 1)
                : net.minecraft.world.item.ItemStack.EMPTY);
        }
        for (int slot = 0; slot < tank.getOutputHandler().size(); slot++) {
            tank.getOutputHandler().set(slot, index < 2 ? ItemResource.of(Items.GRANITE) : ItemResource.EMPTY, index < 2 ? slot + 1 : 0);
        }
        LargeCauldronRenderProbe.ignite(tank, index >= 4);
    }

    private static void save(Minecraft client) {
        stage = 3;
        try {
            Files.writeString(client.gameDirectory.toPath().resolve("large-cauldron-26.1.json"),
                new GsonBuilder().setPrettyPrinting().create().toJson(RESULTS));
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
        AnvilCraft.LOGGER.info("PORT_LARGE_CAULDRON_RENDER_PASSED: {} cases", RESULTS.size());
        client.stop();
    }
}
