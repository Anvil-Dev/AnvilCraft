package dev.dubhe.anvilcraft.porting;

import com.mojang.authlib.GameProfile;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.saved.storage.BaseStorage;
import dev.dubhe.anvilcraft.saved.storage.ShulkerContainerStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class StorageRemovalTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_storage_removal_crate", StorageRemovalTests::crate,
        "port_storage_removal_large", StorageRemovalTests::large,
        "port_storage_removal_moving", StorageRemovalTests::moving,
        "port_storage_removal_empty", h -> containers(h, "empty"),
        "port_storage_removal_filled", h -> containers(h, "filled"),
        "port_storage_removal_crafting", h -> containers(h, "crafting"),
        "port_storage_removal_upgraded", h -> containers(h, "upgraded")
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, r -> TESTS.forEach((name, test) -> r.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_storage_removal"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 200, 0, true))));
    }

    private static BaseStorage<?> storage(StorageBlockEntity be) {
        return Storages.get().getOrCreate(be.getId(), be.getStorageType().clazz());
    }

    private static void insert(BaseStorage<?> storage, Item item, int amount) {
        try (Transaction tx = Transaction.openRoot()) {
            if (storage.getItems().insert(ItemResource.of(item), amount, tx) != amount) throw new IllegalStateException("Fixture capacity");
            tx.commit();
        }
    }

    private static void unlock(BaseStorage<?> storage) {
        insert(storage, Items.CRAFTING_TABLE, 1);
        insert(storage, Items.STONECUTTER, 1);
        if (!storage.unlockCrafting()) throw new IllegalStateException("Fixture crafting unlock");
    }

    private static ServerPlayer player(GameTestHelper h, GameType mode, BlockPos pos) {
        var player = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "PortStorageDrop"));
        player.setGameMode(mode);
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 5.5);
        player.setItemInHand(InteractionHand.MAIN_HAND, ModItems.EMBER_METAL_PICKAXE.asStack());
        return player;
    }

    private static List<ItemEntity> drops(GameTestHelper h, BlockPos pos) {
        return h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(4));
    }

    private static int count(GameTestHelper h, BlockPos pos, Item item) {
        return drops(h, pos).stream().map(ItemEntity::getItem).filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    private static void crate(GameTestHelper h) {
        BlockPos pos = h.absolutePos(StorageUpgradeTests.CORE);
        for (GameType mode : new GameType[]{GameType.SURVIVAL, GameType.CREATIVE}) {
            h.getLevel().setBlockAndUpdate(pos, ModBlocks.CRATE.getDefaultState());
            var be = (StorageBlockEntity) h.getLevel().getBlockEntity(pos);
            be.setId(UUID.randomUUID());
            var storage = storage(be);
            insert(storage, Items.STONE, 130);
            insert(storage, Items.DIAMOND_SWORD, 2);
            unlock(storage);
            h.assertTrue(player(h, mode, pos).gameMode.destroyBlock(pos), "Crate break succeeds");
            h.assertTrue(count(h, pos, Items.STONE) == 130 && count(h, pos, Items.DIAMOND_SWORD) == 2,
                "Both modes spill crate contents exactly once");
            h.assertTrue(count(h, pos, Items.CRAFTING_TABLE) == 1 && count(h, pos, Items.STONECUTTER) == 1,
                "Crate returns crafting unlock materials");
            h.assertTrue(count(h, pos, ModBlocks.CRATE.asItem()) == (mode == GameType.SURVIVAL ? 1 : 0),
                "Creative crate does not drop a storage-reference item");
            h.assertTrue(Storages.get().get(be.getId()).isEmpty(), "Removed crate has no orphan storage");
            drops(h, pos).forEach(Entity::discard);
        }
        h.succeed();
    }

    private static void large(GameTestHelper h) {
        for (GameType mode : new GameType[]{GameType.SURVIVAL, GameType.CREATIVE}) {
            for (boolean child : new boolean[]{false, true}) {
                var be = StorageUpgradeTests.place(h, ModBlocks.LARGE_CRATE.get());
                var storage = storage(be);
                insert(storage, Items.DIAMOND, 1000);
                unlock(storage);
                BlockPos pos = be.getBlockPos();
                BlockPos target = child ? pos.above().east() : pos;
                h.assertTrue(player(h, mode, target).gameMode.destroyBlock(target), "Large crate break succeeds");
                h.assertTrue(count(h, pos, Items.DIAMOND) == 1000 && count(h, pos, Items.CRAFTING_TABLE) == 1
                    && count(h, pos, Items.STONECUTTER) == 1, "All part/mode paths spill exactly once");
                h.assertTrue(count(h, pos, ModBlocks.LARGE_CRATE.asItem()) == (mode == GameType.SURVIVAL ? 1 : 0),
                    "Only survival drops an empty large crate item");
                h.assertTrue(Storages.get().get(be.getId()).isEmpty(), "Large crate storage removed");
                assertRemoved(h, pos, ModBlocks.LARGE_CRATE.get());
                drops(h, pos).forEach(Entity::discard);
            }
        }
        h.succeed();
    }

    private static void moving(GameTestHelper h) {
        var block = ModBlocks.LARGE_CRATE.get();
        var be = StorageUpgradeTests.place(h, block);
        var storage = storage(be);
        insert(storage, Items.DIAMOND, 1000);
        for (var part : block.getParts()) {
            h.getLevel().setBlock(be.getBlockPos().offset(part.getOffset()), Blocks.AIR.defaultBlockState(), Block.UPDATE_MOVE_BY_PISTON);
        }
        h.assertTrue(drops(h, be.getBlockPos()).isEmpty() && Storages.get().get(be.getId()).orElseThrow() == storage,
            "Movement/upgrade flag preserves contents without drops");
        h.succeed();
    }

    private static void assertRemoved(GameTestHelper h, BlockPos pos, Block block) {
        for (BlockPos part : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 3, 1))) {
            h.assertTrue(!h.getLevel().getBlockState(part).is(block), "No orphan multipart blocks remain at " + part);
        }
    }

    private static void containers(GameTestHelper h, String content) {
        for (var block : new AbstractMultiPartBlock<?>[]{
            ModBlocks.SHULKER_CONTAINER.get(), ModBlocks.HYPERDIMENSION_STORAGE_STATION.get()}) {
            if (content.equals("upgraded") && block != ModBlocks.SHULKER_CONTAINER.get()) continue;
            for (GameType mode : new GameType[]{GameType.SURVIVAL, GameType.CREATIVE}) {
                for (boolean child : new boolean[]{false, true}) {
                    var be = StorageUpgradeTests.place(h, block);
                    var storage = storage(be);
                    final UUID id = be.getId();
                    if (content.equals("filled")) insert(storage, Items.DIAMOND, 1000);
                    if (content.equals("crafting")) unlock(storage);
                    if (content.equals("upgraded")) ((ShulkerContainerStorage) storage).getItems().addTypeLimit(n -> n * 2);
                    BlockPos pos = be.getBlockPos();
                    BlockPos target = child ? pos.above().east() : pos;
                    var player = player(h, mode, target);
                    h.assertTrue(player.gameMode.destroyBlock(target), "Container break succeeds");
                    boolean empty = content.equals("empty");
                    int expected = empty && mode == GameType.CREATIVE ? 0 : 1;
                    h.assertTrue(count(h, pos, block.asItem()) == expected, "Container count " + content + " " + mode + " child=" + child);
                    h.assertTrue(count(h, pos, Items.DIAMOND) == 0, "Advanced storage contents remain inside the dropped container");
                    h.assertTrue(Storages.get().get(id).isEmpty() == empty, "Only empty unupgraded storage is removed");
                    assertRemoved(h, pos, block);
                    if (expected == 1) {
                        var stack = drops(h, pos).stream().map(ItemEntity::getItem).filter(s -> s.is(block.asItem()))
                            .findFirst().orElseThrow();
                        if (empty) {
                            h.assertTrue(ItemStack.isSameItemSameComponents(stack, new ItemStack(block)),
                                "Empty drop has default components");
                        } else {
                            h.assertTrue(stack.get(ModComponents.STORAGE).id().orElseThrow().equals(id),
                                "Drop preserves original storage ID");
                            h.getLevel().setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
                            player.setItemInHand(InteractionHand.MAIN_HAND, stack.copy());
                            var context = new BlockPlaceContext(h.getLevel(), player, InteractionHand.MAIN_HAND, player.getMainHandItem(),
                                new BlockHitResult(Vec3.atCenterOf(pos.below()), Direction.UP, pos.below(), false));
                            h.assertTrue(((BlockItem) stack.getItem()).place(context).consumesAction(),
                                "Dropped container can be replaced");
                            var restored = (StorageBlockEntity) h.getLevel().getBlockEntity(pos);
                            h.assertTrue(id.equals(restored.getId()) && storage(restored) == storage,
                                "Replacement retains contents/upgrade/crafting");
                            for (BlockPos part : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 3, 1))) {
                                h.getLevel().setBlock(part, Blocks.AIR.defaultBlockState(), Block.UPDATE_NONE);
                            }
                        }
                    }
                    drops(h, pos).forEach(Entity::discard);
                }
            }
        }
        h.succeed();
    }
}
