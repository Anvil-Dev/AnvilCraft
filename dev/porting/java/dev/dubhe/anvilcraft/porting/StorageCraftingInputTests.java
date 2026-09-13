package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.TypeLimitItemStacksResourceHandler;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import dev.dubhe.anvilcraft.saved.storage.BaseStorage;
import dev.dubhe.anvilcraft.saved.storage.CraftingStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class StorageCraftingInputTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_crafting_input_exchange", StorageCraftingInputTests::exchange,
        "port_crafting_input_components", StorageCraftingInputTests::components,
        "port_crafting_input_stonecutter", StorageCraftingInputTests::stonecutter,
        "port_crafting_input_clear", StorageCraftingInputTests::clear,
        "port_crafting_input_grid_clear", StorageCraftingInputTests::gridClear
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_crafting_input"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static BaseStorage<?> storage(GameTestHelper helper, StorageFluidRpcTests.Fixture fixture) {
        var core = (StorageBlockEntity) helper.getLevel().getBlockEntity(fixture.core());
        return Storages.get().get(core.getId()).orElseThrow();
    }

    private static StorageServerStub.InteractionResult put(StorageFluidRpcTests.Fixture fixture, int slot, int button) {
        fixture.authorize();
        return StorageServerStub.craftingPutCraftingSlot(fixture.playerId(), fixture.core().asLong(), slot, button, ItemStack.EMPTY);
    }

    private static boolean clearInputs(StorageFluidRpcTests.Fixture fixture) {
        fixture.authorize();
        return StorageServerStub.craftingClearToStorage(fixture.playerId(), fixture.core().asLong());
    }

    private static void exchange(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            final var storage = storage(helper, fixture);
            fixture.player().inventoryMenu.setCarried(new ItemStack(Items.STICK, 5));
            fixture.authorize();
            var result = StorageServerStub.craftingPutCraftingSlot(fixture.playerId(), fixture.core().asLong(), 4, 0,
                new ItemStack(Items.DIAMOND, 64));
            helper.assertTrue(result.carried().isEmpty() && storage.getCrafting().craftingInput().get(4).is(Items.STICK),
                "生存玩家必须使用服务端指针，不能凭客户端参数生成物品");
            result = put(fixture, 4, 1);
            helper.assertTrue(result.carried().getCount() == 3 && storage.getCrafting().craftingInput().get(4).getCount() == 2,
                "右键取半堆应向上取整");
            result = put(fixture, 4, 1);
            helper.assertTrue(result.carried().getCount() == 2 && storage.getCrafting().craftingInput().get(4).getCount() == 3,
                "右键堆叠只放一个");
            fixture.player().inventoryMenu.setCarried(new ItemStack(Items.DIAMOND, 4));
            result = put(fixture, 4, 1);
            helper.assertTrue(result.carried().is(Items.STICK) && result.carried().getCount() == 3
                && storage.getCrafting().craftingInput().get(4).getCount() == 4, "异种物品右键仍应整堆交换");
            try {
                put(fixture, 9, 0);
                helper.fail("越界合成槽没有被拒绝");
            } catch (IllegalArgumentException expected) {
                helper.assertTrue(storage.getCrafting().craftingInput().size() == 9, "非法请求不能改变合成格结构");
            }
        }
        helper.succeed();
    }

    private static void components(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            final var storage = storage(helper, fixture);
            var first = new ItemStack(Items.DIAMOND, 64);
            first.set(DataComponents.CUSTOM_NAME, Component.literal("first"));
            storage.setCrafting(CraftingStorage.EMPTY.withCraftingSlot(0, first));
            fixture.player().inventoryMenu.setCarried(first.copyWithCount(3));
            helper.assertTrue(!put(fixture, 0, 0).changed(), "同种已满时必须拒绝继续堆叠");
            var second = first.copyWithCount(2);
            second.set(DataComponents.CUSTOM_NAME, Component.literal("second"));
            fixture.player().inventoryMenu.setCarried(second);
            var result = put(fixture, 0, 0);
            helper.assertTrue(result.carried().getCount() == 64
                && storage.getCrafting().craftingInput().getFirst().getHoverName().getString().equals("second"),
                "不同组件必须交换而非合并");
        }
        helper.succeed();
    }

    private static void stonecutter(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            final var storage = storage(helper, fixture);
            fixture.player().inventoryMenu.setCarried(new ItemStack(Items.DIAMOND));
            fixture.authorize();
            helper.assertTrue(!StorageServerStub.craftingPutStonecutterInput(fixture.playerId(), fixture.core().asLong(), 0,
                ItemStack.EMPTY).changed(), "无切石配方的物品不能放入切石槽");
            fixture.player().inventoryMenu.setCarried(new ItemStack(Items.STONE, 5));
            fixture.authorize();
            helper.assertTrue(StorageServerStub.craftingPutStonecutterInput(fixture.playerId(), fixture.core().asLong(), 0,
                ItemStack.EMPTY).changed(), "石头应可作为切石输入");
            fixture.authorize();
            var recipes = StorageServerStub.craftingStonecutterRecipes(fixture.playerId(), fixture.core().asLong());
            helper.assertTrue(recipes.stream().anyMatch(stack -> stack.is(Items.STONE_SLAB) && stack.getCount() == 2),
                "原生切石候选结果必须包含两个石台阶");
            storage.setCrafting(storage.getCrafting().withStonecutterSelected(5));
            fixture.player().inventoryMenu.setCarried(new ItemStack(Items.ANDESITE, 3));
            fixture.authorize();
            var swapped = StorageServerStub.craftingPutStonecutterInput(fixture.playerId(), fixture.core().asLong(), 1, ItemStack.EMPTY);
            helper.assertTrue(swapped.carried().is(Items.STONE) && swapped.carried().getCount() == 5
                && storage.getCrafting().stonecutterSelected() == 0, "切石输入异种交换时应重置选中配方");
        }
        helper.succeed();
    }

    private static int nearlyFill(StorageFluidRpcTests.Fixture fixture) {
        var handler = (TypeLimitItemStacksResourceHandler) fixture.items();
        int capacity = TypeLimitItemStacksResourceHandler.computeCount(ItemResource.of(Items.STONE), handler.getSpaceSize());
        fixture.stock(ItemResource.of(Items.STONE), capacity - 5);
        return capacity;
    }

    private static void clear(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            int capacity = nearlyFill(fixture);
            final var storage = storage(helper, fixture);
            storage.setCrafting(CraftingStorage.EMPTY.withStonecutterInput(new ItemStack(Items.STONE, 12))
                .withCraftingSlot(0, new ItemStack(Items.DIAMOND, 3)).withAutoFill(true));
            helper.assertTrue(clearInputs(fixture) && fixture.count(ItemResource.of(Items.STONE)) == capacity,
                "清空应先把切石输入中装得下的部分存入");
            helper.assertTrue(storage.getCrafting().stonecutterInput().getCount() == 7
                && storage.getCrafting().craftingInput().getFirst().getCount() == 3, "剩余切石输入和未处理合成格必须保留");
            try (Transaction transaction = Transaction.openRoot()) {
                fixture.items().extract(ItemResource.of(Items.STONE), 100, transaction);
                transaction.commit();
            }
            helper.assertTrue(clearInputs(fixture) && storage.getCrafting().stonecutterInput().isEmpty()
                && storage.getCrafting().craftingInput().stream().allMatch(ItemStack::isEmpty), "容量恢复后可以完成清空");
            helper.assertTrue(storage.getCrafting().autoFill() && fixture.count(ItemResource.of(Items.DIAMOND)) == 3,
                "清空必须保留选项且不丢失材料");
            helper.assertTrue(!clearInputs(fixture), "空输入不应重复报告修改");
        }
        helper.succeed();
    }

    private static void gridClear(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            nearlyFill(fixture);
            final var storage = storage(helper, fixture);
            storage.setCrafting(CraftingStorage.EMPTY.withCraftingSlot(0, new ItemStack(Items.IRON_INGOT, 2))
                .withCraftingSlot(1, new ItemStack(Items.STONE, 12)).withCraftingSlot(2, new ItemStack(Items.DIAMOND, 3)));
            clearInputs(fixture);
            var grid = storage.getCrafting().craftingInput();
            helper.assertTrue(grid.get(0).isEmpty() && grid.get(1).getCount() == 7 && grid.get(2).getCount() == 3,
                "中途容量不足时只清除已完成槽位，并保留当前余量和后续槽位");
            helper.assertTrue(fixture.count(ItemResource.of(Items.IRON_INGOT)) == 2
                && fixture.count(ItemResource.of(Items.DIAMOND)) == 0, "清空顺序不得跳过未完成槽位继续转移");
        }
        helper.succeed();
    }
}
