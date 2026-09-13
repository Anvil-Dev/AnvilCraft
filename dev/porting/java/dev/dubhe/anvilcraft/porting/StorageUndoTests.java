package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class StorageUndoTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_storage_undo_history", StorageUndoTests::history,
        "port_storage_undo_capacity", StorageUndoTests::capacity,
        "port_storage_undo_components", StorageUndoTests::components,
        "port_storage_undo_group", StorageUndoTests::group,
        "port_storage_undo_fluid", StorageUndoTests::fluid,
        "port_storage_undo_scope", StorageUndoTests::scope,
        "port_storage_same_components", StorageUndoTests::sameComponents,
        "port_storage_same_fluids", StorageUndoTests::sameFluids
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        final var environment = event.registerEnvironment(AnvilCraft.of("port_storage_undo"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static void deposit(StorageFluidRpcTests.Fixture fixture) {
        fixture.authorize();
        StorageServerStub.deposit(fixture.playerId(), fixture.core().asLong(), true, true);
    }

    private static boolean undo(StorageFluidRpcTests.Fixture fixture) {
        fixture.authorize();
        return StorageServerStub.undo(fixture.playerId(), fixture.core().asLong()).changed();
    }

    private static int carriedInInventory(StorageFluidRpcTests.Fixture fixture, ItemResource resource) {
        int count = 0;
        for (int index = 0; index < Inventory.INVENTORY_SIZE; index++) {
            ItemStack stack = fixture.player().getInventory().getItem(index);
            if (!stack.isEmpty() && ItemResource.of(stack).equals(resource)) count += stack.getCount();
        }
        return count;
    }

    private static void history(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            final var diamond = ItemResource.of(Items.DIAMOND);
            for (int count = 1; count <= 5; count++) {
                fixture.player().getInventory().setItem(9, new ItemStack(Items.DIAMOND, count));
                deposit(fixture);
            }
            for (int count = 5; count >= 2; count--) {
                int before = carriedInInventory(fixture, diamond);
                helper.assertTrue(undo(fixture) && carriedInInventory(fixture, diamond) == before + count,
                    "撤销必须按后进先出顺序恢复最近操作");
            }
            helper.assertTrue(!undo(fixture) && fixture.count(diamond) == 1, "只保留最近四条记录，最早的物品留在仓储");
        }
        helper.succeed();
    }

    private static void capacity(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            final var diamond = ItemResource.of(Items.DIAMOND);
            fixture.player().getInventory().setItem(9, new ItemStack(Items.DIAMOND, 10));
            deposit(fixture);
            for (int index = 0; index < Inventory.INVENTORY_SIZE; index++) {
                fixture.player().getInventory().setItem(index, new ItemStack(Items.COBBLESTONE, 64));
            }
            fixture.player().getInventory().setItem(0, new ItemStack(Items.DIAMOND, 60));
            helper.assertTrue(undo(fixture) && fixture.count(diamond) == 6
                && fixture.player().getInventory().getItem(0).getCount() == 64, "撤销只返回背包装得下的数量");
            fixture.player().getInventory().clearContent();
            helper.assertTrue(!undo(fixture) && fixture.count(diamond) == 6, "源版每次尝试消费一条记录，余量不得重复取出");
        }
        helper.succeed();
    }

    private static void components(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            ItemStack first = new ItemStack(Items.DIAMOND, 2);
            ItemStack second = new ItemStack(Items.DIAMOND, 3);
            first.set(DataComponents.CUSTOM_NAME, Component.literal("first"));
            second.set(DataComponents.CUSTOM_NAME, Component.literal("second"));
            final var firstResource = ItemResource.of(first);
            final var secondResource = ItemResource.of(second);
            fixture.player().getInventory().setItem(9, first);
            fixture.player().getInventory().setItem(10, second);
            deposit(fixture);
            first.set(DataComponents.CUSTOM_NAME, Component.literal("changed"));
            helper.assertTrue(undo(fixture) && carriedInInventory(fixture, firstResource) == 2
                && carriedInInventory(fixture, secondResource) == 3, "记录必须按组件区分且不受原物品后续变更影响");
        }
        helper.succeed();
    }

    private static void group(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            fixture.authorize();
            StorageServerStub.beginUndoGroup(fixture.playerId(), fixture.core().asLong());
            fixture.player().getInventory().setItem(9, new ItemStack(Items.DIAMOND, 2));
            fixture.player().getInventory().setItem(10, new ItemStack(Items.IRON_INGOT, 3));
            fixture.authorize();
            StorageServerStub.quickMoveToStorage(fixture.playerId(), fixture.core().asLong(),
                new IntArrayList(new int[]{9, 9, -1, 999, 10}));
            fixture.player().getInventory().setItem(11, new ItemStack(Items.DIAMOND, 4));
            fixture.authorize();
            StorageServerStub.quickMoveToStorage(fixture.playerId(), fixture.core().asLong(), new IntArrayList(new int[]{11}));
            fixture.authorize();
            StorageServerStub.endUndoGroup(fixture.playerId(), fixture.core().asLong());
            helper.assertTrue(undo(fixture) && carriedInInventory(fixture, ItemResource.of(Items.DIAMOND)) == 6
                && carriedInInventory(fixture, ItemResource.of(Items.IRON_INGOT)) == 3,
                "多个分批拖动必须合为一条记录，重复及越界槽位不能重复转移");
            helper.assertTrue(!undo(fixture), "一个拖动组只能撤销一次");
        }
        helper.succeed();
    }

    private static void fluid(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            final var port = fixture.fluid(FluidResource.of(Fluids.WATER), 1000);
            fixture.stock(ItemResource.of(Items.WATER_BUCKET), 1);
            fixture.authorize();
            StorageServerStub.beginUndoGroup(fixture.playerId(), fixture.core().asLong());
            fixture.player().getInventory().setItem(9, new ItemStack(Items.WATER_BUCKET));
            fixture.player().getInventory().setItem(10, new ItemStack(Items.DIAMOND, 4));
            fixture.authorize();
            StorageServerStub.quickMoveToStorage(fixture.playerId(), fixture.core().asLong(), new IntArrayList(new int[]{9, 10}));
            fixture.authorize();
            StorageServerStub.endUndoGroup(fixture.playerId(), fixture.core().asLong());
            helper.assertTrue(undo(fixture) && carriedInInventory(fixture, ItemResource.of(Items.DIAMOND)) == 4,
                "混合拖动只撤销真正存入的物品");
            helper.assertTrue(port.getFluid().getAmount() == 2000 && fixture.count(ItemResource.of(Items.WATER_BUCKET)) == 1
                && carriedInInventory(fixture, ItemResource.of(Items.WATER_BUCKET)) == 0,
                "倒入的流体不能使撤销额外取走仓储原有的满桶");
        }
        helper.succeed();
    }

    private static void scope(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            final var diamond = ItemResource.of(Items.DIAMOND);
            fixture.player().getInventory().setItem(9, new ItemStack(Items.DIAMOND, 10));
            deposit(fixture);
            final var core = (StorageBlockEntity) helper.getLevel().getBlockEntity(fixture.core());
            final var other = ModBlockEntities.SHULKER_CONTAINER.create(fixture.core(), core.getBlockState());
            other.setId(UUID.randomUUID());
            helper.getLevel().setBlockEntity(other);
            helper.assertTrue(!undo(fixture), "撤销记录必须按仓储归属隔离");
            core.clearRemoved();
            helper.getLevel().setBlockEntity(core);
            try (Transaction transaction = Transaction.openRoot()) {
                fixture.items().extract(diamond, 7, transaction);
                transaction.commit();
            }
            helper.assertTrue(undo(fixture) && carriedInInventory(fixture, diamond) == 3 && fixture.count(diamond) == 0,
                "自动化取走物品后撤销只能返回实际剩余量");
            helper.assertTrue(!undo(fixture), "已消费记录不能再次产生物品");
        }
        helper.succeed();
    }

    private static boolean moveSame(StorageFluidRpcTests.Fixture fixture, int slot, boolean pour) {
        fixture.authorize();
        return StorageServerStub.moveSameToStorage(fixture.playerId(), fixture.core().asLong(), slot, pour);
    }

    private static void sameComponents(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            final ItemStack sample = new ItemStack(Items.DIAMOND);
            sample.set(DataComponents.CUSTOM_NAME, Component.literal("same"));
            final ItemResource resource = ItemResource.of(sample);
            fixture.player().getInventory().setItem(0, sample.copyWithCount(2));
            fixture.player().getInventory().setItem(9, sample.copyWithCount(3));
            fixture.player().getInventory().setItem(10, new ItemStack(Items.DIAMOND, 4));
            fixture.player().setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, sample.copy());
            helper.assertTrue(moveSame(fixture, 9, false) && fixture.count(resource) == 5,
                "同类批量存入应覆盖快捷栏与主背包，并按完整组件匹配");
            helper.assertTrue(fixture.player().getInventory().getItem(10).getCount() == 4
                && fixture.player().getOffhandItem().getCount() == 1, "不同组件和副手不得被同类操作移走");
            helper.assertTrue(undo(fixture)
                && carriedInInventory(fixture, resource) + fixture.player().getOffhandItem().getCount() == 6,
                "同类批量操作应作为一条记录撤销");
            helper.assertTrue(!moveSame(fixture, -1, false) && !moveSame(fixture, 9999, false)
                && !moveSame(fixture, 12, false), "空样本及越界槽位不得改变仓储");
        }
        helper.succeed();
    }

    private static void sameFluids(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            final var port = fixture.fluid(FluidResource.of(Fluids.WATER), 1000);
            fixture.player().getInventory().setItem(0, new ItemStack(Items.WATER_BUCKET));
            fixture.player().getInventory().setItem(9, new ItemStack(Items.WATER_BUCKET));
            fixture.player().getInventory().setItem(10, new ItemStack(Items.LAVA_BUCKET));
            helper.assertTrue(moveSame(fixture, 9, true) && port.getFluid().getAmount() == 3000,
                "同类左键应倾倒所有匹配流体桶");
            helper.assertTrue(fixture.player().getInventory().getItem(10).is(Items.LAVA_BUCKET) && !undo(fixture),
                "其他流体桶不受影响，纯倒液不产生物品撤销记录");
            fixture.player().getInventory().setItem(0, new ItemStack(Items.WATER_BUCKET));
            fixture.player().getInventory().setItem(9, new ItemStack(Items.WATER_BUCKET));
            helper.assertTrue(moveSame(fixture, 9, false) && port.getFluid().getAmount() == 3000
                && fixture.count(ItemResource.of(Items.WATER_BUCKET)) == 2, "同类右键应存入桶物品而不倒液");
            helper.assertTrue(undo(fixture) && carriedInInventory(fixture, ItemResource.of(Items.WATER_BUCKET)) == 2,
                "右键存入的满桶可以按物品撤销");
        }
        helper.succeed();
    }
}
