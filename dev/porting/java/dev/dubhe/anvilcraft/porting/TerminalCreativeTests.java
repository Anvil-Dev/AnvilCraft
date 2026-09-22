package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.TerminalSessions;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.TypeLimitItemStacksResourceHandler;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModFoodItems;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.PillBoxContents;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import dev.dubhe.anvilcraft.saved.setting.PlayerSettings;
import dev.dubhe.anvilcraft.saved.setting.mode.OrderMode;
import dev.dubhe.anvilcraft.saved.setting.mode.SortMode;
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
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class TerminalCreativeTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_terminal_creative_roundtrip", TerminalCreativeTests::roundtrip,
        "port_terminal_creative_capacity", TerminalCreativeTests::capacity,
        "port_terminal_creative_fallback", TerminalCreativeTests::fallback,
        "port_terminal_creative_stale", TerminalCreativeTests::stale,
        "port_terminal_creative_authority", TerminalCreativeTests::authority,
        "port_terminal_creative_sort", TerminalCreativeTests::sort,
        "port_terminal_creative_nested", TerminalCreativeTests::nested
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_terminal_creative"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static ItemStack creative(StorageFluidRpcTests.Fixture fixture) {
        var terminal = TerminalAccessTests.bound(fixture).copy();
        fixture.player().getInventory().setItem(0, ItemStack.EMPTY);
        fixture.player().gameMode.changeGameModeForPlayer(GameType.CREATIVE);
        return terminal;
    }

    private static UUID target(StorageFluidRpcTests.Fixture fixture, ItemStack terminal) {
        if (terminal.is(ModItems.SHULKER_TERMINAL)) return TerminalSessions.shulkerTerminalId(fixture.playerId());
        return terminal.get(ModComponents.TERMINAL_BINDING).id().orElseThrow();
    }

    private static StorageServerStub.InteractionResult transfer(StorageFluidRpcTests.Fixture fixture, ItemStack terminal,
                                                                int slot, boolean extract, ItemStack expected) {
        return StorageServerStub.creativeTerminalTransfer(fixture.playerId(), target(fixture, terminal), slot, extract, expected, terminal);
    }

    private static void roundtrip(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            final var terminal = creative(fixture);
            fixture.stock(ItemResource.of(Items.STONE), 130);
            var take = transfer(fixture, terminal, 10, true, ItemStack.EMPTY);
            helper.assertTrue(take.changed() && ItemStack.matches(take.carried(), terminal)
                && fixture.player().getInventory().getItem(10).getCount() == 64
                && fixture.count(ItemResource.of(Items.STONE)) == 66, "客户端指针终端应原子扣库并填充服务端槽位");
            var insert = transfer(fixture, terminal, 10, false, new ItemStack(Items.STONE, 64));
            helper.assertTrue(insert.changed() && fixture.player().getInventory().getItem(10).isEmpty()
                && fixture.count(ItemResource.of(Items.STONE)) == 130, "放入应同步扣除实际背包内容");
            helper.assertTrue(fixture.player().inventoryMenu.getCarried().isEmpty(), "服务端不应伪造创造模式指针");
            var offhand = transfer(fixture, terminal, 45, true, ItemStack.EMPTY);
            helper.assertTrue(offhand.changed() && fixture.player().getOffhandItem().getCount() == 64, "副手槽位映射错误");
        }
        helper.succeed();
    }

    private static void capacity(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, false)) {
            fixture.player().gameMode.changeGameModeForPlayer(GameType.CREATIVE);
            var terminal = new ItemStack(ModItems.SHULKER_TERMINAL.get());
            var stone = ItemResource.of(Items.STONE);
            int maximum = TypeLimitItemStacksResourceHandler.computeCount(stone, 65536);
            fixture.stock(stone, maximum - 3);
            fixture.player().getInventory().setItem(10, new ItemStack(Items.STONE, 10));
            helper.assertTrue(transfer(fixture, terminal, 10, false, new ItemStack(Items.STONE, 10)).changed()
                && fixture.player().getInventory().getItem(10).getCount() == 7 && fixture.count(stone) == maximum,
                "容量不足只能存入可接收部分，槽位必须保留剩余数量");
            helper.assertTrue(!transfer(fixture, terminal, 10, false, new ItemStack(Items.STONE, 7)).changed()
                && fixture.player().getInventory().getItem(10).getCount() == 7, "满仓不能吞掉物品");
        }
        helper.succeed();
    }

    private static void fallback(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            final var terminal = creative(fixture);
            var fluid = fixture.fluid(FluidResource.of(Fluids.WATER), 1000);
            var result = transfer(fixture, terminal, 10, true, ItemStack.EMPTY);
            helper.assertTrue(!result.changed() && result.carried().isEmpty()
                && ItemStack.matches(fixture.player().getInventory().getItem(10), terminal), "只有流体时应放回终端而非索引流体伪槽");
            helper.assertTrue(fluid.getTank().getAmountAsInt(0) == 1000, "物品提取不能消耗端口流体");
        }
        helper.succeed();
    }

    private static void stale(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            final var terminal = creative(fixture);
            fixture.stock(ItemResource.of(Items.STONE), 70);
            fixture.player().getInventory().setItem(10, new ItemStack(Items.DIAMOND, 3));
            helper.assertTrue(!transfer(fixture, terminal, 10, true, ItemStack.EMPTY).changed()
                && fixture.count(ItemResource.of(Items.STONE)) == 70, "迟到请求不能覆盖已占用的目标槽");
            helper.assertTrue(!transfer(fixture, terminal, 10, false, new ItemStack(Items.DIAMOND, 4)).changed()
                && fixture.player().getInventory().getItem(10).getCount() == 3
                && fixture.count(ItemResource.of(Items.DIAMOND)) == 0, "伪造数量不能存入或扣除背包");
            fixture.player().getInventory().setItem(10, terminal.copy());
            helper.assertTrue(!transfer(fixture, terminal, 10, false, terminal.copy()).changed()
                && ItemStack.matches(fixture.player().getInventory().getItem(10), terminal), "超维仓储不能吞入终端自身");
        }
        helper.succeed();
    }

    private static boolean allowed(StorageFluidRpcTests.Fixture fixture, Object[] args) {
        try {
            return new StorageServerStub.CreativeTerminalAccessValidator().validate(TerminalAccessTests.context(fixture),
                StorageServerStub.class.getMethod("creativeTerminalTransfer", UUID.class, UUID.class, int.class,
                    boolean.class, ItemStack.class, ItemStack.class), args);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    private static void authority(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            final var terminal = creative(fixture);
            Object[] args = {fixture.playerId(), target(fixture, terminal), 10, true, ItemStack.EMPTY, terminal};
            helper.assertTrue(allowed(fixture, args), "创造客户端指针终端应通过校验");
            args[0] = UUID.randomUUID();
            helper.assertTrue(!allowed(fixture, args), "不得冒用玩家 UUID");
            args[0] = fixture.playerId();
            args[1] = UUID.randomUUID();
            helper.assertTrue(!allowed(fixture, args), "终端绑定与目标必须一致");
            args[1] = target(fixture, terminal);
            for (int index : new int[]{-1, 0, 1, 46, Integer.MAX_VALUE}) {
                args[2] = index;
                helper.assertTrue(!allowed(fixture, args), "不能访问非背包或越界槽位");
            }
            args[2] = 10;
            fixture.player().containerMenu = ChestMenu.threeRows(7, fixture.player().getInventory());
            helper.assertTrue(!allowed(fixture, args), "打开其它菜单时不能接受创造背包请求");
            fixture.player().containerMenu = fixture.player().inventoryMenu;
            fixture.player().gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
            helper.assertTrue(!allowed(fixture, args), "生存玩家不能调用客户端自报终端接口");
        }
        helper.succeed();
    }

    private static void nested(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            final var terminal = creative(fixture);
            var box = new ItemStack(ModItems.PILL_BOX.get());
            box.set(ModComponents.PILL_BOX_CONTENTS, new PillBoxContents(List.of(new ItemStack(ModFoodItems.PILL.get(), 4))));
            final var originalResource = ItemResource.of(box);
            fixture.player().getInventory().setItem(10, box);
            ItemStack expected;
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), fixture.player().registryAccess());
            try {
                ItemStack.STREAM_CODEC.encode(buffer, box);
                expected = ItemStack.STREAM_CODEC.decode(buffer);
            } finally {
                buffer.release();
            }
            var wrong = expected.copy();
            wrong.set(ModComponents.PILL_BOX_CONTENTS, new PillBoxContents(List.of(new ItemStack(ModFoodItems.PILL.get(), 3))));
            helper.assertTrue(!transfer(fixture, terminal, 10, false, wrong).changed(), "内嵌物品数量变化必须使快照失效");
            helper.assertTrue(transfer(fixture, terminal, 10, false, expected).changed(), "网络往返后的带内容药盒不能因引用不同而被拒绝");
            helper.assertTrue(fixture.player().getInventory().getItem(10).isEmpty(), "存入后实际背包必须扣除药盒");
            helper.assertTrue(fixture.count(originalResource) == 1, "仓储必须保留原药盒完整内容且不能复制");
        }
        helper.succeed();
    }

    private static void sort(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            final var terminal = creative(fixture);
            fixture.stock(ItemResource.of(Items.STONE), 70);
            fixture.stock(ItemResource.of(Items.DIAMOND), 2);
            var setting = PlayerSettings.getSetting(fixture.player().registryAccess(), fixture.playerId()).storage();
            setting.setSort(SortMode.COUNT);
            setting.setOrder(OrderMode.SEQUENTIAL);
            setting.setSearchContent("@missing_mod");
            helper.assertTrue(transfer(fixture, terminal, 10, true, ItemStack.EMPTY).changed()
                && fixture.player().getInventory().getItem(10).is(Items.DIAMOND), "取首项遵循排序但不能被主界面搜索限制");
            fixture.player().getInventory().setItem(0, terminal);
            helper.assertTrue(StorageServerStub.extractFromTerminal(fixture.player(), target(fixture, terminal), 64,
                fixture.player().inventoryMenu.getSlot(11)).is(Items.STONE), "生存与创造应共用相同首项提取规则");
        }
        helper.succeed();
    }
}
