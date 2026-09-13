package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.UnlimitedItemStacksResourceHandler;
import dev.dubhe.anvilcraft.rpc.StorageInput;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import dev.dubhe.anvilcraft.util.FormattingUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class StorageLongCountTests {
    private static final long COUNT = 2L * Integer.MAX_VALUE + 7;
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_storage_long_sync", StorageLongCountTests::sync,
        "port_storage_long_save", StorageLongCountTests::save,
        "port_storage_long_format", StorageLongCountTests::format
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_storage_long"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static void stock(StorageFluidRpcTests.Fixture fixture) {
        fixture.stock(ItemResource.of(Items.DIAMOND), Integer.MAX_VALUE);
        fixture.stock(ItemResource.of(Items.DIAMOND), Integer.MAX_VALUE);
        fixture.stock(ItemResource.of(Items.DIAMOND), 7);
        fixture.stock(ItemResource.of(Items.IRON_INGOT), Integer.MAX_VALUE);
    }

    private static void sync(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            stock(fixture);
            var order = fixture.order();
            helper.assertTrue(order.size() == 2 && order.getInt(0) == 1, "超过 int 的合并数量必须正确参与排序");
            var result = fixture.sync(order);
            helper.assertTrue(result.fullness() == 0, "无容量上限的超维仓储不应把物品组数当作占用比例");
            var diamond = result.updates().stream().filter(update -> update.stack().getItem() == Items.DIAMOND).findFirst().orElseThrow();
            helper.assertTrue(diamond.count() == COUNT && diamond.stack().getCount() == Integer.MAX_VALUE,
                "真实数量保持 long，图标堆叠保持 int 上限");
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
            try {
                StorageServerStub.StackUpdate.STREAM_CODEC.encode(buffer, diamond);
                helper.assertTrue(StorageServerStub.StackUpdate.STREAM_CODEC.decode(buffer).count() == COUNT,
                    "网络数量不能截断或变为负数");
            } finally {
                buffer.release();
            }
            fixture.authorize();
            var picked = StorageServerStub.interact(fixture.playerId(), fixture.core().asLong(), diamond.index(), 0,
                StorageInput.PICKUP, FluidStack.EMPTY);
            helper.assertTrue(picked.changed() && picked.carried().getCount() == 64, "大数量条目单次取出仍按一组限制");
            var after = fixture.sync(fixture.order());
            helper.assertTrue(after.updates().stream().filter(update -> update.stack().getItem() == Items.DIAMOND)
                .findFirst().orElseThrow().count() == COUNT - 64, "取出后必须精确扣减合并数量");
        }
        helper.succeed();
    }

    private static void save(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            stock(fixture);
            var ops = helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
            var handler = (UnlimitedItemStacksResourceHandler) fixture.items();
            var encoded = UnlimitedItemStacksResourceHandler.CODEC.codec().encodeStart(ops, handler).getOrThrow();
            var restored = UnlimitedItemStacksResourceHandler.CODEC.codec().parse(ops, encoded).getOrThrow();
            long count = 0;
            for (int slot = 0; slot < restored.size(); slot++) {
                if (restored.getResource(slot).is(Items.DIAMOND)) count += restored.getAmountAsLong(slot);
            }
            helper.assertTrue(count == COUNT, "存档仍以原有 int 槽位保存，合并总量不能丢失");
        }
        helper.succeed();
    }

    private static void format(GameTestHelper helper) {
        helper.assertTrue(FormattingUtil.toAbbrNum(COUNT).equals("4.2B"), "十亿级数量应沿用源版缩写");
        helper.assertTrue(FormattingUtil.toAbbrNum(1_234_567_890_123L).equals("1.2T"), "万亿级数量应显示 T");
        helper.assertTrue(FormattingUtil.toAbbrNum(1_000_000_000_000_000L).equals("1P"), "千万亿级数量应显示 P");
        helper.assertTrue(FormattingUtil.toAbbrNum(Long.MAX_VALUE).equals("9.2E"), "long 上限应能显示而不溢出");
        helper.succeed();
    }
}
