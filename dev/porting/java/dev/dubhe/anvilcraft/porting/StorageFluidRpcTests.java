package dev.dubhe.anvilcraft.porting;

import com.mojang.authlib.GameProfile;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.StoragePortManager;
import dev.dubhe.anvilcraft.block.entity.StorageFluidPortBlockEntity;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.rpc.StorageInput;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import dev.dubhe.anvilcraft.saved.setting.PlayerSettings;
import dev.dubhe.anvilcraft.saved.storage.HyperdimensionStorage;
import dev.dubhe.anvilcraft.saved.storage.ShulkerContainerStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import dev.dubhe.anvilcraft.saved.storage.category.FluidCategory;
import dev.dubhe.anvilcraft.saved.storage.category.store.CategoryMode;
import dev.dubhe.anvilcraft.util.FluidAmountUtil;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class StorageFluidRpcTests {
    private static final BlockPos CORE = new BlockPos(5, 2, 5);
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_fluid_rpc_sync", StorageFluidRpcTests::sync,
        "port_fluid_rpc_pickup", StorageFluidRpcTests::pickup,
        "port_fluid_rpc_containers", StorageFluidRpcTests::containers,
        "port_fluid_rpc_notices", StorageFluidRpcTests::notices,
        "port_fluid_rpc_rollback", StorageFluidRpcTests::rollback,
        "port_fluid_rpc_pour", StorageFluidRpcTests::pour,
        "port_fluid_rpc_filters", StorageFluidRpcTests::filters,
        "port_fluid_rpc_bottles", StorageFluidRpcTests::bottles
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        final var environment = event.registerEnvironment(AnvilCraft.of("port_storage_fluid_rpc"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 200, 0, true)
        )));
    }

    static final class Fixture implements AutoCloseable {
        private final GameTestHelper helper;
        private final UUID id = UUID.randomUUID();
        private final BlockPos core;
        private final ServerPlayer player;
        private final ResourceHandler<ItemResource> items;
        private final List<BlockPos> ports = new ArrayList<>();

        Fixture(GameTestHelper helper) {
            this(helper, false);
        }

        Fixture(GameTestHelper helper, boolean hyperdimension) {
            this.helper = helper;
            this.core = helper.absolutePos(CORE);
            if (hyperdimension) this.placeCore(helper, ModBlocks.HYPERDIMENSION_STORAGE_STATION.get());
            else this.placeCore(helper, ModBlocks.SHULKER_CONTAINER.get());
            ((StorageBlockEntity) helper.getLevel().getBlockEntity(this.core)).setId(this.id);
            this.items = hyperdimension ? Storages.get().getOrCreate(this.id, HyperdimensionStorage.class).getItems()
                : Storages.get().getOrCreate(this.id, ShulkerContainerStorage.class).getItems();
            this.player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "port-fluid-test"));
            this.playerLookup().put(this.playerId(), this.player);
            this.player.setPos(this.core.getX() + 0.5, this.core.getY() + 1, this.core.getZ() + 0.5);
        }

        StorageFluidPortBlockEntity fluid(FluidResource resource, int amount) {
            BlockPos pos = this.core.west(2 + this.ports.size());
            this.helper.getLevel().setBlock(pos, ModBlocks.STORAGE_FLUID_PORT.getDefaultState(), Block.UPDATE_ALL);
            final var port = (StorageFluidPortBlockEntity) this.helper.getLevel().getBlockEntity(pos);
            port.getTank().set(0, resource, amount);
            port.tickServer();
            this.ports.add(pos);
            this.helper.assertTrue(StoragePortManager.positions(this.id).contains(pos), "流体端口必须真实连接核心");
            return port;
        }

        private <P extends Enum<P>> void placeCore(GameTestHelper helper, AbstractMultiPartBlock<P> block) {
            var state = block.defaultBlockState();
            for (P part : block.getParts()) {
                helper.getLevel().setBlock(this.core.offset(block.offsetFrom(state, part)), block.placedState(part, state),
                    Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }

        void authorize() {
            IPayloadContext context = (IPayloadContext) Proxy.newProxyInstance(IPayloadContext.class.getClassLoader(),
                new Class<?>[]{IPayloadContext.class}, (proxy, method, args) -> {
                    if (method.getName().equals("player")) return this.player;
                    throw new UnsupportedOperationException(method.getName());
                });
            try {
                boolean valid = new StorageServerStub.StorageAccessValidator().validate(context,
                    StorageServerStub.class.getMethod("load", UUID.class, long.class), new Object[]{this.playerId(), this.core.asLong()});
                this.helper.assertTrue(valid, "必须通过玩家身份、核心及距离校验");
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException(exception);
            }
        }

        UUID playerId() {
            return this.player.getGameProfile().id();
        }

        /** 仅为 RPC 玩家查找登记测试替身，登录握手与真实网络由客户端场景覆盖。 */
        @SuppressWarnings("unchecked")
        private Map<UUID, ServerPlayer> playerLookup() {
            try {
                var players = this.helper.getLevel().getServer().getPlayerList();
                var field = net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");
                field.setAccessible(true);
                return (Map<UUID, ServerPlayer>) field.get(players);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException(exception);
            }
        }

        IntList order() {
            this.authorize();
            return StorageServerStub.reorder(this.playerId(), this.core.asLong());
        }

        StorageServerStub.SyncResult sync(IntList slots) {
            this.authorize();
            return StorageServerStub.sync(this.playerId(), this.core.asLong(), slots);
        }

        private StorageServerStub.InteractionResult interact(StorageInput action, int button, FluidStack fluid) {
            this.authorize();
            return StorageServerStub.interact(this.playerId(), this.core.asLong(), StoragePortManager.FLUID_SLOT_BASE + 999,
                button, action, fluid);
        }

        void stock(ItemResource resource, int amount) {
            try (Transaction transaction = Transaction.openRoot()) {
                this.helper.assertTrue(this.items.insert(resource, amount, transaction) == amount, "仓储测试存入必须足量");
                transaction.commit();
            }
        }

        int count(ItemResource resource) {
            int count = 0;
            for (int index = 0; index < this.items.size(); index++) {
                if (this.items.getResource(index).equals(resource)) count += this.items.getAmountAsInt(index);
            }
            return count;
        }

        @Override
        public void close() {
            for (BlockPos port : this.ports) StoragePortManager.unregister(this.id, this.helper.getLevel().dimension(), port);
            StorageServerStub.remove(this.playerId());
            PlayerSettings.get().getSettings().remove(this.playerId());
            this.playerLookup().remove(this.playerId(), this.player);
        }

        ServerPlayer player() {
            return this.player;
        }

        BlockPos core() {
            return this.core;
        }

        ResourceHandler<ItemResource> items() {
            return this.items;
        }

    }

    private static FluidStack water() {
        return new FluidStack(Fluids.WATER, 1);
    }

    private static void sync(GameTestHelper helper) {
        try (Fixture fixture = new Fixture(helper)) {
            fixture.fluid(FluidResource.of(Fluids.WATER), 400);
            fixture.fluid(FluidResource.of(ModFluids.HONEY.get()), 1500);
            fixture.stock(ItemResource.of(Items.DIAMOND), 1000);
            final var order = fixture.order();
            helper.assertTrue(order.size() == 3 && order.getInt(1) < StoragePortManager.FLUID_SLOT_BASE,
                "数量排序必须按 1 mB 等于 1 个物品混排");
            var result = fixture.sync(new IntArrayList(new int[]{-1, 0, 0, StoragePortManager.FLUID_SLOT_BASE}));
            helper.assertTrue(result.updates().size() == 1 && result.fluids().size() == 2, "流体必须独立同步，不能作为物品伪更新");
            final var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
            try {
                StorageServerStub.SyncResult.STREAM_CODEC.encode(buffer, result);
                final var decoded = StorageServerStub.SyncResult.STREAM_CODEC.decode(buffer);
                helper.assertTrue(decoded.fluids().size() == 2 && decoded.updates().size() == 1, "网络往返必须携带完整流体列表");
            } finally {
                buffer.release();
            }
        }
        helper.succeed();
    }

    private static void pickup(GameTestHelper helper) {
        try (Fixture fixture = new Fixture(helper)) {
            final var first = fixture.fluid(FluidResource.of(Fluids.WATER), 400);
            final var second = fixture.fluid(FluidResource.of(Fluids.WATER), 600);
            fixture.player.inventoryMenu.setCarried(new ItemStack(Items.BUCKET));
            var result = fixture.interact(StorageInput.FLUID_BUCKET, 0, water());
            helper.assertTrue(result.changed() && result.carried().is(Items.WATER_BUCKET), "按流体身份取桶，不能依赖已过期的伪槽编号");
            helper.assertTrue(first.getFluid().isEmpty() && second.getFluid().isEmpty(), "应跨端口凑足一桶且精确扣液");
            final var synced = fixture.sync(new IntArrayList());
            helper.assertTrue(synced.fluids().getFirst().amount() == 0 && !synced.fluids().getFirst().icon().isEmpty(),
                "抽空后同步必须保留图标与零数量");
        }
        helper.succeed();
    }

    private static void containers(GameTestHelper helper) {
        try (Fixture fixture = new Fixture(helper)) {
            final var port = fixture.fluid(FluidResource.of(Fluids.WATER), 2500);
            fixture.stock(ItemResource.of(Items.BUCKET), 2);
            var result = fixture.interact(StorageInput.FLUID_BUCKET, 0, water());
            helper.assertTrue(result.changed() && fixture.count(ItemResource.of(Items.BUCKET)) == 1,
                "指针和背包无桶时应消耗仓储内的空桶");
            fixture.player.inventoryMenu.setCarried(new ItemStack(Items.DIAMOND));
            fixture.player.getInventory().setItem(12, new ItemStack(Items.BUCKET));
            result = fixture.interact(StorageInput.QUICK_MOVE_FROM_STORAGE, 0, water());
            helper.assertTrue(result.changed() && result.carried().is(Items.DIAMOND) && port.getFluid().getAmount() == 500,
                "Shift 装桶进入背包并保留被占用的指针");
            helper.assertTrue(fixture.count(ItemResource.of(Items.BUCKET)) == 1, "背包空桶必须优先于仓储空桶消耗");
        }
        helper.succeed();
    }

    private static void notices(GameTestHelper helper) {
        try (Fixture fixture = new Fixture(helper)) {
            final var port = fixture.fluid(FluidResource.of(Fluids.WATER), 250);
            helper.assertTrue(fixture.interact(StorageInput.FLUID_BUCKET, 0, water()).notice()
                == StorageServerStub.FluidNotice.BUCKET_MISSING,
                "同时缺桶和缺液时优先提示缺桶");
            fixture.player.inventoryMenu.setCarried(new ItemStack(Items.BUCKET));
            var result = fixture.interact(StorageInput.FLUID_BUCKET, 0, water());
            helper.assertTrue(result.notice() == StorageServerStub.FluidNotice.NOT_ENOUGH && !result.changed()
                && result.carried().is(Items.BUCKET) && port.getFluid().getAmount() == 250, "不足一桶时不能吞桶或部分抽液");
            fixture.player.inventoryMenu.setCarried(new ItemStack(Items.DIAMOND));
            helper.assertTrue(fixture.interact(StorageInput.FLUID_BUCKET, 0, water()).notice() == StorageServerStub.FluidNotice.NONE,
                "指针被其它物品占用时不应给出无关缺桶提示");
        }
        helper.succeed();
    }

    /** 模拟查询有存量、实际能力拒绝抽取的端口，用于证明多端口与空桶一起回滚。 */
    private static final class RefusingPort extends StorageFluidPortBlockEntity {
        private final ResourceHandler<FluidResource> refusing = new FluidStacksResourceHandler(1, CAPACITY_MB);

        private RefusingPort(BlockPos pos, BlockState state) {
            super(ModBlockEntities.STORAGE_FLUID_PORT.get(), pos, state);
        }

        @Override
        public ResourceHandler<FluidResource> getFluidHandler() {
            return this.refusing;
        }
    }

    private static void rollback(GameTestHelper helper) {
        try (Fixture fixture = new Fixture(helper)) {
            final var good = fixture.fluid(FluidResource.of(Fluids.WATER), 500);
            final var old = fixture.fluid(FluidResource.of(Fluids.WATER), 500);
            final var bad = new RefusingPort(old.getBlockPos(), old.getBlockState());
            bad.getTank().set(0, FluidResource.of(Fluids.WATER), 500);
            helper.getLevel().setBlockEntity(bad);
            StoragePortManager.register(fixture.id, helper.getLevel(), bad.getBlockPos());
            fixture.player.inventoryMenu.setCarried(new ItemStack(Items.BUCKET));
            var result = fixture.interact(StorageInput.FLUID_BUCKET, 0, water());
            helper.assertTrue(!result.changed() && result.notice() == StorageServerStub.FluidNotice.NOT_ENOUGH,
                "实际能力拒绝抽液时必须报告不足");
            helper.assertTrue(good.getFluid().getAmount() == 500 && fixture.player.inventoryMenu.getCarried().is(Items.BUCKET),
                "之前已抽的端口和已消耗的空桶必须一起回滚");
        }
        helper.succeed();
    }

    private static void pour(GameTestHelper helper) {
        try (Fixture fixture = new Fixture(helper)) {
            final var port = fixture.fluid(FluidResource.of(Fluids.WATER), 1000);
            fixture.player.inventoryMenu.setCarried(new ItemStack(Items.WATER_BUCKET, 2));
            var result = fixture.interact(StorageInput.FLUID_BUCKET, 0, water());
            helper.assertTrue(result.changed() && result.carried().isEmpty() && port.getFluid().getAmount() == 3000,
                "左键应倾倒整叠容器且清空指针");
            helper.assertTrue(fixture.count(ItemResource.of(Items.BUCKET)) == 2, "倾倒后的空容器必须优先归入仓储");
            fixture.player.inventoryMenu.setCarried(new ItemStack(Items.WATER_BUCKET));
            fixture.interact(StorageInput.PICKUP, 1, water());
            helper.assertTrue(port.getFluid().getAmount() == 3000 && fixture.count(ItemResource.of(Items.WATER_BUCKET)) == 1,
                "右键应存桶物品，不倾倒其中流体");
            fixture.player.getInventory().setItem(10, new ItemStack(Items.WATER_BUCKET));
            fixture.authorize();
            StorageServerStub.deposit(fixture.playerId(), fixture.core.asLong(), false, true);
            helper.assertTrue(port.getFluid().getAmount() == 4000, "批量存入按钮的左键也应倾倒桶");
            fixture.player.getInventory().setItem(10, new ItemStack(Items.WATER_BUCKET));
            fixture.authorize();
            StorageServerStub.deposit(fixture.playerId(), fixture.core.asLong(), true, false);
            helper.assertTrue(port.getFluid().getAmount() == 4000 && fixture.count(ItemResource.of(Items.WATER_BUCKET)) == 2,
                "批量存入按钮的右键应保留桶内流体");
        }
        helper.succeed();
    }

    private static void filters(GameTestHelper helper) {
        try (Fixture fixture = new Fixture(helper)) {
            fixture.fluid(FluidResource.of(Fluids.WATER), 1500);
            fixture.stock(ItemResource.of(Items.DIAMOND), 1000);
            final var setting = PlayerSettings.getSetting(helper.getLevel().registryAccess(), fixture.playerId());
            setting.listed().stream().filter(entry -> entry.getCategory() instanceof FluidCategory)
                .forEach(entry -> entry.changeMode(CategoryMode.ALLOWLIST));
            helper.assertTrue(fixture.order().size() == 1 && fixture.order().getFirst() >= StoragePortManager.FLUID_SLOT_BASE,
                "流体白名单必须过滤物品，保留流体");
            setting.storage().setSearchContent("水");
            helper.assertTrue(fixture.order().size() == 1, "服务端不能用自身语言过滤客户端本地化流体名称");
            setting.storage().setSearchContent("#water");
            helper.assertTrue(fixture.order().isEmpty(), "物品标签搜索不能混入流体");
            helper.assertTrue(FluidAmountUtil.formatAmount(1575).equals("1.58 B")
                && FluidAmountUtil.formatExactAmount(1575).equals("1.575 B")
                && FluidAmountUtil.formatAmount(1000000).equals("1K B"), "数量文本必须保留源版单位和精度");
        }
        helper.succeed();
    }

    private static void bottles(GameTestHelper helper) {
        try (Fixture fixture = new Fixture(helper)) {
            final var honey = FluidResource.of(ModFluids.HONEY.get());
            final var port = fixture.fluid(honey, 1000);
            fixture.player.inventoryMenu.setCarried(new ItemStack(Items.HONEY_BOTTLE, 2));
            var result = fixture.interact(StorageInput.FLUID_BUCKET, 0, new FluidStack(ModFluids.HONEY.get(), 1));
            helper.assertTrue(result.changed() && result.carried().isEmpty() && port.getFluid().getAmount() == 1500,
                "蜂蜜瓶必须按每瓶 250 mB 倾倒，不能按桶量扣算");
            helper.assertTrue(fixture.count(ItemResource.of(Items.GLASS_BOTTLE)) == 2, "玻璃瓶必须优先返还仓储");
            port.getTank().set(0, honey, 127900);
            fixture.player.inventoryMenu.setCarried(new ItemStack(Items.HONEY_BOTTLE));
            result = fixture.interact(StorageInput.FLUID_BUCKET, 0, new FluidStack(ModFluids.HONEY.get(), 1));
            helper.assertTrue(!result.changed() && result.carried().is(Items.HONEY_BOTTLE) && port.getFluid().getAmount() == 127900,
                "剩余容量不足一瓶时不得部分倒液或吞瓶");
        }
        helper.succeed();
    }
}
