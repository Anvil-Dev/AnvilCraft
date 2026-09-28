package dev.dubhe.anvilcraft.porting;

import com.mojang.authlib.GameProfile;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.fluid.network.FluidEndpoint;
import dev.dubhe.anvilcraft.api.fluid.network.FluidPipeNetwork;
import dev.dubhe.anvilcraft.block.entity.FluidTankBlockEntity;
import dev.dubhe.anvilcraft.block.entity.LargeFluidTankBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class TankDisposalTests {
    private static final BlockPos POS = new BlockPos(6, 3, 6);
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_tank_disposal_capacity", TankDisposalTests::capacity,
        "port_tank_disposal_neighbors", TankDisposalTests::neighbors,
        "port_tank_disposal_enhanced", TankDisposalTests::enhanced,
        "port_tank_disposal_reload", TankDisposalTests::reload,
        "port_tank_disposal_network", TankDisposalTests::network,
        "port_tank_disposal_bottles", TankDisposalTests::bottles
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, r -> TESTS.forEach((name, test) -> r.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_tank_disposal"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 200, 0, true))));
    }

    private static FluidTankBlockEntity tank(GameTestHelper h) {
        h.setBlock(POS, ModBlocks.FLUID_TANK.get());
        return h.getBlockEntity(POS, FluidTankBlockEntity.class);
    }

    private static void sponge(GameTestHelper h, BlockPos pos, boolean present) {
        h.getLevel().setBlockAndUpdate(pos, present ? ModBlocks.MENGER_SPONGE.getDefaultState() : Blocks.AIR.defaultBlockState());
    }

    private static int insert(ResourceHandler<FluidResource> handler, FluidResource fluid, int amount, boolean commit) {
        try (Transaction tx = Transaction.openRoot()) {
            int inserted = handler.insert(fluid, amount, tx);
            if (commit) tx.commit();
            return inserted;
        }
    }

    private static void capacity(GameTestHelper h) {
        var be = tank(h);
        var cached = be.getFluidHandler();
        insert(cached, FluidResource.of(Fluids.WATER), 15000, true);
        sponge(h, be.getBlockPos().north(), true);
        h.assertTrue(insert(cached, FluidResource.of(Fluids.WATER), 5000, false) == 5000 && cached.getAmountAsInt(0) == 15000,
            "Simulated overflow accepts all without changing storage");
        h.assertTrue(insert(cached, FluidResource.of(Fluids.WATER), 5000, true) == 5000 && cached.getAmountAsInt(0) == 16000,
            "Committed overflow stores only remaining capacity");
        h.assertTrue(insert(cached, FluidResource.of(Fluids.LAVA), 1000, true) == 0 && cached.getAmountAsInt(0) == 16000,
            "Foreign fluid is rejected instead of discarded");
        sponge(h, be.getBlockPos().north(), false);
        h.assertTrue(insert(cached, FluidResource.of(Fluids.WATER), 1000, true) == 0, "Removing sponge updates cached capability");
        h.succeed();
    }

    private static void neighbors(GameTestHelper h) {
        var be = tank(h);
        var handler = be.getFluidHandler();
        insert(handler, FluidResource.of(Fluids.WATER), 16000, true);
        for (Direction face : Direction.values()) {
            sponge(h, be.getBlockPos().relative(face), true);
            h.assertTrue(insert(handler, FluidResource.of(Fluids.WATER), 1, false) == 1, "Face adjacency enables " + face);
            sponge(h, be.getBlockPos().relative(face), false);
            h.assertTrue(insert(handler, FluidResource.of(Fluids.WATER), 1, false) == 0, "Removing face disables " + face);
        }
        sponge(h, be.getBlockPos().east().south(), true);
        h.getLevel().setBlockAndUpdate(be.getBlockPos().west(), ModBlocks.VOID_MATTER_BLOCK.getDefaultState());
        h.assertTrue(insert(be.getFluidHandler(), FluidResource.of(Fluids.WATER), 1, false) == 0,
            "Diagonal sponge and ordinary void matter do not enable tank disposal");
        h.succeed();
    }

    private static void enhanced(GameTestHelper h) {
        var be = tank(h);
        sponge(h, be.getBlockPos().north(), true);
        be.onFormed();
        var handler = be.getFluidHandler();
        insert(handler, FluidResource.of(Fluids.WATER), FluidTankBlockEntity.INFINITY_THRESHOLD - 1, true);
        try (Transaction root = Transaction.openRoot()) {
            try (Transaction nested = Transaction.open(root)) {
                h.assertTrue(handler.insert(FluidResource.of(Fluids.WATER), 2, nested) == 2 && be.isInfinite(),
                    "Reach infinity inside transaction");
                nested.commit();
            }
        }
        h.assertTrue(!be.isInfinite() && handler.getAmountAsInt(0) == FluidTankBlockEntity.INFINITY_THRESHOLD - 1,
            "Rollback restores amount and infinite state");
        insert(handler, FluidResource.of(Fluids.WATER), 2, true);
        h.assertTrue(be.isInfinite() && handler.getAmountAsInt(0) == FluidTankBlockEntity.INFINITY_THRESHOLD, "Commit reaches infinity");
        try (Transaction tx = Transaction.openRoot()) {
            h.assertTrue(handler.extract(FluidResource.of(Fluids.WATER), 20000, tx) == 20000, "Infinite extraction remains unlimited");
            tx.commit();
        }
        be.onUnformed();
        h.assertTrue(!be.isInfinite() && insert(handler, FluidResource.of(Fluids.WATER), 1000, true) == 1000
            && handler.getAmountAsInt(0) == FluidTankBlockEntity.INFINITY_THRESHOLD,
                "Downgrade preserves stored excess while disposing new input");
        sponge(h, be.getBlockPos().north(), false);
        h.assertTrue(insert(handler, FluidResource.of(Fluids.WATER), 1, false) == 0, "Downgraded normal tank rejects input over capacity");
        var tag = be.getUpdateTag(h.getLevel().registryAccess());
        tag.getCompoundOrEmpty("Tank").putBoolean("Enhanced", true);
        tag.getCompoundOrEmpty("Tank").putBoolean("Infinite", false);
        be.loadAdditional(TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), tag));
        sponge(h, be.getBlockPos().north(), true);
        h.assertTrue(insert(handler, FluidResource.of(Fluids.WATER), 1, false) == 1 && !be.isInfinite(),
            "Full enhanced reload can simulate reaching infinity without committing it");
        insert(handler, FluidResource.of(Fluids.WATER), 1, true);
        h.assertTrue(be.isInfinite(), "Full enhanced reload enters infinity when the next insertion commits");
        sponge(h, be.getBlockPos().north(), false);
        be.loadAdditional(TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), tag));
        insert(handler, FluidResource.of(Fluids.WATER), 1, true);
        h.assertTrue(be.isInfinite(), "Full enhanced reload also enters infinity without disposal mode");
        h.succeed();
    }

    private static void reload(GameTestHelper h) {
        var be = tank(h);
        sponge(h, be.getBlockPos().north(), true);
        insert(be.getFluidHandler(), FluidResource.of(Fluids.WATER), 20000, true);
        var tag = be.getUpdateTag(h.getLevel().registryAccess());
        h.assertTrue(!tag.getCompoundOrEmpty("Tank").contains("Dispose"), "Disposal mode is not persisted");
        h.setBlock(POS.east(4), ModBlocks.FLUID_TANK.get());
        var restored = h.getBlockEntity(POS.east(4), FluidTankBlockEntity.class);
        restored.loadAdditional(TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), tag));
        restored.onLoad();
        h.assertTrue(insert(restored.getFluidHandler(), FluidResource.of(Fluids.WATER), 1, false) == 0,
                "Reload outside sponge adjacency disables disposal");
        h.getLevel().setBlock(restored.getBlockPos().north(), ModBlocks.MENGER_SPONGE.getDefaultState(), Block.UPDATE_CLIENTS);
        var handler = h.getLevel().getCapability(Capabilities.Fluid.BLOCK, restored.getBlockPos(), Direction.UP);
        h.assertTrue(handler != null && insert(handler, FluidResource.of(Fluids.WATER), 1, false) == 1,
            "Capability lookup refreshes neighbor state even without neighbor events");
        h.succeed();
    }

    private static void network(GameTestHelper h) {
        var be = tank(h);
        sponge(h, be.getBlockPos().north(), true);
        var handler = be.getFluidHandler();
        insert(handler, FluidResource.of(Fluids.WATER), 16000, true);
        var source = new FluidStacksResourceHandler(1, 1000);
        source.set(0, FluidResource.of(Fluids.WATER), 1000);
        BlockPos a = be.getBlockPos().west(3);
        BlockPos b = a.east();
        BlockPos c = a.east(2);
        var network = new FluidPipeNetwork(h.getLevel(), Set.of(a, b, c),
            Map.of(a, List.of(b), b, List.of(a, c), c, List.of(b)), Map.of(), Map.of(), Map.of(), Set.of(),
            List.of(new FluidEndpoint(a.west(), a, Direction.EAST, source, a.getY() + 10, false),
                new FluidEndpoint(be.getBlockPos(), c, Direction.WEST, handler, c.getY(), false)));
        network.tick();
        h.assertTrue(source.getAmountAsInt(0) < 1000 && handler.getAmountAsInt(0) == 16000,
                "Pipe network transfers into a full disposal tank");
        source.set(0, FluidResource.of(Fluids.WATER), 1000);
        sponge(h, be.getBlockPos().north(), false);
        network.tick();
        h.assertTrue(source.getAmountAsInt(0) == 1000, "Pipe network stops after disposal is disabled");
        h.succeed();
    }

    private static void bottles(GameTestHelper h) {
        var player = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "PortTankBottle"));
        player.setGameMode(GameType.SURVIVAL);
        BlockPos pos = h.absolutePos(POS);
        for (Block block : new Block[]{ModBlocks.FLUID_TANK.get(), ModBlocks.LARGE_FLUID_TANK.get()}) {
            var state = block.defaultBlockState();
            h.getLevel().setBlock(pos, state, Block.UPDATE_CLIENTS);
            block.setPlacedBy(h.getLevel(), pos, state, player, ItemStack.EMPTY);
            var entity = h.getLevel().getBlockEntity(pos);
            var handler = entity instanceof FluidTankBlockEntity single ? single.getFluidHandler()
                : ((LargeFluidTankBlockEntity) entity).getFluidHandler();
            BlockPos clicked = block == ModBlocks.LARGE_FLUID_TANK.get() ? pos.above().east() : pos;
            var hit = new BlockHitResult(Vec3.atCenterOf(clicked), Direction.SOUTH, clicked, false);
            player.setItemInHand(InteractionHand.MAIN_HAND, PotionContents.createItemStack(Items.POTION, Potions.WATER));
            var result = h.getLevel().getBlockState(clicked).useItemOn(
                player.getMainHandItem(), h.getLevel(), player, InteractionHand.MAIN_HAND, hit);
            h.assertTrue(result.consumesAction() && handler.getAmountAsInt(0) == 250 && player.getMainHandItem().is(Items.GLASS_BOTTLE),
                "Tank bottle input exchanges exactly 250 mB through actual block interaction");
            result = h.getLevel().getBlockState(clicked).useItemOn(
                player.getMainHandItem(), h.getLevel(), player, InteractionHand.MAIN_HAND, hit);
            h.assertTrue(result.consumesAction() && handler.getAmountAsInt(0) == 0 && player.getMainHandItem().is(Items.POTION),
                "Tank bottle output uses shared interaction, including large-tank child parts");
            BlockPos spongePos = pos.north(block == ModBlocks.FLUID_TANK.get() ? 1 : 2);
            sponge(h, spongePos, true);
            long capacity = handler.getCapacityAsLong(0, FluidResource.of(Fluids.WATER));
            insert(handler, FluidResource.of(Fluids.WATER), (int) capacity, true);
            int expected = block == ModBlocks.FLUID_TANK.get() ? 1000 : 0;
            h.assertTrue(insert(handler, FluidResource.of(Fluids.WATER), 1000, false) == expected,
                "Only the ordinary tank supports adjacency disposal");
            sponge(h, spongePos, false);
        }
        h.succeed();
    }
}
