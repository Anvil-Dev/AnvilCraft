package dev.dubhe.anvilcraft.porting;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.TypeLimitItemStacksResourceHandler;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import dev.dubhe.anvilcraft.saved.storage.BaseStorage;
import dev.dubhe.anvilcraft.saved.storage.CraftingStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
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
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class StorageCraftingExecutionTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_craft_remainder_offsets", StorageCraftingExecutionTests::offsets,
        "port_craft_remainder_square", StorageCraftingExecutionTests::square,
        "port_craft_reject_cursor", StorageCraftingExecutionTests::rejected,
        "port_craft_shift_consumption", StorageCraftingExecutionTests::shift,
        "port_craft_refill_order", StorageCraftingExecutionTests::refill,
        "port_craft_refill_fluid", StorageCraftingExecutionTests::fluid,
        "port_craft_stonecutter_result", StorageCraftingExecutionTests::stonecutter,
        "port_craft_catalyst", StorageCraftingExecutionTests::catalyst,
        "port_craft_input_actions", StorageCraftingExecutionTests::inputActions
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_crafting_execution"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    static BaseStorage<?> storage(GameTestHelper helper, StorageFluidRpcTests.Fixture fixture) {
        var core = (StorageBlockEntity) helper.getLevel().getBlockEntity(fixture.core());
        return Storages.get().get(core.getId()).orElseThrow();
    }

    private static StorageServerStub.InteractionResult take(StorageFluidRpcTests.Fixture fixture, boolean stonecutter, boolean shift) {
        fixture.authorize();
        return StorageServerStub.craftingTakeResult(fixture.playerId(), fixture.core().asLong(), stonecutter, shift);
    }

    private static void offsets(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            var storage = storage(helper, fixture);
            for (int slot = 0; slot < 9; slot++) {
                storage.setCrafting(CraftingStorage.EMPTY.withCraftingSlot(slot, new ItemStack(Items.HONEY_BOTTLE)));
                fixture.player().containerMenu.setCarried(ItemStack.EMPTY);
                var result = take(fixture, false, false);
                helper.assertTrue(result.changed() && result.carried().is(Items.SUGAR) && result.carried().getCount() == 3,
                    "任意偏移位置都必须正确产出");
                helper.assertTrue(storage.getCrafting().craftingInput().get(slot).is(Items.GLASS_BOTTLE), "剩余瓶必须返回原槽");
                helper.assertTrue(storage.getCrafting().craftingInput().stream().filter(stack -> !stack.isEmpty()).count() == 1,
                    "剩余物不能错位或复制");
            }
        }
        helper.succeed();
    }

    private static void square(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            var state = CraftingStorage.EMPTY;
            for (int slot : new int[]{4, 5, 7, 8}) state = state.withCraftingSlot(slot, new ItemStack(Items.HONEY_BOTTLE, 2));
            var storage = storage(helper, fixture);
            storage.setCrafting(state);
            helper.assertTrue(take(fixture, false, false).carried().is(Items.HONEY_BLOCK), "右下方 2×2 配方必须使用裁剪后的宽度");
            for (int slot : new int[]{4, 5, 7, 8}) {
                helper.assertTrue(storage.getCrafting().craftingInput().get(slot).getCount() == 1, "每格只能消耗一个原料");
            }
            int bottles = 0;
            for (int i = 0; i < 36; i++) {
                var stack = fixture.player().getInventory().getItem(i);
                if (stack.is(Items.GLASS_BOTTLE)) bottles += stack.getCount();
            }
            helper.assertTrue(bottles == 4, "槽内仍有原料时剩余瓶必须完整返回背包");
        }
        helper.succeed();
    }

    private static void rejected(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            var storage = storage(helper, fixture);
            storage.setCrafting(CraftingStorage.EMPTY.withCraftingSlot(8, new ItemStack(Items.HONEY_BOTTLE)));
            fixture.player().containerMenu.setCarried(new ItemStack(Items.DIAMOND));
            helper.assertTrue(!take(fixture, false, false).changed(), "异种指针不能消耗输入");
            fixture.player().containerMenu.setCarried(new ItemStack(Items.SUGAR, 62));
            helper.assertTrue(!take(fixture, false, false).changed(), "指针容量不足不能消耗输入");
            helper.assertTrue(storage.getCrafting().craftingInput().get(8).is(Items.HONEY_BOTTLE), "拒绝后原料必须保留");
            fixture.player().containerMenu.setCarried(new ItemStack(Items.SUGAR, 61));
            helper.assertTrue(take(fixture, false, false).carried().getCount() == 64, "容量恰好足够时应合并结果");
            for (int slot = 0; slot < 36; slot++) fixture.player().getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
            fixture.player().containerMenu.setCarried(new ItemStack(Items.DIAMOND));
            var handler = (TypeLimitItemStacksResourceHandler) fixture.items();
            fixture.stock(ItemResource.of(Items.SUGAR),
                TypeLimitItemStacksResourceHandler.computeCount(ItemResource.of(Items.SUGAR), handler.getSpaceSize()));
            storage.setCrafting(CraftingStorage.EMPTY.withCraftingSlot(8, new ItemStack(Items.HONEY_BOTTLE)));
            helper.assertTrue(!take(fixture, false, true).changed()
                && storage.getCrafting().craftingInput().get(8).is(Items.HONEY_BOTTLE), "所有去向都满时不能消耗原料");
            helper.assertTrue(fixture.player().getInventory().getItem(36).isEmpty() && fixture.player().getOffhandItem().isEmpty(),
                "产物不得进入装备槽或副手");
        }
        helper.succeed();
    }

    private static void shift(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            var storage = storage(helper, fixture);
            storage.setCrafting(CraftingStorage.EMPTY.withCraftingSlot(8, new ItemStack(Items.HONEY_BOTTLE)).withToStorage(true));
            helper.assertTrue(take(fixture, false, true).changed() && fixture.count(ItemResource.of(Items.SUGAR)) == 3,
                "Shift 应按设置把结果存入仓储");
            helper.assertTrue(storage.getCrafting().craftingInput().get(8).is(Items.GLASS_BOTTLE), "Shift 同样必须消耗一次输入");
            helper.assertTrue(!take(fixture, false, true).changed() && fixture.count(ItemResource.of(Items.SUGAR)) == 3,
                "剩余物不能继续无成本产出原配方结果");
        }
        helper.succeed();
    }

    private static void refill(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            var storage = storage(helper, fixture);
            storage.setCrafting(CraftingStorage.EMPTY.withCraftingSlot(8, new ItemStack(Items.HONEY_BOTTLE)).withAutoFill(true));
            fixture.player().getInventory().setItem(12, new ItemStack(Items.HONEY_BOTTLE));
            fixture.stock(ItemResource.of(Items.HONEY_BOTTLE), 2);
            helper.assertTrue(take(fixture, false, false).refilledSlots() == 1 << 9, "补料应报告实际槽位掩码");
            helper.assertTrue(fixture.player().getInventory().getItem(12).isEmpty()
                && fixture.count(ItemResource.of(Items.HONEY_BOTTLE)) == 2, "先取主背包，再取仓储");
            fixture.player().containerMenu.setCarried(ItemStack.EMPTY);
            take(fixture, false, false);
            helper.assertTrue(fixture.count(ItemResource.of(Items.HONEY_BOTTLE)) == 1
                && storage.getCrafting().craftingInput().get(8).is(Items.HONEY_BOTTLE), "背包耗尽后才能从仓储补一个");
        }
        helper.succeed();
    }

    private static void fluid(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            final var port = fixture.fluid(FluidResource.of(ModFluids.HONEY.get()), 250);
            var storage = storage(helper, fixture);
            storage.setCrafting(CraftingStorage.EMPTY.withCraftingSlot(8, new ItemStack(Items.HONEY_BOTTLE)).withAutoFill(true));
            helper.assertTrue(take(fixture, false, false).refilledSlots() == 1 << 9, "剩余玻璃瓶应能用端口蜂蜜补料");
            helper.assertTrue(port.getFluid().isEmpty() && storage.getCrafting().craftingInput().get(8).is(Items.HONEY_BOTTLE),
                "每瓶补料只能消耗 250 mB，且不能丢瓶");
            fixture.player().containerMenu.setCarried(ItemStack.EMPTY);
            helper.assertTrue(take(fixture, false, false).refilledSlots() == 0, "流体不足时不可继续补料");
            int bottles = 0;
            for (int i = 0; i < 36; i++) {
                var stack = fixture.player().getInventory().getItem(i);
                if (stack.is(Items.GLASS_BOTTLE)) bottles += stack.getCount();
            }
            helper.assertTrue(bottles == 1, "补料失败必须保留空瓶");
        }
        helper.succeed();
    }

    private static void stonecutter(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            var storage = storage(helper, fixture);
            storage.setCrafting(CraftingStorage.EMPTY.withStonecutterInput(new ItemStack(Items.STONE, 2)));
            fixture.authorize();
            var choices = StorageServerStub.craftingStonecutterRecipes(fixture.playerId(), fixture.core().asLong());
            int selected = choices.size() - 1;
            storage.setCrafting(storage.getCrafting().withStonecutterSelected(selected));
            var result = take(fixture, true, false);
            helper.assertTrue(ItemStack.matches(result.carried(), choices.get(selected))
                && storage.getCrafting().stonecutterInput().getCount() == 1, "切石选中结果与单次消耗必须对应");
            storage.setCrafting(storage.getCrafting().withStonecutterSelected(choices.size()));
            helper.assertTrue(!take(fixture, true, false).changed(), "越界配方选择不能产出");
        }
        helper.succeed();
    }

    private static void catalyst(GameTestHelper helper) {
        var manager = helper.getLevel().getServer().getRecipeManager();
        var previous = manager.recipeMap();
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            manager.recipes = RecipeMap.create(List.of(new RecipeHolder<>(
                ResourceKey.create(Registries.RECIPE, AnvilCraft.of("port_catalyst")), new CatalystRecipe(fixture.player()))));
            var item = new ItemStack(Items.PRISMARINE_SHARD, 5);
            item.set(DataComponents.CUSTOM_NAME, Component.literal("Reusable catalyst"));
            var storage = storage(helper, fixture);
            storage.setCrafting(CraftingStorage.EMPTY.withCraftingSlot(8, item));
            helper.assertTrue(take(fixture, false, false).carried().is(Items.DIAMOND), "催化剂配方应只执行一次");
            helper.assertTrue(ItemStack.matches(item, storage.getCrafting().craftingInput().get(8)), "催化剂数量与组件必须完整保留");
            helper.assertTrue(CommonHooks.getCraftingPlayer() == null, "配方玩家上下文必须恢复");
        } finally {
            manager.recipes = previous;
        }
        helper.succeed();
    }

    private static void inputActions(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            var storage = storage(helper, fixture);
            storage.setCrafting(CraftingStorage.EMPTY.withCraftingSlot(8, new ItemStack(Items.OAK_LOG, 4)));
            fixture.authorize();
            helper.assertTrue(!StorageServerStub.craftingCloneSlot(fixture.playerId(), fixture.core().asLong(), 9).changed(),
                "生存玩家不能克隆合成输入");
            fixture.player().containerMenu.setCarried(new ItemStack(Items.DIAMOND));
            fixture.authorize();
            helper.assertTrue(!StorageServerStub.craftingThrowSlot(fixture.playerId(), fixture.core().asLong(), 9, true).changed(),
                "指针非空时不能丢出合成输入");
            fixture.player().containerMenu.setCarried(ItemStack.EMPTY);
            fixture.authorize();
            helper.assertTrue(StorageServerStub.craftingThrowSlot(fixture.playerId(), fixture.core().asLong(), 9, false).changed()
                && storage.getCrafting().craftingInput().get(8).getCount() == 3, "Q 只能丢出一件输入");
            fixture.authorize();
            helper.assertTrue(StorageServerStub.craftingQuickMoveOut(fixture.playerId(), fixture.core().asLong(), 9)
                && storage.getCrafting().craftingInput().get(8).isEmpty(), "Shift 必须移出指定输入槽");
            int logs = 0;
            for (int i = 0; i < 36; i++) {
                var stack = fixture.player().getInventory().getItem(i);
                if (stack.is(Items.OAK_LOG)) logs += stack.getCount();
            }
            helper.assertTrue(logs == 3 && fixture.player().containerMenu.getCarried().isEmpty(), "移出输入不能占用指针或丢失数量");
        }
        helper.succeed();
    }

    private record CatalystRecipe(ServerPlayer owner) implements CraftingRecipe {
        @Override
        public boolean matches(CraftingInput input, Level level) {
            return input.size() == 1 && input.getItem(0).is(Items.PRISMARINE_SHARD);
        }

        @Override
        public ItemStack assemble(CraftingInput input) {
            return new ItemStack(Items.DIAMOND);
        }

        @Override
        public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
            if (CommonHooks.getCraftingPlayer() != this.owner) throw new IllegalStateException("配方玩家上下文缺失");
            return NonNullList.of(ItemStack.EMPTY, input.getItem(0).copyWithCount(1));
        }

        @Override
        public RecipeSerializer<CatalystRecipe> getSerializer() {
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
