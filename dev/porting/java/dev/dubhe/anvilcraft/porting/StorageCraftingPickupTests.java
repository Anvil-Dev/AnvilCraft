package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import dev.dubhe.anvilcraft.saved.storage.CraftingStorage;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class StorageCraftingPickupTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_craft_pickup_priority", StorageCraftingPickupTests::priority,
        "port_craft_pickup_limits", StorageCraftingPickupTests::limits,
        "port_craft_pickup_authority", StorageCraftingPickupTests::authority,
        "port_craft_pickup_components", StorageCraftingPickupTests::components,
        "port_craft_pickup_scope", StorageCraftingPickupTests::scope
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_crafting_pickup"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static StorageServerStub.InteractionResult collect(StorageFluidRpcTests.Fixture fixture, int slot, ItemStack snapshot) {
        fixture.authorize();
        return StorageServerStub.craftingPickupAll(fixture.playerId(), fixture.core().asLong(), slot, snapshot);
    }

    private static void priority(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            var storage = StorageCraftingExecutionTests.storage(helper, fixture);
            storage.setCrafting(CraftingStorage.EMPTY.withStonecutterInput(new ItemStack(Items.STONE, 40))
                .withCraftingSlot(0, new ItemStack(Items.STONE, 20)).withCraftingSlot(8, new ItemStack(Items.STONE, 10)));
            fixture.player().getInventory().setItem(10, new ItemStack(Items.STONE, 64));
            var result = collect(fixture, 9, ItemStack.EMPTY);
            helper.assertTrue(result.changed() && result.carried().getCount() == 64, "应收集到一组");
            var state = storage.getCrafting();
            helper.assertTrue(state.craftingInput().get(8).isEmpty() && state.stonecutterInput().isEmpty()
                && state.craftingInput().get(0).getCount() == 6 && fixture.player().getInventory().getItem(10).getCount() == 64,
                "顺序必须是目标槽、其余输入槽、最后背包");
            helper.assertTrue(!collect(fixture, 9, new ItemStack(Items.STONE, 1)).changed(), "过期的小指针快照不能再次收集");
        }
        helper.succeed();
    }

    private static void limits(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            var storage = StorageCraftingExecutionTests.storage(helper, fixture);
            storage.setCrafting(CraftingStorage.EMPTY.withCraftingSlot(8, new ItemStack(Items.ENDER_PEARL, 20)));
            var result = collect(fixture, 9, ItemStack.EMPTY);
            helper.assertTrue(result.carried().getCount() == 16 && storage.getCrafting().craftingInput().get(8).getCount() == 4,
                "空指针也必须按目标物品的堆叠上限取出");
            helper.assertTrue(!collect(fixture, 10, ItemStack.EMPTY).changed(), "越界槽位不得收集");
        }
        helper.succeed();
    }

    private static void authority(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            var storage = StorageCraftingExecutionTests.storage(helper, fixture);
            storage.setCrafting(CraftingStorage.EMPTY.withCraftingSlot(2, new ItemStack(Items.STICK, 10)));
            fixture.player().containerMenu.setCarried(new ItemStack(Items.STICK, 2));
            var result = collect(fixture, 3, new ItemStack(Items.DIAMOND, 64));
            helper.assertTrue(result.carried().is(Items.STICK) && result.carried().getCount() == 12,
                "客户端数据不能替换服务端指针或生成物品");
            fixture.player().getAbilities().instabuild = true;
            storage.setCrafting(CraftingStorage.EMPTY.withCraftingSlot(0, new ItemStack(Items.STICK, 3)));
            result = collect(fixture, 1, new ItemStack(Items.STICK, 64));
            helper.assertTrue(result.carried().getCount() == 15, "创造模式的收集也不能把旧快照当成复制请求");
        }
        helper.succeed();
    }

    private static void components(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            var named = new ItemStack(Items.STICK, 4);
            named.set(DataComponents.CUSTOM_NAME, Component.literal("Tagged"));
            var storage = StorageCraftingExecutionTests.storage(helper, fixture);
            storage.setCrafting(CraftingStorage.EMPTY.withCraftingSlot(8, named).withCraftingSlot(0, new ItemStack(Items.STICK, 20)));
            fixture.player().getInventory().setItem(9, named.copyWithCount(5));
            fixture.player().getInventory().setItem(10, new ItemStack(Items.STICK, 30));
            helper.assertTrue(collect(fixture, 9, ItemStack.EMPTY).carried().getCount() == 9, "收集必须区分同类物品的组件");
            storage.setCrafting(storage.getCrafting().withCraftingSlot(8, new ItemStack(Items.DIAMOND)));
            helper.assertTrue(!collect(fixture, 9, ItemStack.EMPTY).changed(), "目标槽和指针异种时不能收集或交换");
        }
        helper.succeed();
    }

    private static void scope(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            var storage = StorageCraftingExecutionTests.storage(helper, fixture);
            storage.setCrafting(CraftingStorage.EMPTY.withCraftingSlot(8, new ItemStack(Items.STICK, 4)));
            fixture.player().getInventory().setItem(9, new ItemStack(Items.STICK, 5));
            fixture.player().setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STICK, 30));
            fixture.stock(ItemResource.of(Items.STICK), 100);
            helper.assertTrue(collect(fixture, 9, ItemStack.EMPTY).carried().getCount() == 9
                && fixture.player().getOffhandItem().getCount() == 30 && fixture.count(ItemResource.of(Items.STICK)) == 100,
                "双击不能从副手或仓储后台抽取额外物品");
            storage.setCrafting(CraftingStorage.EMPTY.withCraftingSlot(2, new ItemStack(Items.STICK, 3)));
            fixture.player().getInventory().setItem(9, new ItemStack(Items.STICK, 6));
            fixture.authorize();
            var result = StorageServerStub.craftingPickupIntoCarried(fixture.playerId(), fixture.core().asLong(), ItemStack.EMPTY);
            helper.assertTrue(result.carried().getCount() == 12 && fixture.player().getInventory().getItem(9).getCount() == 6,
                "背包双击补充接口只读取合成输入，不重复扫描背包");
            fixture.player().containerMenu.setCarried(ItemStack.EMPTY);
            fixture.authorize();
            helper.assertTrue(!StorageServerStub.craftingPickupIntoCarried(fixture.playerId(), fixture.core().asLong(),
                new ItemStack(Items.STICK)).changed(), "服务端空指针不能凭客户端快照创建收集样本");
        }
        helper.succeed();
    }
}
