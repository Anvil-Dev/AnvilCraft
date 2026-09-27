package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.fluid.network.FluidEndpoint;
import dev.dubhe.anvilcraft.api.fluid.network.FluidPipeNetwork;
import dev.dubhe.anvilcraft.api.fluid.network.InfiniteGasPressureSource;
import dev.dubhe.anvilcraft.api.fluidtank.CreativeFluidHandler;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.cfa.interfaces.CelestialForgingAnvilInterfaceBlock;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilFluidInterfaceBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CreativeFluidTankBlockEntity;
import dev.dubhe.anvilcraft.block.entity.fluid.GlassPipeBlockEntity;
import dev.dubhe.anvilcraft.building.BuildingCommit;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
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
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class FluidNetworkParityTests {
    private static final FluidType GAS_TYPE = new FluidType(FluidType.Properties.create().density(-100)) {};
    private static final BaseFlowingFluid.Properties GAS_PROPERTIES = new BaseFlowingFluid.Properties(
        () -> GAS_TYPE, FluidNetworkParityTests::gasFluid, FluidNetworkParityTests::gasFluid);
    private static @org.jspecify.annotations.Nullable BaseFlowingFluid GAS;
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_network_glass_path", FluidNetworkParityTests::path,
        "port_network_gas_balance", FluidNetworkParityTests::gas,
        "port_network_multi_entry", FluidNetworkParityTests::entries,
        "port_network_atomic", FluidNetworkParityTests::atomic,
        "port_network_creative_sink", FluidNetworkParityTests::creative,
        "port_network_infinite_pressure", FluidNetworkParityTests::pressure,
        "port_network_gas_display", FluidNetworkParityTests::gasDisplay,
        "port_network_creative_config", FluidNetworkParityTests::creativeConfig,
        "port_network_cfa_pressure_gate", FluidNetworkParityTests::pressureGate
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(NeoForgeRegistries.Keys.FLUID_TYPES, registry -> registry.register(AnvilCraft.of("port_gas"), GAS_TYPE));
        event.register(Registries.FLUID, registry -> {
            GAS = new BaseFlowingFluid.Source(GAS_PROPERTIES);
            registry.register(AnvilCraft.of("port_gas"), GAS);
        });
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_fluid_network_parity"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static BaseFlowingFluid gasFluid() {
        return java.util.Objects.requireNonNull(GAS);
    }

    private static FluidStacksResourceHandler tank(FluidResource resource, int amount) {
        var tank = new FluidStacksResourceHandler(1, 1000);
        tank.set(0, resource, amount);
        return tank;
    }

    private static List<BlockPos> positions(GameTestHelper helper) {
        var a = helper.absolutePos(new BlockPos(4, 8, 4));
        return List.of(a, a.east(), a.east(2));
    }

    private static FluidPipeNetwork network(
        GameTestHelper helper, FluidStacksResourceHandler first, FluidStacksResourceHandler last, boolean glass, int head
    ) {
        var p = positions(helper);
        return new FluidPipeNetwork(helper.getLevel(), Set.copyOf(p),
            Map.of(p.get(0), List.of(p.get(1)), p.get(1), List.of(p.get(0), p.get(2)), p.get(2), List.of(p.get(1))),
            Map.of(), Map.of(), Map.of(), glass ? Set.copyOf(p) : Set.of(),
            List.of(new FluidEndpoint(p.get(0).west(), p.get(0), Direction.EAST, first, p.get(0).getY() + head, false),
                new FluidEndpoint(p.get(2).east(), p.get(2), Direction.WEST, last, p.get(2).getY(), false)));
    }

    private static void path(GameTestHelper helper) {
        var first = tank(FluidResource.of(Fluids.WATER), 1000);
        var last = tank(FluidResource.EMPTY, 0);
        var positions = positions(helper);
        BuildingCommit.quietly(helper.getLevel(), () -> positions.forEach(pos ->
            BuildingCommit.set(helper.getLevel(), pos, ModBlocks.GLASS_PIPE_STRAIGHT.getDefaultState())));
        var network = network(helper, first, last, true, 10);
        network.tick();
        helper.assertTrue(first.getAmountAsInt(0) == 500 && last.getAmountAsInt(0) == 500, "Liquid transfer budget changed");
        for (var pos : positions) {
            var pipe = (GlassPipeBlockEntity) helper.getLevel().getBlockEntity(pos);
            helper.assertTrue(pipe.getDisplayFluid().is(Fluids.WATER)
                && pipe.getDisplayDirections().equals(Set.of(Direction.WEST, Direction.EAST)), "Transferred path was not displayed");
        }
        first.set(0, FluidResource.EMPTY, 0);
        last.set(0, FluidResource.EMPTY, 0);
        helper.runAfterDelay(3, () -> {
            network.tick();
            for (var pos : positions) {
                helper.assertTrue(((GlassPipeBlockEntity) helper.getLevel().getBlockEntity(pos)).getDisplayFluid().isEmpty(),
                    "Expired path remained visible");
            }
            helper.succeed();
        });
    }

    private static void gasDisplay(GameTestHelper helper) {
        var first = tank(FluidResource.of(gasFluid()), 1000);
        var last = tank(FluidResource.EMPTY, 0);
        var positions = positions(helper);
        BuildingCommit.quietly(helper.getLevel(), () -> positions.forEach(pos ->
            BuildingCommit.set(helper.getLevel(), pos, ModBlocks.GLASS_PIPE_STRAIGHT.getDefaultState())));
        var network = network(helper, first, last, true, 0);
        network.tick();
        network.tick();
        for (var pos : positions) {
            var pipe = (GlassPipeBlockEntity) helper.getLevel().getBlockEntity(pos);
            helper.assertTrue(pipe.isShowingGas() && pipe.getGasAlpha() == 0.5F, "Gas display did not reflect equilibrium fill");
        }
        first.set(0, FluidResource.EMPTY, 0);
        last.set(0, FluidResource.EMPTY, 0);
        helper.runAfterDelay(3, () -> {
            network.tick();
            for (var pos : positions) {
                var pipe = (GlassPipeBlockEntity) helper.getLevel().getBlockEntity(pos);
                helper.assertTrue(!pipe.isShowingGas() && pipe.getDisplayFluid().isEmpty(), "Removed gas remained visible");
            }
            helper.succeed();
        });
    }

    private static void creativeConfig(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(4, 8, 4));
        helper.getLevel().setBlockAndUpdate(pos, ModBlocks.CREATIVE_FLUID_TANK.getDefaultState());
        var tank = (CreativeFluidTankBlockEntity) helper.getLevel().getBlockEntity(pos);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        helper.assertTrue(tank.onPlayerUse(player, InteractionHand.MAIN_HAND)
            && player.getMainHandItem().is(Items.BUCKET) && tank.getFluidHandler().getResource(0).is(Fluids.WATER),
            "Filled bucket did not configure the creative tank");
        helper.assertTrue(tank.onPlayerUse(player, InteractionHand.MAIN_HAND) && player.getMainHandItem().is(Items.WATER_BUCKET),
            "Configured creative tank did not supply an empty bucket");
        var creative = helper.makeMockPlayer(GameType.CREATIVE);
        creative.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        helper.assertTrue(tank.onPlayerUse(creative, InteractionHand.MAIN_HAND) && tank.getFluidHandler().getResource(0).isEmpty(),
            "Creative empty-bucket interaction did not clear the configured source");
        helper.succeed();
    }

    private static void pressureGate(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(4, 8, 4));
        var level = helper.getLevel();
        level.setBlockAndUpdate(pos, ModBlocks.CELESTIAL_FORGING_ANVIL_FLUID_INTERFACE.getDefaultState());
        var port = (CelestialForgingAnvilFluidInterfaceBlockEntity) level.getBlockEntity(pos);
        helper.assertTrue(port.getFluidHandler() instanceof InfiniteGasPressureSource
            && port.getInternalFluidHandler() instanceof InfiniteGasPressureSource, "Interface handlers lack pressure capability");
        helper.assertTrue(!port.isSupplyingInfiniteGasPressure(), "Passive interface supplied infinite pressure");
        level.setBlockAndUpdate(pos, level.getBlockState(pos).setValue(CelestialForgingAnvilInterfaceBlock.ACTIVE, true));
        helper.assertTrue(!port.isSupplyingInfiniteGasPressure(), "Unpowered interface supplied infinite pressure");
        port.setGrid(new PowerGrid(level) {
            @Override
            public boolean isWorking() {
                return true;
            }
        });
        helper.assertTrue(((InfiniteGasPressureSource) port.getFluidHandler()).isSupplyingInfiniteGasPressure()
            && ((InfiniteGasPressureSource) port.getInternalFluidHandler()).isSupplyingInfiniteGasPressure(),
            "Powered active interface did not expose pressure on both handlers");
        helper.succeed();
    }

    private static void gas(GameTestHelper helper) {
        var first = tank(FluidResource.of(gasFluid()), 1000);
        var last = tank(FluidResource.EMPTY, 0);
        helper.assertTrue(GAS_TYPE.isLighterThanAir(), "Fixture fluid is not a gas");
        var network = network(helper, first, last, false, 0);
        network.tick();
        helper.assertTrue(first.getAmountAsInt(0) == 500 && last.getAmountAsInt(0) == 500, "Gas did not equalize at equal height");
        network.tick();
        helper.assertTrue(first.getAmountAsInt(0) == 500 && last.getAmountAsInt(0) == 500, "Balanced gas kept moving");
        helper.succeed();
    }

    private static void entries(GameTestHelper helper) {
        var p = positions(helper);
        var alternate = p.get(1).south();
        var first = tank(FluidResource.of(Fluids.WATER), 1000);
        var last = tank(FluidResource.EMPTY, 0);
        var destination = new FluidEndpoint(p.get(2).south(),
            List.of(new FluidEndpoint.Entry(p.get(2), Direction.NORTH, 0), new FluidEndpoint.Entry(alternate, Direction.WEST, 0)),
            last, false, null);
        var network = new FluidPipeNetwork(helper.getLevel(), Set.of(p.get(0), p.get(1), p.get(2), alternate),
            Map.of(p.get(0), List.of(p.get(1)), p.get(1), List.of(p.get(0), p.get(2), alternate),
                p.get(2), List.of(p.get(1)), alternate, List.of(p.get(1))),
            Map.of(), Map.of(), Map.of(p.get(2), Map.of(Direction.SOUTH, Direction.NORTH)), Set.of(),
            List.of(new FluidEndpoint(p.get(0).west(), p.get(0), Direction.EAST, first, 10, false), destination));
        network.tick();
        helper.assertTrue(last.getAmountAsInt(0) == 500, "Blocked primary entry hid a valid secondary connection");
        helper.succeed();
    }

    private static final class RefusingTank extends FluidStacksResourceHandler {
        private int bulkCalls;

        private RefusingTank() {
            super(1, 1000);
        }

        @Override
        public int insert(FluidResource resource, int amount, TransactionContext transaction) {
            if (amount > 1 && ++this.bulkCalls % 2 == 0) return 0;
            return super.insert(resource, amount, transaction);
        }
    }

    private static void atomic(GameTestHelper helper) {
        var first = tank(FluidResource.of(Fluids.WATER), 1000);
        var last = new RefusingTank();
        network(helper, first, last, false, 10).tick();
        helper.assertTrue(first.getAmountAsInt(0) == 1000 && last.getAmountAsInt(0) == 0, "Refused transfer lost fluid");
        helper.succeed();
    }

    private static void creative(GameTestHelper helper) {
        var tank = new CreativeFluidHandler();
        try (Transaction transaction = Transaction.openRoot()) {
            helper.assertTrue(tank.insert(FluidResource.of(gasFluid()), 1000, transaction) == 1000, "Empty creative sink rejected gas");
        }
        helper.assertTrue(tank.getResource(0).isEmpty(), "Simulation configured an infinite source");
        try (Transaction transaction = Transaction.openRoot()) {
            tank.insert(FluidResource.of(gasFluid()), 1000, transaction);
            transaction.commit();
        }
        helper.assertTrue(tank.getResource(0).isEmpty(), "Creative sink became a source after insertion");
        tank.replaceStacks(List.of(new FluidStack(gasFluid(), Integer.MAX_VALUE)));
        var destination = tank(FluidResource.EMPTY, 0);
        network(helper, tank, destination, false, 0).tick();
        helper.assertTrue(destination.getAmountAsInt(0) == 1000 && tank.getAmountAsInt(0) == Integer.MAX_VALUE,
            "Infinite gas source did not fill a finite tank");
        helper.succeed();
    }

    private static final class PressureTank extends FluidStacksResourceHandler implements InfiniteGasPressureSource {
        private PressureTank() {
            super(1, 1000);
            this.set(0, FluidResource.of(gasFluid()), 100);
        }

        @Override
        public boolean isSupplyingInfiniteGasPressure() {
            return true;
        }
    }

    private static void pressure(GameTestHelper helper) {
        var first = new PressureTank();
        var last = tank(FluidResource.of(gasFluid()), 900);
        network(helper, first, last, false, 0).tick();
        helper.assertTrue(first.getAmountAsInt(0) == 0 && last.getAmountAsInt(0) == 1000,
            "Infinite pressure failed to expend finite stock against a fuller tank");
        helper.succeed();
    }
}
