package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.IStoragePort;
import dev.dubhe.anvilcraft.api.StoragePortManager;
import dev.dubhe.anvilcraft.block.entity.ChuteBlockEntity;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class StoragePortConnectivityTests {
    private static final UUID SHUTDOWN_MARKER = UUID.fromString("c571d1e9-711b-4bfc-a474-5367815314ba");
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_storage_component", StoragePortConnectivityTests::component,
        "port_storage_core_boundary", StoragePortConnectivityTests::coreBoundary,
        "port_storage_missing_id", StoragePortConnectivityTests::missingId,
        "port_storage_limit", StoragePortConnectivityTests::limit,
        "port_storage_registry", StoragePortConnectivityTests::registry
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_storage_connectivity"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of(name.equals("port_storage_limit") ? "port_storage_chain" : "port_logistics_empty"),
                100, 0, true)
        )));
    }

    /** 使用已注册的溜槽类型承载测试节点，只验证端口接口和相连关系，不代表已实现真实端口。 */
    private static final class TestPortNode extends ChuteBlockEntity implements IStoragePort {
        private InteractionHand rightHand;
        private List<IStoragePort> clickedPorts;
        private int leftClicks;

        private TestPortNode(BlockPos pos, BlockState state) {
            super(ModBlockEntities.CHUTE.get(), pos, state);
        }

        @Override
        public void tick() {
        }

        @Override
        public boolean onLeftClick(Player player, List<IStoragePort> ports) {
            this.leftClicks++;
            this.clickedPorts = ports;
            return true;
        }

        @Override
        public boolean onRightClick(Player player, InteractionHand hand, List<IStoragePort> ports) {
            this.rightHand = hand;
            this.clickedPorts = ports;
            return true;
        }
    }

    private static TestPortNode node(GameTestHelper helper, BlockPos relative) {
        BlockPos pos = helper.absolutePos(relative);
        BlockState state = ModBlocks.CHUTE.getDefaultState();
        helper.getLevel().setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        helper.getLevel().removeBlockEntity(pos);
        TestPortNode node = new TestPortNode(pos, state);
        helper.getLevel().setBlockEntity(node);
        return node;
    }

    private static <P extends Enum<P>> StorageBlockEntity core(
        GameTestHelper helper, BlockPos relative, AbstractMultiPartBlock<P> block
    ) {
        BlockPos pos = helper.absolutePos(relative);
        BlockState state = block.defaultBlockState();
        for (P part : block.getParts()) {
            helper.getLevel().setBlock(pos.offset(block.offsetFrom(state, part)), block.placedState(part, state),
                Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        var entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof StorageBlockEntity, "真实存储核心必须在主方块上产生存储实体");
        return (StorageBlockEntity) entity;
    }

    private static void component(GameTestHelper helper) {
        BlockPos center = new BlockPos(6, 2, 3);
        core(helper, center, ModBlocks.SHULKER_CONTAINER.get()).setId(UUID.randomUUID());
        TestPortNode start = null;
        for (int y = 2; y <= 3; y++) {
            for (int z = 2; z <= 4; z++) {
                TestPortNode port = node(helper, new BlockPos(4, y, z));
                if (y == 2 && z == 3) start = port;
            }
        }
        TestPortNode diagonal = node(helper, new BlockPos(3, 2, 5));
        var linked = StoragePortManager.scan(helper.getLevel(), start.getBlockPos());
        helper.assertTrue(helper.absolutePos(center).equals(linked.core()) && linked.ports().size() == 5,
            "环路应去重，多次接触同一核心仍应视为单核心，返回端口不含起点");
        helper.assertTrue(!linked.ports().contains(diagonal), "对角相邻不能连接");
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        start.onClick(ClickAction.PRIMARY, player, InteractionHand.MAIN_HAND, linked.ports());
        start.onClick(ClickAction.SECONDARY, player, InteractionHand.OFF_HAND, linked.ports());
        helper.assertTrue(start.leftClicks == 1 && start.rightHand == InteractionHand.OFF_HAND && start.clickedPorts == linked.ports(),
            "点击分发必须保留类型、交互手和相连端口");
        helper.succeed();
    }

    private static void coreBoundary(GameTestHelper helper) {
        UUID id = UUID.randomUUID();
        core(helper, new BlockPos(6, 2, 3), ModBlocks.SHULKER_CONTAINER.get()).setId(id);
        core(helper, new BlockPos(12, 2, 3), ModBlocks.HYPERDIMENSION_STORAGE_STATION.get()).setId(id);
        TestPortNode left = node(helper, new BlockPos(4, 2, 3));
        TestPortNode right = node(helper, new BlockPos(8, 2, 3));
        node(helper, new BlockPos(9, 2, 3));
        node(helper, new BlockPos(10, 2, 3));
        var leftComponent = StoragePortManager.scan(helper.getLevel(), left.getBlockPos());
        var rightComponent = StoragePortManager.scan(helper.getLevel(), right.getBlockPos());
        helper.assertTrue(leftComponent.core() != null && leftComponent.ports().isEmpty(), "不能穿过核心连到另一侧端口");
        helper.assertTrue(rightComponent.core() == null && rightComponent.ports().size() == 2,
            "两个核心即使存储 ID 相同，夹在中间的组件也必须无效");
        helper.succeed();
    }

    private static void missingId(GameTestHelper helper) {
        StorageBlockEntity core = core(helper, new BlockPos(6, 2, 3), ModBlocks.SHULKER_CONTAINER.get());
        TestPortNode start = node(helper, new BlockPos(4, 2, 3));
        helper.assertTrue(StoragePortManager.findSoleCore(helper.getLevel(), start.getBlockPos()) == null,
            "未分配存储 ID 的核心不能工作");
        core.setId(UUID.randomUUID());
        helper.assertTrue(core.getBlockPos().equals(StoragePortManager.findSoleCore(helper.getLevel(), start.getBlockPos())),
            "分配存储 ID 后重新扫描必须恢复连接");
        helper.succeed();
    }

    private static void limit(GameTestHelper helper) {
        for (int x = 1; x <= 514; x++) node(helper, new BlockPos(x, 2, 1));
        var linked = StoragePortManager.scan(helper.getLevel(), helper.absolutePos(new BlockPos(1, 2, 1)));
        helper.assertTrue(linked.core() == null && linked.ports().size() == 512, "必须保留源版 512 次节点访问上限");
        helper.succeed();
    }

    private static void registry(GameTestHelper helper) {
        UUID oldId = UUID.randomUUID();
        UUID newId = UUID.randomUUID();
        BlockPos.MutableBlockPos mutable = helper.absolutePos(new BlockPos(3, 2, 2)).mutable();
        final BlockPos original = mutable.immutable();
        var nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        StoragePortManager.register(oldId, helper.getLevel(), mutable);
        StoragePortManager.register(oldId, nether, mutable);
        StoragePortManager.register(newId, helper.getLevel(), mutable);
        mutable.move(1, 0, 0);
        helper.assertTrue(StoragePortManager.positions(oldId).contains(original), "注册必须保存不可变坐标");
        StoragePortManager.unregister(oldId, helper.getLevel().dimension(), original);
        helper.assertTrue(StoragePortManager.positions(oldId).contains(original), "不能误注销另一维度同坐标的端口");
        StoragePortManager.unregister(oldId, Level.NETHER, original);
        helper.assertTrue(StoragePortManager.positions(oldId).isEmpty() && StoragePortManager.positions(newId).contains(original),
            "改挂存储时不能删除新存储的登记");
        StoragePortManager.unregister(newId, helper.getLevel().dimension(), original);
        StoragePortManager.register(SHUTDOWN_MARKER, helper.getLevel(), original);
        helper.succeed();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void stopped(ServerStoppedEvent event) {
        if (!StoragePortManager.positions(SHUTDOWN_MARKER).isEmpty()) throw new IllegalStateException("停止服务端后端口登记未清空");
        AnvilCraft.LOGGER.info("PORT_STORAGE_LIFECYCLE_PASSED");
    }
}
