package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.StoragePortManager;
import dev.dubhe.anvilcraft.api.TerminalSessions;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.StorageRef;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import dev.dubhe.anvilcraft.rpc.StorageTerminalServerStub;
import dev.dubhe.anvilcraft.saved.setting.PlayerSettings;
import dev.dubhe.anvilcraft.saved.storage.StorageType;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
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

import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class TerminalOverlayRpcTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_overlay_active_cursor", TerminalOverlayRpcTests::cursor,
        "port_overlay_shift_capacity", TerminalOverlayRpcTests::shift,
        "port_overlay_creative_cursor", TerminalOverlayRpcTests::creative,
        "port_overlay_fluid_insert", TerminalOverlayRpcTests::fluid,
        "port_overlay_search", TerminalOverlayRpcTests::search,
        "port_overlay_readonly_reach", TerminalOverlayRpcTests::reachability,
        "port_overlay_invalid", TerminalOverlayRpcTests::invalid
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_terminal_overlay_rpc"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static long open(StorageFluidRpcTests.Fixture fixture) {
        var terminal = TerminalAccessTests.bound(fixture);
        return TerminalSessions.open(fixture.player(), terminal.get(ModComponents.TERMINAL_BINDING).id().orElseThrow());
    }

    private static StorageServerStub.InteractionResult take(
        StorageFluidRpcTests.Fixture fixture, long token, int button, ItemStack carried
    ) {
        TerminalAccessTests.authorize(fixture, token);
        return StorageServerStub.terminalTake(fixture.playerId(), token, 0, button, carried);
    }

    private static void cursor(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            final long token = open(fixture);
            fixture.stock(ItemResource.of(Items.STONE), 70);
            var player = fixture.player();
            player.inventoryMenu.setCarried(new ItemStack(Items.DIAMOND, 4));
            player.containerMenu = ChestMenu.threeRows(7, player.getInventory());
            helper.assertTrue(take(fixture, token, 1, new ItemStack(Items.DIRT, 64)).carried().getCount() == 1,
                "生存取出应使用活动菜单空指针，忽略伪造客户端指针");
            helper.assertTrue(player.containerMenu.getCarried().is(Items.STONE) && player.inventoryMenu.getCarried().is(Items.DIAMOND),
                "箱子指针与背包指针不能混用");
            helper.assertTrue(!take(fixture, token, 0, ItemStack.EMPTY).changed() && fixture.count(ItemResource.of(Items.STONE)) == 69,
                "指针已有物品时不能覆盖或继续扣库");
            TerminalAccessTests.authorize(fixture, token);
            var result = StorageServerStub.terminalInsert(fixture.playerId(), token, new ItemStack(Items.DIAMOND, 64));
            helper.assertTrue(result.carried().isEmpty() && fixture.count(ItemResource.of(Items.STONE)) == 70
                && fixture.count(ItemResource.of(Items.DIAMOND)) == 0, "生存存入必须使用服务端实际指针");
        }
        helper.succeed();
    }

    private static void shift(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            final long token = open(fixture);
            fixture.stock(ItemResource.of(Items.DIAMOND), 100);
            var player = fixture.player();
            for (int i = 1; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.STONE, 64));
            player.getInventory().setItem(9, new ItemStack(Items.DIAMOND, 63));
            player.containerMenu = ChestMenu.threeRows(7, player.getInventory());
            player.containerMenu.setCarried(new ItemStack(Items.EMERALD, 4));
            TerminalAccessTests.authorize(fixture, token);
            var result = StorageServerStub.terminalTakeToInventory(fixture.playerId(), token, 0, 0);
            helper.assertTrue(result.changed() && player.getInventory().getItem(9).getCount() == 64
                && fixture.count(ItemResource.of(Items.DIAMOND)) == 99, "Shift 仅移动背包实际能容纳的量");
            TerminalAccessTests.authorize(fixture, token);
            helper.assertTrue(!StorageServerStub.terminalTakeToInventory(fixture.playerId(), token, 0, 0).changed()
                && player.containerMenu.getCarried().getCount() == 4, "满背包不得取到指针或丢出");
        }
        helper.succeed();
    }

    private static void creative(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            final long token = open(fixture);
            fixture.stock(ItemResource.of(Items.STONE), 70);
            var player = fixture.player();
            player.getAbilities().instabuild = true;
            player.inventoryMenu.setCarried(new ItemStack(Items.DIAMOND, 7));
            helper.assertTrue(take(fixture, token, 0, ItemStack.EMPTY).carried().getCount() == 64
                && fixture.count(ItemResource.of(Items.STONE)) == 6, "创造指针本地已清空时应替换过期服务端镜像");
            helper.assertTrue(!take(fixture, token, 0, new ItemStack(Items.DIRT)).changed(), "创造客户端指针非空时拒绝覆盖");
            TerminalAccessTests.authorize(fixture, token);
            var result = StorageServerStub.terminalInsert(fixture.playerId(), token, new ItemStack(Items.IRON_INGOT, 5));
            helper.assertTrue(result.carried().isEmpty() && fixture.count(ItemResource.of(Items.IRON_INGOT)) == 5,
                "创造模式应存入当前客户端指针，而不是过期服务端镜像");
        }
        helper.succeed();
    }

    private static void fluid(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            final long token = open(fixture);
            final var port = fixture.fluid(FluidResource.of(Fluids.WATER), 1000);
            fixture.player().containerMenu = ChestMenu.threeRows(7, fixture.player().getInventory());
            fixture.player().containerMenu.setCarried(new ItemStack(Items.WATER_BUCKET, 2));
            TerminalAccessTests.authorize(fixture, token);
            var result = StorageServerStub.terminalInsert(fixture.playerId(), token, ItemStack.EMPTY);
            helper.assertTrue(result.carried().isEmpty() && port.getFluid().getAmount() == 3000
                && fixture.count(ItemResource.of(Items.BUCKET)) == 2, "桶应优先倾倒，空容器完整返还仓储");
            port.getTank().set(0, FluidResource.of(Fluids.WATER), 127500);
            fixture.player().containerMenu.setCarried(new ItemStack(Items.WATER_BUCKET));
            TerminalAccessTests.authorize(fixture, token);
            StorageServerStub.terminalInsert(fixture.playerId(), token, ItemStack.EMPTY);
            helper.assertTrue(port.getFluid().getAmount() == 127500 && fixture.count(ItemResource.of(Items.WATER_BUCKET)) == 1,
                "端口容量不足时应保留桶内容并作为物品存入");
        }
        helper.succeed();
    }

    private static void search(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            final long token = open(fixture);
            fixture.stock(ItemResource.of(Items.STONE), 70);
            fixture.stock(ItemResource.of(Items.OAK_PLANKS), 3);
            var setting = PlayerSettings.getSetting(helper.getLevel().registryAccess(), fixture.playerId()).storage();
            setting.setSearchContent("keep main search");
            TerminalAccessTests.authorize(fixture, token);
            helper.assertTrue(StorageServerStub.terminalReorder(fixture.playerId(), token, "@minecraft").size() == 2,
                "浮窗命名空间搜索应独立生效");
            TerminalAccessTests.authorize(fixture, token);
            helper.assertTrue(StorageServerStub.terminalReorder(fixture.playerId(), token, "#minecraft:planks").size() == 1,
                "标签搜索应沿用仓储规则");
            helper.assertTrue(setting.getSearchContent().equals("keep main search"), "浮窗不能覆盖主界面搜索设置");
        }
        helper.succeed();
    }

    private static void reachability(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            fixture.player().getInventory().setItem(0, new ItemStack(ModItems.SHULKER_TERMINAL.get()));
            var box = new ItemStack(ModBlocks.SHULKER_CONTAINER.asItem());
            box.set(ModComponents.STORAGE, new StorageRef(StorageType.SHULKER_CONTAINER));
            fixture.player().getInventory().setItem(1, box);
            helper.assertTrue(StorageTerminalServerStub.isTerminalReachable(fixture.playerId(),
                TerminalSessions.shulkerTerminalId(fixture.playerId())),
                "查询可读取世界中的可达集装箱");
            helper.assertTrue(box.get(ModComponents.STORAGE).id().isEmpty(), "悬停查询不能为随身集装箱分配 UUID");
        }
        helper.succeed();
    }

    private static void invalid(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            final long token = open(fixture);
            fixture.stock(ItemResource.of(Items.STONE), 5);
            for (int slot : new int[]{-1, Integer.MAX_VALUE, StoragePortManager.FLUID_SLOT_BASE}) {
                TerminalAccessTests.authorize(fixture, token);
                helper.assertTrue(!StorageServerStub.terminalTake(fixture.playerId(), token, slot, 0, ItemStack.EMPTY).changed(),
                    "非法槽位不得扣库");
            }
            helper.assertTrue(!take(fixture, token, 2, ItemStack.EMPTY).changed() && fixture.count(ItemResource.of(Items.STONE)) == 5,
                "中键不能作为普通取出请求");
        }
        helper.succeed();
    }
}
