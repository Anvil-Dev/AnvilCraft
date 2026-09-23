package dev.dubhe.anvilcraft.porting;

import com.mojang.serialization.JsonOps;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.StoragePortManager;
import dev.dubhe.anvilcraft.api.TerminalSessions;
import dev.dubhe.anvilcraft.api.TerminalSourceManager;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.HyperdimensionTerminalItem;
import dev.dubhe.anvilcraft.item.property.component.StorageRef;
import dev.dubhe.anvilcraft.item.property.component.TerminalBinding;
import dev.dubhe.anvilcraft.rpc.StorageInput;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import dev.dubhe.anvilcraft.rpc.StorageTerminalServerStub;
import dev.dubhe.anvilcraft.saved.storage.CraftingStorage;
import dev.dubhe.anvilcraft.saved.storage.StorageType;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class TerminalAccessTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_terminal_binding", TerminalAccessTests::binding,
        "port_terminal_upgrade", TerminalAccessTests::upgrade,
        "port_terminal_self_input", TerminalAccessTests::selfInput,
        "port_terminal_hyper_access", TerminalAccessTests::hyper,
        "port_terminal_local_access", TerminalAccessTests::local,
        "port_terminal_shulker_access", TerminalAccessTests::shulker,
        "port_terminal_crafting", TerminalAccessTests::crafting,
        "port_terminal_bundle", TerminalAccessTests::bundle,
        "port_terminal_fluid", TerminalAccessTests::fluid,
        "port_terminal_sessions", TerminalAccessTests::sessions
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_terminal_access"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    static IPayloadContext context(StorageFluidRpcTests.Fixture fixture) {
        return (IPayloadContext) Proxy.newProxyInstance(IPayloadContext.class.getClassLoader(), new Class<?>[]{IPayloadContext.class},
            (proxy, method, args) -> {
                if (method.getName().equals("player")) return fixture.player();
                throw new UnsupportedOperationException(method.getName());
            });
    }

    static boolean authorize(StorageFluidRpcTests.Fixture fixture, long token) {
        try {
            return new StorageServerStub.StorageAccessValidator().validate(context(fixture),
                StorageServerStub.class.getMethod("load", UUID.class, long.class), new Object[]{fixture.playerId(), token});
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    public static ItemStack bound(StorageFluidRpcTests.Fixture fixture) {
        var terminal = new ItemStack(ModItems.HYPERDIMENSION_TERMINAL.get());
        var core = (StorageBlockEntity) fixture.player().level().getBlockEntity(fixture.core());
        HyperdimensionTerminalItem.bindToStation(fixture.player(), terminal, core);
        fixture.player().getInventory().setItem(0, terminal);
        return terminal;
    }

    private static UUID id(ItemStack terminal) {
        return terminal.get(ModComponents.TERMINAL_BINDING).id().orElseThrow();
    }

    private static void binding(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var terminal = new ItemStack(ModItems.HYPERDIMENSION_TERMINAL.get());
            fixture.player().getInventory().setItem(0, terminal);
            helper.getLevel().getBlockState(fixture.core()).useItemOn(terminal, helper.getLevel(), fixture.player(),
                InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(fixture.core()), Direction.SOUTH, fixture.core(), false));
            UUID id = id(terminal);
            var other = TerminalSourceTests.place(helper, ModBlocks.HYPERDIMENSION_STORAGE_STATION.get(), new BlockPos(9, 1, 3));
            HyperdimensionTerminalItem.bindToStation(fixture.player(), terminal, other);
            helper.assertTrue(id.equals(id(terminal)), "已绑定终端不能被普通右键覆盖目标");
            var binding = terminal.get(ModComponents.TERMINAL_BINDING);
            var encoded = TerminalBinding.CODEC.codec().encodeStart(JsonOps.INSTANCE, binding).getOrThrow();
            helper.assertTrue(TerminalBinding.CODEC.codec().parse(JsonOps.INSTANCE, encoded).getOrThrow().equals(binding), "绑定必须持久化");
            var buffer = Unpooled.buffer();
            try {
                TerminalBinding.STREAM_CODEC.encode(buffer, binding);
                helper.assertTrue(TerminalBinding.STREAM_CODEC.decode(buffer).equals(binding), "绑定网络编解码必须一致");
            } finally {
                buffer.release();
            }
        }
        helper.succeed();
    }

    private static void hyper(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var terminal = bound(fixture);
            fixture.stock(ItemResource.of(Items.DIAMOND), 4);
            long token = TerminalSessions.open(fixture.player(), id(terminal));
            fixture.player().setPos(fixture.core().getX() + 1000, fixture.core().getY(), fixture.core().getZ());
            helper.assertTrue(token != -1 && authorize(fixture, token), "超维终端必须能在世界仓储交互距离外访问");
            helper.assertTrue(StorageServerStub.reorder(fixture.playerId(), token).size() == 1, "远程视图应读到绑定存储");
            fixture.player().getInventory().setItem(0, ItemStack.EMPTY);
            helper.assertTrue(!authorize(fixture, token), "不再持有终端时不得沿用旧会话访问");
            try {
                var validator = new StorageTerminalServerStub.TerminalAccessValidator();
                var method = StorageTerminalServerStub.class.getMethod("openRemote", UUID.class, UUID.class);
                helper.assertTrue(!validator.validate(context(fixture), method, new Object[]{fixture.playerId(), id(terminal)}),
                    "只知道存储 UUID 不能打开远程会话");
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException(error);
            }
        }
        helper.succeed();
    }

    private static void local(GameTestHelper helper) {
        TerminalSourceManager.clear(helper.getLevel());
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            var crate = TerminalSourceTests.place(helper, ModBlocks.LARGE_CRATE.get(), new BlockPos(9, 1, 3));
            fixture.player().getInventory().setItem(0, new ItemStack(ModItems.LOCAL_TERMINAL.get()));
            long token = TerminalSessions.open(fixture.player(), TerminalSessions.localTerminalId(fixture.playerId()));
            helper.assertTrue(token != -1 && TerminalSessions.storage(fixture.player(), token).getId().equals(crate.getId()),
                "本地终端必须连接附近大型板条箱");
            authorize(fixture, token);
            final var before = StorageServerStub.load(fixture.playerId(), token);
            fixture.player().setPos(crate.getBlockPos().getX() + 40, crate.getBlockPos().getY(), crate.getBlockPos().getZ());
            helper.assertTrue(authorize(fixture, token), "持有终端时会话仍有效，目标脱离范围应显示空视图");
            helper.assertTrue(StorageServerStub.reorder(fixture.playerId(), token).isEmpty(), "超出 32 格后不能继续显示旧目标");
            authorize(fixture, token);
            var after = StorageServerStub.load(fixture.playerId(), token);
            helper.assertTrue(!before.storageId().equals(after.storageId()), "切换目标必须同步身份，不能只依赖可能相等的版本号");
            var buffer = Unpooled.buffer();
            try {
                StorageServerStub.Metadata.STREAM_CODEC.encode(buffer, after);
                helper.assertTrue(StorageServerStub.Metadata.STREAM_CODEC.decode(buffer).equals(after), "目标身份必须参与网络元数据");
            } finally {
                buffer.release();
            }
            helper.assertTrue(TerminalSessions.storage(fixture.player(), token).getItems().size() == 0, "空视图不能接受存入");
        }
        helper.succeed();
    }

    private static void shulker(GameTestHelper helper) {
        TerminalSourceManager.clear(helper.getLevel());
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            var player = fixture.player();
            var terminal = new ItemStack(ModItems.SHULKER_TERMINAL.get());
            player.getInventory().setItem(0, terminal);
            var first = new ItemStack(ModBlocks.SHULKER_CONTAINER.asItem());
            first.set(ModComponents.STORAGE, new StorageRef(StorageType.SHULKER_CONTAINER));
            var second = new ItemStack(ModBlocks.SHULKER_CONTAINER.asItem());
            UUID secondId = UUID.randomUUID();
            second.set(ModComponents.STORAGE, new StorageRef(StorageType.SHULKER_CONTAINER, secondId));
            player.getInventory().setItem(1, first);
            player.getInventory().setItem(2, second);
            long token = TerminalSessions.open(player, TerminalSessions.shulkerTerminalId(player.getUUID()));
            UUID firstId = first.get(ModComponents.STORAGE).id().orElseThrow();
            helper.assertTrue(TerminalSessions.storage(player, token).getId().equals(firstId), "必须先给最靠前空集装箱分配身份并连接");
            player.getInventory().setItem(1, ItemStack.EMPTY);
            helper.assertTrue(TerminalSessions.storage(player, token).getId().equals(secondId), "会话应随随身集装箱顺序变化重新解析");
            player.getInventory().setItem(2, ItemStack.EMPTY);
            helper.assertTrue(TerminalSessions.storage(player, token).getId()
                .equals(StorageCraftingExecutionTests.storage(helper, fixture).getId()),
                "没有随身集装箱后应连接 64 格内世界集装箱");
        }
        helper.succeed();
    }

    private static void crafting(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var terminal = bound(fixture);
            final var world = StorageCraftingExecutionTests.storage(helper, fixture);
            world.setCrafting(CraftingStorage.EMPTY.withCraftingSlot(0, new ItemStack(Items.DIAMOND)));
            long token = TerminalSessions.open(fixture.player(), id(terminal));
            fixture.player().inventoryMenu.setCarried(new ItemStack(Items.OAK_LOG, 2));
            authorize(fixture, token);
            StorageServerStub.craftingPutCraftingSlot(fixture.playerId(), token, 8, 0, ItemStack.EMPTY);
            helper.assertTrue(terminal.get(ModComponents.CRAFTING).craftingInput().get(8).getCount() == 2
                && world.getCrafting().craftingInput().get(0).is(Items.DIAMOND), "终端合成输入不能写入世界主存储");
            authorize(fixture, token);
            var result = StorageServerStub.craftingTakeResult(fixture.playerId(), token, false, false);
            helper.assertTrue(result.carried().is(Items.OAK_PLANKS) && result.carried().getCount() == 4
                && terminal.get(ModComponents.CRAFTING).craftingInput().get(8).getCount() == 1, "终端必须完成实际合成并只消耗一次");
            authorize(fixture, token);
            StorageServerStub.craftingSetOptions(fixture.playerId(), token, true, true);
            authorize(fixture, token);
            StorageServerStub.craftingSetLastOpened(fixture.playerId(), token, true);
            helper.assertTrue(terminal.get(ModComponents.CRAFTING).autoFill() && terminal.get(ModComponents.CRAFTING).lastOpened()
                && !world.getCrafting().autoFill(), "终端选项和模式记忆必须保存在物品上");
        }
        helper.succeed();
    }

    private static void bundle(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var terminal = bound(fixture);
            int inserted = StorageServerStub.insertIntoTerminal(fixture.player(), id(terminal), new ItemStack(Items.STONE, 5), 5);
            helper.assertTrue(inserted == 5, "新物品类型必须可从终端插入");
            var extracted = StorageServerStub.extractFromTerminal(fixture.player(), id(terminal), 64,
                fixture.player().inventoryMenu.getSlot(9));
            helper.assertTrue(extracted.is(Items.STONE) && extracted.getCount() == 5
                && fixture.count(ItemResource.of(Items.STONE)) == 0,
                "终端取出必须使用真实库存并守恒");
            helper.assertTrue(StorageServerStub.insertIntoTerminal(fixture.player(), id(terminal), terminal, 1) == 0,
                "不能把超维终端嵌入超维/潜影目标");
        }
        helper.succeed();
    }

    private static void fluid(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var terminal = bound(fixture);
            final var port = fixture.fluid(FluidResource.of(Fluids.WATER), 1000);
            long token = TerminalSessions.open(fixture.player(), id(terminal));
            fixture.player().inventoryMenu.setCarried(new ItemStack(Items.BUCKET));
            authorize(fixture, token);
            var result = StorageServerStub.interact(fixture.playerId(), token, StoragePortManager.FLUID_SLOT_BASE, 0,
                StorageInput.FLUID_BUCKET, new FluidStack(Fluids.WATER, 1));
            helper.assertTrue(result.carried().is(Items.WATER_BUCKET) && port.getFluid().isEmpty(), "远程终端必须接通真实流体端口");
        }
        helper.succeed();
    }

    private static void upgrade(GameTestHelper helper) {
        var terminal = new ItemStack(ModItems.LOCAL_TERMINAL.get());
        terminal.set(ModComponents.CRAFTING, CraftingStorage.EMPTY.withCraftingSlot(8, new ItemStack(Items.DIAMOND, 5)));
        var input = CraftingInput.of(1, 3, List.of(new ItemStack(Items.SHULKER_SHELL), terminal, new ItemStack(Items.SHULKER_SHELL)));
        var recipe = helper.getLevel().getServer().getRecipeManager()
            .getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).orElseThrow();
        var output = recipe.value().assemble(input);
        helper.assertTrue(output.is(ModItems.SHULKER_TERMINAL)
            && output.get(ModComponents.CRAFTING).craftingInput().get(8).getCount() == 5, "终端升级不能丢失合成输入");
        helper.assertTrue(terminal.get(ModComponents.CRAFTING).craftingInput().get(8).getCount() == 5, "配方预览不得消耗输入组件");
        output.get(ModComponents.CRAFTING).craftingInput().get(8).shrink(1);
        helper.assertTrue(terminal.get(ModComponents.CRAFTING).craftingInput().get(8).getCount() == 5, "产物内容与输入不能共享可变栈");
        var bound = new ItemStack(ModItems.HYPERDIMENSION_TERMINAL.get());
        bound.set(ModComponents.TERMINAL_BINDING, new TerminalBinding(Optional.of(UUID.randomUUID())));
        bound.set(ModComponents.CRAFTING, terminal.get(ModComponents.CRAFTING));
        var unbindInput = CraftingInput.of(1, 1, List.of(bound));
        var unbind = helper.getLevel().getServer().getRecipeManager()
            .getRecipeFor(RecipeType.CRAFTING, unbindInput, helper.getLevel()).orElseThrow();
        var unbound = unbind.value().assemble(unbindInput);
        helper.assertTrue(unbound.get(ModComponents.TERMINAL_BINDING).id().isEmpty()
            && unbound.get(ModComponents.CRAFTING).craftingInput().get(8).getCount() == 5, "解除绑定只能清除目标，不能清除合成格物品");

        helper.succeed();
    }

    private static void selfInput(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var terminal = bound(fixture);
            var player = fixture.player();
            long token = TerminalSessions.open(player, id(terminal));
            player.getInventory().setItem(0, ItemStack.EMPTY);
            player.inventoryMenu.setCarried(terminal);
            authorize(fixture, token);
            helper.assertTrue(!StorageServerStub.craftingPutCraftingSlot(fixture.playerId(), token, 0, 0, terminal).changed()
                && player.inventoryMenu.getCarried() == terminal, "终端不能把自身吞入自己的合成格");
            authorize(fixture, token);
            helper.assertTrue(!StorageServerStub.craftingQuickCraft(fixture.playerId(), token, 0,
                new IntArrayList(new int[]{1}), new IntArrayList(), terminal).changed(), "拖拽也不能把终端填入自身");
            player.inventoryMenu.setCarried(ItemStack.EMPTY);
            player.getInventory().setItem(0, terminal);
            authorize(fixture, token);
            helper.assertTrue(!StorageServerStub.craftingTransfer(fixture.playerId(), token, false, false,
                List.of(terminal.copy()), ItemStack.EMPTY, new IntArrayList(new int[]{1})), "JEI 填料不能消费提供当前合成区的终端");
            helper.assertTrue(player.getInventory().getItem(0) == terminal
                && terminal.get(ModComponents.CRAFTING).craftingInput().stream().allMatch(ItemStack::isEmpty), "拒绝后终端及其数据必须保持完整");
        }
        helper.succeed();
    }

    private static void sessions(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var terminal = bound(fixture);
            long first = TerminalSessions.open(fixture.player(), id(terminal));
            for (int i = 0; i < 16; i++) TerminalSessions.open(fixture.player(), id(terminal));
            helper.assertTrue(!TerminalSessions.contains(fixture.playerId(), first) && !authorize(fixture, first),
                "会话数量应有界，过期虚拟坐标不能回退到世界访问");
            long last = TerminalSessions.open(fixture.player(), id(terminal));
            StorageServerStub.remove(fixture.playerId());
            helper.assertTrue(!TerminalSessions.contains(fixture.playerId(), last), "玩家退出应清除远程会话");
        }
        helper.succeed();
    }
}
