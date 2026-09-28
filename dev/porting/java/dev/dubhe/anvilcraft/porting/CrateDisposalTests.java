package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.OverflowDisposalItemStacksResourceHandler;
import dev.dubhe.anvilcraft.block.container.storage.CrateBlock;
import dev.dubhe.anvilcraft.block.entity.storage.CrateBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.Eternal;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import dev.dubhe.anvilcraft.saved.setting.PlayerSettings;
import dev.dubhe.anvilcraft.saved.storage.CrateStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class CrateDisposalTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_crate_disposal_handler", CrateDisposalTests::handler,
        "port_crate_disposal_transactions", CrateDisposalTests::transactions,
        "port_crate_disposal_neighbors", CrateDisposalTests::neighbors,
        "port_crate_disposal_automation", CrateDisposalTests::automation,
        "port_crate_disposal_rpc", CrateDisposalTests::rpc,
        "port_crate_disposal_reload", CrateDisposalTests::reload
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, r -> TESTS.forEach((name, test) -> r.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_crate_disposal"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 200, 0, true))));
    }

    private static int insert(ResourceHandler<ItemResource> items, ItemResource item, int amount, boolean commit) {
        try (Transaction tx = Transaction.openRoot()) {
            int accepted = items.insert(item, amount, tx);
            if (commit) tx.commit();
            return accepted;
        }
    }

    private static int count(ResourceHandler<ItemResource> items) {
        int count = 0;
        for (int i = 0; i < items.size(); i++) count += items.getAmountAsInt(i);
        return count;
    }

    private static OverflowDisposalItemStacksResourceHandler newHandler() {
        var items = new OverflowDisposalItemStacksResourceHandler(2048);
        items.setDispose(true);
        return items;
    }

    private static void handler(GameTestHelper h) {
        var items = newHandler();
        h.assertTrue(insert(items, ItemResource.of(Items.STONE), 2000, true) == 2000, "Store before capacity");
        try (Transaction tx = Transaction.openRoot()) {
            h.assertTrue(items.insert(0, ItemResource.of(Items.DIRT), 100, tx) == 100, "Occupied slot redirects to sparse insertion");
            tx.commit();
        }
        h.assertTrue(count(items) == 2048 && items.getAmountAsInt(1) == 48, "Store 48 new-type items before disposing overflow");
        h.assertTrue(insert(items, ItemResource.of(Items.DIRT), 10, true) == 10 && count(items) == 2048, "Full crate consumes overflow");
        var eternal = new ItemStack(Items.DIAMOND);
        eternal.set(ModComponents.ETERNAL, Eternal.DEFAULT);
        h.assertTrue(insert(items, ItemResource.of(eternal), 64, true) == 0, "Eternal input is never destroyed");
        items.setDispose(false);
        h.assertTrue(insert(items, ItemResource.of(Items.DIRT), 64, true) == 0, "Disabled mode rejects overflow");
        var weighted = newHandler();
        h.assertTrue(insert(weighted, ItemResource.of(Items.DIAMOND_SWORD), 40, true) == 40
            && count(weighted) == 32, "Weighted capacity stores 32 swords and disposes eight");
        h.succeed();
    }

    private static void transactions(GameTestHelper h) {
        var items = newHandler();
        insert(items, ItemResource.of(Items.STONE), 2000, true);
        var source = new ItemStacksResourceHandler(1);
        source.set(0, ItemResource.of(Items.DIRT), 64);
        try (Transaction root = Transaction.openRoot()) {
            try (Transaction nested = Transaction.open(root)) {
                h.assertTrue(items.insert(ItemResource.of(Items.DIRT), 64, nested) == 64, "Simulation accepts overflow");
                source.extract(ItemResource.of(Items.DIRT), 64, nested);
                nested.commit();
            }
        }
        h.assertTrue(count(items) == 2000 && count(source) == 64, "Root rollback restores both endpoints after nested commit");
        ItemHandlerUtil.exportToTarget(source, 64, (resource, amount) -> true, items);
        h.assertTrue(count(source) == 0 && count(items) == 2048, "Committed transfer stores what fits and consumes the rest");
        h.succeed();
    }

    private static CrateBlockEntity crate(GameTestHelper h, BlockPos pos) {
        h.getLevel().setBlock(pos, ModBlocks.CRATE.getDefaultState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        var be = (CrateBlockEntity) h.getLevel().getBlockEntity(pos);
        be.setId(UUID.randomUUID());
        Storages.get().getOrCreate(be.getId(), CrateStorage.class);
        return be;
    }

    private static void neighbors(GameTestHelper h) {
        BlockPos pos = h.absolutePos(new BlockPos(4, 3, 4));
        var be = crate(h, pos);
        var id = be.getId();
        var items = Storages.get().get(id, CrateStorage.class).orElseThrow().getItems();
        insert(items, ItemResource.of(Items.STONE), 17, true);
        for (Direction direction : Direction.values()) {
            h.getLevel().setBlockAndUpdate(pos.relative(direction), ModBlocks.VOID_MATTER_BLOCK.getDefaultState());
            h.assertTrue(be.getBlockState().getValue(CrateBlock.DISPOSE) && items.isDispose(), "Face adjacency enables " + direction);
            h.getLevel().setBlockAndUpdate(pos.relative(direction), Blocks.AIR.defaultBlockState());
            h.assertTrue(!be.getBlockState().getValue(CrateBlock.DISPOSE) && !items.isDispose(), "Removal disables " + direction);
        }
        h.getLevel().setBlockAndUpdate(pos.east().south(), ModBlocks.VOID_MATTER_BLOCK.getDefaultState());
        h.getLevel().setBlockAndUpdate(pos.west(), ModBlocks.EXCITED_STATE_VOID_MATTER_BLOCK.getDefaultState());
        be.refreshDispose();
        h.assertTrue(!items.isDispose(), "Diagonal and excited-state matter do not trigger disposal");
        h.assertTrue(h.getLevel().getBlockEntity(pos) == be && be.getId().equals(id) && count(items) == 17,
            "Mode toggles preserve the block entity, ID and inventory");
        h.succeed();
    }

    private static void automation(GameTestHelper h) {
        BlockPos pos = h.absolutePos(new BlockPos(4, 3, 4));
        var be = crate(h, pos);
        h.getLevel().setBlock(pos.north(), ModBlocks.VOID_MATTER_BLOCK.getDefaultState(), Block.UPDATE_CLIENTS);
        Storages.get().remove(be.getId());
        var handler = h.getLevel().getCapability(Capabilities.Item.BLOCK, pos, Direction.UP);
        h.assertTrue(handler != null && insert(handler, ItemResource.of(Items.STONE), 3000, true) == 3000
            && count(handler) == 2048, "First capability access creates storage and synchronizes current surroundings");
        h.getLevel().setBlockAndUpdate(pos.north(), Blocks.AIR.defaultBlockState());
        h.assertTrue(insert(handler, ItemResource.of(Items.DIRT), 64, true) == 0, "Cached capability stops disposal after removal");
        h.succeed();
    }

    private static void rpc(GameTestHelper h) {
        try (var fixture = new StorageFluidRpcTests.Fixture(h)) {
            var be = crate(h, fixture.core());
            h.getLevel().setBlock(fixture.core().north(), ModBlocks.VOID_MATTER_BLOCK.getDefaultState(), Block.UPDATE_CLIENTS);
            Storages.get().remove(be.getId());
            fixture.player().getInventory().setItem(9, new ItemStack(Items.STONE, 64));
            fixture.authorize();
            StorageServerStub.deposit(fixture.playerId(), fixture.core().asLong(), true, false);
            var items = Storages.get().get(be.getId(), CrateStorage.class).orElseThrow().getItems();
            h.assertTrue(items.isDispose() && count(items) == 64, "RPC creates then synchronizes disposal handler");
            insert(items, ItemResource.of(Items.STONE), 1984, true);
            fixture.player().getInventory().setItem(9, new ItemStack(Items.STONE, 64));
            fixture.authorize();
            StorageServerStub.deposit(fixture.playerId(), fixture.core().asLong(), true, false);
            h.assertTrue(fixture.player().getInventory().getItem(9).isEmpty() && count(items) == 2048, "RPC consumes overflow");
            var nearby = crate(h, fixture.core().east());
            final var secondary = Storages.get().get(nearby.getId(), CrateStorage.class).orElseThrow().getItems();
            h.getLevel().setBlock(nearby.getBlockPos().east(), ModBlocks.VOID_MATTER_BLOCK.getDefaultState(), Block.UPDATE_CLIENTS);
            PlayerSettings.getSetting(fixture.player().registryAccess(), fixture.playerId()).storage().setSearchContent("stone");
            fixture.authorize();
            StorageServerStub.load(fixture.playerId(), fixture.core().asLong());
            h.assertTrue(secondary.isDispose(), "Search-mode neighboring crates refresh their own disposal state");
        }
        h.succeed();
    }

    private static void reload(GameTestHelper h) {
        BlockPos pos = h.absolutePos(new BlockPos(4, 3, 4));
        var be = crate(h, pos);
        var storage = Storages.get().get(be.getId(), CrateStorage.class).orElseThrow();
        insert(storage.getItems(), ItemResource.of(Items.DIAMOND), 17, true);
        storage.getItems().setDispose(true);
        var encoded = CrateStorage.CODEC.codec().encodeStart(NbtOps.INSTANCE, storage).getOrThrow();
        var decoded = CrateStorage.CODEC.codec().parse(NbtOps.INSTANCE, encoded).getOrThrow();
        h.assertTrue(!decoded.getItems().isDispose() && count(decoded.getItems()) == 17, "Disposal mode is transient across serialization");
        Storages.get().put(decoded);
        h.getLevel().setBlock(pos.north(), ModBlocks.VOID_MATTER_BLOCK.getDefaultState(), Block.UPDATE_CLIENTS);
        be.onLoad();
        h.assertTrue(decoded.getItems().isDispose(), "Chunk load recomputes mode from surroundings");
        h.succeed();
    }
}
