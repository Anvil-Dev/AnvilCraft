package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.StoragePortManager;
import dev.dubhe.anvilcraft.block.entity.StorageFluidPortBlockEntity;
import dev.dubhe.anvilcraft.block.entity.StoragePortBlockEntity;
import dev.dubhe.anvilcraft.block.entity.StoragePortConsolidatorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class StorageConsolidatorTests {
    private static final BlockPos POS = new BlockPos(5, 2, 5);
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_consolidator_items", StorageConsolidatorTests::items,
        "port_consolidator_fluids", StorageConsolidatorTests::fluids,
        "port_consolidator_inventory", StorageConsolidatorTests::inventory,
        "port_consolidator_pour", StorageConsolidatorTests::pour,
        "port_consolidator_empty_bucket", StorageConsolidatorTests::emptyBucket,
        "port_consolidator_links", StorageConsolidatorTests::links
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        final var environment = event.registerEnvironment(AnvilCraft.of("port_consolidator"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static StoragePortConsolidatorBlockEntity consolidator(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.STORAGE_PORT_CONSOLIDATOR.get());
        final var port = helper.getBlockEntity(POS, StoragePortConsolidatorBlockEntity.class);
        port.tickServer();
        return port;
    }

    private static StoragePortBlockEntity item(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, ModBlocks.STORAGE_PORT.get());
        return helper.getBlockEntity(pos, StoragePortBlockEntity.class);
    }

    private static StorageFluidPortBlockEntity fluid(GameTestHelper helper, BlockPos pos, FluidResource fluid, int amount) {
        helper.setBlock(pos, ModBlocks.STORAGE_FLUID_PORT.get());
        final var port = helper.getBlockEntity(pos, StorageFluidPortBlockEntity.class);
        port.getTank().set(0, fluid, amount);
        return port;
    }

    private static void items(GameTestHelper helper) {
        final var plain = item(helper, POS.west());
        final var marked = item(helper, POS.east());
        marked.setMarkedItem(new ItemStack(Items.IRON_INGOT));
        final var port = consolidator(helper);
        final var handler = port.getItemHandler();
        final var iron = ItemResource.of(Items.IRON_INGOT);
        helper.assertTrue(handler.size() == 64, "无核心时两个端口仍须暴露 64 格");
        for (Direction side : Direction.values()) {
            helper.assertTrue(helper.getLevel().getCapability(Capabilities.Item.BLOCK, port.getBlockPos(), side) == handler,
                "六个面必须暴露物品能力");
        }
        try (Transaction transaction = Transaction.openRoot()) {
            helper.assertTrue(handler.insert(0, iron, 80, transaction) == 80, "指定任意槽插入仍应优先匹配标记");
            helper.assertTrue(plain.getBuffer().getAmountAsLong(0) == 0 && marked.getBuffer().getAmountAsLong(0) == 64,
                "标记端口应优先装满第一格");
        }
        helper.assertTrue(marked.getBuffer().getAmountAsLong(0) == 0, "模拟插入必须完整回滚");
        try (Transaction transaction = Transaction.openRoot()) {
            helper.assertTrue(handler.insert(iron, 2100, transaction) == 2100, "标记端口装满后应继续进入无标记端口");
            transaction.commit();
        }
        helper.assertTrue(plain.getBuffer().getAmountAsLong(0) == 52 && marked.getBuffer().getAmountAsLong(31) == 64,
            "溢出分配必须为标记端口 2048 个和另一端口 52 个");
        try (Transaction transaction = Transaction.openRoot()) {
            helper.assertTrue(handler.extract(iron, 2100, transaction) == 2100, "读取映射必须覆盖所有缓存槽");
            transaction.commit();
        }
        helper.succeed();
    }

    private static void fluids(GameTestHelper helper) {
        final var water = FluidResource.of(Fluids.WATER);
        final var empty = fluid(helper, POS.west(), FluidResource.EMPTY, 0);
        final var filled = fluid(helper, POS.east(), water, 127500);
        final var port = consolidator(helper);
        final var handler = port.getFluidHandler();
        helper.assertTrue(handler.size() == 2, "每个相连流体端口必须暴露一格");
        for (Direction side : Direction.values()) {
            helper.assertTrue(helper.getLevel().getCapability(Capabilities.Fluid.BLOCK, port.getBlockPos(), side) == handler,
                "六个面必须暴露流体能力");
        }
        try (Transaction transaction = Transaction.openRoot()) {
            helper.assertTrue(handler.insert(water, 1000, transaction) == 500, "整批灌入只填同种流体目标，不把余量拆入空罐");
        }
        helper.assertTrue(filled.getFluid().getAmount() == 127500 && empty.getFluid().isEmpty(), "流体模拟必须不改变任一端口");
        filled.getTank().set(0, water, 100);
        empty.getTank().set(0, water, 200);
        try (Transaction transaction = Transaction.openRoot()) {
            int extracted = handler.extract(water, 1000, transaction);
            helper.assertTrue(extracted == 100 || extracted == 200, "整批抽取只取首个可用端口，不能合并多个端口");
        }
        helper.assertTrue(filled.getFluid().getAmount() == 100 && empty.getFluid().getAmount() == 200, "抽取回滚应恢复原量");
        for (int index = 0; index < handler.size(); index++) {
            helper.assertTrue(handler.getCapacityAsLong(index, water) == 128000, "每格必须保留端口完整容量");
            helper.assertTrue(!handler.isValid(index, FluidResource.of(Fluids.LAVA)), "有水的端口不能混入岩浆");
        }
        helper.succeed();
    }

    private static void inventory(GameTestHelper helper) {
        final var target = item(helper, POS.east());
        target.setMarkedItem(new ItemStack(Items.IRON_HELMET));
        final var port = consolidator(helper);
        final var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().setItem(12, new ItemStack(Items.IRON_HELMET));
        player.getInventory().setItem(13, new ItemStack(Items.GOLD_INGOT));
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.IRON_HELMET));
        port.onRightClick(player, InteractionHand.MAIN_HAND, List.of(target));
        helper.assertTrue(!player.getInventory().getItem(12).isEmpty(), "第一次空手点击不应批量清背包");
        port.onRightClick(player, InteractionHand.MAIN_HAND, List.of(target));
        helper.assertTrue(player.getInventory().getItem(12).isEmpty() && player.getOffhandItem().isEmpty(),
            "双击应存入背包及副手中的标记物品");
        helper.assertTrue(!player.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && !player.getInventory().getItem(13).isEmpty(),
            "双击必须保留穿戴盔甲和未标记物品");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ANVIL_HAMMER.get()));
        helper.assertTrue(!port.onRightClick(player, InteractionHand.MAIN_HAND, List.of(target)), "铁砧锤应走自身交互");
        helper.succeed();
    }

    private static void pour(GameTestHelper helper) {
        final var water = fluid(helper, POS.east(), FluidResource.of(Fluids.WATER), 1000);
        final var port = consolidator(helper);
        final var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        helper.assertTrue(port.onRightClick(player, InteractionHand.MAIN_HAND, List.of(water)), "同种流体容器应交给流体端口");
        helper.assertTrue(water.getFluid().getAmount() == 2000 && player.getMainHandItem().is(Items.BUCKET),
            "倒入后必须回写空桶");
        port.onRightClick(player, InteractionHand.MAIN_HAND, List.of(water));
        helper.assertTrue(water.getFluid().getAmount() == 2000, "空桶再次右键不能重复倒出流体或反向抽取");
        helper.succeed();
    }

    private static void emptyBucket(GameTestHelper helper) {
        final var empty = fluid(helper, POS.east(), FluidResource.EMPTY, 0);
        final var items = item(helper, POS.west());
        final var port = consolidator(helper);
        final var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        port.onRightClick(player, InteractionHand.MAIN_HAND, List.of(empty, items));
        helper.assertTrue(empty.getFluid().isEmpty() && player.getMainHandItem().isEmpty(), "空端口不能截走桶内流体");
        helper.assertTrue(items.getBuffer().getResource(0).is(Items.WATER_BUCKET), "没有同种流体时应把桶作为普通物品存入");
        helper.succeed();
    }

    private static void links(GameTestHelper helper) {
        final var adjacent = item(helper, POS.east());
        final var block = ModBlocks.SHULKER_CONTAINER.get();
        BlockPos center = helper.absolutePos(POS.east(3));
        final var state = block.defaultBlockState();
        for (var part : block.getParts()) {
            helper.getLevel().setBlock(center.offset(block.offsetFrom(state, part)), block.placedState(part, state),
                Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        final var core = (StorageBlockEntity) helper.getLevel().getBlockEntity(center);
        UUID id = UUID.randomUUID();
        core.setId(id);
        item(helper, POS.east(5));
        final var port = consolidator(helper);
        helper.assertTrue(port.getItemHandler().size() == 32, "扫描不得穿过集装箱连接另一侧端口");
        helper.assertTrue(StoragePortManager.positions(id).contains(port.getBlockPos()), "整合器必须登记到唯一核心");
        helper.getLevel().removeBlock(adjacent.getBlockPos(), false);
        helper.assertTrue(port.getItemHandler().getResource(0).isEmpty(), "端口拆除后缓存映射应立即拒绝失效实体");
        for (int tick = 0; tick < 22; tick++) port.tickServer();
        helper.assertTrue(port.getItemHandler().size() == 0 && !StoragePortManager.positions(id).contains(port.getBlockPos()),
            "周期扫描后应更新格数并撤销旧核心归属");
        helper.setBlock(POS.east(), ModBlocks.STORAGE_PORT.get());
        for (int tick = 0; tick < 22; tick++) port.tickServer();
        helper.assertTrue(StoragePortManager.positions(id).contains(port.getBlockPos()), "重接链路应重新登记");
        helper.setBlock(POS, Blocks.AIR);
        helper.assertTrue(!StoragePortManager.positions(id).contains(port.getBlockPos()), "拆除整合器必须注销登记");
        helper.succeed();
    }
}
