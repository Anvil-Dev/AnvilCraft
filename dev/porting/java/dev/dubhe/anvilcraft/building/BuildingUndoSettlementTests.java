package dev.dubhe.anvilcraft.building;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.StoragePortManager;
import dev.dubhe.anvilcraft.block.entity.StorageFluidPortBlockEntity;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.porting.StorageFluidRpcTests;
import dev.dubhe.anvilcraft.porting.TerminalAccessTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class BuildingUndoSettlementTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_undo_picked_up", BuildingUndoSettlementTests::pickedUp,
        "port_undo_missing_contents", BuildingUndoSettlementTests::contents,
        "port_undo_harvest_conflict", BuildingUndoSettlementTests::harvest,
        "port_undo_pending_refund", BuildingUndoSettlementTests::pending,
        "port_undo_consumed_fuel", BuildingUndoSettlementTests::fuel
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_undo_settlement"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static void pickedUp(GameTestHelper helper) {
        final var player = BuildingMaterialUndoTests.player(helper);
        var pos = BuildingMaterialUndoTests.pos(helper);
        player.getInventory().setItem(0, new ItemStack(Items.DIAMOND_BLOCK));
        var undo = BuildingMaterialUndoTests.paid(helper, player,
            BuildingMaterialUndoTests.block(pos, Blocks.DIAMOND_BLOCK, new ItemStack(Items.DIAMOND_BLOCK)));
        undo.finish(player);
        helper.getLevel().destroyBlock(pos, true);
        var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1));
        helper.assertTrue(!drops.isEmpty(), "Missing actual block drops");
        drops.forEach(drop -> {
            player.getInventory().placeItemBackInInventory(drop.getItem().copy());
            drop.discard();
        });
        helper.assertTrue(BuildingRodUndo.restore(player) == BuildingRodUndo.Result.UNDONE
            && player.getInventory().countItem(Items.DIAMOND_BLOCK) == 1, "Picked-up drops were refunded twice");
        helper.succeed();
    }

    private static void contents(GameTestHelper helper) {
        var level = helper.getLevel();
        final var player = BuildingMaterialUndoTests.player(helper);
        var pos = BuildingMaterialUndoTests.pos(helper);
        level.setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
        ((ChestBlockEntity) level.getBlockEntity(pos)).setItem(0, new ItemStack(Items.DIAMOND, 5));
        var group = BuildingMaterialUndoTests.block(pos, Blocks.CHEST, ItemStack.EMPTY);
        var undo = new BuildingRodUndo(player, java.util.List.of(group));
        undo.finish(player);
        ((ChestBlockEntity) level.getBlockEntity(pos)).setItem(0, new ItemStack(Items.DIAMOND, 3));
        helper.assertTrue(BuildingRodUndo.restore(player) == BuildingRodUndo.Result.MISSING_MATERIALS
            && ((ChestBlockEntity) level.getBlockEntity(pos)).getItem(0).getCount() == 3, "Missing contents restored for free");
        player.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 2));
        helper.assertTrue(BuildingRodUndo.restore(player) == BuildingRodUndo.Result.UNDONE
            && ((ChestBlockEntity) level.getBlockEntity(pos)).getItem(0).getCount() == 5
            && player.getInventory().countItem(Items.DIAMOND) == 0, "Inventory deficit was not paid exactly once");
        helper.succeed();
    }

    private static void harvest(GameTestHelper helper) {
        var level = helper.getLevel();
        final var player = BuildingMaterialUndoTests.player(helper);
        var pos = BuildingMaterialUndoTests.pos(helper);
        level.setBlockAndUpdate(pos, Blocks.CAKE.defaultBlockState());
        var undo = new BuildingRodUndo(player,
            java.util.List.of(BuildingMaterialUndoTests.block(pos, Blocks.CAKE, ItemStack.EMPTY)));
        undo.finish(player);
        level.setBlockAndUpdate(pos, Blocks.CAKE.defaultBlockState().setValue(BlockStateProperties.BITES, 1));
        helper.assertTrue(BuildingRodUndo.restore(player) == BuildingRodUndo.Result.CONFLICT
            && level.getBlockState(pos).getValue(BlockStateProperties.BITES) == 1, "Eaten cake was restored for free");
        helper.succeed();
    }

    private static void fuel(GameTestHelper helper) {
        var level = helper.getLevel();
        final var player = BuildingMaterialUndoTests.player(helper);
        var pos = BuildingMaterialUndoTests.pos(helper);
        var state = Blocks.FURNACE.defaultBlockState();
        level.setBlockAndUpdate(pos, state);
        var data = level.getBlockEntity(pos).saveWithFullMetadata(level.registryAccess());
        data.putInt("lit_time_remaining", 100);
        level.setBlockEntity(net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos, state, data, level.registryAccess()));
        var group = BuildingMaterialUndoTests.block(pos, Blocks.FURNACE, ItemStack.EMPTY);
        var undo = new BuildingRodUndo(player, java.util.List.of(group));
        undo.finish(player);
        data.putInt("lit_time_remaining", 50);
        level.setBlockEntity(net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos, state, data, level.registryAccess()));
        helper.assertTrue(BuildingRodUndo.restore(player) == BuildingRodUndo.Result.CONFLICT
            && level.getBlockEntity(pos).saveWithFullMetadata(level.registryAccess()).getIntOr("lit_time_remaining", 0) == 50,
            "Consumed furnace burn time was restored for free");
        helper.succeed();
    }

    private static final class RetryHandler extends FluidStacksResourceHandler {
        private int inserts;
        private boolean retry;

        private RetryHandler() {
            super(1, 10000);
            this.set(0, FluidResource.of(Fluids.WATER), 1000);
        }

        @Override
        public int insert(FluidResource resource, int amount, TransactionContext transaction) {
            if (++this.inserts > 1 && !this.retry) return 0;
            return super.insert(resource, amount, transaction);
        }
    }

    private static final class RetryPort extends StorageFluidPortBlockEntity {
        private final RetryHandler handler = new RetryHandler();

        private RetryPort(BlockPos pos, BlockState state) {
            super(ModBlockEntities.STORAGE_FLUID_PORT.get(), pos, state);
        }

        @Override
        public ResourceHandler<FluidResource> getFluidHandler() {
            return this.handler;
        }
    }

    private static void pending(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            player.setGameMode(GameType.SURVIVAL);
            TerminalAccessTests.bound(fixture);
            var original = fixture.fluid(FluidResource.of(Fluids.WATER), 2000);
            var pos = BuildingMaterialUndoTests.pos(helper);
            var group = BuildingMaterialUndoTests.block(pos, Blocks.WATER, ItemStack.EMPTY);
            group.fluids.add(new FluidStack(Fluids.WATER, 1000));
            var undo = BuildingMaterialUndoTests.paid(helper, player, group);
            undo.finish(player);
            var port = new RetryPort(original.getBlockPos(), original.getBlockState());
            port.getTank().set(0, FluidResource.of(Fluids.WATER), 1000);
            helper.getLevel().setBlockEntity(port);
            var storage = (StorageBlockEntity) helper.getLevel().getBlockEntity(fixture.core());
            StoragePortManager.register(storage.getId(), helper.getLevel(), port.getBlockPos());
            helper.assertTrue(BuildingRodUndo.restore(player) == BuildingRodUndo.Result.PARTIAL
                && helper.getLevel().getBlockState(pos).isAir() && BuildingRodUndo.hasPendingRefund(player),
                "Execution-time refusal did not retain fluid debt");
            player.getInventory().setItem(0, new ItemStack(Items.STONE));
            helper.assertTrue(!BuildingRodService.commit(player,
                java.util.List.of(BuildingMaterialUndoTests.block(pos, Blocks.STONE, new ItemStack(Items.STONE))),
                false, true) && helper.getLevel().getBlockState(pos).isAir(), "New placement overwrote a pending refund");
            port.handler.retry = true;
            helper.assertTrue(BuildingRodUndo.restore(player) == BuildingRodUndo.Result.UNDONE
                && port.handler.getAmountAsInt(0) == 2000 && !BuildingRodUndo.hasPendingRefund(player), "Refund retry lost fluid");
            helper.assertTrue(BuildingRodUndo.restore(player) == BuildingRodUndo.Result.NOTHING
                && port.handler.getAmountAsInt(0) == 2000, "Refund retry duplicated fluid");
        }
        helper.succeed();
    }
}
