package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.StoragePortManager;
import dev.dubhe.anvilcraft.block.entity.StorageFluidPortBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class StorageFluidQueryTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_storage_fluid_query_identity", StorageFluidQueryTests::identity,
        "port_storage_fluid_query_transaction", StorageFluidQueryTests::transaction,
        "port_storage_fluid_query_acceptor", StorageFluidQueryTests::acceptor,
        "port_storage_fluid_query_liveness", StorageFluidQueryTests::liveness,
        "port_storage_fluid_query_dimensions", StorageFluidQueryTests::dimensions,
        "port_storage_fluid_query_codec", StorageFluidQueryTests::codec
    );

    private static FluidStack water() {
        return new FluidStack(Fluids.WATER, 1);
    }

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_storage_fluid_query"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    /** 查询测试直接登记真实端口；实际核心连接另由 StorageFluidPortTests 覆盖。 */
    private record Fixture(GameTestHelper helper, UUID id, List<BlockPos> ports) implements AutoCloseable {
        private Fixture(GameTestHelper helper) {
            this(helper, UUID.randomUUID(), new ArrayList<>());
        }

        private StorageFluidPortBlockEntity port(FluidStack fluid, int amount) {
            BlockPos pos = new BlockPos(2 + this.ports.size() * 2, 2, 2);
            this.helper.setBlock(pos, ModBlocks.STORAGE_FLUID_PORT.get());
            var port = this.helper.getBlockEntity(pos, StorageFluidPortBlockEntity.class);
            port.getTank().set(0, FluidResource.of(fluid), amount);
            this.ports.add(port.getBlockPos());
            StoragePortManager.register(this.id, this.helper.getLevel(), port.getBlockPos());
            return port;
        }

        @Override
        public void close() {
            for (BlockPos pos : this.ports) StoragePortManager.unregister(this.id, this.helper.getLevel().dimension(), pos);
        }
    }

    private static void identity(GameTestHelper helper) {
        try (Fixture fixture = new Fixture(helper)) {
            fixture.port(water(), 400);
            fixture.port(water(), 600);
            FluidStack named = water().copy();
            named.set(DataComponents.CUSTOM_NAME, Component.literal("query variant"));
            fixture.port(named, 250);
            fixture.port(new FluidStack(Fluids.LAVA, 1), 100);
            helper.assertTrue(StoragePortManager.collect(fixture.id).size() == 3, "同种流体合并，不同组件与种类必须分开");
            helper.assertTrue(Objects.requireNonNull(StoragePortManager.find(fixture.id, water())).amount() == 1000,
                "普通水必须跨端口合计");
            StoragePortManager.drain(fixture.id, water(), 1000);
            var zero = Objects.requireNonNull(StoragePortManager.find(fixture.id, water()));
            helper.assertTrue(zero.amount() == 0 && !zero.icon().isEmpty(), "抽空后仍须保留非空图标与零数量");
            helper.assertTrue(Objects.requireNonNull(StoragePortManager.find(fixture.id, named)).amount() == 250,
                "按身份抽取不能波及组件不同的流体");
            helper.assertTrue(StoragePortManager.collect(fixture.id).size() == 3, "抽空不能移除条目");
        }
        helper.succeed();
    }

    private static void transaction(GameTestHelper helper) {
        try (Fixture fixture = new Fixture(helper)) {
            var first = fixture.port(water(), 400);
            var second = fixture.port(water(), 700);
            helper.assertTrue(StoragePortManager.drain(fixture.id, water(), 2000, true) == 1100, "模拟必须汇总全部可抽取量");
            helper.assertTrue(first.getFluid().getAmount() == 400 && second.getFluid().getAmount() == 700,
                "模拟后各端口必须保持原量");
            try (Transaction transaction = Transaction.openRoot()) {
                helper.assertTrue(StoragePortManager.drain(fixture.id, water(), 1000, transaction) == 1000,
                    "调用方事务应能跨端口凑足一桶");
            }
            helper.assertTrue(first.getFluid().getAmount() == 400 && second.getFluid().getAmount() == 700,
                "容器步骤取消时必须回滚所有端口");
            helper.assertTrue(StoragePortManager.drain(fixture.id, water(), 1000) == 1000, "实际抽取必须提交");
            helper.assertTrue(Objects.requireNonNull(StoragePortManager.find(fixture.id, water())).amount() == 100,
                "提交后剩余总量必须精确");
            helper.assertTrue(StoragePortManager.drain(fixture.id, water(), 0) == 0, "零量抽取不能改变流体");
        }
        helper.succeed();
    }

    private static void acceptor(GameTestHelper helper) {
        try (Fixture fixture = new Fixture(helper)) {
            var empty = fixture.port(FluidStack.EMPTY, 0);
            helper.assertTrue(StoragePortManager.findAcceptor(fixture.id, water()) == null, "自动倒桶不能占用空端口");
            helper.assertTrue(StoragePortManager.findRefillTarget(fixture.id, water()) == empty.getFluidHandler(),
                "归还已抽出的流体必须允许空端口");
            var water = fixture.port(water(), 1000);
            helper.assertTrue(StoragePortManager.findAcceptor(fixture.id, water()) == water.getFluidHandler(),
                "自动倒桶应选择已有相同流体的端口");
            helper.assertTrue(StoragePortManager.findRefillTarget(fixture.id, water()) == water.getFluidHandler(),
                "归还目标也必须优先同种流体");
            water.clearFluid();
            helper.assertTrue(StoragePortManager.findAcceptor(fixture.id, water()) == null,
                "记忆的空条目不能被误认为实际存量");
        }
        helper.succeed();
    }

    private static void liveness(GameTestHelper helper) {
        try (Fixture fixture = new Fixture(helper)) {
            var port = fixture.port(water(), 1000);
            UUID other = UUID.randomUUID();
            StoragePortManager.unregister(fixture.id, helper.getLevel().dimension(), port.getBlockPos());
            StoragePortManager.register(other, helper.getLevel(), port.getBlockPos());
            try {
                helper.assertTrue(StoragePortManager.collect(fixture.id).isEmpty(), "转移归属后旧仓储不能继续显示流体");
                helper.assertTrue(StoragePortManager.drain(fixture.id, water(), 1000) == 0, "旧仓储不能抽走新仓储流体");
                helper.getLevel().setBlock(port.getBlockPos(), Blocks.STONE.defaultBlockState(), Block.UPDATE_ALL);
                helper.assertTrue(StoragePortManager.collect(other).isEmpty(), "残留登记必须忽略已经被替换的端口");
            } finally {
                StoragePortManager.unregister(other, helper.getLevel().dimension(), port.getBlockPos());
            }
        }
        helper.succeed();
    }

    private static void dimensions(GameTestHelper helper) {
        try (Fixture fixture = new Fixture(helper)) {
            fixture.port(water(), 1000);
            var nether = Objects.requireNonNull(helper.getLevel().getServer().getLevel(Level.NETHER));
            BlockPos remotePos = fixture.ports.getFirst().atY(250);
            nether.getChunkAt(remotePos);
            helper.assertTrue(nether.isEmptyBlock(remotePos), "跨维度测试目标必须为空气");
            nether.setBlock(remotePos, ModBlocks.STORAGE_FLUID_PORT.getDefaultState(), Block.UPDATE_CLIENTS);
            try {
                var remote = (StorageFluidPortBlockEntity) nether.getBlockEntity(remotePos);
                Objects.requireNonNull(remote).getTank().set(0, FluidResource.of(water()), 2000);
                StoragePortManager.register(fixture.id, nether, remotePos);
                helper.assertTrue(Objects.requireNonNull(StoragePortManager.find(fixture.id, water())).amount() == 3000,
                    "同一仓储必须汇总其他维度中的端口");
                StoragePortManager.unregister(fixture.id, helper.getLevel().dimension(), remotePos);
                helper.assertTrue(Objects.requireNonNull(StoragePortManager.find(fixture.id, water())).amount() == 3000,
                    "注销相同坐标的其他维度不能影响远端登记");
                helper.assertTrue(StoragePortManager.drain(fixture.id, water(), 2500) == 2500,
                    "抽取必须能跨维度凑足");
            } finally {
                StoragePortManager.unregister(fixture.id, nether.dimension(), remotePos);
                nether.removeBlock(remotePos, false);
            }
        }
        helper.succeed();
    }

    private static void codec(GameTestHelper helper) {
        FluidStack named = water().copy();
        named.set(DataComponents.CUSTOM_NAME, Component.literal("zero amount icon"));
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            var entry = new StorageServerStub.FluidEntry(named, 0);
            StorageServerStub.FluidEntry.STREAM_CODEC.encode(buffer, entry);
            var decoded = StorageServerStub.FluidEntry.STREAM_CODEC.decode(buffer);
            helper.assertTrue(decoded.amount() == 0 && FluidStack.isSameFluidSameComponents(named, decoded.icon()),
                "零数量网络条目必须保留流体类型和组件");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }
}
