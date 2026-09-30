package dev.dubhe.anvilcraft.porting;

import com.google.gson.GsonBuilder;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.cfa.interfaces.CelestialForgingAnvilInterfaceBlock;
import dev.dubhe.anvilcraft.block.container.LargeFluidTankBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidStack;

import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GasContainerRenderScene {
    private static final BlockPos POS = new BlockPos(0, 162, 0);
    private static final String[] CASES = {"fish-one", "fish-quarter", "fish-full", "fish-water",
        "auto-quarter", "auto-full", "auto-water", "large-single", "large-two", "large-mixed", "large-milk",
        "large-enhanced-single", "large-enhanced-mixed", "cauldron-single", "cauldron-two", "cauldron-mixed", "cauldron-milk",
        "interface-north", "interface-east", "interface-south", "interface-west", "interface-single"};
    private static final Map<String, Object> RESULTS = new LinkedHashMap<>();
    private static int index;
    private static int stage;
    private static volatile int prepared = -1;
    private static long next;
    private static long deadline;
    private static boolean capturing;
    private static boolean reloadStarted;
    private static boolean reloaded;

    public static void frame(Minecraft client) {
        if (stage == 4) return;
        if (deadline == 0) {
            deadline = System.currentTimeMillis() + 240000;
            client.options.hideGui = true;
            client.options.fov().set(50);
        }
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Gas container case " + index + ":" + stage);
        if (capturing || client.getOverlay() != null || System.currentTimeMillis() < next) return;
        if (stage == 0) {
            client.setScreen(null);
            client.getSingleplayerServer().execute(() -> prepare(client));
            stage = 1;
            next = System.currentTimeMillis() + 1500;
        } else if (stage == 1) {
            var entity = client.level.getBlockEntity(POS);
            if (prepared != index || entity == null || !entity.getBlockState().is(block())) return;
            var fluids = fluids();
            GasContainerRenderProbe.configure(entity, fluids, enhanced());
            RESULTS.put(CASES[index], GasContainerRenderProbe.capture(client, entity, fluids, enhanced()));
            if (index >= 7 && index < 13) RESULTS.put(CASES[index] + "-item", LargeTankRenderProbe.capture(client, POS).get("item"));
            stage = 2;
            next = System.currentTimeMillis() + 350;
        } else if (stage == 2) {
            capturing = true;
            Screenshot.grab(client.gameDirectory, "gas-container-26.1-" + CASES[index] + ".png", client.getMainRenderTarget(), 1,
                message -> client.execute(() -> {
                    capturing = false;
                    AnvilCraft.LOGGER.info("PORT_GAS_CONTAINER_CAPTURED: {}", CASES[index]);
                    if (++index < CASES.length) stage = 0;
                    else stage = 3;
                }));
        } else if (stage == 3) {
            if (!reloadStarted) {
                reloadStarted = true;
                client.reloadResourcePacks().whenComplete((ignored, error) -> client.execute(() -> {
                    if (error != null) throw new IllegalStateException(error);
                    reloaded = true;
                }));
                return;
            }
            if (!reloaded) return;
            index = CASES.length - 1;
            var entity = client.level.getBlockEntity(POS);
            GasContainerRenderProbe.configure(entity, fluids(), false);
            RESULTS.put("reloaded-interface", GasContainerRenderProbe.capture(client, entity, fluids(), false));
            try {
                Files.writeString(client.gameDirectory.toPath().resolve("gas-container-26.1.json"),
                    new GsonBuilder().setPrettyPrinting().create().toJson(RESULTS));
            } catch (java.io.IOException exception) {
                throw new IllegalStateException(exception);
            }
            stage = 4;
            AnvilCraft.LOGGER.info("PORT_GAS_CONTAINERS_PASSED: {} cases, reused empty states and resource reload", CASES.length);
            client.stop();
        }
    }

    private static Block block() {
        return index < 4 ? ModBlocks.FISH_TANK.get() : index < 7 ? ModBlocks.AUTO_ENCHANTING_TABLE.get()
            : index < 13 ? ModBlocks.LARGE_FLUID_TANK.get() : index < 17 ? ModBlocks.LARGE_CAULDRON.get()
            : ModBlocks.CELESTIAL_FORGING_ANVIL_FLUID_INTERFACE.get();
    }

    private static boolean enhanced() {
        return index == 11 || index == 12;
    }

    private static List<FluidStack> fluids() {
        Fluid hydrogen = ModFluids.HYDROGEN.get();
        Fluid xenon = ModFluids.XENON.get();
        return switch (index) {
            case 0 -> List.of(new FluidStack(hydrogen, 1));
            case 1 -> List.of(new FluidStack(hydrogen, 250));
            case 2 -> List.of(new FluidStack(xenon, 1000));
            case 3 -> List.of(new FluidStack(Fluids.WATER, 250));
            case 4 -> List.of(new FluidStack(hydrogen, 8000));
            case 5 -> List.of(new FluidStack(xenon, 32000));
            case 6 -> List.of(new FluidStack(Fluids.WATER, 8000));
            case 7, 13 -> List.of(new FluidStack(hydrogen, 64000));
            case 8, 14 -> List.of(new FluidStack(hydrogen, 64000), new FluidStack(xenon, 32000));
            case 9, 15 -> List.of(new FluidStack(hydrogen, 64000), new FluidStack(Fluids.WATER, 64000), new FluidStack(xenon, 32000));
            case 10, 16 -> List.of(new FluidStack(NeoForgeMod.MILK.get(), 64000), new FluidStack(hydrogen, 64000));
            case 11 -> List.of(new FluidStack(hydrogen, 12800000));
            case 12 -> List.of(new FluidStack(Fluids.WATER, 12800000), new FluidStack(hydrogen, 12800000), new FluidStack(xenon, 6400000));
            case 21 -> List.of(new FluidStack(hydrogen, 20000));
            default -> List.of(new FluidStack(hydrogen, 10000), new FluidStack(Fluids.WATER, 20000), new FluidStack(xenon, 5000));
        };
    }

    private static void prepare(Minecraft client) {
        var server = client.getSingleplayerServer();
        var level = server.overworld();
        for (BlockPos pos : BlockPos.betweenClosed(POS.offset(-2, -2, -2), POS.offset(2, 3, 2))) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        var block = block();
        var state = block.defaultBlockState();
        var origin = index >= 13 && index < 17 ? POS.below() : POS;
        if (index >= 7 && index < 13) state = state.setValue(LargeFluidTankBlock.HALF, Cube3x3PartHalf.MID_CENTER);
        if (index >= 17) {
            var directions = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
            state = state.setValue(CelestialForgingAnvilInterfaceBlock.FACING, directions.get((index - 17) % 4));
        }
        level.setBlock(origin, state, Block.UPDATE_CLIENTS);
        block.setPlacedBy(level, origin, state, null, ItemStack.EMPTY);
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 0.5 165 7.5 180 32");
        server.getPlayerList().getPlayers().forEach(player -> player.setNoGravity(true));
        prepared = index;
    }
}
