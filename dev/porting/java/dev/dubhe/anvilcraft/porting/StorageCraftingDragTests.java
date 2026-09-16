package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import dev.dubhe.anvilcraft.saved.storage.CraftingStorage;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
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

import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class StorageCraftingDragTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_craft_drag_divide", StorageCraftingDragTests::divide,
        "port_craft_drag_single", StorageCraftingDragTests::single,
        "port_craft_drag_limits", StorageCraftingDragTests::limits,
        "port_craft_drag_creative", StorageCraftingDragTests::creative,
        "port_craft_drag_stonecutter", StorageCraftingDragTests::stonecutter
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_crafting_drag"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static StorageServerStub.InteractionResult drag(
        StorageFluidRpcTests.Fixture fixture, int button, int[] inputs, int[] inventory, ItemStack clientCarried
    ) {
        fixture.authorize();
        return StorageServerStub.craftingQuickCraft(fixture.playerId(), fixture.core().asLong(), button,
            new IntArrayList(inputs), new IntArrayList(inventory), clientCarried);
    }

    private static void divide(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            fixture.player().containerMenu.setCarried(new ItemStack(Items.OAK_LOG, 10));
            var result = drag(fixture, 0, new int[]{1, 9}, new int[]{9}, new ItemStack(Items.DIAMOND, 64));
            var state = StorageCraftingExecutionTests.storage(helper, fixture).getCrafting();
            helper.assertTrue(result.changed() && result.carried().is(Items.OAK_LOG) && result.carried().getCount() == 1,
                "生存拖拽必须使用服务端指针并保留均分余数");
            helper.assertTrue(state.craftingInput().get(0).getCount() == 3 && state.craftingInput().get(8).getCount() == 3
                && fixture.player().getInventory().getItem(9).getCount() == 3, "合成区和背包必须共用同一分配基数");
        }
        helper.succeed();
    }

    private static void single(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            fixture.player().containerMenu.setCarried(new ItemStack(Items.STICK, 5));
            var result = drag(fixture, 1, new int[]{1, 1, -1, 10, 9}, new int[]{0, 0, 36, -1}, ItemStack.EMPTY);
            var state = StorageCraftingExecutionTests.storage(helper, fixture).getCrafting();
            helper.assertTrue(result.carried().getCount() == 2 && state.craftingInput().get(0).getCount() == 1
                && state.craftingInput().get(8).getCount() == 1 && fixture.player().getInventory().getItem(0).getCount() == 1,
                "右键每个有效目标只放一个，重复和越界目标不得计入");
            helper.assertTrue(fixture.player().getOffhandItem().isEmpty(), "拖拽不能写入副手或装备槽");
        }
        helper.succeed();
    }

    private static void limits(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            var storage = StorageCraftingExecutionTests.storage(helper, fixture);
            var named = new ItemStack(Items.STICK, 10);
            named.set(DataComponents.CUSTOM_NAME, Component.literal("Named"));
            storage.setCrafting(CraftingStorage.EMPTY.withCraftingSlot(0, new ItemStack(Items.STICK, 60)));
            fixture.player().containerMenu.setCarried(named);
            helper.assertTrue(!drag(fixture, 0, new int[]{1}, new int[]{}, ItemStack.EMPTY).changed(),
                "不同组件的槽位不能在拖拽时被交换或合并");
            fixture.player().containerMenu.setCarried(new ItemStack(Items.STICK, 10));
            var result = drag(fixture, 0, new int[]{1, 2}, new int[]{}, ItemStack.EMPTY);
            helper.assertTrue(storage.getCrafting().craftingInput().get(0).getCount() == 64
                && storage.getCrafting().craftingInput().get(1).getCount() == 5 && result.carried().getCount() == 1,
                "满堆叠限制后的余量必须留在指针");
            fixture.player().containerMenu.setCarried(new ItemStack(Items.STICK, 2));
            helper.assertTrue(!drag(fixture, 2, new int[]{3}, new int[]{}, named).changed(), "生存玩家不能使用中键复制");
            helper.assertTrue(!drag(fixture, 7, new int[]{3}, new int[]{}, named).changed(), "无效按键不得修改材料");
        }
        helper.succeed();
    }

    private static void creative(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            fixture.player().getAbilities().instabuild = true;
            var storage = StorageCraftingExecutionTests.storage(helper, fixture);
            storage.setCrafting(CraftingStorage.EMPTY.withCraftingSlot(0, new ItemStack(Items.STICK, 60)));
            var result = drag(fixture, 2, new int[]{1, 2}, new int[]{9}, new ItemStack(Items.STICK, 2));
            helper.assertTrue(result.carried().getCount() == 2 && storage.getCrafting().craftingInput().get(0).getCount() == 64
                && storage.getCrafting().craftingInput().get(1).getCount() == 64
                && fixture.player().getInventory().getItem(9).getCount() == 64, "创造中键应填满各槽且不消耗指针");
            storage.setCrafting(CraftingStorage.EMPTY);
            result = drag(fixture, 0, new int[]{1}, new int[]{}, new ItemStack(Items.ENDER_PEARL, 64));
            helper.assertTrue(storage.getCrafting().craftingInput().get(0).getCount() == 16 && result.carried().getCount() == 48,
                "即使客户端传入超大堆叠，空合成槽也不能超过物品上限");
        }
        helper.succeed();
    }

    private static void stonecutter(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            fixture.player().containerMenu.setCarried(new ItemStack(Items.DIAMOND, 4));
            helper.assertTrue(!drag(fixture, 0, new int[]{0}, new int[]{}, ItemStack.EMPTY).changed(), "切石输入不能接收无配方物品");
            fixture.player().containerMenu.setCarried(new ItemStack(Items.STONE, 6));
            var result = drag(fixture, 0, new int[]{0, 1}, new int[]{9}, ItemStack.EMPTY);
            var state = StorageCraftingExecutionTests.storage(helper, fixture).getCrafting();
            helper.assertTrue(result.carried().isEmpty() && state.stonecutterInput().getCount() == 2
                && state.craftingInput().get(0).getCount() == 2 && fixture.player().getInventory().getItem(9).getCount() == 2,
                "有效切石输入应参与跨区均分");
            fixture.authorize();
            var empty = StorageServerStub.craftingQuickCraft(fixture.playerId(), fixture.core().asLong(), 0,
                IntList.of(), IntList.of(), new ItemStack(Items.STONE));
            helper.assertTrue(!empty.changed(), "无目标不能修改指针");
        }
        helper.succeed();
    }
}
