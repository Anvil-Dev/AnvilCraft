package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.UnlimitedItemStacksResourceHandler;
import dev.dubhe.anvilcraft.client.rpc.TerminalSnapshotLoader;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import io.netty.buffer.Unpooled;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class TerminalContentsPageTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_terminal_contents_pages", TerminalContentsPageTests::pages,
        "port_terminal_contents_merge", TerminalContentsPageTests::merge,
        "port_terminal_contents_consistency", TerminalContentsPageTests::consistency
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_terminal_contents_pages"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static void pages(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            final UUID target = TerminalAccessTests.bound(fixture).get(ModComponents.TERMINAL_BINDING).id().orElseThrow();
            var items = (UnlimitedItemStacksResourceHandler) fixture.items();
            var fillers = BuiltInRegistries.ITEM.stream().filter(item -> item != Items.AIR && item != Items.DIAMOND).limit(300).toList();
            for (int slot = 0; slot < fillers.size(); slot++) items.set(slot, ItemResource.of(fillers.get(slot)), 1);
            items.set(5000, ItemResource.of(Items.DIAMOND), 7);
            items.set(65535, ItemResource.of(Items.DIAMOND), 9);
            var first = StorageServerStub.terminalContentsPage(fixture.playerId(), List.of(target), 0);
            helper.assertTrue(first.items().size() == 256 && first.nextOffset() == 256 && !first.last(), "单页必须限制为 256 个非空槽");
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
            try {
                StorageServerStub.TerminalContentsPage.STREAM_CODEC.encode(buffer, first);
                var synced = StorageServerStub.TerminalContentsPage.STREAM_CODEC.decode(buffer);
                helper.assertTrue(synced.storageId().equals(first.storageId()) && synced.version() == first.version()
                    && synced.items().size() == 256 && synced.nextOffset() == 256 && !synced.last(), "分页信息必须完整经过网络编解码");
            } finally {
                buffer.release();
            }
            items.set(65535, ItemResource.of(Items.DIAMOND), 99);
            var tail = StorageServerStub.terminalContentsPage(fixture.playerId(), List.of(target), first.nextOffset());
            helper.assertTrue(tail.last() && tail.version() == first.version() && tail.items().stream()
                .filter(stack -> stack.is(Items.DIAMOND)).mapToInt(ItemStack::getCount).sum() == 16,
                "持续入库时后续页必须读取原快照而非混入新数量");
            items.set(65535, ItemResource.of(Items.DIAMOND), 9);
            var snapshot = TerminalSnapshotLoader.load(List.of(target), (id, offset) -> CompletableFuture.completedFuture(
                StorageServerStub.terminalContentsPage(fixture.playerId(), List.of(id), offset)), Runnable::run).join();
            helper.assertTrue(snapshot.complete() && snapshot.items().stream().filter(stack -> stack.is(Items.DIAMOND))
                .mapToInt(ItemStack::getCount).sum() == 16, "完整分页应包含第 5000 及第 65535 槽并合并数量");
            var restart = StorageServerStub.terminalContentsPage(fixture.playerId(), List.of(target), 0);
            var replacement = StorageServerStub.terminalContentsPage(fixture.playerId(), List.of(target), 0);
            helper.assertTrue(restart.version() != replacement.version(), "新批次必须使用独立编号，不能混用重复请求的页面");
            helper.assertTrue(StorageServerStub.terminalContentsPage(fixture.playerId(), List.of(target), -1).last()
                && StorageServerStub.terminalContentsPage(fixture.playerId(), List.of(UUID.randomUUID()), 0).items().isEmpty(),
                "非法游标及未绑定目标不能泄露物品");
        }
        helper.succeed();
    }

    private static void merge(GameTestHelper helper) {
        UUID first = UUID.randomUUID();
        UUID alias = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        AtomicInteger calls = new AtomicInteger();
        var snapshot = TerminalSnapshotLoader.load(List.of(first, alias, second), (target, offset) -> {
            calls.incrementAndGet();
            UUID actual = target.equals(alias) ? first : target;
            boolean last = !target.equals(first) || offset > 0;
            return CompletableFuture.completedFuture(new StorageServerStub.TerminalContentsPage(actual, 4, offset + 4096,
                List.of(new ItemStack(Items.DIAMOND, Integer.MAX_VALUE)),
                offset == 0 ? List.of(new StorageServerStub.FluidEntry(new FluidStack(Fluids.WATER, 1000), 1000)) : List.of(), last));
        }, Runnable::run).join();
        helper.assertTrue(snapshot.complete() && calls.get() == 4 && snapshot.items().size() == 1
            && snapshot.items().getFirst().getCount() == Integer.MAX_VALUE && snapshot.fluids().size() == 1
            && snapshot.fluids().getFirst().amount() == 2000, "同一实际仓储只能合并一次，跨页数量不能整数溢出");
        helper.succeed();
    }

    private static void consistency(GameTestHelper helper) {
        UUID target = UUID.randomUUID();
        for (int scenario = 0; scenario < 3; scenario++) {
            final int current = scenario;
            var result = TerminalSnapshotLoader.load(List.of(target), (id, offset) -> CompletableFuture.completedFuture(
                new StorageServerStub.TerminalContentsPage(current == 1 && offset > 0 ? UUID.randomUUID() : target,
                    current == 0 && offset > 0 ? 2 : 1, current == 2 ? 0 : offset + 4096,
                    List.of(new ItemStack(Items.IRON_INGOT)), List.of(), offset > 0)), Runnable::run);
            helper.assertTrue(result.isCompletedExceptionally(), "版本变化、目标切换或游标不前进必须丢弃整批数据");
        }
        var rejected = TerminalSnapshotLoader.load(List.of(target), (id, offset) -> {
            throw new IllegalStateException("request rejected");
        }, Runnable::run);
        helper.assertTrue(rejected.isCompletedExceptionally(), "同步发送失败必须转为可清理的异步错误");
        List<CompletableFuture<StorageServerStub.TerminalContentsPage>> responses = new ArrayList<>();
        var pending = TerminalSnapshotLoader.load(List.of(target), (id, offset) -> {
            var response = new CompletableFuture<StorageServerStub.TerminalContentsPage>();
            responses.add(response);
            return response;
        }, Runnable::run);
        responses.getFirst().complete(new StorageServerStub.TerminalContentsPage(target, 1, 4096,
            List.of(new ItemStack(Items.IRON_INGOT)), List.of(), false));
        helper.assertTrue(!pending.isDone() && responses.size() == 2, "不能发布尚未收齐的分页快照");
        responses.get(1).completeExceptionally(new IllegalStateException("disconnected"));
        helper.assertTrue(pending.isCompletedExceptionally(), "后续页失败不能把前面页当成完整快照");
        helper.succeed();
    }
}
