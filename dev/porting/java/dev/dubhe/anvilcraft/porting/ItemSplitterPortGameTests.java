package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.block.entity.ItemSplitterBlockEntity;
import dev.dubhe.anvilcraft.block.logistics.ItemSplitterBlock;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class ItemSplitterPortGameTests {
    private static final BlockPos POS = new BlockPos(2, 2, 2);
    private static final Map<BlockPos, Double> FALL_DISTANCES = new HashMap<>();

    @SubscribeEvent
    public static void landed(AnvilEvent.OnLand event) {
        if (event.getLevel().getBlockState(event.getPos().below()).is(ModBlocks.ITEM_SPLITTER)) {
            FALL_DISTANCES.put(event.getPos().below(), event.getFallDistance());
            AnvilCraft.LOGGER.info("PORT_SPLITTER_FALL_DISTANCE: {}", event.getFallDistance());
        }
    }

    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_splitter_inventory", ItemSplitterPortGameTests::inventory,
        "port_splitter_containers", ItemSplitterPortGameTests::containers,
        "port_splitter_double_chest", ItemSplitterPortGameTests::doubleChest,
        "port_splitter_space", ItemSplitterPortGameTests::space,
        "port_splitter_anvil", ItemSplitterPortGameTests::anvil,
        "port_splitter_persistence", ItemSplitterPortGameTests::persistence,
        "port_splitter_remove", ItemSplitterPortGameTests::remove
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_splitter"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static ItemSplitterBlockEntity setup(GameTestHelper helper, int count) {
        for (int x = 1; x <= 20; x++) helper.setBlock(new BlockPos(x, 1, 2), Blocks.STONE);
        helper.setBlock(POS, ModBlocks.ITEM_SPLITTER.getDefaultState().setValue(ItemSplitterBlock.FACING, Direction.EAST));
        var be = helper.getBlockEntity(POS, ItemSplitterBlockEntity.class);
        insert(be.getItemHandler(), ItemResource.of(Items.IRON_INGOT), count);
        return be;
    }

    private static int insert(ResourceHandler<ItemResource> handler, ItemResource resource, int amount) {
        try (Transaction transaction = Transaction.openRoot()) {
            int inserted = handler.insert(resource, amount, transaction);
            transaction.commit();
            return inserted;
        }
    }

    private static int count(ResourceHandler<ItemResource> handler) {
        int total = 0;
        for (int slot = 0; slot < handler.size(); slot++) total += handler.getAmountAsInt(slot);
        return total;
    }

    private static ResourceHandler<ItemResource> capability(GameTestHelper helper, BlockPos pos) {
        return helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(pos), Direction.WEST);
    }

    private static void inventory(GameTestHelper helper) {
        var be = setup(helper, 1);
        for (Direction side : Direction.values()) {
            helper.assertTrue(helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(POS), side)
                == be.getItemHandler(), "所有面必须暴露同一库存能力");
        }
        helper.assertTrue(insert(be.getItemHandler(), ItemResource.of(Items.GOLD_INGOT), 5) == 0, "不同物品不得进入其它空槽");
        ItemStack named = new ItemStack(Items.IRON_INGOT);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("different"));
        helper.assertTrue(insert(be.getItemHandler(), ItemResource.of(named), 5) == 0, "组件不同也必须拒收");
        try (Transaction transaction = Transaction.openRoot()) {
            helper.assertTrue(be.getItemHandler().insert(ItemResource.of(Items.IRON_INGOT), 10, transaction) == 10,
                "模拟应接受同种物品");
        }
        helper.assertTrue(be.getTotalCount() == 1, "回滚不能改变库存");
        helper.assertTrue(insert(be.getItemHandler(), ItemResource.of(Items.IRON_INGOT), 2000) == 1023
            && be.getTotalCount() == 1024, "容量必须是 16 个原生槽位");
        helper.succeed();
    }

    private static void containers(GameTestHelper helper) {
        var be = setup(helper, 10);
        for (int distance = 1; distance <= 3; distance++) helper.setBlock(POS.east(distance), Blocks.BARREL);
        helper.setBlock(POS.east(5), Blocks.BARREL);
        be.splitToContainers();
        helper.assertTrue(be.getTotalCount() == 1, "三份严格均分必须留下一个余数");
        for (int distance = 1; distance <= 3; distance++) {
            helper.assertTrue(count(capability(helper, POS.east(distance))) == 3, "每个连续容器都应收到三件物品");
        }
        helper.assertTrue(count(capability(helper, POS.east(5))) == 0, "断开的容器不能参与分配");
        helper.assertTrue(!be.splitToSpace(1), "正前方有容器时铁砧不能触发空间分配");
        var barrel = helper.getBlockEntity(POS.east(), BarrelBlockEntity.class);
        for (int slot = 0; slot < barrel.getContainerSize(); slot++) barrel.setItem(slot, new ItemStack(Items.IRON_INGOT, 64));
        barrel.setItem(0, new ItemStack(Items.IRON_INGOT, 62));
        insert(be.getItemHandler(), ItemResource.of(Items.IRON_INGOT), 9);
        be.splitToContainers();
        helper.assertTrue(be.getTotalCount() == 2 && count(capability(helper, POS.east(2))) == 6,
            "被拒收的份额必须留在内部，不能增大后续容器的份额");
        helper.succeed();
    }

    private static void doubleChest(GameTestHelper helper) {
        final var be = setup(helper, 21);
        helper.setBlock(POS.east(), Blocks.CHEST.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
            .setValue(BlockStateProperties.CHEST_TYPE, ChestType.LEFT));
        helper.setBlock(POS.east(2), Blocks.CHEST.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
            .setValue(BlockStateProperties.CHEST_TYPE, ChestType.RIGHT));
        helper.setBlock(POS.east(3), Blocks.BARREL);
        be.splitToContainers();
        helper.assertTrue(count(capability(helper, POS.east())) == 14 && count(capability(helper, POS.east(3))) == 7,
            "沿选取方向的大箱子占两格，必须收到两份");
        helper.assertTrue(be.getTotalCount() == 0, "均分后总量必须守恒");
        helper.succeed();
    }

    private static List<ItemEntity> drops(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class,
            new AABB(helper.absolutePos(POS)).expandTowards(18, 0, 0), entity -> entity.getItem().is(Items.IRON_INGOT));
    }

    private static void space(GameTestHelper helper) {
        var be = setup(helper, 17);
        helper.setBlock(POS.east(2), Blocks.STONE);
        helper.assertTrue(be.splitToSpace(5), "空间均分必须成功");
        List<ItemEntity> items = drops(helper);
        helper.assertTrue(items.size() == 5 && be.getTotalCount() == 2, "必须分出五份三件并保留余数");
        for (ItemEntity item : items) {
            helper.assertTrue(item.getItem().getCount() == 3 && item.getDeltaMovement().lengthSqr() == 0,
                "每份必须数量相同并以零初速度生成");
            helper.assertTrue(!item.blockPosition().equals(helper.absolutePos(POS.east(2))), "必须越过障碍格");
            item.discard();
        }
        for (int distance = 1; distance < 16; distance++) helper.setBlock(POS.east(distance), Blocks.STONE);
        insert(be.getItemHandler(), ItemResource.of(Items.IRON_INGOT), 15);
        helper.assertTrue(be.splitToSpace(5) && be.getTotalCount() == 14, "只有一份能在最远十六格落位，其余份额必须保留");
        helper.assertTrue(drops(helper).size() == 1
            && drops(helper).getFirst().blockPosition().equals(helper.absolutePos(POS.east(16))), "不能越过十六格范围");
        helper.succeed();
    }

    private static void anvil(GameTestHelper helper) {
        var be = setup(helper, 17);
        final FallingBlockEntity falling = FallingBlockEntity.fall(helper.getLevel(), helper.absolutePos(POS.above(5)),
            Blocks.ANVIL.defaultBlockState());
        helper.runAtTickTime(80, () -> {
            AnvilCraft.LOGGER.info("PORT_SPLITTER_ANVIL_STATE: time={}, pos={}, removed={}",
                falling.time, falling.position(), falling.isRemoved());
            double distance = FALL_DISTANCES.getOrDefault(helper.absolutePos(POS), -1.0);
            helper.assertTrue(distance > 3 && distance <= 4, "真实落地必须产生四格落差对应的事件");
            helper.assertTrue(be.getTotalCount() == 1 && drops(helper).size() == 4
                && drops(helper).stream().allMatch(item -> item.getItem().getCount() == 4),
                "实际下落铁砧必须通过已注册行为触发均分: remaining=" + be.getTotalCount() + ", drops="
                    + drops(helper).stream().map(item -> item.getItem().getCount()).toList());
            helper.succeed();
        });
    }

    private static void persistence(GameTestHelper helper) {
        var be = setup(helper, 10);
        helper.setBlock(POS.east(), Blocks.BARREL);
        be.tick();
        insert(be.getItemHandler(), ItemResource.of(Items.IRON_INGOT), 8);
        for (int i = 0; i < 3; i++) be.tick();
        CompoundTag tag = be.saveCustomOnly(helper.getLevel().registryAccess());
        var loaded = new ItemSplitterBlockEntity(ModBlockEntities.ITEM_SPLITTER.get(), helper.absolutePos(POS), be.getBlockState());
        loaded.setLevel(helper.getLevel());
        loaded.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));
        for (int i = 0; i < 4; i++) loaded.tick();
        helper.assertTrue(loaded.getTotalCount() == 8, "重载后必须保留剩余冷却");
        loaded.tick();
        helper.assertTrue(loaded.getTotalCount() == 0 && count(capability(helper, POS.east())) == 18, "第八刻应恢复分配");
        final CompoundTag legacy = new CompoundTag();
        CompoundTag inventory = new CompoundTag();
        ListTag items = new ListTag();
        CompoundTag stack = (CompoundTag) ItemStack.CODEC.encodeStart(helper.getLevel().registryAccess().createSerializationContext(
            NbtOps.INSTANCE), new ItemStack(Items.IRON_INGOT, 12)).getOrThrow();
        stack.putByte("Slot", (byte) 4);
        items.add(stack);
        inventory.put("Items", items);
        inventory.putInt("Size", 16);
        legacy.put("Inventory", inventory);
        loaded.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), legacy));
        helper.assertTrue(loaded.getTotalCount() == 12 && loaded.getItemHandler().getAmountAsInt(4) == 12,
            "源版库存格式必须保留数量和槽位");
        helper.succeed();
    }

    private static void remove(GameTestHelper helper) {
        var be = setup(helper, 96);
        helper.getLevel().destroyBlock(helper.absolutePos(POS), false);
        helper.assertTrue(drops(helper).stream().mapToInt(item -> item.getItem().getCount()).sum() == 96 && be.getTotalCount() == 0,
            "破坏必须掉落所有内容并清空旧库存");
        helper.succeed();
    }
}
