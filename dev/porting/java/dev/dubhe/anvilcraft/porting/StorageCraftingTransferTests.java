package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.TypeLimitItemStacksResourceHandler;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import dev.dubhe.anvilcraft.saved.storage.CraftingStorage;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class StorageCraftingTransferTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_transfer_layout", StorageCraftingTransferTests::layout,
        "port_transfer_sets", StorageCraftingTransferTests::sets,
        "port_transfer_components", StorageCraftingTransferTests::components,
        "port_transfer_fluids", StorageCraftingTransferTests::fluids,
        "port_transfer_partial_fluid", StorageCraftingTransferTests::partialFluid,
        "port_transfer_clear_fallback", StorageCraftingTransferTests::clearFallback,
        "port_transfer_stonecutter", StorageCraftingTransferTests::stonecutter,
        "port_transfer_invalid", StorageCraftingTransferTests::invalid
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_crafting_transfer"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    static void unlock(StorageFluidRpcTests.Fixture fixture) {
        fixture.stock(ItemResource.of(Items.CRAFTING_TABLE), 1);
        fixture.stock(ItemResource.of(Items.STONECUTTER), 1);
        fixture.authorize();
        if (!StorageServerStub.craftingUnlock(fixture.playerId(), fixture.core().asLong())) {
            throw new IllegalStateException("Fixture crafting unlock failed");
        }
    }

    private static boolean transfer(StorageFluidRpcTests.Fixture fixture, boolean max, List<ItemStack> inputs, int... counts) {
        fixture.authorize();
        return StorageServerStub.craftingTransfer(fixture.playerId(), fixture.core().asLong(), false, max, inputs,
            ItemStack.EMPTY, new IntArrayList(counts));
    }

    private static void layout(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            unlock(fixture);
            final var storage = StorageCraftingExecutionTests.storage(helper, fixture);
            storage.setCrafting(CraftingStorage.EMPTY.withStonecutterInput(new ItemStack(Items.STONE, 3))
                .withCraftingSlot(8, new ItemStack(Items.STICK, 2)).withAutoFill(true).withToStorage(true).withLastOpened(true));
            fixture.player().getInventory().setItem(9, new ItemStack(Items.STICK, 4));
            fixture.player().containerMenu.setCarried(new ItemStack(Items.DIAMOND, 7));
            helper.assertTrue(transfer(fixture, false, List.of(ItemStack.EMPTY, new ItemStack(Items.STICK), ItemStack.EMPTY,
                new ItemStack(Items.STICK)), 0, 2, 0, 1), "单次转移必须填入指定份数和偏移");
            var state = storage.getCrafting();
            helper.assertTrue(state.craftingInput().get(1).getCount() == 2 && state.craftingInput().get(3).getCount() == 1
                && state.craftingInput().get(8).isEmpty() && state.stonecutterInput().isEmpty(), "旧输入必须归还，新输入保持布局");
            helper.assertTrue(fixture.count(ItemResource.of(Items.STONE)) == 3 && fixture.count(ItemResource.of(Items.STICK)) == 2
                && fixture.player().getInventory().getItem(9).getCount() == 1, "旧输入先返仓储，新材料优先使用背包");
            helper.assertTrue(state.autoFill() && state.toStorage() && state.lastOpened(), "转移不能重置选项");
            helper.assertTrue(fixture.player().containerMenu.getCarried().getCount() == 7, "转移不能消费指针");
        }
        helper.succeed();
    }

    private static void sets(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            unlock(fixture);
            final var storage = StorageCraftingExecutionTests.storage(helper, fixture);
            fixture.stock(ItemResource.of(Items.STICK), 10);
            helper.assertTrue(transfer(fixture, true, List.of(new ItemStack(Items.STICK), new ItemStack(Items.STICK)), 2, 1),
                "最大转移必须支持非等量槽位份数");
            helper.assertTrue(storage.getCrafting().craftingInput().get(0).getCount() == 6
                && storage.getCrafting().craftingInput().get(1).getCount() == 3 && fixture.count(ItemResource.of(Items.STICK)) == 1,
                "只能填三整组，最后一根必须保留");
            helper.assertTrue(transfer(fixture, true, List.of(new ItemStack(Items.STICK)), 1), "原合成格物品应可重新参与转移");
            helper.assertTrue(storage.getCrafting().craftingInput().getFirst().getCount() == 10, "再次转移不得丢失或复制材料");
        }
        helper.succeed();
    }

    private static void components(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            unlock(fixture);
            final var storage = StorageCraftingExecutionTests.storage(helper, fixture);
            var named = new ItemStack(Items.STICK, 3);
            named.set(DataComponents.CUSTOM_NAME, Component.literal("Keep me"));
            fixture.player().getInventory().setItem(9, named);
            fixture.player().setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.STICK, 8));
            helper.assertTrue(!transfer(fixture, false, List.of(new ItemStack(Items.STICK)), 1), "不可取异组件物品或副手材料");
            helper.assertTrue(fixture.player().getOffhandItem().getCount() == 8 && ItemStack.matches(named,
                fixture.player().getInventory().getItem(9)), "失败不能改动候选材料");
            fixture.stock(ItemResource.of(Items.STICK), 100);
            helper.assertTrue(transfer(fixture, true, List.of(new ItemStack(Items.STICK, 99)), 1), "客户端物品数量不应决定实际扣料");
            helper.assertTrue(storage.getCrafting().craftingInput().getFirst().getCount() == 64
                && fixture.count(ItemResource.of(Items.STICK)) == 36, "最大转移必须服从物品堆叠上限");
        }
        helper.succeed();
    }

    private static void fluids(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            unlock(fixture);
            final var storage = StorageCraftingExecutionTests.storage(helper, fixture);
            final var water = fixture.fluid(FluidResource.of(Fluids.WATER), 1000);
            final var lava = fixture.fluid(FluidResource.of(Fluids.LAVA), 1000);
            fixture.player().getInventory().setItem(9, new ItemStack(Items.BUCKET));
            fixture.player().getInventory().setItem(10, new ItemStack(Items.STICK));
            var inputs = List.of(new ItemStack(Items.STICK), new ItemStack(Items.WATER_BUCKET), new ItemStack(Items.LAVA_BUCKET));
            helper.assertTrue(!transfer(fixture, false, inputs, 1, 1, 1), "一个空桶不能同时分配给两种流体");
            helper.assertTrue(water.getFluid().getAmount() == 1000 && lava.getFluid().getAmount() == 1000
                && fixture.player().getInventory().getItem(9).is(Items.BUCKET)
                && fixture.player().getInventory().getItem(10).is(Items.STICK), "失败整组必须回滚流体、桶及先取物品");
            helper.assertTrue(storage.getCrafting().craftingInput().stream().allMatch(ItemStack::isEmpty), "失败整组不能留下部分填料");
            fixture.stock(ItemResource.of(Items.BUCKET), 1);
            helper.assertTrue(transfer(fixture, true, inputs, 1, 1, 1), "背包与仓储空桶可同时用于填充不同流体");
            helper.assertTrue(water.getFluid().isEmpty() && lava.getFluid().isEmpty()
                && storage.getCrafting().craftingInput().get(1).is(Items.WATER_BUCKET)
                && storage.getCrafting().craftingInput().get(2).is(Items.LAVA_BUCKET), "成功组必须精准扣料并生成两个桶");
        }
        helper.succeed();
    }

    private static void partialFluid(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            unlock(fixture);
            final var water = fixture.fluid(FluidResource.of(Fluids.WATER), 1500);
            fixture.stock(ItemResource.of(Items.BUCKET), 2);
            helper.assertTrue(!transfer(fixture, false,
                List.of(new ItemStack(Items.WATER_BUCKET), new ItemStack(Items.WATER_BUCKET)), 1, 1),
                "不足两桶流体时不得部分填入");
            helper.assertTrue(water.getFluid().getAmount() == 1500 && fixture.count(ItemResource.of(Items.BUCKET)) == 2,
                "此前成功盛装及随后不足一桶的抽取必须整体回滚");
        }
        helper.succeed();
    }

    private static void clearFallback(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            unlock(fixture);
            final var storage = StorageCraftingExecutionTests.storage(helper, fixture);
            var handler = (TypeLimitItemStacksResourceHandler) fixture.items();
            fixture.stock(ItemResource.of(Items.DIAMOND), TypeLimitItemStacksResourceHandler.computeCount(
                ItemResource.of(Items.DIAMOND), handler.getSpaceSize()));
            for (int slot = 0; slot < 36; slot++) fixture.player().getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
            storage.setCrafting(CraftingStorage.EMPTY.withCraftingSlot(8, new ItemStack(Items.DIAMOND, 3)));
            helper.assertTrue(transfer(fixture, false, List.of(new ItemStack(Items.STICK)), 1), "仅归还旧输入也必须报告状态变化");
            int dropped = fixture.player().level().getEntitiesOfClass(ItemEntity.class, fixture.player().getBoundingBox().inflate(4))
                .stream().map(ItemEntity::getItem).filter(stack -> stack.is(Items.DIAMOND)).mapToInt(ItemStack::getCount).sum();
            helper.assertTrue(dropped == 3 && storage.getCrafting().craftingInput().get(8).isEmpty(), "满仓满背包时必须落地保全旧输入");
        }
        helper.succeed();
    }

    private static void stonecutter(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            unlock(fixture);
            final var storage = StorageCraftingExecutionTests.storage(helper, fixture);
            fixture.stock(ItemResource.of(Items.STONE), 70);
            fixture.authorize();
            helper.assertTrue(StorageServerStub.craftingTransfer(fixture.playerId(), fixture.core().asLong(), true, false,
                List.of(new ItemStack(Items.STONE)), new ItemStack(Items.STONE_SLAB), new IntArrayList()), "单次切石转移应成功");
            helper.assertTrue(storage.getCrafting().stonecutterInput().getCount() == 1, "单次只能填一个切石原料");
            var recipes = fixture.player().level().recipeAccess().stonecutterRecipes().selectByInput(new ItemStack(Items.STONE)).entries();
            var chosen = recipes.get(storage.getCrafting().stonecutterSelected()).recipe().recipe().orElseThrow().value();
            helper.assertTrue(chosen.assemble(new SingleRecipeInput(new ItemStack(Items.STONE))).is(Items.STONE_SLAB),
                "必须选中 JEI 指定的切石产物");
            fixture.authorize();
            StorageServerStub.craftingTransfer(fixture.playerId(), fixture.core().asLong(), true, true,
                List.of(new ItemStack(Items.STONE)), new ItemStack(Items.STONE_SLAB), new IntArrayList());
            helper.assertTrue(storage.getCrafting().stonecutterInput().getCount() == 64 && fixture.count(ItemResource.of(Items.STONE)) == 6,
                "最大切石转移上限一组，旧输入必须计入总量");
        }
        helper.succeed();
    }

    private static void invalid(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            unlock(fixture);
            final var storage = StorageCraftingExecutionTests.storage(helper, fixture);
            var state = CraftingStorage.EMPTY.withCraftingSlot(8, new ItemStack(Items.DIAMOND, 3));
            storage.setCrafting(state);
            helper.assertTrue(!transfer(fixture, true, List.of(new ItemStack(Items.STICK)), Integer.MAX_VALUE), "拒绝溢出份数");
            helper.assertTrue(!transfer(fixture, true, List.of(new ItemStack(Items.STICK)), -1), "拒绝负份数");
            helper.assertTrue(!transfer(fixture, true, Collections.nCopies(10, new ItemStack(Items.STICK)), 1), "拒绝超出九宫格的请求");
            helper.assertTrue(ItemStack.matches(storage.getCrafting().craftingInput().get(8), state.craftingInput().get(8)),
                "非法请求必须在清空旧输入前拒绝");
        }
        helper.succeed();
    }
}
