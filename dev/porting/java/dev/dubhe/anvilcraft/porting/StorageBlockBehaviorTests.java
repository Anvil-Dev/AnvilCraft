package dev.dubhe.anvilcraft.porting;

import com.mojang.authlib.GameProfile;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.saved.storage.BaseStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.network.protocol.game.ServerboundPickItemFromBlockPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class StorageBlockBehaviorTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_storage_behavior_levels", StorageBlockBehaviorTests::levels,
        "port_storage_behavior_aliases", StorageBlockBehaviorTests::aliases,
        "port_storage_behavior_parts", StorageBlockBehaviorTests::parts,
        "port_storage_behavior_pick", StorageBlockBehaviorTests::pick
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, r -> TESTS.forEach((name, test) -> r.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_storage_behavior"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 200, 0, true))));
    }

    private static StorageBlockEntity crate(GameTestHelper h, BlockPos pos, UUID id) {
        h.getLevel().setBlockAndUpdate(pos, ModBlocks.CRATE.getDefaultState());
        var be = (StorageBlockEntity) h.getLevel().getBlockEntity(pos);
        be.setId(id);
        Storages.get().getOrCreate(id, be.getStorageType().clazz());
        return be;
    }

    private static void insert(BaseStorage<?> storage, ItemResource item, int count, boolean commit) {
        try (Transaction transaction = Transaction.openRoot()) {
            if (storage.getItems().insert(item, count, transaction) != count) throw new IllegalStateException("Fixture capacity");
            if (commit) transaction.commit();
        }
    }

    private static void levels(GameTestHelper h) {
        var be = crate(h, h.absolutePos(new BlockPos(3, 3, 3)), UUID.randomUUID());
        var storage = Storages.get().get(be.getId()).orElseThrow();
        var stone = ItemResource.of(Items.STONE);
        h.assertTrue(be.getComparatorSignal() == 0, "Empty storage outputs zero");
        insert(storage, stone, 1, false);
        h.assertTrue(be.getComparatorSignal() == 0, "Rolled back insertion stays empty");
        insert(storage, stone, 1, true);
        h.assertTrue(be.getComparatorSignal() == 1, "One item rounds up to one");
        insert(storage, stone, 1023, true);
        h.assertTrue(be.getComparatorSignal() == 8, "Half full rounds up to eight");
        insert(storage, ItemResource.of(Items.DIAMOND_SWORD), 16, true);
        h.assertTrue(be.getComparatorSignal() == 15, "Unstackable items consume 64 capacity each");
        h.succeed();
    }

    private static void comparator(GameTestHelper h, BlockPos source, Direction facing) {
        BlockPos pos = source.relative(facing.getOpposite());
        h.getLevel().setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos, Blocks.COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING, facing));
    }

    private static void signal(GameTestHelper h, BlockPos source, Direction facing, int expected) {
        var be = (ComparatorBlockEntity) h.getLevel().getBlockEntity(source.relative(facing.getOpposite()));
        h.assertTrue(be.getOutputSignal() == expected, "Comparator at " + be.getBlockPos() + " expected " + expected
            + " got " + be.getOutputSignal());
    }

    private static void aliases(GameTestHelper h) {
        BlockPos a = h.absolutePos(new BlockPos(3, 3, 3));
        BlockPos b = h.absolutePos(new BlockPos(8, 3, 3));
        var be = crate(h, a, UUID.randomUUID());
        final var alias = crate(h, b, be.getId());
        var storage = Storages.get().get(be.getId()).orElseThrow();
        comparator(h, a, Direction.NORTH);
        comparator(h, b, Direction.NORTH);
        h.runAfterDelay(4, () -> insert(storage, ItemResource.of(Items.STONE), 1024, true));
        h.runAfterDelay(8, () -> {
            signal(h, a, Direction.NORTH, 8);
            signal(h, b, Direction.NORTH, 8);
            insert(storage, ItemResource.of(Items.STONE), 1024, false);
            alias.onChunkUnloaded();
            insert(storage, ItemResource.of(Items.STONE), 1024, true);
        });
        h.runAfterDelay(12, () -> {
            signal(h, a, Direction.NORTH, 15);
            signal(h, b, Direction.NORTH, 8);
            alias.onLoad();
            try (Transaction transaction = Transaction.openRoot()) {
                storage.getItems().extract(ItemResource.of(Items.STONE), 2048, transaction);
                transaction.commit();
            }
        });
        h.runAfterDelay(16, () -> {
            signal(h, a, Direction.NORTH, 0);
            signal(h, b, Direction.NORTH, 0);
            h.succeed();
        });
    }

    private static void parts(GameTestHelper h) {
        var be = StorageUpgradeTests.place(h, ModBlocks.LARGE_CRATE.get());
        var storage = Storages.get().getOrCreate(be.getId(), be.getStorageType().clazz());
        BlockPos main = be.getBlockPos();
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (int y = 0; y < 3; y++) {
                comparator(h, main.above(y).relative(facing.getOpposite()).relative(facing.getClockWise(), y - 1), facing);
            }
        }
        h.runAfterDelay(4, () -> insert(storage, ItemResource.of(Items.STONE), 32768, true));
        h.runAfterDelay(8, () -> {
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                for (int y = 0; y < 3; y++) {
                    signal(h, main.above(y).relative(facing.getOpposite()).relative(facing.getClockWise(), y - 1), facing, 8);
                }
            }
            h.succeed();
        });
    }

    private static void pick(GameTestHelper h) {
        var player = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "PortStoragePick"));
        player.setGameMode(GameType.CREATIVE);
        for (Block block : new Block[]{ModBlocks.CRATE.get(), ModBlocks.LARGE_CRATE.get(),
            ModBlocks.SHULKER_CONTAINER.get(), ModBlocks.HYPERDIMENSION_STORAGE_STATION.get()}) {
            StorageBlockEntity be = block instanceof AbstractMultiPartBlock<?> multipart
                ? StorageUpgradeTests.place(h, multipart) : crate(h, h.absolutePos(StorageUpgradeTests.CORE), UUID.randomUUID());
            BlockPos pos = block instanceof AbstractMultiPartBlock<?> ? be.getBlockPos().above(1).east() : be.getBlockPos();
            player.setPos(pos.getX(), pos.getY(), pos.getZ() + 2);
            for (boolean includeData : new boolean[]{false, true}) {
                player.getInventory().clearContent();
                player.connection.handlePickItemFromBlock(new ServerboundPickItemFromBlockPacket(pos, includeData));
                var stack = player.getMainHandItem();
                h.assertTrue(stack.is(block.asItem()), "Picked expected storage block " + block);
                var ref = stack.get(ModComponents.STORAGE);
                h.assertTrue(ref != null && ref.id().isPresent() == includeData, "Only Ctrl pick binds storage");
                if (includeData) {
                    h.assertTrue(ref.id().orElseThrow().equals(be.getId()), "Child pick preserves main storage ID");
                    BlockPos target = be.getBlockPos().west(5);
                    for (BlockPos clear : BlockPos.betweenClosed(target.offset(-1, 0, -1), target.offset(1, 2, 1))) {
                        h.getLevel().setBlock(clear, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                    }
                    h.getLevel().setBlockAndUpdate(target.below(), Blocks.STONE.defaultBlockState());
                    var context = new BlockPlaceContext(h.getLevel(), player, InteractionHand.MAIN_HAND, stack,
                        new BlockHitResult(Vec3.atCenterOf(target.below()), Direction.UP, target.below(), false));
                    h.assertTrue(((BlockItem) stack.getItem()).place(context).consumesAction(), "Picked stack can be placed");
                    var placed = (StorageBlockEntity) h.getLevel().getBlockEntity(target);
                    h.assertTrue(placed != null && be.getId().equals(placed.getId()), "Replacement aliases original storage");
                }
            }
        }
        h.succeed();
    }
}
