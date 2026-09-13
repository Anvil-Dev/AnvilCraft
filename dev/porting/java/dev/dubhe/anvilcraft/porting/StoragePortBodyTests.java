package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.StoragePortManager;
import dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil;
import dev.dubhe.anvilcraft.block.entity.StoragePortBlockEntity;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.block.logistics.storage.AbstractStoragePortBlock;
import dev.dubhe.anvilcraft.block.logistics.storage.StoragePortBlock;
import dev.dubhe.anvilcraft.block.state.StoragePortType;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.network.StoragePortTakeOutPacket;
import dev.dubhe.anvilcraft.network.StoragePortUnmarkPacket;
import dev.dubhe.anvilcraft.saved.storage.ShulkerContainerStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import dev.dubhe.anvilcraft.util.BlockEntityItemUtil;
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
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class StoragePortBodyTests {
    private static final BlockPos POS = new BlockPos(4, 2, 3);
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_storage_transfer", StoragePortBodyTests::transfer,
        "port_storage_balance", StoragePortBodyTests::balance,
        "port_storage_clicks", StoragePortBodyTests::clicks,
        "port_storage_take_hold", StoragePortBodyTests::takeHold,
        "port_storage_save", StoragePortBodyTests::save,
        "port_storage_visibility", StoragePortBodyTests::visibility,
        "port_storage_export_all", StoragePortBodyTests::exportAll,
        "port_storage_drop_replace", StoragePortBodyTests::dropReplace
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_storage_body"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static StoragePortBlockEntity port(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.STORAGE_PORT.get());
        return helper.getBlockEntity(POS, StoragePortBlockEntity.class);
    }

    private static ResourceHandler<ItemResource> core(GameTestHelper helper) {
        var block = ModBlocks.SHULKER_CONTAINER.get();
        BlockPos pos = helper.absolutePos(POS.east(2));
        var state = block.defaultBlockState();
        for (var part : block.getParts()) {
            helper.getLevel().setBlock(pos.offset(block.offsetFrom(state, part)), block.placedState(part, state),
                Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        StorageBlockEntity be = (StorageBlockEntity) helper.getLevel().getBlockEntity(pos);
        UUID id = UUID.randomUUID();
        be.setId(id);
        return Storages.get().getOrCreate(id, ShulkerContainerStorage.class).getItems();
    }

    private static int insert(ResourceHandler<ItemResource> handler, ItemStack stack) {
        try (Transaction transaction = Transaction.openRoot()) {
            int inserted = handler.insert(ItemResource.of(stack), stack.getCount(), transaction);
            transaction.commit();
            return inserted;
        }
    }

    private static int count(ResourceHandler<ItemResource> handler) {
        int count = 0;
        for (int slot = 0; slot < handler.size(); slot++) count += handler.getAmountAsInt(slot);
        return count;
    }

    private static Player player(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(helper.absolutePos(POS).getCenter());
        return player;
    }

    private static void transfer(GameTestHelper helper) {
        var be = port(helper);
        var core = core(helper);
        var config = AnvilCraft.CONFIG.storagePort;
        int oldInterval = config.workInterval;
        int oldAmount = config.maxItemsPerScan;
        try {
            config.workInterval = 2;
            config.maxItemsPerScan = 3;
            insert(be.getBuffer(), new ItemStack(Items.IRON_INGOT, 10));
            be.tickServer();
            helper.assertTrue(be.isWorking() && count(core) == 3 && count(be.getBuffer()) == 7, "第一次扫描必须限量转移");
            be.tickServer();
            be.tickServer();
            helper.assertTrue(count(core) == 3, "源版递减计数期间不能继续转移");
            be.tickServer();
            helper.assertTrue(count(core) == 6 && count(be.getBuffer()) == 4, "计数归零后的扫描必须恢复转移");
            helper.assertTrue(be.getBuffer().size() == 32, "端口必须保持三十二槽缓存");
        } finally {
            config.workInterval = oldInterval;
            config.maxItemsPerScan = oldAmount;
        }
        helper.succeed();
    }

    private static void balance(GameTestHelper helper) {
        var be = port(helper);
        var core = core(helper);
        insert(core, new ItemStack(Items.IRON_INGOT, 256));
        be.setMarkedItem(new ItemStack(Items.IRON_INGOT));
        be.tickServer();
        helper.assertTrue(count(be.getBuffer()) == 64 && count(core) == 192, "标记模式应从核心补足一组");
        helper.assertTrue(insert(be.getBuffer(), new ItemStack(Items.GOLD_INGOT, 5)) == 0, "标记模式必须拒绝其它物品");
        insert(be.getBuffer(), new ItemStack(Items.IRON_INGOT, 32));
        for (int i = 0; i <= AnvilCraft.CONFIG.storagePort.workInterval; i++) be.tickServer();
        helper.assertTrue(count(be.getBuffer()) == 64 && count(core) == 224, "超过一组的部分应送回核心");
        be.setMarkedItem(new ItemStack(Items.GOLD_INGOT));
        helper.assertTrue(be.isBufferEmpty() && count(core) == 288, "改变标记时应先把旧缓存送入核心");
        helper.succeed();
    }

    private static void clicks(GameTestHelper helper) {
        var be = port(helper);
        Player player = player(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_INGOT, 20));
        player.getInventory().setItem(1, new ItemStack(Items.IRON_INGOT, 40));
        be.onRightClick(player, InteractionHand.MAIN_HAND, List.of());
        helper.assertTrue(be.getMarkedItem().is(Items.IRON_INGOT) && count(be.getBuffer()) == 20
            && player.getMainHandItem().isEmpty(), "首次右键必须标记并存入手持物品");
        be.onRightClick(player, InteractionHand.MAIN_HAND, List.of());
        helper.assertTrue(count(be.getBuffer()) == 60 && player.getInventory().getItem(1).isEmpty(), "空手第二击应存入背包同类物品");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GOLD_INGOT, 3));
        be.onRightClick(player, InteractionHand.MAIN_HAND, List.of());
        helper.assertTrue(be.getMarkedItem().is(Items.IRON_INGOT) && player.getMainHandItem().getCount() == 3,
            "不同物品右键不能替换标记或消耗物品");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ANVIL_HAMMER.get()));
        helper.assertTrue(!be.onRightClick(player, InteractionHand.MAIN_HAND, List.of()), "普通铁砧锤右键不能改变标记");
        new StoragePortUnmarkPacket(be.getBlockPos()).handleOnServer(player);
        helper.assertTrue(be.getMarkedItem().isEmpty() && count(be.getBuffer()) == 60, "未连接核心时去标记仍应保留缓存");
        helper.succeed();
    }

    private static void takeHold(GameTestHelper helper) {
        var be = port(helper);
        insert(be.getBuffer(), new ItemStack(Items.IRON_INGOT, 5));
        Player player = player(helper);
        final Player other = player(helper);
        new StoragePortTakeOutPacket(be.getBlockPos()).handleOnServer(player);
        helper.assertTrue(count(be.getBuffer()) == 4, "普通取出请求应只取一个");
        player.setShiftKeyDown(true);
        new StoragePortTakeOutPacket(be.getBlockPos()).handleOnServer(player);
        helper.assertTrue(be.isBufferEmpty() && player.getInventory().countItem(Items.IRON_INGOT) == 5, "潜行取出应取整组且数量守恒");
        BlockHitResult center = new BlockHitResult(be.getBlockPos().getCenter(), Direction.NORTH, be.getBlockPos(), false);
        helper.assertTrue(be.interceptsLeftClick(player, center) && !be.interceptsLeftClick(other, center),
            "取空后只应继续拦截正在取出的玩家");
        BlockHitResult edge = new BlockHitResult(be.getBlockPos().getCenter().add(-7.0 / 16, 0, 0), Direction.NORTH,
            be.getBlockPos(), false);
        helper.assertTrue(!be.interceptsLeftClick(player, edge), "一像素边框必须允许挖掘");
        helper.runAtTickTime(12, () -> {
            helper.assertTrue(!be.interceptsLeftClick(player, center), "停止取出后拦截窗口应过期");
            helper.succeed();
        });
    }

    private static void save(GameTestHelper helper) {
        var be = port(helper);
        ItemStack empty = new ItemStack(ModBlocks.STORAGE_PORT.asItem());
        BlockEntityItemUtil.saveToItem(be, empty, helper.getLevel().registryAccess());
        helper.assertTrue(!empty.has(DataComponents.BLOCK_ENTITY_DATA), "空端口不能产生影响堆叠的空数据组件");
        ItemStack emptyClone = ModBlocks.STORAGE_PORT.get().getCloneItemStack(helper.getLevel(), be.getBlockPos(), be.getBlockState(),
            true, player(helper));
        helper.assertTrue(!emptyClone.has(DataComponents.BLOCK_ENTITY_DATA), "携带数据的中键克隆也必须让空端口保持可堆叠");
        be.setMarkedItem(new ItemStack(Items.IRON_INGOT));
        insert(be.getBuffer(), new ItemStack(Items.IRON_INGOT, 25));
        ItemStack packed = new ItemStack(ModBlocks.STORAGE_PORT.asItem());
        be.saveToDrop(packed, helper.getLevel().registryAccess());
        helper.assertTrue(packed.has(DataComponents.BLOCK_ENTITY_DATA), "非空端口物品必须保留实体数据");
        CompoundTag tag = be.saveCustomOnly(helper.getLevel().registryAccess());
        be.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));
        helper.assertTrue(count(be.getBuffer()) == 25 && be.getMarkedItem().is(Items.IRON_INGOT), "原生数据重载必须保留缓存和标记");
        be.setMarkedItem(packed);
        helper.assertTrue(!be.getMarkedItem().has(DataComponents.BLOCK_ENTITY_DATA), "端口标记不能递归携带另一个端口的数据");
        final CompoundTag legacy = new CompoundTag();
        CompoundTag buffer = new CompoundTag();
        ListTag items = new ListTag();
        CompoundTag entry = (CompoundTag) ItemStack.CODEC.encodeStart(helper.getLevel().registryAccess().createSerializationContext(
            NbtOps.INSTANCE), new ItemStack(Items.DIAMOND, 12)).getOrThrow();
        entry.putByte("Slot", (byte) 7);
        items.add(entry);
        buffer.put("Items", items);
        buffer.putInt("Size", 32);
        legacy.put("buffer", buffer);
        be.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), legacy));
        helper.assertTrue(be.getMarkedItem().isEmpty() && be.getBuffer().getAmountAsInt(7) == 12,
            "源版缓存格式必须保留槽位，缺省标记必须清除旧标记");
        be.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), new CompoundTag()));
        helper.assertTrue(be.isBufferEmpty() && be.getUpdateTag(helper.getLevel().registryAccess()).contains("buffer"),
            "空缓存同步也必须显式发送数据，清除客户端旧库存");
        helper.succeed();
    }

    private static void visibility(GameTestHelper helper) {
        var be = port(helper);
        helper.assertTrue(be.isMarkedFaceVisible(Direction.NORTH), "无遮挡面应显示标记");
        helper.setBlock(POS.north(), Blocks.STONE);
        helper.assertTrue(!be.isMarkedFaceVisible(Direction.NORTH), "完整遮挡面不应渲染标记");
        helper.setBlock(POS.south(), ModBlocks.STORAGE_PORT.get());
        helper.assertTrue(!be.isMarkedFaceVisible(Direction.SOUTH), "相邻端口之间不应显示内部标记");
        AbstractStoragePortBlock.refreshType(helper.getLevel(), be.getBlockPos(), null);
        helper.assertTrue(be.getBlockState().getValue(AbstractStoragePortBlock.TYPE) == StoragePortType.SHULKER_CONTAINER,
            "无核心时应保持潜影集装箱外观");
        var far = helper.absolutePos(new BlockPos(14, 2, 3));
        helper.getLevel().setBlock(far, ModBlocks.HYPERDIMENSION_STORAGE_STATION.getDefaultState(), Block.UPDATE_CLIENTS);
        AbstractStoragePortBlock.refreshType(helper.getLevel(), be.getBlockPos(), far);
        helper.assertTrue(be.getBlockState().getValue(AbstractStoragePortBlock.TYPE) == StoragePortType.HYPERDIMENSION,
            "超维存储核心应切换端口外观");
        helper.assertTrue(!be.getBlockState().getValue(StoragePortBlock.MARKED), "外观切换不能改变标记状态");
        helper.succeed();
    }

    private static void exportAll(GameTestHelper helper) {
        var source = new ItemStacksResourceHandler(3);
        var target = new ItemStacksResourceHandler(1);
        source.set(0, ItemResource.of(Items.IRON_INGOT), 10);
        source.set(1, ItemResource.of(Items.GOLD_INGOT), 5);
        target.set(0, ItemResource.of(Items.IRON_INGOT), 63);
        ItemHandlerUtil.exportAllToTarget(source, (resource, amount) -> resource.getItem() == Items.IRON_INGOT, target);
        helper.assertTrue(source.getAmountAsInt(0) == 9 && source.getAmountAsInt(1) == 5 && target.getAmountAsInt(0) == 64,
            "全部转移必须跳过空槽、保留筛选掉的物品，并只扣除实际接收量");
        target.set(0, ItemResource.EMPTY, 0);
        ItemHandlerUtil.exportAllToTarget(source, (resource, amount) -> false, target);
        helper.assertTrue(count(source) == 14 && count(target) == 0, "筛选拒绝必须回滚目标与源容器");
        helper.succeed();
    }

    private static void dropReplace(GameTestHelper helper) {
        var be = port(helper);
        be.setMarkedItem(new ItemStack(Items.DIAMOND));
        insert(be.getBuffer(), new ItemStack(Items.DIAMOND, 12));
        helper.getLevel().destroyBlock(be.getBlockPos(), true);
        var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(be.getBlockPos()).inflate(1),
            entity -> entity.getItem().is(ModBlocks.STORAGE_PORT.asItem()));
        helper.assertTrue(drops.size() == 1, "实际破坏只能掉落一个携带数据的端口");
        ItemStack stack = drops.getFirst().getItem();
        drops.getFirst().discard();
        Player player = player(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos target = helper.absolutePos(POS.east(5));
        var result = stack.useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack,
            new BlockHitResult(target.getCenter(), Direction.UP, target, false)));
        helper.assertTrue(result.consumesAction(), "携带库存的端口物品必须可以重新放置");
        helper.runAtTickTime(2, () -> {
            var placed = (StoragePortBlockEntity) helper.getLevel().getBlockEntity(target);
            helper.assertTrue(placed != null && placed.getMarkedItem().is(Items.DIAMOND) && count(placed.getBuffer()) == 12
                && placed.getBlockState().getValue(StoragePortBlock.MARKED), "重新放置必须恢复库存、标记和可见状态");
            helper.succeed();
        });
    }
}
