package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.TerminalSessions;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.TypeLimitItemStacksResourceHandler;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.UnlimitedItemStacksResourceHandler;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModFoodItems;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.PillBoxContents;
import dev.dubhe.anvilcraft.item.property.component.TerminalBinding;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import dev.dubhe.anvilcraft.saved.setting.PlayerSettings;
import dev.dubhe.anvilcraft.saved.storage.HyperdimensionStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import dev.dubhe.anvilcraft.saved.storage.category.store.CategoryMode;
import io.netty.buffer.Unpooled;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.PlayerInventoryWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class TerminalRestockTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_terminal_restock_totals", TerminalRestockTests::totals,
        "port_terminal_restock_snapshot", TerminalRestockTests::snapshot,
        "port_terminal_restock_fluid", TerminalRestockTests::fluid,
        "port_terminal_restock_rollback", TerminalRestockTests::rollback,
        "port_terminal_restock_return_capacity", TerminalRestockTests::returnCapacity,
        "port_terminal_restock_authority", TerminalRestockTests::authority,
        "port_terminal_restock_nested", TerminalRestockTests::nested
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_terminal_restock"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static UUID bind(StorageFluidRpcTests.Fixture fixture) {
        return TerminalAccessTests.bound(fixture).get(ModComponents.TERMINAL_BINDING).id().orElseThrow();
    }

    private static StorageServerStub.TerminalRestock restock(StorageFluidRpcTests.Fixture fixture, List<UUID> targets, ItemStack desired) {
        return StorageServerStub.terminalRestock(fixture.playerId(), fixture.player().containerMenu.containerId, targets, List.of(desired));
    }

    private static int count(StorageFluidRpcTests.Fixture fixture, net.minecraft.world.item.Item item) {
        int count = 0;
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = fixture.player().getInventory().getItem(slot);
            if (stack.is(item)) count += stack.getCount();
        }
        return count;
    }

    private static void totals(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            final UUID first = bind(fixture);
            final UUID second = UUID.randomUUID();
            final var storage = Storages.get().getOrCreate(second, HyperdimensionStorage.class);
            var terminal = new ItemStack(ModItems.HYPERDIMENSION_TERMINAL.get());
            terminal.set(ModComponents.TERMINAL_BINDING, new TerminalBinding(Optional.of(second)));
            fixture.player().getInventory().setItem(1, terminal);
            fixture.stock(ItemResource.of(Items.STONE), 5);
            try (Transaction transaction = Transaction.openRoot()) {
                storage.getItems().insert(ItemResource.of(Items.STONE), 7, transaction);
                transaction.commit();
            }
            fixture.player().getInventory().setItem(10, new ItemStack(Items.STONE, 2));
            final List<UUID> targets = List.of(first, second, first);
            var result = StorageServerStub.terminalRestock(fixture.playerId(), 0, targets,
                List.of(new ItemStack(Items.STONE, 10), new ItemStack(Items.STONE, 10)));
            helper.assertTrue(result.withdrawn().getFirst().getCount() == 8 && result.inventoryBefore().getFirst().getCount() == 2
                && count(fixture, Items.STONE) == 10 && fixture.count(ItemResource.of(Items.STONE)) == 0,
                "多终端与重复目标只能补足一次最终需求，不能再次扣减已有数量");
            try (Transaction transaction = Transaction.openRoot()) {
                helper.assertTrue(PlayerInventoryWrapper.of(fixture.player()).getMainSlots()
                    .extract(ItemResource.of(Items.STONE), 3, transaction) == 3, "模拟配方应真实消耗三个石头");
                transaction.commit();
            }
            helper.assertTrue(StorageServerStub.terminalReturnExcess(fixture.playerId(), targets, result)
                && count(fixture, Items.STONE) == 2, "退回只能移走本次未用余量，必须保留补库前的物品");
            helper.assertTrue(storage.getItems().getAmountAsInt(0) == 9, "余料应优先回到已有相同物品的存储");
        }
        helper.succeed();
    }

    private static void snapshot(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            final UUID target = bind(fixture);
            fixture.stock(ItemResource.of(Items.DIAMOND), 20);
            ((UnlimitedItemStacksResourceHandler) fixture.items()).set(5000, ItemResource.of(Items.EMERALD), 1);
            var settings = PlayerSettings.getSetting(fixture.player().registryAccess(), fixture.playerId());
            settings.storage().setSearchContent("@missing_namespace");
            settings.listed().forEach(entry -> entry.changeMode(CategoryMode.BLOCKLIST));
            var result = StorageServerStub.terminalSnapshot(fixture.playerId(), List.of(target, target));
            helper.assertTrue(result.items().size() == 1 && result.items().getFirst().is(Items.DIAMOND)
                && result.items().getFirst().getCount() == 20 && !result.complete(),
                "库存检查必须忽略界面搜索/分类，去重且标明扫描截断");
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), fixture.player().registryAccess());
            try {
                StorageServerStub.TerminalSnapshot.STREAM_CODEC.encode(buffer, result);
                var decoded = StorageServerStub.TerminalSnapshot.STREAM_CODEC.decode(buffer);
                helper.assertTrue(decoded.items().getFirst().getCount() == 20 && !decoded.complete(), "快照网络编码错误");
            } finally {
                buffer.release();
            }
        }
        helper.succeed();
    }

    private static void fluid(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            final UUID target = bind(fixture);
            fixture.stock(ItemResource.of(Items.BUCKET), 2);
            var water = fixture.fluid(FluidResource.of(Fluids.WATER), 2000);
            fixture.player().containerMenu = ChestMenu.threeRows(7, fixture.player().getInventory());
            fixture.player().containerMenu.setCarried(new ItemStack(Items.EMERALD, 3));
            var result = restock(fixture, List.of(target), new ItemStack(Items.WATER_BUCKET, 2));
            helper.assertTrue(result.withdrawn().getFirst().getCount() == 2 && count(fixture, Items.WATER_BUCKET) == 2
                && fixture.count(ItemResource.of(Items.BUCKET)) == 0 && water.getTank().getAmountAsInt(0) == 0,
                "空桶、流体和填好容器应一起提交");
            helper.assertTrue(fixture.player().containerMenu.getCarried().getCount() == 3, "补库不能改动当前菜单指针");
        }
        helper.succeed();
    }

    private static void rollback(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            final UUID target = bind(fixture);
            fixture.stock(ItemResource.of(Items.BUCKET), 2);
            var water = fixture.fluid(FluidResource.of(Fluids.WATER), 750);
            helper.assertTrue(restock(fixture, List.of(target), new ItemStack(Items.WATER_BUCKET)).withdrawn().isEmpty()
                && water.getTank().getAmountAsInt(0) == 750 && fixture.count(ItemResource.of(Items.BUCKET)) == 2,
                "流体不足时不能留下部分抽取或空桶消耗");
            water.getTank().set(0, FluidResource.of(Fluids.WATER), 2000);
            for (int slot = 1; slot < 36; slot++) fixture.player().getInventory().setItem(slot, new ItemStack(Items.STICK, 64));
            helper.assertTrue(restock(fixture, List.of(target), new ItemStack(Items.WATER_BUCKET)).withdrawn().isEmpty()
                && water.getTank().getAmountAsInt(0) == 2000 && fixture.count(ItemResource.of(Items.BUCKET)) == 2,
                "满背包必须回滚容器和流体，不得丢出物品");
            fixture.player().getInventory().setItem(1, new ItemStack(Items.BUCKET));
            helper.assertTrue(restock(fixture, List.of(target), new ItemStack(Items.WATER_BUCKET)).withdrawn().size() == 1
                && count(fixture, Items.WATER_BUCKET) == 1 && fixture.count(ItemResource.of(Items.BUCKET)) == 2,
                "消耗背包最后一只空桶腾出的槽位应能接收满桶");
        }
        helper.succeed();
    }

    private static void returnCapacity(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, false)) {
            fixture.player().getInventory().setItem(0, new ItemStack(ModItems.SHULKER_TERMINAL.get()));
            var targets = List.of(TerminalSessions.shulkerTerminalId(fixture.playerId()));
            var stone = ItemResource.of(Items.STONE);
            int maximum = TypeLimitItemStacksResourceHandler.computeCount(stone, 65536);
            fixture.stock(stone, maximum);
            fixture.player().getInventory().setItem(10, new ItemStack(Items.STONE, 5));
            var result = new StorageServerStub.TerminalRestock(
                List.of(new ItemStack(Items.STONE, 5)), List.of(new ItemStack(Items.STONE, 2)));
            helper.assertTrue(!StorageServerStub.terminalReturnExcess(fixture.playerId(), targets, result)
                && count(fixture, Items.STONE) == 5, "满仓退回失败必须保留背包物品");
            try (Transaction transaction = Transaction.openRoot()) {
                fixture.items().extract(stone, 2, transaction);
                transaction.commit();
            }
            helper.assertTrue(StorageServerStub.terminalReturnExcess(fixture.playerId(), targets, result)
                && count(fixture, Items.STONE) == 3 && fixture.count(stone) == maximum, "部分退回必须同时扣除相同数量");
        }
        helper.succeed();
    }

    private static void authority(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            final UUID target = bind(fixture);
            var validator = new StorageServerStub.TerminalRecipeAccessValidator();
            try {
                var method = StorageServerStub.class.getMethod("terminalSnapshot", UUID.class, List.class);
                helper.assertTrue(validator.validate(TerminalAccessTests.context(fixture), method,
                    new Object[]{fixture.playerId(), List.of(target)}), "自己持有的终端应允许查询");
                helper.assertTrue(!validator.validate(TerminalAccessTests.context(fixture), method,
                    new Object[]{fixture.playerId(), List.of(target, UUID.randomUUID())}), "不能混入无权访问的目标");
                helper.assertTrue(!validator.validate(TerminalAccessTests.context(fixture), method,
                    new Object[]{UUID.randomUUID(), List.of(target)}), "不能冒用其他玩家身份");
                var restock = StorageServerStub.class.getMethod("terminalRestock", UUID.class, int.class, List.class, List.class);
                helper.assertTrue(!validator.validate(TerminalAccessTests.context(fixture), restock,
                    new Object[]{fixture.playerId(), 8, List.of(target), List.of(new ItemStack(Items.STONE))}), "旧菜单请求应被拒绝");
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException(error);
            }
        }
        helper.succeed();
    }

    private static void nested(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            final UUID target = bind(fixture);
            var box = new ItemStack(ModItems.PILL_BOX.get());
            box.set(ModComponents.PILL_BOX_CONTENTS, new PillBoxContents(List.of(new ItemStack(ModFoodItems.PILL.get(), 4))));
            fixture.stock(ItemResource.of(box), 3);
            fixture.player().getInventory().setItem(10, box.copy());
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), fixture.player().registryAccess());
            try {
                ItemStack.STREAM_CODEC.encode(buffer, box.copyWithCount(2));
                var desired = ItemStack.STREAM_CODEC.decode(buffer);
                var result = restock(fixture, List.of(target), desired);
                helper.assertTrue(result.withdrawn().getFirst().getCount() == 1 && count(fixture, ModItems.PILL_BOX.get()) == 2,
                    "组件物品经网络往返后仍应按真实数量补库");
                buffer.clear();
                StorageServerStub.TerminalRestock.STREAM_CODEC.encode(buffer, result);
                var reply = StorageServerStub.TerminalRestock.STREAM_CODEC.decode(buffer);
                helper.assertTrue(StorageServerStub.terminalReturnExcess(fixture.playerId(), List.of(target), reply)
                    && count(fixture, ModItems.PILL_BOX.get()) == 1 && fixture.count(ItemResource.of(box)) == 3,
                    "网络编码后的退回结果必须保留原药盒并完整还库");
            } finally {
                buffer.release();
            }
        }
        helper.succeed();
    }
}
