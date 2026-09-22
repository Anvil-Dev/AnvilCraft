package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.TerminalSessions;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.UnlimitedItemStacksResourceHandler;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.StorageRef;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import dev.dubhe.anvilcraft.saved.setting.PlayerSettings;
import dev.dubhe.anvilcraft.saved.storage.ShulkerContainerStorage;
import dev.dubhe.anvilcraft.saved.storage.StorageType;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import dev.dubhe.anvilcraft.saved.storage.category.store.CategoryMode;
import io.netty.buffer.Unpooled;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class StorageUnfilteredTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_unfiltered_pages", StorageUnfilteredTests::pages,
        "port_unfiltered_filters", StorageUnfilteredTests::filters,
        "port_unfiltered_long", StorageUnfilteredTests::longCount,
        "port_unfiltered_fluid", StorageUnfilteredTests::fluid,
        "port_unfiltered_target", StorageUnfilteredTests::target
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_unfiltered"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static StorageServerStub.ContentsPage page(StorageFluidRpcTests.Fixture fixture, int offset) {
        fixture.authorize();
        return StorageServerStub.craftingStorageContents(fixture.playerId(), fixture.core().asLong(), offset);
    }

    private static void pages(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var items = (UnlimitedItemStacksResourceHandler) fixture.items();
            for (int index = 0; index < 257; index++) {
                var stack = new ItemStack(Items.STONE);
                stack.set(DataComponents.CUSTOM_NAME, Component.literal("page-" + index));
                items.set(index * 2, ItemResource.of(stack), index + 1);
            }
            var first = page(fixture, 0);
            var second = page(fixture, 256);
            helper.assertTrue(first.items().size() == 256 && !first.last() && second.items().size() == 1 && second.last(),
                "分页应按非空合并条目计数，跳过物理空槽");
            helper.assertTrue(second.items().getFirst().count() == 257 && first.version() == second.version(), "页偏移或版本错误");
            helper.assertTrue(page(fixture, -1).items().isEmpty() && page(fixture, Integer.MAX_VALUE).last(), "非法偏移不能溢出或重放第一页");
            fixture.stock(ItemResource.of(Items.DIAMOND), 1);
            helper.assertTrue(page(fixture, 256).version() > first.version(), "分页期间库存变化必须带出不同版本");
        }
        helper.succeed();
    }

    private static void filters(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            fixture.stock(ItemResource.of(Items.DIAMOND), 9);
            var setting = PlayerSettings.getSetting(fixture.player().registryAccess(), fixture.playerId());
            setting.storage().setSearchContent("@missing");
            setting.listed().forEach(entry -> entry.changeMode(CategoryMode.BLOCKLIST));
            helper.assertTrue(fixture.order().isEmpty(), "对照的正常页面应被筛选为空");
            var result = page(fixture, 0);
            helper.assertTrue(result.items().size() == 1 && result.items().getFirst().stack().toStack().is(Items.DIAMOND)
                && result.items().getFirst().count() == 9, "搜索或分类不能影响配方材料快照");
            helper.assertTrue(setting.storage().getSearchContent().equals("@missing"), "查询不能改写页面筛选设置");
        }
        helper.succeed();
    }

    private static void longCount(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var items = (UnlimitedItemStacksResourceHandler) fixture.items();
            items.set(0, ItemResource.of(Items.DIAMOND), Integer.MAX_VALUE);
            items.set(5000, ItemResource.of(Items.DIAMOND), Integer.MAX_VALUE);
            var result = page(fixture, 0);
            helper.assertTrue(result.items().size() == 1 && result.items().getFirst().count() == 2L * Integer.MAX_VALUE,
                "未过滤快照必须保留跨物理槽聚合的 long 数量");
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), fixture.player().registryAccess());
            try {
                StorageServerStub.ContentsPage.STREAM_CODEC.encode(buffer, result);
                var decoded = StorageServerStub.ContentsPage.STREAM_CODEC.decode(buffer);
                helper.assertTrue(decoded.storageId().equals(result.storageId())
                    && decoded.items().getFirst().count() == 2L * Integer.MAX_VALUE, "网络页不能截断数量或丢失目标身份");
            } finally {
                buffer.release();
            }
        }
        helper.succeed();
    }

    private static void fluid(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            fixture.fluid(FluidResource.of(Fluids.WATER), 2000);
            var first = page(fixture, 0);
            helper.assertTrue(first.items().isEmpty() && first.last() && first.fluids().getFirst().amount() == 2000,
                "物品为空时首页仍应携带完整流体快照");
            helper.assertTrue(page(fixture, 256).fluids().isEmpty(), "后续页不应重复累计流体");
        }
        helper.succeed();
    }

    private static void target(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            player.getInventory().setItem(0, new ItemStack(ModItems.SHULKER_TERMINAL.get()));
            UUID firstId = UUID.randomUUID();
            UUID secondId = UUID.randomUUID();
            var firstStorage = Storages.get().getOrCreate(firstId, ShulkerContainerStorage.class);
            var secondStorage = Storages.get().getOrCreate(secondId, ShulkerContainerStorage.class);
            try (Transaction transaction = Transaction.openRoot()) {
                firstStorage.getItems().insert(ItemResource.of(Items.STONE), 5, transaction);
                secondStorage.getItems().insert(ItemResource.of(Items.DIAMOND), 5, transaction);
                transaction.commit();
            }
            var container = new ItemStack(ModBlocks.SHULKER_CONTAINER.asItem());
            container.set(ModComponents.STORAGE, new StorageRef(StorageType.SHULKER_CONTAINER, firstId));
            player.getInventory().setItem(1, container);
            long token = TerminalSessions.open(player, TerminalSessions.shulkerTerminalId(player.getUUID()));
            TerminalAccessTests.authorize(fixture, token);
            var first = StorageServerStub.craftingStorageContents(fixture.playerId(), token, 0);
            container.set(ModComponents.STORAGE, new StorageRef(StorageType.SHULKER_CONTAINER, secondId));
            TerminalAccessTests.authorize(fixture, token);
            var second = StorageServerStub.craftingStorageContents(fixture.playerId(), token, 0);
            helper.assertTrue(first.version() == second.version() && !first.storageId().equals(second.storageId())
                && second.items().getFirst().stack().toStack().is(Items.DIAMOND), "版本相同的终端目标也必须按实际 ID 区分");
        }
        helper.succeed();
    }
}
