package dev.dubhe.anvilcraft.porting;

import com.mojang.authlib.GameProfile;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil;
import dev.dubhe.anvilcraft.block.entity.HyperdimensionUploaderBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.TerminalBinding;
import dev.dubhe.anvilcraft.saved.storage.HyperdimensionStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class HyperdimensionUploaderTests {
    private static final BlockPos POS = new BlockPos(3, 3, 3);
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_uploader_binding", HyperdimensionUploaderTests::binding,
        "port_uploader_transfer", HyperdimensionUploaderTests::transfer,
        "port_uploader_persistence", HyperdimensionUploaderTests::persistence,
        "port_uploader_drop", HyperdimensionUploaderTests::drop,
        "port_uploader_hopper", HyperdimensionUploaderTests::hopper,
        "port_uploader_extract", HyperdimensionUploaderTests::extract,
        "port_uploader_chute", HyperdimensionUploaderTests::chute,
        "port_uploader_weighted", HyperdimensionUploaderTests::weighted,
        "port_uploader_scan_type", HyperdimensionUploaderTests::scanType
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_uploader"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 200, 0, true))));
    }

    private static HyperdimensionUploaderBlockEntity place(GameTestHelper helper) {
        helper.setBlock(POS, Blocks.AIR);
        helper.setBlock(POS, ModBlocks.HYPERDIMENSION_UPLOADER.get());
        return helper.getBlockEntity(POS, HyperdimensionUploaderBlockEntity.class);
    }

    private static void binding(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "uploader-bind"));
        helper.setBlock(POS, ModBlocks.SINGULARITY_CRYSTAL.get());
        var pos = helper.absolutePos(POS);
        var hit = new BlockHitResult(pos.getCenter(), Direction.UP, pos, false);
        var terminal = ModItems.HYPERDIMENSION_TERMINAL.asStack();
        var state = helper.getBlockState(POS);
        state.useItemOn(terminal, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(helper.getBlockState(POS).is(ModBlocks.SINGULARITY_CRYSTAL), "Unbound terminal must not convert crystal");
        UUID first = UUID.randomUUID();
        terminal.set(ModComponents.TERMINAL_BINDING, new TerminalBinding(Optional.of(first)));
        state.useItemOn(terminal, helper.getLevel(), player, InteractionHand.OFF_HAND, hit);
        helper.assertTrue(helper.getBlockState(POS).is(ModBlocks.SINGULARITY_CRYSTAL), "Conversion uses the main hand only");
        state.useItemOn(terminal, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        var entity = helper.getBlockEntity(POS, HyperdimensionUploaderBlockEntity.class);
        helper.assertTrue(first.equals(entity.getStorageId()) && terminal.getCount() == 1, "Conversion binds without consuming terminal");
        UUID second = UUID.randomUUID();
        terminal.set(ModComponents.TERMINAL_BINDING, new TerminalBinding(Optional.of(second)));
        helper.getBlockState(POS).useItemOn(terminal, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(second.equals(entity.getStorageId()), "Bound terminal rebinds an existing uploader");
        terminal.remove(ModComponents.TERMINAL_BINDING);
        helper.getBlockState(POS).useItemOn(terminal, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(second.equals(entity.getStorageId()), "Unbound terminal cannot erase a binding");
        entity.setStorageId(null);
        helper.succeed();
    }

    private static void transfer(GameTestHelper helper) {
        var entity = place(helper);
        var buffer = entity.getBuffer();
        helper.assertTrue(buffer.size() == 16, "Sixteen buffer slots");
        try (var transaction = Transaction.openRoot()) {
            helper.assertTrue(buffer.insert(ItemResource.of(Items.DIAMOND), 2000, transaction) == 1024, "Sixteen ordinary stacks capacity");
        }
        helper.assertTrue(entity.isBufferEmpty(), "Aborted insertion leaves buffer empty");
        buffer.set(0, ItemResource.of(Items.DIAMOND), 64);
        buffer.set(1, ItemResource.of(Items.DIAMOND), 64);
        entity.tickServer();
        helper.assertTrue(buffer.getAmountAsInt(0) == 64, "Unbound uploader retains contents");
        UUID id = UUID.randomUUID();
        entity.setStorageId(id);
        final int chunks = helper.getLevel().getChunkSource().getLoadedChunksCount();
        entity.tickServer();
        var storage = Storages.get().get(id, HyperdimensionStorage.class).orElseThrow();
        helper.assertTrue(storage.getItems().getAmountAsLong(0) == 64 && buffer.getAmountAsInt(1) == 64, "Default scan transfers 64 items");
        for (int tick = 0; tick < 5; tick++) entity.tickServer();
        helper.assertTrue(buffer.getAmountAsInt(1) == 64, "Source countdown waits five intervening ticks");
        entity.tickServer();
        helper.assertTrue(entity.isBufferEmpty() && storage.getItems().getAmountAsLong(0) == 128, "Next scan consumes remaining stack");
        helper.assertTrue(helper.getLevel().getChunkSource().getLoadedChunksCount() == chunks, "Global target access loads no chunks");
        entity.setStorageId(null);
        Storages.get().remove(id);
        helper.succeed();
    }

    private static void weighted(GameTestHelper helper) {
        for (var item : java.util.List.of(Items.DIAMOND_SWORD, Items.ENDER_PEARL)) {
            var entity = place(helper);
            UUID id = UUID.randomUUID();
            entity.setStorageId(id);
            var resource = ItemResource.of(item);
            int maximum = resource.getMaxStackSize();
            entity.getBuffer().set(0, resource, maximum);
            entity.getBuffer().set(1, resource, maximum);
            entity.tickServer();
            var target = Storages.get().get(id, HyperdimensionStorage.class).orElseThrow();
            helper.assertTrue(target.getItems().getAmountAsLong(0) == maximum
                && entity.getBuffer().getAmountAsInt(1) == maximum, "A 64-unit scan moves one stack, including swords and pearls");
            entity.setStorageId(null);
            Storages.get().remove(id);
        }
        helper.succeed();
    }

    private static void scanType(GameTestHelper helper) {
        var source = new ItemStacksResourceHandler(3);
        source.set(0, ItemResource.of(Items.DIAMOND), 4);
        source.set(1, ItemResource.of(Items.GOLD_INGOT), 10);
        source.set(2, ItemResource.of(Items.DIAMOND), 20);
        var target = new ItemStacksResourceHandler(2);
        ItemHandlerUtil.exportToTarget(source, 64, (resource, amount) -> true, target);
        helper.assertTrue(source.getAmountAsInt(1) == 10 && target.getAmountAsInt(0) == 24,
            "One scan locks to the first accepted type across buffer slots");
        source.set(0, ItemResource.of(Items.DIAMOND), 4);
        var filtered = new ItemStacksResourceHandler(2) {
            @Override
            public boolean isValid(int index, ItemResource resource) {
                return resource.is(Items.GOLD_INGOT);
            }
        };
        ItemHandlerUtil.exportToTarget(source, 64, (resource, amount) -> true, filtered);
        helper.assertTrue(source.getAmountAsInt(0) == 4 && source.getAmountAsInt(1) == 0 && filtered.getAmountAsInt(0) == 10,
            "A rejected first type does not block another accepted type");
        var protectedSource = new ItemStacksResourceHandler(2) {
            @Override
            public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
                return index == 0 ? 0 : super.extract(index, resource, amount, transaction);
            }
        };
        protectedSource.set(0, ItemResource.of(Items.DIAMOND), 4);
        protectedSource.set(1, ItemResource.of(Items.GOLD_INGOT), 10);
        var rollbackTarget = new ItemStacksResourceHandler(2);
        ItemHandlerUtil.exportToTarget(protectedSource, 64, (resource, amount) -> true, rollbackTarget);
        helper.assertTrue(protectedSource.getAmountAsInt(0) == 4 && rollbackTarget.getResource(0).is(Items.GOLD_INGOT)
            && rollbackTarget.getAmountAsInt(0) == 10, "Rejected extraction rolls back target insertion and permits another type");
        var limitedSource = new ItemStacksResourceHandler(1) {
            @Override
            public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
                return super.extract(index, resource, Math.min(3, amount), transaction);
            }
        };
        limitedSource.set(0, ItemResource.of(Items.EMERALD), 10);
        var limitedTarget = new ItemStacksResourceHandler(1);
        ItemHandlerUtil.exportToTarget(limitedSource, 64, (resource, amount) -> amount == 3, limitedTarget);
        helper.assertTrue(limitedSource.getAmountAsInt(0) == 7 && limitedTarget.getAmountAsInt(0) == 3,
            "Source extraction limits and predicate stack sizes survive transactional simulation");
        helper.succeed();
    }

    private static void persistence(GameTestHelper helper) {
        final var entity = place(helper);
        UUID id = UUID.randomUUID();
        var registry = helper.getLevel().registryAccess();
        var legacy = new CompoundTag();
        legacy.put("storage_id", UUIDUtil.CODEC.encodeStart(NbtOps.INSTANCE, id).getOrThrow());
        var item = (CompoundTag) ItemStack.CODEC.encodeStart(registry.createSerializationContext(NbtOps.INSTANCE),
            new ItemStack(Items.EMERALD, 13)).getOrThrow();
        item.putInt("Slot", 7);
        var list = new ListTag();
        list.add(item);
        var inventory = new CompoundTag();
        inventory.put("Items", list);
        inventory.putInt("Size", 16);
        legacy.put("buffer", inventory);
        entity.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, registry, legacy));
        helper.assertTrue(id.equals(entity.getStorageId()) && entity.getBuffer().getAmountAsInt(7) == 13,
            "Legacy UUID and buffer survive load");
        var saved = entity.getUpdateTag(registry);
        entity.setStorageId(null);
        entity.getBuffer().set(7, ItemResource.EMPTY, 0);
        entity.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, registry, saved));
        helper.assertTrue(id.equals(entity.getStorageId()) && entity.getBuffer().getAmountAsInt(7) == 13, "Native update data roundtrip");
        entity.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, registry, new CompoundTag()));
        helper.assertTrue(entity.getStorageId() == null && entity.isBufferEmpty(), "Empty update clears binding and buffer");
        helper.succeed();
    }

    private static void drop(GameTestHelper helper) {
        var entity = place(helper);
        UUID id = UUID.randomUUID();
        entity.setStorageId(id);
        entity.getBuffer().set(0, ItemResource.of(Items.DIAMOND), 37);
        var level = helper.getLevel();
        var pos = helper.absolutePos(POS);
        var params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, pos.getCenter())
            .withParameter(LootContextParams.TOOL, ItemStack.EMPTY).withOptionalParameter(LootContextParams.BLOCK_ENTITY, entity);
        var drops = helper.getBlockState(POS).getDrops(params);
        helper.assertTrue(drops.size() == 1 && drops.getFirst().has(DataComponents.BLOCK_ENTITY_DATA), "Survival loot preserves data");
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "uploader-drop"));
        player.setGameMode(GameType.CREATIVE);
        helper.getBlockState(POS).getBlock().playerWillDestroy(level, pos, helper.getBlockState(POS), player);
        var items = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1));
        helper.assertTrue(items.size() == 1 && items.getFirst().getItem().has(DataComponents.BLOCK_ENTITY_DATA),
            "Creative breaking of a nonempty buffer preserves contents");
        helper.setBlock(POS, Blocks.AIR);
        helper.setBlock(POS.below(), Blocks.STONE);
        var stack = drops.getFirst();
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var result = stack.useOn(new UseOnContext(level, player, InteractionHand.MAIN_HAND, stack,
            new BlockHitResult(Vec3.atCenterOf(pos.below()).add(0, 0.5, 0), Direction.UP, pos.below(), false)));
        helper.assertTrue(result.consumesAction(), "Dropped uploader places normally");
        var placed = helper.getBlockEntity(POS, HyperdimensionUploaderBlockEntity.class);
        helper.assertTrue(id.equals(placed.getStorageId()) && placed.getBuffer().getAmountAsInt(0) == 37,
            "Placed item restores binding and buffer");
        placed.setStorageId(null);
        helper.succeed();
    }

    private static void hopper(GameTestHelper helper) {
        var entity = place(helper);
        for (Direction side : Direction.values()) {
            helper.assertTrue(helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(POS), side) == entity.getBuffer(),
                "All sides expose the same buffer");
        }
        helper.setBlock(POS.above(), Blocks.HOPPER.defaultBlockState().setValue(BlockStateProperties.FACING_HOPPER, Direction.DOWN));
        var hopper = helper.getBlockEntity(POS.above(), HopperBlockEntity.class);
        hopper.setItem(0, new ItemStack(Items.GOLD_INGOT, 5));
        helper.succeedWhen(() -> helper.assertTrue(entity.getBuffer().getResource(0).is(Items.GOLD_INGOT)
            && entity.getBuffer().getAmountAsInt(0) > 0, "Real hopper inserts into the capability buffer"));
    }

    private static void extract(GameTestHelper helper) {
        var entity = place(helper);
        entity.getBuffer().set(0, ItemResource.of(Items.DIAMOND), 5);
        helper.setBlock(POS.below(), Blocks.HOPPER.defaultBlockState().setValue(BlockStateProperties.FACING_HOPPER, Direction.EAST));
        var hopper = helper.getBlockEntity(POS.below(), HopperBlockEntity.class);
        helper.succeedWhen(() -> helper.assertTrue(hopper.getItem(0).is(Items.DIAMOND)
            && entity.getBuffer().getAmountAsInt(0) < 5, "Real hopper extracts buffered items"));
    }

    private static void chute(GameTestHelper helper) {
        var entity = place(helper);
        helper.setBlock(POS.west(), ModBlocks.SIMPLE_CHUTE.getDefaultState()
            .setValue(BlockStateProperties.FACING_HOPPER, Direction.EAST));
        var input = helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(POS.west()), Direction.UP);
        try (var transaction = Transaction.openRoot()) {
            helper.assertTrue(input.insert(ItemResource.of(Items.EMERALD), 12, transaction) == 12, "Stock real chute");
            transaction.commit();
        }
        helper.succeedWhen(() -> helper.assertTrue(entity.getBuffer().getResource(0).is(Items.EMERALD)
            && entity.getBuffer().getAmountAsInt(0) == 12, "Real chute exports into uploader"));
    }
}
