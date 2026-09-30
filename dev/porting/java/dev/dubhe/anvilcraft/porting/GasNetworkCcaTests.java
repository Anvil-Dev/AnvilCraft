package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.fluid.GasDisplayFillProvider;
import dev.dubhe.anvilcraft.api.fluid.network.FluidEndpoint;
import dev.dubhe.anvilcraft.api.fluid.network.FluidNetworkManager;
import dev.dubhe.anvilcraft.api.fluid.network.FluidNetworkScanner;
import dev.dubhe.anvilcraft.api.fluid.network.FluidPipeNetwork;
import dev.dubhe.anvilcraft.api.fluid.network.ValveState;
import dev.dubhe.anvilcraft.api.fluidtank.CreativeFluidHandler;
import dev.dubhe.anvilcraft.block.entity.FluidTankBlockEntity;
import dev.dubhe.anvilcraft.block.entity.LargeFluidTankBlockEntity;
import dev.dubhe.anvilcraft.block.entity.fluid.ControlValveBlockEntity;
import dev.dubhe.anvilcraft.block.entity.fluid.GlassPipeBlockEntity;
import dev.dubhe.anvilcraft.block.fluid.PipeBlock;
import dev.dubhe.anvilcraft.building.BuildingCommit;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class GasNetworkCcaTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_gas_cca_single_and_valves", GasNetworkCcaTests::directions,
        "port_gas_cca_all_branches", GasNetworkCcaTests::branches,
        "port_gas_cca_shared_handler", GasNetworkCcaTests::shared,
        "port_gas_cca_fill_and_order", GasNetworkCcaTests::fill,
        "port_gas_cca_valve_interaction", GasNetworkCcaTests::interaction
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, r -> TESTS.forEach((name, test) -> r.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_gas_cca"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 150, 0, true))));
    }

    private static List<BlockPos> pipes(GameTestHelper h) {
        var first = h.absolutePos(new BlockPos(4, 7, 4));
        var positions = List.of(first, first.east(), first.east(2));
        BuildingCommit.quietly(h.getLevel(), () -> positions.forEach(pos -> BuildingCommit.set(h.getLevel(), pos,
            ModBlocks.GLASS_PIPE_STRAIGHT.getDefaultState().setValue(PipeBlock.AXIS, Direction.Axis.X))));
        return positions;
    }

    private static FluidPipeNetwork network(
        GameTestHelper h, List<BlockPos> positions, ResourceHandler<FluidResource> source,
        Map<BlockPos, Map<Direction, Direction>> faces, Map<BlockPos, Direction> diodes
    ) {
        return network(h, positions, source, faces, diodes, Map.of());
    }

    private static FluidPipeNetwork network(
        GameTestHelper h, List<BlockPos> positions, ResourceHandler<FluidResource> source,
        Map<BlockPos, Map<Direction, Direction>> faces, Map<BlockPos, Direction> diodes, Map<BlockPos, ValveState> valves
    ) {
        var first = positions.getFirst();
        var middle = positions.get(1);
        var last = positions.getLast();
        return new FluidPipeNetwork(h.getLevel(), Set.copyOf(positions),
            Map.of(first, List.of(middle), middle, List.of(first, last), last, List.of(middle)), valves, diodes, faces,
            Set.copyOf(positions), List.of(new FluidEndpoint(first.west(), first, Direction.EAST, source, first.getY(), false)));
    }

    private static GlassPipeBlockEntity pipe(GameTestHelper h, BlockPos pos) {
        return (GlassPipeBlockEntity) h.getLevel().getBlockEntity(pos);
    }

    private static void directions(GameTestHelper h) {
        var positions = pipes(h);
        var tank = new FluidStacksResourceHandler(1, 1000);
        tank.set(0, FluidResource.of(ModFluids.HYDROGEN.get()), 250);
        var valve = new HashMap<Direction, Direction>();
        valve.put(Direction.EAST, Direction.WEST);
        var faces = new HashMap<BlockPos, Map<Direction, Direction>>();
        faces.put(positions.get(1), valve);
        var network = network(h, positions, tank, faces, Map.of());
        network.tick();
        h.assertTrue(pipe(h, positions.getFirst()).isShowingGas() && pipe(h, positions.get(1)).isShowingGas()
            && !pipe(h, positions.getLast()).isShowingGas(), "Single static source displays only up to the closed check valve");
        h.assertTrue(pipe(h, positions.getFirst()).getGasAlpha() == 0.25F && tank.getAmountAsInt(0) == 250,
            "Display does not require transfer or consume gas");
        valve.put(Direction.EAST, Direction.EAST);
        network.tick();
        h.assertTrue(pipe(h, positions.getLast()).isShowingGas(), "Next tick invalidates the old reachability cache");
        faces.put(positions.getFirst(), Map.of(Direction.WEST, Direction.WEST));
        network.tick();
        h.assertTrue(positions.stream().noneMatch(pos -> pipe(h, pos).isShowingGas()), "Source-facing valve blocks entry entirely");
        network(h, positions, tank, Map.of(), Map.of(positions.get(1), Direction.EAST)).tick();
        h.assertTrue(!pipe(h, positions.getLast()).isShowingGas(), "Pump does not display gas against its inflow direction");
        network(h, positions, tank, Map.of(), Map.of(positions.get(1), Direction.WEST)).tick();
        h.assertTrue(pipe(h, positions.getLast()).isShowingGas(), "Pump displays gas in its permitted direction");
        var control = new ControlValveBlockEntity(ModBlockEntities.CONTROL_VALVE.get(), positions.get(1),
            ModBlocks.CONTROL_VALVE.getDefaultState());
        control.setFilter(0, new FluidStack(ModFluids.OXYGEN.get(), 1));
        var filtered = network(h, positions, tank, Map.of(), Map.of(), Map.of(positions.get(1), new ValveState(control)));
        filtered.tick();
        h.assertTrue(pipe(h, positions.getFirst()).isShowingGas() && !pipe(h, positions.get(1)).isShowingGas()
            && !pipe(h, positions.getLast()).isShowingGas(), "Control valve excludes a gas that fails its filter");
        control.setFilter(0, new FluidStack(ModFluids.HYDROGEN.get(), 1));
        filtered.tick();
        h.assertTrue(pipe(h, positions.getLast()).isShowingGas(), "Updated filter applies to the next display tick");
        h.succeed();
    }

    private static void branches(GameTestHelper h) {
        var center = h.absolutePos(new BlockPos(8, 6, 8));
        BuildingCommit.set(h.getLevel(), center, ModBlocks.FLUID_TANK.getDefaultState());
        var tank = (FluidTankBlockEntity) h.getLevel().getBlockEntity(center);
        insert(tank.getFluidHandler(), FluidResource.of(ModFluids.HYDROGEN.get()), 1000);
        for (Direction direction : Direction.values()) {
            BuildingCommit.set(h.getLevel(), center.relative(direction), ModBlocks.GLASS_PIPE_STRAIGHT.getDefaultState()
                .setValue(PipeBlock.AXIS, direction.getAxis()));
        }
        FluidNetworkManager.INSTANCE.addContainer(h.getLevel(), center);
        FluidNetworkManager.INSTANCE.markDirty(h.getLevel());
        FluidNetworkManager.INSTANCE.tick();
        FluidNetworkManager.INSTANCE.tick();
        for (Direction direction : Direction.values()) {
            h.assertTrue(pipe(h, center.relative(direction)).isShowingGas(), "Every independent container branch is scanned: " + direction);
        }
        h.succeed();
    }

    private static void shared(GameTestHelper h) {
        var center = h.absolutePos(new BlockPos(8, 4, 8));
        var block = ModBlocks.LARGE_FLUID_TANK.get();
        BlockState state = block.defaultBlockState();
        BuildingCommit.quietly(h.getLevel(), () -> {
            for (var part : block.getParts()) {
                BuildingCommit.set(h.getLevel(), center.offset(block.offsetFrom(state, part)), block.placedState(part, state));
            }
            var first = ModBlocks.GLASS_PIPE_NODE.getDefaultState().setValue(PipeBlock.getPropertyForDirection(Direction.EAST),
                PipeBlock.NodePipe.END).setValue(PipeBlock.getPropertyForDirection(Direction.UP), PipeBlock.NodePipe.PIPE);
            var second = ModBlocks.GLASS_PIPE_NODE.getDefaultState().setValue(PipeBlock.getPropertyForDirection(Direction.EAST),
                PipeBlock.NodePipe.END).setValue(PipeBlock.getPropertyForDirection(Direction.DOWN), PipeBlock.NodePipe.PIPE);
            BuildingCommit.set(h.getLevel(), center.west(2), first);
            BuildingCommit.set(h.getLevel(), center.west(2).above(), second);
        });
        var network = FluidNetworkScanner.scan(h.getLevel(), center.west(2));
        h.assertTrue(network != null && network.getEndpoints().size() == 1 && network.getEndpoints().getFirst().entries().size() == 2,
            "Two multiblock part capabilities merge into one endpoint with both entrances");
        h.succeed();
    }

    private static void insert(ResourceHandler<FluidResource> tank, FluidResource fluid, int amount) {
        try (Transaction tx = Transaction.openRoot()) {
            if (tank.insert(fluid, amount, tx) != amount) throw new IllegalStateException("Fixture fluid insertion failed");
            tx.commit();
        }
    }

    private static void fill(GameTestHelper h) {
        var positions = pipes(h);
        var hydrogen = new FluidStack(ModFluids.HYDROGEN.get(), 1);
        final var xenon = new FluidStack(ModFluids.XENON.get(), 1);
        var creative = new CreativeFluidHandler();
        creative.replaceStacks(List.of(hydrogen));
        network(h, positions, creative, Map.of(), Map.of()).tick();
        h.assertTrue(pipe(h, positions.getFirst()).getGasAlpha() == 1, "Creative source retains full display opacity");
        var large = new LargeFluidTankBlockEntity(ModBlockEntities.LARGE_FLUID_TANK.get(),
            BlockPos.ZERO, ModBlocks.LARGE_FLUID_TANK.getDefaultState());
        var tank = large.getFluidHandler();
        h.assertTrue(tank instanceof GasDisplayFillProvider, "Actual multipart handler exposes the display contract");
        var provider = (GasDisplayFillProvider) tank;
        insert(tank, FluidResource.of(hydrogen), LargeFluidTankBlockEntity.BASE_CAPACITY / 4);
        h.assertTrue(provider.gasDisplayFill(hydrogen) == 0.25F && provider.gasDisplayFill(xenon) == 0,
            "Normal tank denominator is its physical capacity");
        large.onFormed();
        insert(tank, FluidResource.of(hydrogen), LargeFluidTankBlockEntity.INFINITY_THRESHOLD);
        h.assertTrue(provider.gasDisplayFill(hydrogen) == 1, "Do not count the extra acceptance slot as display capacity");
        insert(tank, FluidResource.of(xenon), LargeFluidTankBlockEntity.INFINITY_THRESHOLD);
        h.assertTrue(provider.gasDisplayFill(hydrogen) == 0.5F && provider.gasDisplayFill(xenon) == 0.5F,
            "Enhanced denominator is total stored amount when it exceeds the threshold");
        var network = network(h, positions, tank, Map.of(), Map.of());
        for (int tick = 0; tick < 20; tick++) {
            network.tick();
            h.assertTrue(pipe(h, positions.getFirst()).getDisplayFluid().is(ModFluids.XENON.get())
                && pipe(h, positions.getFirst()).getGasAlpha() == 0.5F, "Stable gas selection and provider opacity across ticks");
        }
        h.succeed();
    }

    private static void interaction(GameTestHelper h) {
        var pos = pipes(h).getFirst();
        var level = h.getLevel();
        var state = level.getBlockState(pos);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos).add(0.48, 0, 0), Direction.EAST, pos, false);
        h.assertTrue(((PipeBlock) state.getBlock()).addCheckValve(level, pos, state, Direction.EAST, Direction.EAST),
            "Install fixture valve");
        player.setItemInHand(InteractionHand.MAIN_HAND, ModItems.ANVIL_HAMMER.asStack());
        level.getBlockState(pos).useWithoutItem(level, player, hit);
        h.assertTrue(pipe(h, pos).hasValveOn(Direction.EAST), "Hammer fallback must not act as an empty hand");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STICK));
        level.getBlockState(pos).useWithoutItem(level, player, hit);
        h.assertTrue(!pipe(h, pos).hasValveOn(Direction.EAST), "Empty main hand removes a valve despite occupied offhand");
        var valves = ModItems.CHECK_VALVE.asStack(2);
        player.setItemInHand(InteractionHand.MAIN_HAND, valves);
        level.getBlockState(pos).useItemOn(valves, level, player, InteractionHand.MAIN_HAND, hit);
        h.assertTrue(pipe(h, pos).hasValveOn(Direction.EAST) && valves.getCount() == 1, "Valve item installs and consumes once");
        level.getBlockState(pos).useItemOn(valves, level, player, InteractionHand.MAIN_HAND, hit);
        h.assertTrue(!pipe(h, pos).hasValveOn(Direction.EAST), "Valve item also removes an existing valve");
        h.succeed();
    }
}
