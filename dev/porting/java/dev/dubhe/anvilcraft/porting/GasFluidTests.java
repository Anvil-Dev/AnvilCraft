package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.fluid.network.FluidEndpoint;
import dev.dubhe.anvilcraft.api.fluid.network.FluidPipeNetwork;
import dev.dubhe.anvilcraft.block.entity.FluidTankBlockEntity;
import dev.dubhe.anvilcraft.block.entity.fluid.GlassPipeBlockEntity;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class GasFluidTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_gas_definitions", GasFluidTests::definitions,
        "port_gas_buckets", GasFluidTests::buckets,
        "port_gas_network", GasFluidTests::network,
        "port_gas_storage", GasFluidTests::storage
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_gas_fluids"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true))));
    }

    private static List<Fluid> fluids() {
        return List.of(ModFluids.HYDROGEN.get(), ModFluids.OXYGEN.get(), ModFluids.HELIUM.get(), ModFluids.DEUTERIUM.get(),
            ModFluids.XENON.get(), ModFluids.KRYPTON.get(), ModFluids.PRIMORDIAL_MATTER.get());
    }

    private static List<BucketItem> bucketItems() {
        return List.of(ModItems.HYDROGEN_BUCKET.get(), ModItems.OXYGEN_BUCKET.get(), ModItems.HELIUM_BUCKET.get(),
            ModItems.DEUTERIUM_BUCKET.get(), ModItems.XENON_BUCKET.get(), ModItems.KRYPTON_BUCKET.get(),
            ModItems.PRIMORDIAL_MATTER_BUCKET.get());
    }

    private static void definitions(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        for (Fluid fluid : fluids()) {
            var state = fluid.defaultFluidState();
            helper.assertTrue(fluid.getBucket() == bucketItems().get(fluids().indexOf(fluid))
                && fluid.getFluidType().isLighterThanAir() && state.isSource() && state.getAmount() == 8
                && state.getOwnHeight() == 0 && state.createLegacyBlock().isAir()
                && state.getShape(helper.getLevel(), BlockPos.ZERO).isEmpty(), "Non-placeable source fluid semantics are preserved");
            if (fluid == ModFluids.PRIMORDIAL_MATTER.get()) continue;
            var type = fluid.getFluidType();
            helper.assertTrue(type.isLighterThanAir() && type.getDensity() == -1000 && type.getViscosity() == 100
                && type.getFallDistanceModifier(player) == 0, "All six gases retain the source physical profile");
        }
        var primordialType = ModFluids.PRIMORDIAL_MATTER.get().getFluidType();
        helper.assertTrue(primordialType.getSound(SoundActions.BUCKET_FILL) == SoundEvents.BUCKET_FILL
            && primordialType.getSound(SoundActions.BUCKET_EMPTY) == SoundEvents.BUCKET_EMPTY, "Primordial bucket sounds match source");
        helper.succeed();
    }

    private static void buckets(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        for (int index = 0; index < fluids().size(); index++) {
            var fluid = fluids().get(index);
            var bucket = bucketItems().get(index);
            var stack = bucket.getDefaultInstance();
            helper.assertTrue(stack.getMaxStackSize() == 1 && stack.is(Tags.Items.BUCKETS)
                && stack.getCraftingRemainder().is(Items.BUCKET), "Bucket stack, tag and crafting remainder match source");
            var items = new ItemStacksResourceHandler(1);
            items.set(0, ItemResource.of(stack), 1);
            var handler = ItemAccess.forHandlerIndexStrict(items, 0).getCapability(Capabilities.Fluid.ITEM);
            helper.assertTrue(handler != null && handler.getResource(0).is(fluid) && handler.getAmountAsInt(0) == 1000,
                "Each filled bucket exposes the corresponding fluid capability");
            try (Transaction tx = Transaction.openRoot()) {
                helper.assertTrue(handler.extract(FluidResource.of(fluid), 500, tx) == 0, "Partial bucket draining is rejected");
                helper.assertTrue(handler.extract(FluidResource.of(fluid), 1000, tx) == 1000, "Full bucket can be simulated");
            }
            helper.assertTrue(items.getResource(0).is(bucket), "Aborted draining retains the original bucket");
            try (Transaction tx = Transaction.openRoot()) {
                helper.assertTrue(handler.extract(FluidResource.of(fluid), 1000, tx) == 1000, "Full bucket drains atomically");
                tx.commit();
            }
            helper.assertTrue(items.getResource(0).is(Items.BUCKET) && items.getAmountAsInt(0) == 1,
                "Drain returns exactly one empty bucket");
            var emptyBucket = ItemAccess.forHandlerIndexStrict(items, 0).getCapability(Capabilities.Fluid.ITEM);
            try (Transaction tx = Transaction.openRoot()) {
                helper.assertTrue(emptyBucket.insert(FluidResource.of(fluid), 1000, tx) == 1000,
                    "Every gas can fill an empty bucket through its registered supplier");
                tx.commit();
            }
            helper.assertTrue(items.getResource(0).is(bucket), "Refilling returns the corresponding registered bucket");
            helper.assertTrue(!bucket.emptyContents(player, helper.getLevel(), pos, null)
                && helper.getLevel().getBlockState(pos).isAir(), "These fluids cannot place or erase world blocks");
        }
        helper.succeed();
    }

    private static void network(GameTestHelper helper) {
        var a = helper.absolutePos(new BlockPos(4, 8, 4));
        var b = a.east();
        var c = b.east();
        var positions = Set.of(a, b, c);
        BuildingCommit.quietly(helper.getLevel(), () -> positions.forEach(pos ->
            BuildingCommit.set(helper.getLevel(), pos, ModBlocks.GLASS_PIPE_STRAIGHT.getDefaultState())));
        for (Fluid fluid : fluids()) {
            var first = new FluidStacksResourceHandler(1, 1000);
            var last = new FluidStacksResourceHandler(1, 1000);
            first.set(0, FluidResource.of(fluid), 1000);
            var network = new FluidPipeNetwork(helper.getLevel(), positions,
                Map.of(a, List.of(b), b, List.of(a, c), c, List.of(b)), Map.of(), Map.of(), Map.of(), positions,
                List.of(new FluidEndpoint(a.west(), a, Direction.EAST, first, a.getY(), false),
                    new FluidEndpoint(c.east(), c, Direction.WEST, last, c.getY(), false)));
            network.tick();
            network.tick();
            helper.assertTrue(first.getAmountAsInt(0) == 500 && last.getAmountAsInt(0) == 500,
                "Actual registered gases equalize without liquid head and conserve volume");
            for (BlockPos pos : positions) {
                var pipe = (GlassPipeBlockEntity) helper.getLevel().getBlockEntity(pos);
                helper.assertTrue(pipe.isShowingGas() && pipe.getDisplayFluid().is(fluid) && pipe.getGasAlpha() == 0.5F,
                    "Actual gas identities propagate to the complete glass-pipe display");
            }
        }
        helper.succeed();
    }

    private static void storage(GameTestHelper helper) {
        var pos = new BlockPos(2, 2, 2);
        for (Fluid fluid : fluids()) {
            helper.setBlock(pos, Blocks.AIR);
            helper.setBlock(pos, ModBlocks.FLUID_TANK.get());
            var tank = helper.getBlockEntity(pos, FluidTankBlockEntity.class);
            try (Transaction tx = Transaction.openRoot()) {
                helper.assertTrue(tank.getFluidHandler().insert(FluidResource.of(fluid), 1234, tx) == 1234,
                    "Tank accepts contained fluids");
                tx.commit();
            }
            var data = tank.saveWithFullMetadata(helper.getLevel().registryAccess());
            var restored = ModBlockEntities.FLUID_TANK.get().create(tank.getBlockPos(), tank.getBlockState());
            restored.setLevel(helper.getLevel());
            restored.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), data));
            helper.assertTrue(restored.getFluidHandler().getResource(0).is(fluid) && restored.getFluidHandler().getAmountAsInt(0) == 1234,
                "Tank persistence preserves exact fluid identity and amount");
        }
        helper.succeed();
    }
}
