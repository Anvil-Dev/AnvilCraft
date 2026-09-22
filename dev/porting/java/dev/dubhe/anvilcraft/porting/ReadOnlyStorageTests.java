package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.itemhandler.ReadOnlyItemResourceHandler;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.UnlimitedItemStacksResourceHandler;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.inventory.RoyalSmithingMenu;
import dev.dubhe.anvilcraft.saved.storage.HyperdimensionStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class ReadOnlyStorageTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_readonly_wrapper", ReadOnlyStorageTests::wrapper,
        "port_readonly_hyper_parts", helper -> parts(helper, true, ModBlocks.HYPERDIMENSION_STORAGE_STATION.get()),
        "port_readonly_shulker_parts", helper -> parts(helper, false, ModBlocks.SHULKER_CONTAINER.get()),
        "port_readonly_template_return", ReadOnlyStorageTests::borrow,
        "port_readonly_template_blocked", ReadOnlyStorageTests::blockedReturn,
        "port_readonly_template_replaced", ReadOnlyStorageTests::replaced,
        "port_readonly_lazy_and_crate", ReadOnlyStorageTests::lazy
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_readonly_storage"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static void wrapper(GameTestHelper helper) {
        var delegate = new ItemStacksResourceHandler(1);
        var stone = ItemResource.of(Items.STONE);
        delegate.set(0, stone, 10);
        var wrapper = new ReadOnlyItemResourceHandler(delegate);
        helper.assertTrue(wrapper.size() == 1 && wrapper.getResource(0).equals(stone)
            && wrapper.getAmountAsLong(0) == 10 && wrapper.getCapacityAsLong(0, stone) == 64 && !wrapper.isValid(0, stone),
            "只读视图必须提供准确资源、数量与容量");
        try (Transaction transaction = Transaction.openRoot()) {
            helper.assertTrue(wrapper.insert(stone, 2, transaction) == 0 && wrapper.extract(stone, 2, transaction) == 0
                && wrapper.insert(0, stone, 2, transaction) == 0 && wrapper.extract(0, stone, 2, transaction) == 0,
                "按槽及聚合自动化都不能写入只读视图");
            transaction.commit();
        }
        try (Transaction transaction = Transaction.openRoot()) {
            helper.assertTrue(wrapper.extractBypass(0, stone, 1, transaction) == 1, "模板特权入口应执行真实提取");
        }
        helper.assertTrue(delegate.getAmountAsInt(0) == 10, "模拟借用必须完整回滚");
        try (Transaction transaction = Transaction.openRoot()) {
            wrapper.extractBypass(0, stone, 1, transaction);
            transaction.commit();
        }
        try (Transaction transaction = Transaction.openRoot()) {
            wrapper.insertBypass(0, stone, 1, transaction);
            transaction.commit();
        }
        helper.assertTrue(delegate.getAmountAsInt(0) == 10, "特权借还必须遵循事务且数量守恒");
        helper.succeed();
    }

    private static <P extends Enum<P>> void parts(GameTestHelper helper, boolean hyper, AbstractMultiPartBlock<P> block) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, hyper)) {
            fixture.stock(ItemResource.of(Items.STONE), 10);
            var state = block.defaultBlockState();
            for (P part : block.getParts()) {
                BlockPos pos = fixture.core().offset(block.offsetFrom(state, part));
                var unsided = helper.getLevel().getCapability(Capabilities.Item.BLOCK, pos, null);
                helper.assertTrue(unsided instanceof ReadOnlyItemResourceHandler, "每个结构部分都应解析主仓储只读能力");
                for (Direction side : Direction.values()) {
                    var handler = helper.getLevel().getCapability(Capabilities.Item.BLOCK, pos, side);
                    helper.assertTrue(handler instanceof ReadOnlyItemResourceHandler && handler.getAmountAsLong(0) == 10,
                        "各面查询必须指向同一个实际仓储");
                    try (Transaction transaction = Transaction.openRoot()) {
                        helper.assertTrue(handler.extract(ItemResource.of(Items.STONE), 1, transaction) == 0
                            && handler.insert(ItemResource.of(Items.DIAMOND), 1, transaction) == 0, "各面的自动化写入应被拒绝");
                        transaction.commit();
                    }
                }
            }
            helper.assertTrue(fixture.count(ItemResource.of(Items.STONE)) == 10, "只读查询不能改变存储数量");
        }
        helper.succeed();
    }

    private static RoyalSmithingMenu borrowFromPart(GameTestHelper helper, StorageFluidRpcTests.Fixture fixture) {
        BlockPos table = fixture.core().west(2);
        helper.getLevel().setBlock(table, ModBlocks.ROYAL_SMITHING_TABLE.getDefaultState(), Block.UPDATE_ALL);
        var menu = new RoyalSmithingMenu(7, fixture.player().getInventory(), ContainerLevelAccess.create(helper.getLevel(), table));
        fixture.player().containerMenu = menu;
        fixture.stock(ItemResource.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), 1);
        helper.assertTrue(menu.selectTemplateForTransfer(fixture.player(), new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)),
            "相邻非主方块的只读仓储应能借出模板");
        helper.assertTrue(fixture.count(ItemResource.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)) == 0
            && menu.isBorrowedTemplate(menu.getSlot(0).getItem()), "借用必须转移实体模板而非复制");
        return menu;
    }

    private static void borrow(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            final var menu = borrowFromPart(helper, fixture);
            menu.removed(fixture.player());
            helper.assertTrue(fixture.count(ItemResource.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)) == 1
                && dropped(helper, fixture.core().west(2)) == 0, "正常关闭应通过特权入口还到原槽");
        }
        helper.succeed();
    }

    private static void blockedReturn(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            final var menu = borrowFromPart(helper, fixture);
            ((UnlimitedItemStacksResourceHandler) fixture.items()).set(0, ItemResource.of(Items.STONE), 1);
            menu.removed(fixture.player());
            helper.assertTrue(fixture.count(ItemResource.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)) == 0
                && dropped(helper, fixture.core().west(2)) == 1, "原槽被占后应安全掉落，不能绕过只读限制改写其它槽");
        }
        helper.succeed();
    }

    private static void replaced(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            final var menu = borrowFromPart(helper, fixture);
            var state = helper.getLevel().getBlockState(fixture.core());
            helper.getLevel().removeBlockEntity(fixture.core());
            var replacement = ModBlockEntities.HYPERDIMENSION_STORAGE_STATION.create(fixture.core(), state);
            replacement.setId(UUID.randomUUID());
            helper.getLevel().setBlockEntity(replacement);
            menu.removed(fixture.player());
            var items = Storages.get().getOrCreate(replacement.getId(), HyperdimensionStorage.class).getItems();
            helper.assertTrue(items.getAmountAsLong(0) == 0 && dropped(helper, fixture.core().west(2)) == 1,
                "即使入口是无方块实体的结构部分，也不能把模板归还给替换后的主方块");
        }
        helper.succeed();
    }

    private static int dropped(GameTestHelper helper, BlockPos table) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(table).inflate(2)).stream()
            .filter(entity -> entity.getItem().is(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE))
            .mapToInt(entity -> entity.getItem().getCount()).sum();
    }

    private static void lazy(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.getLevel().setBlock(pos, ModBlocks.CRATE.getDefaultState(), Block.UPDATE_ALL);
        var entity = (StorageBlockEntity) helper.getLevel().getBlockEntity(pos);
        var handler = helper.getLevel().getCapability(Capabilities.Item.BLOCK, pos, null);
        helper.assertTrue(handler != null && entity.getId() != null && !(handler instanceof ReadOnlyItemResourceHandler),
            "首次能力查询应分配仓储标识，普通板条箱仍可读写");
        final UUID id = entity.getId();
        try (Transaction transaction = Transaction.openRoot()) {
            helper.assertTrue(handler.insert(ItemResource.of(Items.STONE), 1, transaction) == 1, "不能收紧板条箱原有写入能力");
            transaction.commit();
        }
        helper.getLevel().getCapability(Capabilities.Item.BLOCK, pos, Direction.UP);
        helper.assertTrue(id.equals(entity.getId()), "后续能力查询必须保持既有标识");
        helper.succeed();
    }
}
