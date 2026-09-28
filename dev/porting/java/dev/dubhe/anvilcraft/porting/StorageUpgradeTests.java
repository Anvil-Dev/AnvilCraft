package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.TypeLimitItemStacksResourceHandler;
import dev.dubhe.anvilcraft.block.entity.storage.HyperdimensionStorageStationBlockEntity;
import dev.dubhe.anvilcraft.block.entity.storage.ShulkerContainerBlockEntity;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.event.anvil.AnvilEventListener;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.saved.storage.BaseStorage;
import dev.dubhe.anvilcraft.saved.storage.CraftingStorage;
import dev.dubhe.anvilcraft.saved.storage.HyperdimensionStorage;
import dev.dubhe.anvilcraft.saved.storage.LargeCrateStorage;
import dev.dubhe.anvilcraft.saved.storage.ShulkerContainerStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class StorageUpgradeTests {
    static final BlockPos CORE = new BlockPos(8, 3, 3);
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_storage_upgrade_chain", StorageUpgradeTests::chain,
        "port_storage_upgrade_reject", StorageUpgradeTests::reject,
        "port_storage_upgrade_overflow", StorageUpgradeTests::overflow,
        "port_storage_upgrade_roundtrip", StorageUpgradeTests::roundtrip,
        "port_storage_upgrade_opened", StorageUpgradeTests::opened
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, r -> TESTS.forEach((name, test) -> r.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_storage_upgrade"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 200, 0, true))));
    }

    static <P extends Enum<P>> StorageBlockEntity place(GameTestHelper h, AbstractMultiPartBlock<P> block) {
        var state = block.defaultBlockState();
        for (P part : block.getParts()) {
            h.getLevel().setBlock(h.absolutePos(CORE).offset(block.offsetFrom(state, part)),
                block.placedState(part, state), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        var entity = (StorageBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(CORE));
        entity.setId(UUID.randomUUID());
        return entity;
    }

    private static void stock(BaseStorage<?> storage, ItemStack stack, int amount) {
        try (Transaction transaction = Transaction.openRoot()) {
            int inserted = storage.getItems().insert(ItemResource.of(stack), amount, transaction);
            if (inserted != amount) throw new IllegalStateException("Fixture could not stock " + amount);
            transaction.commit();
        }
    }

    private static ItemEntity drop(GameTestHelper h, Item item, int count) {
        var point = h.absolutePos(CORE.above(3)).getCenter();
        var entity = new ItemEntity(h.getLevel(), point.x, point.y, point.z, new ItemStack(item, count));
        h.getLevel().addFreshEntity(entity);
        return entity;
    }

    private static void land(GameTestHelper h) {
        var pos = h.absolutePos(CORE.above(3));
        var entity = new FallingBlockEntity(h.getLevel(), pos.getX(), pos.getY(), pos.getZ(), Blocks.ANVIL.defaultBlockState());
        AnvilEventListener.onLand(new AnvilEvent.OnLand(h.getLevel(), pos, entity, 1));
    }

    private static int count(BaseStorage<?> storage, Item item) {
        int total = 0;
        for (int i = 0; i < storage.getItems().size(); i++) {
            if (storage.getItems().getResource(i).is(item)) total += (int) storage.getItems().getAmountAsLong(i);
        }
        return total;
    }

    private static void chain(GameTestHelper h) {
        var crate = place(h, ModBlocks.LARGE_CRATE.get());
        UUID crateId = crate.getId();
        var old = Storages.get().getOrCreate(crateId, LargeCrateStorage.class);
        stock(old, new ItemStack(Items.DIAMOND), 50000);
        stock(old, new ItemStack(Blocks.CRAFTING_TABLE), 1);
        stock(old, new ItemStack(Blocks.STONECUTTER), 1);
        h.assertTrue(old.unlockCrafting(), "Initial crafting unlock");
        old.setCrafting(CraftingStorage.EMPTY.withCraftingSlot(0, new ItemStack(Items.EMERALD, 3)));
        drop(h, ModBlocks.SPACE_OVERCOMPRESSOR.asItem(), 1);
        var netherite = drop(h, Items.NETHERITE_BLOCK, 9);
        land(h);
        var shulker = h.getBlockEntity(CORE, ShulkerContainerBlockEntity.class);
        var storage = Storages.get().get(shulker.getId(), ShulkerContainerStorage.class).orElseThrow();
        h.assertTrue(netherite.getItem().getCount() == 3, "Crate upgrade consumes exactly six netherite blocks");
        h.assertTrue(storage.getItems().getTypeLimit() == 1024 && count(storage, Items.DIAMOND) == 50000,
            "Initial source limits and stock");
        h.assertTrue(storage.isCraftingUnlocked() && storage.getCrafting().craftingInput().getFirst().getCount() == 3,
            "Crate upgrade preserves crafting unlock and contents");
        h.assertTrue(Storages.get().get(crateId).isEmpty(), "Old crate storage removed");
        var compressors = drop(h, ModBlocks.SPACE_OVERCOMPRESSOR.asItem(), 6);
        land(h);
        h.assertTrue(storage.getItems().getSpaceSize() == 1048576 && storage.getItems().getTypeLimit() == 16384,
            "Four upgrades double both dimensions");
        h.assertTrue(compressors.getItem().getCount() == 2, "Only four compressors consumed");
        var crystal = drop(h, ModBlocks.SINGULARITY_CRYSTAL.asItem(), 2);
        var cubes = drop(h, ModBlocks.HYPERCUBE.asItem(), 20);
        UUID oldId = shulker.getId();
        land(h);
        var station = h.getBlockEntity(CORE, HyperdimensionStorageStationBlockEntity.class);
        var result = Storages.get().get(station.getId(), HyperdimensionStorage.class).orElseThrow();
        h.assertTrue(!station.getId().equals(oldId) && Storages.get().get(oldId).isEmpty(), "Source UUID replacement semantics");
        h.assertTrue(crystal.getItem().getCount() == 1 && cubes.getItem().getCount() == 4 && compressors.getItem().getCount() == 2,
            "Hyper upgrade takes one crystal and sixteen cubes before capacity-upgrade behavior");
        h.assertTrue(count(result, Items.DIAMOND) == 50000 && result.isCraftingUnlocked()
            && result.getCrafting().craftingInput().getFirst().getCount() == 3, "All stored and crafting contents migrate");
        for (var part : Cube3x3PartHalf.values()) {
            h.assertTrue(h.getBlockState(CORE.offset(part.getOffset())).is(ModBlocks.HYPERDIMENSION_STORAGE_STATION),
                "All 27 parts replaced");
        }
        h.succeed();
    }

    private static void reject(GameTestHelper h) {
        var be = place(h, ModBlocks.SHULKER_CONTAINER.get());
        var storage = Storages.get().getOrCreate(be.getId(), ShulkerContainerStorage.class);
        var crystal = drop(h, ModBlocks.SINGULARITY_CRYSTAL.asItem(), 1);
        var cubes = drop(h, ModBlocks.HYPERCUBE.asItem(), 16);
        land(h);
        h.assertTrue(h.getBlockState(CORE).is(ModBlocks.SHULKER_CONTAINER) && !crystal.isRemoved() && cubes.getItem().getCount() == 16,
            "Insufficient capacity preserves block and materials");
        storage.getItems().addSpaceSize(ignored -> 1048576);
        cubes.getItem().setCount(15);
        land(h);
        h.assertTrue(h.getBlockState(CORE).is(ModBlocks.SHULKER_CONTAINER) && crystal.getItem().getCount() == 1,
            "Insufficient cubes do not consume the crystal");
        h.succeed();
    }

    private static void overflow(GameTestHelper h) {
        var be = place(h, ModBlocks.LARGE_CRATE.get());
        var old = Storages.get().getOrCreate(be.getId(), LargeCrateStorage.class);
        for (int i = 0; i < 1025; i++) {
            var stack = new ItemStack(Items.STONE);
            stack.set(DataComponents.CUSTOM_NAME, Component.literal("type " + i));
            stock(old, stack, 1);
        }
        drop(h, ModBlocks.SPACE_OVERCOMPRESSOR.asItem(), 1);
        drop(h, Items.NETHERITE_BLOCK, 6);
        land(h);
        var shulker = h.getBlockEntity(CORE, ShulkerContainerBlockEntity.class);
        var storage = Storages.get().get(shulker.getId(), ShulkerContainerStorage.class).orElseThrow();
        h.assertTrue(storage.getItems().getTypeCount() == 1025, "Upgrade retains types above the new insertion limit");
        try (Transaction transaction = Transaction.openRoot()) {
            h.assertTrue(storage.getItems().insert(ItemResource.of(Items.DIRT), 1, transaction) == 0,
                "New types remain blocked while over limit");
        }
        var ops = RegistryOps.create(NbtOps.INSTANCE, h.getLevel().registryAccess());
        var tag = BaseStorage.CODEC.codec().encodeStart(ops, storage).getOrThrow();
        var restored = BaseStorage.CODEC.codec().parse(ops, tag).getOrThrow();
        h.assertTrue(restored.getItems().getTypeCount() == 1025, "Storage codec does not discard migrated excess types");
        h.succeed();
    }

    private static void roundtrip(GameTestHelper h) {
        var storage = new ShulkerContainerStorage(UUID.randomUUID());
        storage.getItems().addTypeLimit(ignored -> 16384);
        storage.getItems().addSpaceSize(ignored -> 1048576);
        stock(storage, new ItemStack(Items.DIAMOND), 1048576);
        var ops = RegistryOps.create(NbtOps.INSTANCE, h.getLevel().registryAccess());
        var encoded = BaseStorage.CODEC.codec().encodeStart(ops, storage).getOrThrow();
        var restored = (ShulkerContainerStorage) BaseStorage.CODEC.codec().parse(ops, encoded).getOrThrow();
        h.assertTrue(restored.getItems().getTypeLimit() == 16384 && restored.getItems().getSpaceSize() == 1048576
            && count(restored, Items.DIAMOND) == 1048576, "Both upgraded limits and large counts survive reload");
        h.succeed();
    }

    private static void opened(GameTestHelper h) {
        var be = place(h, ModBlocks.SHULKER_CONTAINER.get());
        var storage = Storages.get().getOrCreate(be.getId(), ShulkerContainerStorage.class);
        storage.getItems().addSpaceSize(ignored -> 1048576);
        ModBlocks.SHULKER_CONTAINER.get().setOpened(h.getLevel(), h.absolutePos(CORE), true);
        var main = h.absolutePos(CORE);
        var state = h.getLevel().getBlockState(main);
        var point = main.above().getCenter();
        h.getLevel().addFreshEntity(new ItemEntity(h.getLevel(), point.x, point.y, point.z, ModBlocks.SINGULARITY_CRYSTAL.asStack()));
        h.getLevel().addFreshEntity(new ItemEntity(h.getLevel(), point.x, point.y, point.z, ModBlocks.HYPERCUBE.asStack(16)));
        var event = new AnvilEvent.OnLand(h.getLevel(), main.above(),
            new FallingBlockEntity(h.getLevel(), point.x, point.y, point.z, Blocks.ANVIL.defaultBlockState()), 1);
        h.assertTrue(new dev.dubhe.anvilcraft.anvil.Upgrade2HyperdimensionStationBehavior()
            .handle(h.getLevel(), main, state, 1, event), "Opened container upgrade");
        for (BlockPos pos : BlockPos.betweenClosed(main.offset(-2, 0, -2), main.offset(2, 5, 2))) {
            h.assertTrue(!h.getLevel().getBlockState(pos).is(ModBlocks.SHULKER_CONTAINER), "No opened parts remain at " + pos);
        }
        h.succeed();
    }

}
