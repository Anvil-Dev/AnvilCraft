package dev.dubhe.anvilcraft.porting;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.TypeLimitItemStacksResourceHandler;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import dev.dubhe.anvilcraft.saved.storage.CraftingStorage;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class StorageCraftingBatchTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_craft_batch_budget", StorageCraftingBatchTests::budget,
        "port_craft_batch_chunk", StorageCraftingBatchTests::chunk,
        "port_craft_batch_unstackable", StorageCraftingBatchTests::unstackable,
        "port_craft_batch_lock", StorageCraftingBatchTests::lock,
        "port_craft_batch_catalyst", StorageCraftingBatchTests::catalyst,
        "port_craft_batch_capacity", StorageCraftingBatchTests::capacity,
        "port_craft_batch_partial", StorageCraftingBatchTests::partial,
        "port_craft_batch_throw", StorageCraftingBatchTests::throwResult,
        "port_craft_batch_stonecutter", StorageCraftingBatchTests::stonecutter,
        "port_craft_batch_session", StorageCraftingBatchTests::session
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_crafting_batch"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static StorageServerStub.TakeAllResult take(StorageFluidRpcTests.Fixture fixture, int multiplier) {
        fixture.authorize();
        return StorageServerStub.craftingTakeAll(fixture.playerId(), fixture.core().asLong(), false, multiplier);
    }

    private static StorageServerStub.InteractionResult drop(StorageFluidRpcTests.Fixture fixture, boolean stack) {
        fixture.authorize();
        return StorageServerStub.craftingThrowResult(fixture.playerId(), fixture.core().asLong(), false, stack);
    }

    private static int dropped(StorageFluidRpcTests.Fixture fixture, Item item) {
        return fixture.player().level().getEntitiesOfClass(ItemEntity.class, fixture.player().getBoundingBox().inflate(4)).stream()
            .map(ItemEntity::getItem).filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    private static void budget(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            var storage = StorageCraftingExecutionTests.storage(helper, fixture);
            storage.setCrafting(CraftingStorage.EMPTY.withCraftingSlot(8, new ItemStack(Items.HONEY_BOTTLE))
                .withAutoFill(true).withToStorage(true));
            fixture.stock(ItemResource.of(Items.HONEY_BOTTLE), 100);
            var result = take(fixture, 1);
            helper.assertTrue(result.changed() && result.done() && result.refilledSlots() == 1 << 9, "预算结束应返回完成及补料掩码");
            helper.assertTrue(fixture.count(ItemResource.of(Items.SUGAR)) == 63, "每次产出三件时一次点击应合成二十一次");
            helper.assertTrue(storage.getCrafting().craftingInput().get(8).is(Items.HONEY_BOTTLE)
                && fixture.count(ItemResource.of(Items.HONEY_BOTTLE)) == 79, "预算末尾必须保留下一次点击的原料模板");
            helper.assertTrue(take(fixture, 1).done() && fixture.count(ItemResource.of(Items.SUGAR)) == 126,
                "新点击应开启独立预算，不能把剩余瓶误当成模板");
        }
        helper.succeed();
    }

    private static void chunk(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            var storage = StorageCraftingExecutionTests.storage(helper, fixture);
            storage.setCrafting(CraftingStorage.EMPTY.withCraftingSlot(8, new ItemStack(Items.HONEY_BOTTLE))
                .withAutoFill(true).withToStorage(true));
            fixture.stock(ItemResource.of(Items.HONEY_BOTTLE), 400);
            var first = take(fixture, 4);
            helper.assertTrue(!first.done() && first.changed() && fixture.count(ItemResource.of(Items.SUGAR)) == 192,
                "一次 RPC 最多执行六十四次，不能耗尽整个点击预算");
            helper.assertTrue(take(fixture, 99).done() && fixture.count(ItemResource.of(Items.SUGAR)) == 252,
                "续块必须沿用剩余二十次预算，不接受新的倍率重置");
            var stack = new ItemStack(Items.OAK_LOG, 99);
            stack.set(DataComponents.MAX_STACK_SIZE, 99);
            storage.setCrafting(CraftingStorage.EMPTY.withCraftingSlot(8, stack).withToStorage(true));
            helper.assertTrue(!take(fixture, 1).done() && storage.getCrafting().craftingInput().get(8).getCount() == 35,
                "无自动补料时按现有输入量分块处理");
            helper.assertTrue(take(fixture, 1).done() && fixture.count(ItemResource.of(Items.OAK_PLANKS)) == 396,
                "第二块应恰好消耗剩余原料");
        }
        helper.succeed();
    }

    private static void unstackable(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            var state = CraftingStorage.EMPTY.withAutoFill(true).withToStorage(true);
            for (int slot : new int[]{0, 1, 2}) state = state.withCraftingSlot(slot, new ItemStack(Items.DIAMOND, 64));
            for (int slot : new int[]{4, 7}) state = state.withCraftingSlot(slot, new ItemStack(Items.STICK, 64));
            StorageCraftingExecutionTests.storage(helper, fixture).setCrafting(state);
            helper.assertTrue(take(fixture, 1).done() && fixture.count(ItemResource.of(Items.DIAMOND_PICKAXE)) == 1,
                "不可堆叠产物一次点击只能合成一件");
        }
        helper.succeed();
    }

    private static RecipeHolder<CraftingRecipe> recipe(String name, Item input, Item output, Item remainder) {
        return new RecipeHolder<>(ResourceKey.create(Registries.RECIPE, AnvilCraft.of(name)), new BatchRecipe(input, output, remainder));
    }

    private static void lock(GameTestHelper helper) {
        var manager = helper.getLevel().getServer().getRecipeManager();
        var old = manager.recipeMap();
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            manager.recipes = RecipeMap.create(List.of(recipe("port_batch_first", Items.SUGAR, Items.DIAMOND, Items.PRISMARINE_SHARD),
                recipe("port_batch_second", Items.PRISMARINE_SHARD, Items.EMERALD, Items.AIR)));
            var storage = StorageCraftingExecutionTests.storage(helper, fixture);
            storage.setCrafting(CraftingStorage.EMPTY.withCraftingSlot(8, new ItemStack(Items.SUGAR)).withToStorage(true));
            helper.assertTrue(take(fixture, 1).done() && fixture.count(ItemResource.of(Items.DIAMOND)) == 1
                && fixture.count(ItemResource.of(Items.EMERALD)) == 0, "批量操作不得把余料继续用于另一份配方");
            helper.assertTrue(storage.getCrafting().craftingInput().get(8).is(Items.PRISMARINE_SHARD), "不匹配首轮配方的余料应保留");
        } finally {
            manager.recipes = old;
        }
        helper.succeed();
    }

    private static void catalyst(GameTestHelper helper) {
        var manager = helper.getLevel().getServer().getRecipeManager();
        var old = manager.recipeMap();
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            manager.recipes = RecipeMap.create(List.of(recipe("port_batch_catalyst", Items.SUGAR, Items.DIAMOND, Items.SUGAR)));
            var storage = StorageCraftingExecutionTests.storage(helper, fixture);
            storage.setCrafting(CraftingStorage.EMPTY.withCraftingSlot(8, new ItemStack(Items.SUGAR, 5))
                .withAutoFill(true).withToStorage(true));
            helper.assertTrue(take(fixture, 100).done() && fixture.count(ItemResource.of(Items.DIAMOND)) == 1,
                "不消耗原料的配方必须在生成一份后停止");
            helper.assertTrue(drop(fixture, true).changed() && dropped(fixture, Items.DIAMOND) == 1,
                "Ctrl+Q 同样必须限制不消耗配方");
            helper.assertTrue(storage.getCrafting().craftingInput().get(8).getCount() == 5, "催化剂不能丢失");
        } finally {
            manager.recipes = old;
        }
        helper.succeed();
    }

    private static void capacity(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            var storage = StorageCraftingExecutionTests.storage(helper, fixture);
            storage.setCrafting(CraftingStorage.EMPTY.withCraftingSlot(8, new ItemStack(Items.OAK_LOG)));
            for (int i = 0; i < 36; i++) fixture.player().getInventory().setItem(i, new ItemStack(Items.STONE, 64));
            fixture.player().getInventory().setItem(0, new ItemStack(Items.OAK_PLANKS, 62));
            fixture.player().containerMenu.setCarried(new ItemStack(Items.DIAMOND));
            var result = take(fixture, 1);
            helper.assertTrue(result.done() && !result.changed() && fixture.player().getInventory().getItem(0).getCount() == 62,
                "只能放下部分背包产物时必须回滚，不能复制已有堆叠");
            helper.assertTrue(storage.getCrafting().craftingInput().get(8).is(Items.OAK_LOG)
                && fixture.count(ItemResource.of(Items.OAK_PLANKS)) == 0, "背包模式不能偷偷用仓储兜底");
        }
        helper.succeed();
    }

    private static void partial(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            var storage = StorageCraftingExecutionTests.storage(helper, fixture);
            storage.setCrafting(CraftingStorage.EMPTY.withCraftingSlot(8, new ItemStack(Items.HONEY_BOTTLE, 2)).withToStorage(true));
            var handler = (TypeLimitItemStacksResourceHandler) fixture.items();
            int capacity = TypeLimitItemStacksResourceHandler.computeCount(ItemResource.of(Items.SUGAR), handler.getSpaceSize());
            fixture.stock(ItemResource.of(Items.SUGAR), capacity - 1);
            for (int i = 0; i < 36; i++) fixture.player().getInventory().setItem(i, new ItemStack(Items.STONE, 64));
            fixture.player().containerMenu.setCarried(new ItemStack(Items.DIAMOND));
            var result = take(fixture, 1);
            helper.assertTrue(result.done() && result.changed() && fixture.count(ItemResource.of(Items.SUGAR)) == capacity
                && dropped(fixture, Items.SUGAR) == 2, "部分存入后的其余产物必须落地并立即结束");
            helper.assertTrue(storage.getCrafting().craftingInput().get(8).getCount() == 1, "不能继续消耗下一份材料");
        }
        helper.succeed();
    }

    private static void throwResult(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            var storage = StorageCraftingExecutionTests.storage(helper, fixture);
            storage.setCrafting(CraftingStorage.EMPTY.withCraftingSlot(8, new ItemStack(Items.HONEY_BOTTLE)).withAutoFill(true));
            fixture.stock(ItemResource.of(Items.HONEY_BOTTLE), 100);
            fixture.player().containerMenu.setCarried(new ItemStack(Items.STICK));
            helper.assertTrue(!drop(fixture, true).changed(), "指针非空时不能丢出合成结果");
            fixture.player().containerMenu.setCarried(ItemStack.EMPTY);
            helper.assertTrue(drop(fixture, false).changed() && dropped(fixture, Items.SUGAR) == 3, "Q 仅丢出一次产物");
            helper.assertTrue(drop(fixture, true).changed() && dropped(fixture, Items.SUGAR) == 66, "Ctrl+Q 应额外丢出约一组产物");
            helper.assertTrue(storage.getCrafting().craftingInput().get(8).is(Items.HONEY_BOTTLE)
                && fixture.count(ItemResource.of(Items.HONEY_BOTTLE)) == 78, "丢出后补料和余料必须守恒");
        }
        helper.succeed();
    }

    private static void stonecutter(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            var storage = StorageCraftingExecutionTests.storage(helper, fixture);
            storage.setCrafting(CraftingStorage.EMPTY.withStonecutterInput(new ItemStack(Items.STONE))
                .withAutoFill(true).withToStorage(true));
            fixture.stock(ItemResource.of(Items.STONE), 300);
            fixture.authorize();
            var choices = StorageServerStub.craftingStonecutterRecipes(fixture.playerId(), fixture.core().asLong());
            int selected = -1;
            for (int i = 0; i < choices.size(); i++) {
                if (choices.get(i).is(Items.STONE_SLAB)) selected = i;
            }
            helper.assertTrue(selected >= 0, "石台阶切石配方必须存在");
            storage.setCrafting(storage.getCrafting().withStonecutterSelected(selected));
            fixture.authorize();
            var first = StorageServerStub.craftingTakeAll(fixture.playerId(), fixture.core().asLong(), true, 4);
            helper.assertTrue(!first.done() && first.refilledSlots() == 1 && fixture.count(ItemResource.of(Items.STONE_SLAB)) == 128,
                "切石预算不能在同一分块内重复重置");
            fixture.authorize();
            var second = StorageServerStub.craftingTakeAll(fixture.playerId(), fixture.core().asLong(), true, 1);
            helper.assertTrue(second.done() && fixture.count(ItemResource.of(Items.STONE_SLAB)) == 256,
                "切石续块应遵守最初倍率，且在预算末尾结束");
        }
        helper.succeed();
    }

    private static void session(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            var storage = StorageCraftingExecutionTests.storage(helper, fixture);
            storage.setCrafting(CraftingStorage.EMPTY.withCraftingSlot(8, new ItemStack(Items.OAK_LOG))
                .withAutoFill(true).withToStorage(true));
            fixture.stock(ItemResource.of(Items.OAK_LOG), 400);
            helper.assertTrue(!take(fixture, 8).done(), "大预算应留下待续会话");
            storage.getCrafting().craftingInput().get(8).setCount(2);
            var stale = take(fixture, 1);
            helper.assertTrue(stale.done() && !stale.changed() && fixture.count(ItemResource.of(Items.OAK_PLANKS)) == 256,
                "会话快照不能因共享物品栈被修改而失去变化检测");
            helper.assertTrue(!take(fixture, 8).done(), "新点击可以建立新会话");
            fixture.authorize();
            StorageServerStub.craftingSetOptions(fixture.playerId(), fixture.core().asLong(), true, true);
            helper.assertTrue(take(fixture, 1).done() && fixture.count(ItemResource.of(Items.OAK_PLANKS)) == 576,
                "设置更新后必须取消旧预算，不能继续未完成的点击");
            helper.assertTrue(!take(fixture, 8).done(), "再次建立待续会话");
            StorageServerStub.remove(fixture.playerId());
            helper.assertTrue(take(fixture, 1).done(), "玩家状态移除后不能遗留批量会话");
        }
        helper.succeed();
    }

    private record BatchRecipe(Item inputItem, Item output, Item remainder) implements CraftingRecipe {
        @Override
        public boolean matches(CraftingInput input, Level level) {
            return input.size() == 1 && input.getItem(0).is(this.inputItem);
        }

        @Override
        public ItemStack assemble(CraftingInput input) {
            return new ItemStack(this.output);
        }

        @Override
        public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
            ItemStack rest = this.remainder == this.inputItem ? input.getItem(0).copyWithCount(1)
                : this.remainder == Items.AIR ? ItemStack.EMPTY : new ItemStack(this.remainder);
            return NonNullList.of(ItemStack.EMPTY, rest);
        }

        @Override
        public RecipeSerializer<BatchRecipe> getSerializer() {
            return new RecipeSerializer<>(MapCodec.unit(this), StreamCodec.unit(this));
        }

        @Override
        public CraftingBookCategory category() {
            return CraftingBookCategory.MISC;
        }

        @Override
        public boolean showNotification() {
            return false;
        }

        @Override
        public String group() {
            return "";
        }

        @Override
        public PlacementInfo placementInfo() {
            return PlacementInfo.createFromOptionals(List.of());
        }
    }
}
