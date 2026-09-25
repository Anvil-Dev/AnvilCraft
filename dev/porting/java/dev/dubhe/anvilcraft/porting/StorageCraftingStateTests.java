package dev.dubhe.anvilcraft.porting;

import dev.anvilcraft.lib.v2.util.UnlimitedItemStack;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.UnlimitedItemStacksResourceHandler;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import dev.dubhe.anvilcraft.saved.storage.BaseStorage;
import dev.dubhe.anvilcraft.saved.storage.CraftingStorage;
import dev.dubhe.anvilcraft.saved.storage.StorageType;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.IntObjectBiConsumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class StorageCraftingStateTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_crafting_state_codec", StorageCraftingStateTests::stateCodec,
        "port_crafting_storage_codec", StorageCraftingStateTests::storageCodec,
        "port_crafting_component", StorageCraftingStateTests::component,
        "port_crafting_unlock", StorageCraftingStateTests::unlock,
        "port_crafting_unlock_rollback", StorageCraftingStateTests::rollback,
        "port_crafting_state_rpc", StorageCraftingStateTests::rpc
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_crafting_state"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static CraftingStorage sample() {
        ItemStack named = new ItemStack(Items.DIAMOND, 7);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("crafting state"));
        return CraftingStorage.EMPTY.withStonecutterInput(new ItemStack(Items.STONE, 12)).withCraftingSlot(4, named)
            .withStonecutterSelected(3).withLastOpened(true).withAutoFill(true).withToStorage(true);
    }

    private static void assertState(GameTestHelper helper, CraftingStorage state) {
        helper.assertTrue(state.craftingInput().size() == 9 && state.stonecutterInput().is(Items.STONE)
            && state.stonecutterInput().getCount() == 12, "切石输入及九格布局必须保存");
        helper.assertTrue(state.craftingInput().get(4).getCount() == 7
            && state.craftingInput().get(4).getHoverName().getString().equals("crafting state"), "合成格组件与数量必须保留");
        helper.assertTrue(state.stonecutterSelected() == 3 && state.lastOpened() && state.autoFill() && state.toStorage(),
            "选中配方和三个模式选项必须保留");
    }

    private static void stateCodec(GameTestHelper helper) {
        var ops = helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        var encoded = CraftingStorage.CODEC.codec().encodeStart(ops, sample()).getOrThrow();
        assertState(helper, CraftingStorage.CODEC.codec().parse(ops, encoded).getOrThrow());
        for (int size : new int[]{0, 3, 12}) {
            var input = new CraftingStorage(ItemStack.EMPTY, Collections.nCopies(size, new ItemStack(Items.STICK)), 0, false, false, false);
            var tag = CraftingStorage.CODEC.codec().encodeStart(ops, input).getOrThrow();
            var decoded = CraftingStorage.CODEC.codec().parse(ops, tag).getOrThrow();
            helper.assertTrue(decoded.craftingInput().size() == 9, "旧数据的短/长输入列表必须归一为九格");
        }
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            CraftingStorage.STREAM_CODEC.encode(buffer, sample());
            assertState(helper, CraftingStorage.STREAM_CODEC.decode(buffer));
        } finally {
            buffer.release();
        }
        helper.succeed();
    }

    private static void stock(BaseStorage<?> storage, ItemResource resource, int count) {
        try (Transaction transaction = Transaction.openRoot()) {
            if (storage.getItems().insert(resource, count, transaction) != count) throw new IllegalStateException("测试材料存入不足");
            transaction.commit();
        }
    }

    private static void storageCodec(GameTestHelper helper) {
        var ops = helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        for (StorageType type : StorageType.values()) {
            var storage = type.newInstance(UUID.randomUUID());
            stock(storage, ItemResource.of(Items.CRAFTING_TABLE), 2);
            stock(storage, ItemResource.of(Items.STONECUTTER), 1);
            helper.assertTrue(storage.unlockCrafting(), "四类仓储都应能解锁合成面板");
            storage.setCrafting(sample());
            var tag = (CompoundTag) BaseStorage.CODEC.codec().encodeStart(ops, storage).getOrThrow();
            var restored = BaseStorage.CODEC.codec().parse(ops, tag).getOrThrow();
            helper.assertTrue(restored.isCraftingUnlocked() && restored.getId().equals(storage.getId()), "仓储解锁标记及身份必须保存");
            assertState(helper, restored.getCrafting());
            tag.remove("crafting");
            tag.remove("crafting_unlocked");
            var legacy = BaseStorage.CODEC.codec().parse(ops, tag).getOrThrow();
            helper.assertTrue(!legacy.isCraftingUnlocked() && legacy.getCrafting().craftingInput().stream().allMatch(ItemStack::isEmpty),
                "缺少新字段的旧存储必须正常加载默认状态");
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
            try {
                BaseStorage.STREAM_CODEC.encode(buffer, storage);
                var synced = BaseStorage.STREAM_CODEC.decode(buffer);
                helper.assertTrue(synced.isCraftingUnlocked(), "仓储同步必须携带解锁状态");
                assertState(helper, synced.getCrafting());
            } finally {
                buffer.release();
            }
        }
        helper.succeed();
    }

    private static void component(GameTestHelper helper) {
        var ops = helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        ItemStack item = new ItemStack(Items.STICK);
        item.set(ModComponents.CRAFTING, sample());
        var restored = ItemStack.CODEC.parse(ops, ItemStack.CODEC.encodeStart(ops, item).getOrThrow()).getOrThrow();
        assertState(helper, restored.get(ModComponents.CRAFTING));
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            ItemStack.STREAM_CODEC.encode(buffer, item);
            assertState(helper, ItemStack.STREAM_CODEC.decode(buffer).get(ModComponents.CRAFTING));
        } finally {
            buffer.release();
        }
        helper.succeed();
    }

    private static void unlock(GameTestHelper helper) {
        var storage = StorageType.CRATE.newInstance(UUID.randomUUID());
        stock(storage, ItemResource.of(Items.CRAFTING_TABLE), 2);
        helper.assertTrue(!storage.unlockCrafting() && storage.getItems().getAmountAsLong(0) == 2,
            "缺少切石机时不能消费工作台");
        stock(storage, ItemResource.of(ModBlocks.BATCH_CUTTER.asItem()), 2);
        helper.assertTrue(storage.unlockCrafting() && storage.isCraftingUnlocked(), "批量切石机标签应作为解锁材料");
        helper.assertTrue(storage.getItems().getAmountAsLong(0) == 1 && storage.getItems().getAmountAsLong(1) == 1,
            "首次解锁只消费一件工作台及一件切石机");
        helper.assertTrue(storage.unlockCrafting() && storage.getItems().getAmountAsLong(0) == 1
            && storage.getItems().getAmountAsLong(1) == 1, "再次解锁不得重复扣材料");
        helper.succeed();
    }

    private static void rollback(GameTestHelper helper) {
        BaseStorage<?> storage = new BaseStorage<UnlimitedItemStacksResourceHandler>(UUID.randomUUID()) {
            @Override
            protected UnlimitedItemStacksResourceHandler constructItemHandler(IntObjectBiConsumer<UnlimitedItemStack> changed) {
                return new UnlimitedItemStacksResourceHandler(2) {
                    @Override
                    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
                        return index == 1 ? 0 : super.extract(index, resource, amount, transaction);
                    }
                };
            }
        };
        storage.getItems().set(0, ItemResource.of(Items.CRAFTING_TABLE), 1);
        storage.getItems().set(1, ItemResource.of(Items.STONECUTTER), 1);
        helper.assertTrue(!storage.unlockCrafting() && !storage.isCraftingUnlocked()
            && storage.getItems().getAmountAsLong(0) == 1, "第二种材料拒绝抽取时必须回滚第一种材料");
        helper.succeed();
    }

    private static void rpc(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            fixture.authorize();
            helper.assertTrue(!StorageServerStub.craftingAvailable(fixture.playerId(), fixture.core().asLong()), "默认未解锁");
            fixture.stock(ItemResource.of(Items.CRAFTING_TABLE), 1);
            fixture.stock(ItemResource.of(Items.STONECUTTER), 1);
            fixture.authorize();
            helper.assertTrue(StorageServerStub.craftingUnlock(fixture.playerId(), fixture.core().asLong()), "合法访问应能解锁");
            fixture.authorize();
            StorageServerStub.craftingSetOptions(fixture.playerId(), fixture.core().asLong(), true, true);
            fixture.authorize();
            StorageServerStub.craftingSelect(fixture.playerId(), fixture.core().asLong(), 3);
            fixture.authorize();
            StorageServerStub.craftingSetLastOpened(fixture.playerId(), fixture.core().asLong(), true);
            fixture.authorize();
            var state = StorageServerStub.craftingGet(fixture.playerId(), fixture.core().asLong());
            helper.assertTrue(state.autoFill() && state.toStorage() && state.lastOpened() && state.stonecutterSelected() == 3,
                "选项及关闭模式必须写回同一仓储");
        }
        helper.succeed();
    }
}
