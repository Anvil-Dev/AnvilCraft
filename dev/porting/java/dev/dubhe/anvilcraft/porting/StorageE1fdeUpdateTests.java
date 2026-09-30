package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.StorageUnlockCraftingEvent;
import dev.dubhe.anvilcraft.block.entity.HyperdimensionUploaderBlockEntity;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.saved.storage.BaseStorage;
import dev.dubhe.anvilcraft.saved.storage.CrateStorage;
import dev.dubhe.anvilcraft.saved.storage.HyperdimensionStorage;
import dev.dubhe.anvilcraft.saved.storage.ShulkerContainerStorage;
import dev.dubhe.anvilcraft.saved.storage.StorageType;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class StorageE1fdeUpdateTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_storage_source_bases_codec", StorageE1fdeUpdateTests::codec,
        "port_storage_source_unlock_event", StorageE1fdeUpdateTests::unlock,
        "port_storage_source_unlock_abort", StorageE1fdeUpdateTests::abort,
        "port_storage_source_restrictions", StorageE1fdeUpdateTests::restrictions,
        "port_storage_source_basis_drops", StorageE1fdeUpdateTests::drops
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, r -> TESTS.forEach((name, test) -> r.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_storage_source"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 150, 0, true))));
    }

    private static ItemStack named(Item item, String name) {
        var stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    private static void assertBases(GameTestHelper h, BaseStorage<?> storage, List<ItemStack> expected) {
        var actual = storage.getRecipeBases();
        h.assertTrue(actual != null && actual.size() == expected.size(), "Preserve unlocked state and basis count");
        for (int i = 0; i < expected.size(); i++) {
            h.assertTrue(ItemStack.isSameItemSameComponents(actual.get(i), expected.get(i))
                && actual.get(i).getCount() == expected.get(i).getCount(), "Preserve consumed basis components and count");
        }
    }

    private static void codec(GameTestHelper h) {
        var ops = h.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        List<ItemStack> bases = List.of(named(Items.CRAFTING_TABLE, "workbench"), new ItemStack(Items.STONECUTTER),
            named(Items.DIAMOND, "extension"));
        for (StorageType type : StorageType.values()) {
            var original = type.newInstance(UUID.randomUUID());
            original.setRecipeBases(bases);
            var tag = (CompoundTag) BaseStorage.CODEC.codec().encodeStart(ops, original).getOrThrow();
            assertBases(h, BaseStorage.CODEC.codec().parse(ops, tag).getOrThrow(), bases);
            var target = type.newInstance(UUID.randomUUID());
            target.copyCraftingFrom(original);
            assertBases(h, target, bases);
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
            try {
                BaseStorage.STREAM_CODEC.encode(buffer, original);
                assertBases(h, BaseStorage.STREAM_CODEC.decode(buffer), bases);
                h.assertTrue(!buffer.isReadable(), "Consume complete synchronized state");
            } finally {
                buffer.release();
            }
            tag.remove("recipe_bases");
            assertBases(h, BaseStorage.CODEC.codec().parse(ops, tag).getOrThrow(),
                List.of(new ItemStack(Items.CRAFTING_TABLE), new ItemStack(Items.STONECUTTER)));
            original.setRecipeBases(List.of());
            tag = (CompoundTag) BaseStorage.CODEC.codec().encodeStart(ops, original).getOrThrow();
            assertBases(h, BaseStorage.CODEC.codec().parse(ops, tag).getOrThrow(), List.of());
        }
        h.succeed();
    }

    private static void stock(BaseStorage<?> storage, ItemStack stack) {
        try (Transaction tx = Transaction.openRoot()) {
            if (storage.getItems().insert(ItemResource.of(stack), stack.getCount(), tx) != stack.getCount()) {
                throw new IllegalStateException("Fixture insertion failed");
            }
            tx.commit();
        }
    }

    private static int slot(BaseStorage<?> storage, Item item) {
        for (int slot = 0; slot < storage.getItems().size(); slot++) {
            if (storage.getItems().getResource(slot).is(item)) return slot;
        }
        throw new IllegalStateException("Missing fixture item " + item);
    }

    private static void unlock(GameTestHelper h) {
        var storage = new CrateStorage(UUID.randomUUID());
        var table = named(Items.CRAFTING_TABLE, "custom table");
        var diamond = named(Items.DIAMOND, "custom base");
        stock(storage, table);
        stock(storage, new ItemStack(ModBlocks.BATCH_CUTTER.asItem()));
        stock(storage, diamond);
        int extra = slot(storage, Items.DIAMOND);
        Consumer<StorageUnlockCraftingEvent> listener = event -> {
            if (event.getStorage() == storage) event.addRecipeBaseSlot(extra);
        };
        NeoForge.EVENT_BUS.addListener(listener);
        try {
            h.assertTrue(storage.unlockCrafting(), "Extension can add a consumed basis");
            assertBases(h, storage, List.of(table, new ItemStack(ModBlocks.BATCH_CUTTER.asItem()), diamond));
            h.assertTrue(storage.getItems().getAmountAsLong(extra) == 0 && storage.unlockCrafting(),
                "Consume extra once and preserve idempotence");
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
        }
        h.succeed();
    }

    private static void abort(GameTestHelper h) {
        for (int mode = 0; mode < 4; mode++) {
            var storage = new CrateStorage(UUID.randomUUID());
            stock(storage, new ItemStack(Items.CRAFTING_TABLE));
            stock(storage, new ItemStack(Items.STONECUTTER));
            int table = slot(storage, Items.CRAFTING_TABLE);
            int cutter = slot(storage, Items.STONECUTTER);
            final int selected = mode;
            Consumer<StorageUnlockCraftingEvent> listener = event -> {
                if (event.getStorage() != storage) return;
                if (selected == 0) {
                    event.setCanceled(true);
                } else {
                    event.addRecipeBaseSlot(selected == 1 ? -1 : selected == 2 ? storage.getItems().size() : cutter);
                }
            };
            NeoForge.EVENT_BUS.addListener(listener);
            try {
                h.assertTrue(!storage.unlockCrafting() && !storage.isCraftingUnlocked(), "Reject canceled or impossible consumption");
                h.assertTrue(storage.getItems().getAmountAsLong(table) == 1 && storage.getItems().getAmountAsLong(cutter) == 1,
                    "Rollback all prior extractions, including duplicate slots");
            } finally {
                NeoForge.EVENT_BUS.unregister(listener);
            }
        }
        h.succeed();
    }

    private static void restrictions(GameTestHelper h) {
        var hyper = new HyperdimensionStorage(UUID.randomUUID());
        var shulker = new ShulkerContainerStorage(UUID.randomUUID());
        var uploader = new HyperdimensionUploaderBlockEntity(ModBlockEntities.HYPERDIMENSION_UPLOADER.get(),
            BlockPos.ZERO, ModBlocks.HYPERDIMENSION_UPLOADER.getDefaultState());
        for (var item : List.of(ModItems.LOCAL_TERMINAL.asItem(), ModItems.SHULKER_TERMINAL.asItem(),
            ModItems.HYPERDIMENSION_TERMINAL.asItem(), ModBlocks.HYPERDIMENSION_STORAGE_STATION.asItem(),
            ModBlocks.SHULKER_CONTAINER.asItem())) {
            var resource = ItemResource.of(item);
            try (Transaction tx = Transaction.openRoot()) {
                h.assertTrue(hyper.getItems().insert(resource, 1, tx) == 0 && shulker.getItems().insert(resource, 1, tx) == 0
                    && uploader.getBuffer().insert(resource, 1, tx) == 0, "Every insertion path rejects station-forbidden items");
            }
        }
        for (var storage : List.of(hyper, shulker)) {
            stock(storage, new ItemStack(Items.SHULKER_BOX));
            h.assertTrue(storage.getItems().getResource(slot(storage, Items.SHULKER_BOX)).is(Items.SHULKER_BOX),
                "Unmarked vanilla container follows the new source contract");
        }
        h.assertTrue(!ModBlocks.HYPERDIMENSION_STORAGE_STATION.asItem().canFitInsideContainerItems(),
            "Storage station cannot nest in item containers");
        h.succeed();
    }

    private static void drops(GameTestHelper h) {
        BlockPos pos = h.absolutePos(new BlockPos(4, 3, 4));
        h.setBlock(new BlockPos(4, 3, 4), ModBlocks.CRATE.get());
        var blockEntity = (StorageBlockEntity) h.getLevel().getBlockEntity(pos);
        var storage = new CrateStorage(UUID.randomUUID());
        var base = named(Items.DIAMOND, "refund");
        storage.setRecipeBases(List.of(base));
        Storages.get().put(storage);
        blockEntity.setId(storage.getId());
        blockEntity.dropContents(h.getLevel(), pos);
        blockEntity.dropContents(h.getLevel(), pos);
        var drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2));
        h.assertTrue(drops.size() == 1 && ItemStack.isSameItemSameComponents(drops.getFirst().getItem(), base),
            "Refund exact recorded basis once; never synthesize vanilla bases for a custom unlock");
        h.succeed();
    }
}
