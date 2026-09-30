package dev.dubhe.anvilcraft.porting;

import com.mojang.authlib.GameProfile;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.container.storage.LargeCrateBlock;
import dev.dubhe.anvilcraft.block.entity.storage.CrateBlockEntity;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.StorageRef;
import dev.dubhe.anvilcraft.saved.storage.BaseStorage;
import dev.dubhe.anvilcraft.saved.storage.CraftingStorage;
import dev.dubhe.anvilcraft.saved.storage.CrateStorage;
import dev.dubhe.anvilcraft.saved.storage.LargeCrateStorage;
import dev.dubhe.anvilcraft.saved.storage.StorageType;
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
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class CrateMergeTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_crate_merge_targets", CrateMergeTests::targets,
        "port_crate_merge_aliases", CrateMergeTests::aliases,
        "port_crate_merge_overflow", CrateMergeTests::overflow,
        "port_crate_merge_recipe_bases", CrateMergeTests::recipeBases,
        "port_crate_merge_ambiguous", CrateMergeTests::ambiguous
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, r -> TESTS.forEach((name, test) -> r.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_crate_merge"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 300, 0, true))));
    }

    private static ServerPlayer player(GameTestHelper h) {
        var player = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "PortCrateMerge"));
        player.setGameMode(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        return player;
    }

    private static List<UUID> place(GameTestHelper h, BlockPos origin) {
        List<UUID> ids = new ArrayList<>();
        for (var part : Cube3x3PartHalf.values()) {
            BlockPos pos = origin.offset(part.getOffset());
            h.getLevel().setBlock(pos, ModBlocks.CRATE.getDefaultState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            var be = (CrateBlockEntity) h.getLevel().getBlockEntity(pos);
            be.setId(UUID.randomUUID());
            ids.add(be.getId());
            Storages.get().getOrCreate(be.getId(), CrateStorage.class);
        }
        return ids;
    }

    private static InteractionResult merge(GameTestHelper h, ServerPlayer player, BlockPos clicked, Direction face, ItemStack held) {
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        return h.getLevel().getBlockState(clicked).useItemOn(held, h.getLevel(), player, InteractionHand.MAIN_HAND,
            new BlockHitResult(Vec3.atCenterOf(clicked), face, clicked, false));
    }

    private static LargeCrateStorage result(GameTestHelper h, BlockPos origin) {
        for (var part : Cube3x3PartHalf.values()) {
            var state = h.getLevel().getBlockState(origin.offset(part.getOffset()));
            h.assertTrue(state.is(ModBlocks.LARGE_CRATE) && state.getValue(LargeCrateBlock.HALF) == part,
                "Complete correctly oriented large crate at " + part);
        }
        var be = (StorageBlockEntity) h.getLevel().getBlockEntity(origin);
        return Storages.get().get(be.getId(), LargeCrateStorage.class).orElseThrow();
    }

    private static void insert(BaseStorage<?> storage, Item item, int amount) {
        try (Transaction tx = Transaction.openRoot()) {
            if (storage.getItems().insert(ItemResource.of(item), amount, tx) != amount) throw new IllegalStateException("Fixture capacity");
            tx.commit();
        }
    }

    private static int count(BaseStorage<?> storage, Item item) {
        int count = 0;
        for (int slot = 0; slot < storage.getItems().size(); slot++) {
            if (storage.getItems().getResource(slot).is(item)) count += storage.getItems().getAmountAsInt(slot);
        }
        return count;
    }

    private static void clear(GameTestHelper h, BlockPos origin, LargeCrateStorage storage) {
        Storages.get().remove(storage.getId());
        for (var part : Cube3x3PartHalf.values()) {
            h.getLevel().setBlock(origin.offset(part.getOffset()), Blocks.AIR.defaultBlockState(), Block.UPDATE_NONE);
        }
        h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(origin).inflate(4)).forEach(Entity::discard);
    }

    private static void targets(GameTestHelper h) {
        final BlockPos origin = h.absolutePos(new BlockPos(6, 3, 6));
        var player = player(h);
        for (var clicked : Cube3x3PartHalf.values()) {
            for (Direction face : Direction.values()) {
                var ids = place(h, origin);
                insert(Storages.get().get(ids.getFirst()).orElseThrow(), Items.DIAMOND, 17);
                var held = ModBlocks.LARGE_CRATE.asStack(2);
                h.assertTrue(merge(h, player, origin.offset(clicked.getOffset()), face, held).consumesAction(),
                    "Any clicked part and face merges: " + clicked + "/" + face);
                var storage = result(h, origin);
                h.assertTrue(count(storage, ModBlocks.CRATE.asItem()) == 27 && count(storage, Items.DIAMOND) == 17,
                    "Original crates and contents retained");
                h.assertTrue(held.getCount() == 1 && ids.stream().allMatch(id -> Storages.get().get(id).isEmpty()),
                    "Consume one held large crate and remove old storage records");
                h.assertTrue(h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(origin).inflate(4)).isEmpty(),
                    "Structure replacement never spills contents or drops loose crates");
                clear(h, origin, storage);
            }
        }
        h.succeed();
    }

    private static void aliases(GameTestHelper h) {
        BlockPos origin = h.absolutePos(new BlockPos(6, 3, 6));
        var ids = place(h, origin);
        var source = Storages.get().get(ids.getFirst()).orElseThrow();
        insert(source, Items.DIAMOND, 29);
        source.setCraftingUnlocked(true);
        for (var part : Cube3x3PartHalf.values()) {
            var be = (StorageBlockEntity) h.getLevel().getBlockEntity(origin.offset(part.getOffset()));
            if (!be.getId().equals(source.getId())) Storages.get().remove(be.getId());
            be.clearId();
            be.setId(source.getId());
        }
        var target = new LargeCrateStorage(UUID.randomUUID());
        insert(target, Items.EMERALD, 13);
        target.setCrafting(CraftingStorage.EMPTY.withCraftingSlot(0, new ItemStack(Items.GOLD_INGOT, 3)));
        Storages.get().put(target);
        var held = ModBlocks.LARGE_CRATE.asStack();
        held.set(ModComponents.STORAGE, new StorageRef(StorageType.LARGE_CRATE, target.getId()));
        var player = player(h);
        player.setGameMode(GameType.CREATIVE);
        h.assertTrue(merge(h, player, origin.above(2).west().north(), Direction.DOWN, held).consumesAction(),
            "Creative bound-target merge");
        var merged = result(h, origin);
        h.assertTrue(merged == target && count(merged, Items.DIAMOND) == 29 && count(merged, Items.EMERALD) == 13,
            "Shared source IDs transfer once into the existing target");
        h.assertTrue(count(merged, ModBlocks.CRATE.asItem()) == 27 && held.getCount() == 1 && merged.isCraftingUnlocked(),
            "Creative retains held item while recovering physical crates and crafting unlock");
        h.assertTrue(merged.getCrafting().craftingInput().getFirst().getCount() == 3, "Target crafting grid remains unchanged");
        h.succeed();
    }

    private static void overflow(GameTestHelper h) {
        BlockPos origin = h.absolutePos(new BlockPos(6, 3, 6));
        var ids = place(h, origin);
        var source = Storages.get().get(ids.getFirst()).orElseThrow();
        insert(source, Items.DIAMOND, 100);
        source.setCraftingUnlocked(true);
        var target = new LargeCrateStorage(UUID.randomUUID());
        insert(target, Items.STONE, 65410);
        Storages.get().put(target);
        var held = ModBlocks.LARGE_CRATE.asStack();
        held.set(ModComponents.STORAGE, new StorageRef(StorageType.LARGE_CRATE, target.getId()));
        var player = player(h);
        h.assertTrue(merge(h, player, origin, Direction.UP, held).consumesAction(), "Merge full target without losing overflow");
        long droppedCrates = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(origin).inflate(4)).stream()
            .map(ItemEntity::getItem).filter(stack -> stack.is(ModBlocks.CRATE.asItem())).mapToLong(ItemStack::getCount).sum();
        h.assertTrue(count(target, Items.STONE) == 65410 && count(target, Items.DIAMOND) == 100
            && count(target, ModBlocks.CRATE.asItem()) + droppedCrates == 27 && droppedCrates > 0 && target.isCraftingUnlocked(),
            "Keep all existing items, transferred items, physical crates and the retained recipe bases");
        h.assertTrue(held.isEmpty() && ids.stream().allMatch(id -> Storages.get().get(id).isEmpty()), "Consume and unlink once");
        clear(h, origin, target);
        place(h, origin);
        held = ModBlocks.LARGE_CRATE.asStack();
        BlockPos missing = origin.above(2).east().south();
        h.getLevel().setBlockAndUpdate(missing, Blocks.AIR.defaultBlockState());
        h.assertTrue(merge(h, player, origin, Direction.SOUTH, held) == InteractionResult.FAIL, "Incomplete cube cannot merge");
        h.succeed();
    }

    private static void recipeBases(GameTestHelper h) {
        BlockPos origin = h.absolutePos(new BlockPos(6, 3, 6));
        var ids = place(h, origin);
        for (UUID id : ids) {
            var basis = new ItemStack(Items.DIAMOND);
            basis.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                net.minecraft.network.chat.Component.literal(id.toString()));
            Storages.get().get(id).orElseThrow().setRecipeBases(List.of(basis));
        }
        var target = new LargeCrateStorage(UUID.randomUUID());
        target.setRecipeBases(List.of(new ItemStack(Items.EMERALD)));
        Storages.get().put(target);
        var held = ModBlocks.LARGE_CRATE.asStack();
        held.set(ModComponents.STORAGE, new StorageRef(StorageType.LARGE_CRATE, target.getId()));
        h.assertTrue(merge(h, player(h), origin, Direction.UP, held).consumesAction(), "Merge 27 independently unlocked crates");
        var merged = result(h, origin);
        h.assertTrue(merged.getRecipeBases().size() == 1 && merged.getRecipeBases().getFirst().is(Items.EMERALD)
            && count(merged, Items.DIAMOND) == 27, "Retain target bases and refund every source basis");
        for (int slot = 0; slot < merged.getItems().size(); slot++) {
            var resource = merged.getItems().getResource(slot);
            if (!resource.is(Items.DIAMOND)) continue;
            h.assertTrue(ids.stream().anyMatch(id -> resource.toStack().getHoverName().getString().equals(id.toString())),
                "Refund original basis components");
        }
        var be = (StorageBlockEntity) h.getLevel().getBlockEntity(origin);
        be.dropContents(h.getLevel(), origin);
        be.dropContents(h.getLevel(), origin);
        var drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(origin).inflate(4));
        long diamonds = drops.stream().map(ItemEntity::getItem).filter(stack -> stack.is(Items.DIAMOND))
            .mapToLong(ItemStack::getCount).sum();
        long emeralds = drops.stream().map(ItemEntity::getItem).filter(stack -> stack.is(Items.EMERALD))
            .mapToLong(ItemStack::getCount).sum();
        h.assertTrue(diamonds == 27 && emeralds == 1, "Destroying the result cannot refund any basis twice");
        h.succeed();
    }

    private static void ambiguous(GameTestHelper h) {
        BlockPos center = h.absolutePos(new BlockPos(6, 5, 6));
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-2, -2, -2), center.offset(1, 1, 1))) {
            h.getLevel().setBlock(pos, ModBlocks.CRATE.getDefaultState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        var held = ModBlocks.LARGE_CRATE.asStack();
        h.assertTrue(merge(h, player(h), center, Direction.EAST, held).consumesAction(), "Overlapping possible regions can merge");
        var storage = result(h, center.offset(-1, -2, -1));
        h.assertTrue(count(storage, ModBlocks.CRATE.asItem()) == 27, "Source scan order picks the first complete origin");
        h.assertTrue(h.getLevel().getBlockState(center.offset(1, 1, 1)).is(ModBlocks.CRATE), "Crates outside selected region remain");
        h.succeed();
    }
}
