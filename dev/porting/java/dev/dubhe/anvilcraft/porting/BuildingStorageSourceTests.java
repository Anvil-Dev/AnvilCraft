package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.TerminalSourceManager;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.StorageRef;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import dev.dubhe.anvilcraft.saved.storage.StorageType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class BuildingStorageSourceTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_building_storage_hyper", BuildingStorageSourceTests::hyper,
        "port_building_storage_local", BuildingStorageSourceTests::local,
        "port_building_storage_shulker", BuildingStorageSourceTests::shulker,
        "port_building_storage_order", BuildingStorageSourceTests::order
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_building_storage"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static void hyper(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            var terminal = TerminalAccessTests.bound(fixture);
            player.getInventory().setItem(1, terminal.copy());
            player.setPos(fixture.core().getX() + 1000, fixture.core().getY(), fixture.core().getZ());
            fixture.stock(ItemResource.of(Items.DIAMOND), 12);
            var sources = StorageServerStub.buildingMaterialSources(player);
            var ids = StorageServerStub.buildingFluidSources(player);
            helper.assertTrue(sources.size() == 1 && ids.size() == 1
                && ids.getFirst().equals(terminal.get(ModComponents.TERMINAL_BINDING).id().orElseThrow()),
                "同一超维仓储的重复终端必须去重，远程绑定仍能供料");
            try (Transaction transaction = Transaction.openRoot()) {
                helper.assertTrue(sources.getFirst().extract(ItemResource.of(Items.DIAMOND), 3, transaction) == 3,
                    "建筑供料源必须是实际仓储处理器");
                transaction.commit();
            }
            helper.assertTrue(fixture.count(ItemResource.of(Items.DIAMOND)) == 9, "提交扣料应作用于真实仓储");
            player.getInventory().setItem(0, ItemStack.EMPTY);
            player.getInventory().setItem(1, ItemStack.EMPTY);
            helper.assertTrue(StorageServerStub.buildingMaterialSources(player).isEmpty()
                && StorageServerStub.buildingFluidSources(player).isEmpty(), "移除终端后不能继续解析旧绑定");
        }
        helper.succeed();
    }

    private static void local(GameTestHelper helper) {
        TerminalSourceManager.clear(helper.getLevel());
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            final var crate = TerminalSourceTests.place(helper, ModBlocks.LARGE_CRATE.get(), new BlockPos(9, 1, 3));
            var player = fixture.player();
            player.getInventory().setItem(0, new ItemStack(ModItems.LOCAL_TERMINAL.get()));
            var pos = crate.getBlockPos();
            player.setPos(pos.getX() + 32.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            helper.assertTrue(StorageServerStub.buildingFluidSources(player).equals(List.of(crate.getId())),
                "本地建筑供料应包含 32 格边界");
            player.setPos(pos.getX() + 32.51, pos.getY() + 0.5, pos.getZ() + 0.5);
            helper.assertTrue(StorageServerStub.buildingFluidSources(player).isEmpty()
                && StorageServerStub.buildingMaterialSources(player).isEmpty(), "越界后两种供料都必须立即失效");
            player.getInventory().setItem(0, new ItemStack(ModItems.HYPERDIMENSION_TERMINAL.get()));
            helper.assertTrue(StorageServerStub.buildingMaterialSources(player).isEmpty(), "未绑定的超维终端不能变成材料源");
        }
        helper.succeed();
    }

    private static void shulker(GameTestHelper helper) {
        TerminalSourceManager.clear(helper.getLevel());
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            final var world = TerminalSourceTests.place(helper, ModBlocks.SHULKER_CONTAINER.get(), new BlockPos(9, 1, 3));
            var player = fixture.player();
            player.getInventory().setItem(0, new ItemStack(ModItems.SHULKER_TERMINAL.get()));
            var empty = ModBlocks.SHULKER_CONTAINER.asStack();
            empty.set(ModComponents.STORAGE, new StorageRef(StorageType.SHULKER_CONTAINER));
            var carried = ModBlocks.SHULKER_CONTAINER.asStack();
            UUID id = UUID.randomUUID();
            carried.set(ModComponents.STORAGE, new StorageRef(StorageType.SHULKER_CONTAINER, id));
            player.getInventory().setItem(1, empty);
            player.getInventory().setItem(2, carried);
            helper.assertTrue(StorageServerStub.buildingFluidSources(player).equals(List.of(id))
                && empty.get(ModComponents.STORAGE).id().isEmpty(), "预检不应授予空容器身份，已有随身容器应优先于世界目标");
            player.getInventory().setItem(2, ItemStack.EMPTY);
            helper.assertTrue(StorageServerStub.buildingFluidSources(player).equals(List.of(world.getId())),
                "没有已绑定随身容器时应回退附近世界容器");
            var pos = world.getBlockPos();
            player.setPos(pos.getX() + 64.51, pos.getY() + 0.5, pos.getZ() + 0.5);
            helper.assertTrue(StorageServerStub.buildingMaterialSources(player).isEmpty()
                && empty.get(ModComponents.STORAGE).id().isEmpty(), "超出 64 格后不能通过空随身容器虚构存储");
        }
        helper.succeed();
    }

    private static void order(GameTestHelper helper) {
        TerminalSourceManager.clear(helper.getLevel());
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            final var crate = TerminalSourceTests.place(helper, ModBlocks.LARGE_CRATE.get(), new BlockPos(9, 1, 3));
            var hyper = TerminalAccessTests.bound(fixture);
            final UUID hyperId = hyper.get(ModComponents.TERMINAL_BINDING).id().orElseThrow();
            var player = fixture.player();
            player.getInventory().setItem(0, new ItemStack(ModItems.LOCAL_TERMINAL.get()));
            player.getInventory().setItem(1, hyper);
            player.getInventory().setItem(2, hyper.copy());
            helper.assertTrue(StorageServerStub.buildingFluidSources(player).equals(List.of(crate.getId(), hyperId)),
                "建筑材料顺序应跟随随身终端顺序，不得套用超维优先的自动补货排序");
            helper.assertTrue(StorageServerStub.buildingMaterialSources(player).size() == 2, "两种建筑源应使用同一去重规则");
        }
        helper.succeed();
    }
}
