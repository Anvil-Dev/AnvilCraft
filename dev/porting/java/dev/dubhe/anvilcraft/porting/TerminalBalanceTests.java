package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.TypeLimitItemStacksResourceHandler;
import dev.dubhe.anvilcraft.event.PlayerBalanceHandler;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.TerminalBinding;
import dev.dubhe.anvilcraft.network.TerminalBalanceModePacket;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import dev.dubhe.anvilcraft.saved.setting.mode.BalanceMode;
import dev.dubhe.anvilcraft.saved.storage.HyperdimensionStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class TerminalBalanceTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_balance_deposit", TerminalBalanceTests::deposit,
        "port_balance_capacity", TerminalBalanceTests::capacity,
        "port_balance_target_order", TerminalBalanceTests::targetOrder,
        "port_balance_restock", TerminalBalanceTests::restock,
        "port_balance_manual_moves", TerminalBalanceTests::manualMoves,
        "port_balance_finish_use", TerminalBalanceTests::finishUse,
        "port_balance_modes", TerminalBalanceTests::modes,
        "port_balance_guards", TerminalBalanceTests::guards
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> {
            registry.register(AnvilCraft.of(name), helper -> {
                PlayerBalanceHandler.clear();
                test.accept(helper);
            });
        }));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_terminal_balance"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static ItemStack terminal(StorageFluidRpcTests.Fixture fixture) {
        var terminal = TerminalAccessTests.bound(fixture);
        fixture.player().getInventory().setItem(0, ItemStack.EMPTY);
        fixture.player().getInventory().setItem(8, terminal);
        return terminal;
    }

    private static void use(StorageFluidRpcTests.Fixture fixture, InteractionHand hand) {
        PlayerBalanceHandler.onUseItem(new PlayerInteractEvent.RightClickItem(fixture.player(), hand));
    }

    private static void deposit(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            terminal(fixture);
            var inventory = fixture.player().getInventory();
            inventory.setItem(0, new ItemStack(Items.STONE, 32));
            inventory.setItem(9, new ItemStack(Items.STONE, 64));
            inventory.setItem(35, new ItemStack(Items.STONE, 10));
            fixture.player().setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.STONE, 5));
            var named = new ItemStack(Items.STONE, 70);
            named.set(DataComponents.CUSTOM_NAME, Component.literal("Separate components"));
            inventory.setItem(10, named);
            StorageServerStub.depositExcess(fixture.player());
            helper.assertTrue(inventory.getItem(0).getCount() == 32 && inventory.getItem(9).getCount() == 32
                && inventory.getItem(35).isEmpty() && fixture.count(ItemResource.of(Items.STONE)) == 42, "应从后向前收走超量并优先保留主手");
            helper.assertTrue(fixture.player().getOffhandItem().getCount() == 5 && inventory.getItem(10).getCount() == 64
                && fixture.count(ItemResource.of(named)) == 6, "副手不得收走，组件不同的物品必须独立保留一组");
        }
        helper.succeed();
    }

    private static void capacity(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            fixture.player().getInventory().setItem(8, new ItemStack(ModItems.SHULKER_TERMINAL.get()));
            var handler = (TypeLimitItemStacksResourceHandler) fixture.items();
            int capacity = TypeLimitItemStacksResourceHandler.computeCount(ItemResource.of(Items.STONE), handler.getSpaceSize());
            fixture.stock(ItemResource.of(Items.STONE), capacity - 3);
            fixture.player().getInventory().setItem(0, new ItemStack(Items.STONE, 64));
            fixture.player().getInventory().setItem(9, new ItemStack(Items.STONE, 5));
            StorageServerStub.depositExcess(fixture.player());
            helper.assertTrue(fixture.count(ItemResource.of(Items.STONE)) == capacity
                && fixture.player().getInventory().getItem(9).getCount() == 2, "容量不足时只扣实际存入量");
        }
        helper.succeed();
    }

    private static void targetOrder(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            terminal(fixture);
            var next = new ItemStack(ModItems.HYPERDIMENSION_TERMINAL.get());
            UUID id = UUID.randomUUID();
            next.set(ModComponents.TERMINAL_BINDING, new TerminalBinding(Optional.of(id)));
            fixture.player().getInventory().setItem(9, next);
            fixture.player().getInventory().setItem(10, next.copy());
            var other = Storages.get().getOrCreate(id, HyperdimensionStorage.class).getItems();
            try (Transaction transaction = Transaction.openRoot()) {
                other.insert(ItemResource.of(Items.STONE), 2, transaction);
                transaction.commit();
            }
            fixture.player().getInventory().setItem(0, new ItemStack(Items.STONE, 64));
            fixture.player().getInventory().setItem(11, new ItemStack(Items.STONE, 64));
            StorageServerStub.depositExcess(fixture.player());
            long count = 0;
            for (int i = 0; i < other.size(); i++) count += other.getAmountAsLong(i);
            helper.assertTrue(count == 66 && fixture.count(ItemResource.of(Items.STONE)) == 0,
                "已有同种物品的后方目标应优先于前方空存储，重复终端不得倍增存量");
        }
        helper.succeed();
    }

    private static void restock(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            terminal(fixture);
            fixture.stock(ItemResource.of(Items.STONE), 100);
            fixture.player().getInventory().setItem(0, new ItemStack(Items.STONE));
            PlayerBalanceHandler.tick(fixture.player());
            use(fixture, InteractionHand.MAIN_HAND);
            fixture.player().getInventory().setItem(0, ItemStack.EMPTY);
            PlayerBalanceHandler.tick(fixture.player());
            helper.assertTrue(fixture.player().getMainHandItem().getCount() == 64 && fixture.count(ItemResource.of(Items.STONE)) == 36,
                "主动耗尽后应补满一组并真实扣库");
            use(fixture, InteractionHand.MAIN_HAND);
            fixture.player().getInventory().setItem(0, ItemStack.EMPTY);
            PlayerBalanceHandler.tick(fixture.player());
            helper.assertTrue(fixture.player().getMainHandItem().getCount() == 36 && fixture.count(ItemResource.of(Items.STONE)) == 0,
                "连续下一 tick 耗尽也应按补货后的快照判定，库存不足只取实际存量");
        }
        helper.succeed();
    }

    private static void manualMoves(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            terminal(fixture);
            fixture.stock(ItemResource.of(Items.STONE), 64);
            fixture.player().getInventory().setItem(0, new ItemStack(Items.STONE));
            PlayerBalanceHandler.tick(fixture.player());
            fixture.player().getInventory().setSelectedSlot(1);
            use(fixture, InteractionHand.MAIN_HAND);
            PlayerBalanceHandler.tick(fixture.player());
            helper.assertTrue(fixture.player().getMainHandItem().isEmpty(), "切换到空快捷槽不能补货");
            fixture.player().getInventory().setSelectedSlot(0);
            PlayerBalanceHandler.tick(fixture.player());
            fixture.player().getInventory().setItem(0, ItemStack.EMPTY);
            PlayerBalanceHandler.tick(fixture.player());
            helper.assertTrue(fixture.count(ItemResource.of(Items.STONE)) == 64, "手动移走且未使用时不能补货");
            fixture.player().getInventory().setItem(0, new ItemStack(Items.STONE));
            PlayerBalanceHandler.tick(fixture.player());
            fixture.player().inventoryMenu.setCarried(new ItemStack(Items.STONE));
            fixture.player().getInventory().setItem(0, ItemStack.EMPTY);
            use(fixture, InteractionHand.MAIN_HAND);
            PlayerBalanceHandler.tick(fixture.player());
            helper.assertTrue(fixture.count(ItemResource.of(Items.STONE)) == 64, "指针占用时不得按耗尽补货");
        }
        helper.succeed();
    }

    private static void finishUse(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            terminal(fixture);
            fixture.stock(ItemResource.of(Items.APPLE), 3);
            fixture.player().setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.APPLE));
            PlayerBalanceHandler.tick(fixture.player());
            PlayerBalanceHandler.onUseFinished(new LivingEntityUseItemEvent.Finish(fixture.player(), new ItemStack(Items.APPLE), 32,
                ItemStack.EMPTY));
            fixture.player().setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            PlayerBalanceHandler.tick(fixture.player());
            helper.assertTrue(fixture.player().getOffhandItem().getCount() == 3, "持续使用完成后的副手耗尽也应补货");
        }
        helper.succeed();
    }

    private static void modes(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var terminal = TerminalAccessTests.bound(fixture);
            terminal.set(ModComponents.TERMINAL_BALANCE_MODE, BalanceMode.OFF);
            fixture.player().getInventory().setItem(9, new ItemStack(Items.STONE, 64));
            fixture.player().getInventory().setItem(10, new ItemStack(Items.STONE, 10));
            for (int tick = 0; tick < 25; tick++) PlayerBalanceHandler.tick(fixture.player());
            helper.assertTrue(fixture.count(ItemResource.of(Items.STONE)) == 0, "关闭模式必须停止扫描");
            new TerminalBalanceModePacket(BalanceMode.DEPOSIT).handleOnServer(fixture.player());
            for (int tick = 0; tick < 19; tick++) PlayerBalanceHandler.tick(fixture.player());
            helper.assertTrue(fixture.count(ItemResource.of(Items.STONE)) == 0, "20 tick 节流不能提前存入");
            PlayerBalanceHandler.tick(fixture.player());
            helper.assertTrue(fixture.count(ItemResource.of(Items.STONE)) == 10
                && terminal.get(ModComponents.TERMINAL_BALANCE_MODE) == BalanceMode.DEPOSIT, "模式应存于手持终端并在第 20 tick 存入");
            fixture.player().getInventory().setItem(0, new ItemStack(Items.STONE));
            new TerminalBalanceModePacket(BalanceMode.SMART).handleOnServer(fixture.player());
            helper.assertTrue(!fixture.player().getMainHandItem().has(ModComponents.TERMINAL_BALANCE_MODE), "模式包不能修改非终端物品");
        }
        helper.succeed();
    }

    private static void guards(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            terminal(fixture);
            fixture.stock(ItemResource.of(Items.STONE), 64);
            fixture.player().getInventory().setItem(0, new ItemStack(Items.DIAMOND));
            StorageServerStub.restockHand(fixture.player(), new ItemStack(Items.STONE), 0);
            helper.assertTrue(fixture.player().getMainHandItem().is(Items.DIAMOND) && fixture.count(ItemResource.of(Items.STONE)) == 64,
                "不能覆盖已经有物品的手持槽");
            StorageServerStub.restockHand(fixture.player(), new ItemStack(Items.STONE), 36);
            fixture.player().getAbilities().instabuild = true;
            fixture.player().getInventory().setItem(0, ItemStack.EMPTY);
            StorageServerStub.restockHand(fixture.player(), new ItemStack(Items.STONE), 0);
            helper.assertTrue(fixture.count(ItemResource.of(Items.STONE)) == 64, "装备槽和创造玩家不能补货");
        }
        helper.succeed();
    }
}
