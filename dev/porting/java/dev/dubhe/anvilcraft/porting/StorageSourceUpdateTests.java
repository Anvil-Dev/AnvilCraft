package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import dev.dubhe.anvilcraft.saved.storage.CraftingStorage;
import dev.dubhe.anvilcraft.saved.storage.HyperdimensionStorage;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class StorageSourceUpdateTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_storage_craft_awards", StorageSourceUpdateTests::awards,
        "port_storage_merged_extraction", StorageSourceUpdateTests::merged,
        "port_storage_locked_transfer", StorageSourceUpdateTests::locked
    );
    private static final Map<UUID, List<ItemStack>> EVENTS = new HashMap<>();

    @SubscribeEvent
    public static void crafted(PlayerEvent.ItemCraftedEvent event) {
        var events = EVENTS.get(event.getEntity().getUUID());
        if (events != null) events.add(event.getInventory().getItem(8).copy());
    }

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_storage_source_update"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static void awards(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            var player = fixture.player();
            var storage = StorageCraftingExecutionTests.storage(helper, fixture);
            var events = new ArrayList<ItemStack>();
            EVENTS.put(player.getUUID(), events);
            try {
                var recipe = ResourceKey.create(Registries.RECIPE, Identifier.withDefaultNamespace("oak_planks"));
                for (int mode = 0; mode < 4; mode++) {
                    storage.setCrafting(CraftingStorage.EMPTY.withCraftingSlot(8, new ItemStack(Items.OAK_LOG)));
                    player.containerMenu.setCarried(ItemStack.EMPTY);
                    fixture.authorize();
                    boolean changed = switch (mode) {
                        case 0, 1 -> StorageServerStub.craftingTakeResult(
                            fixture.playerId(), fixture.core().asLong(), false, mode == 1).changed();
                        case 2 -> StorageServerStub.craftingTakeAll(fixture.playerId(), fixture.core().asLong(), false, 1).changed();
                        default -> StorageServerStub.craftingThrowResult(
                            fixture.playerId(), fixture.core().asLong(), false, false).changed();
                    };
                    helper.assertTrue(changed && events.size() == mode + 1 && events.getLast().is(Items.OAK_LOG),
                        "Crafting event must fire once with the original grid");
                    helper.assertTrue(storage.getCrafting().craftingInput().get(8).isEmpty(), "Craft output did not consume input");
                }
                helper.assertTrue(player.getRecipeBook().contains(recipe),
                    "Craft recipe unlock missing");
                storage.setCrafting(CraftingStorage.EMPTY.withCraftingSlot(8, new ItemStack(Items.OAK_LOG)));
                player.containerMenu.setCarried(new ItemStack(Items.DIAMOND));
                fixture.authorize();
                helper.assertTrue(!StorageServerStub.craftingTakeResult(fixture.playerId(), fixture.core().asLong(), false, false).changed()
                    && events.size() == 4, "Rejected cursor emitted a crafting event");
                player.containerMenu.setCarried(ItemStack.EMPTY);
                storage.setCrafting(CraftingStorage.EMPTY.withStonecutterInput(new ItemStack(Items.STONE)));
                var selected = player.level().recipeAccess().stonecutterRecipes().selectByInput(new ItemStack(Items.STONE))
                    .entries().getFirst().recipe().recipe().orElseThrow();
                fixture.authorize();
                helper.assertTrue(StorageServerStub.craftingTakeResult(fixture.playerId(), fixture.core().asLong(), true, false).changed()
                    && player.getRecipeBook().contains(selected.id()) && events.size() == 4,
                    "Stonecutting must unlock its recipe without a crafting-grid event");
            } finally {
                EVENTS.remove(player.getUUID());
            }
        }
        helper.succeed();
    }

    private static void merged(GameTestHelper helper) {
        var first = new HyperdimensionStorage(UUID.randomUUID());
        var second = new HyperdimensionStorage(UUID.randomUUID());
        var resource = ItemResource.of(Items.STONE);
        try (Transaction transaction = Transaction.openRoot()) {
            first.getItems().insert(resource, 2, transaction);
            second.getItems().insert(resource, 5, transaction);
            transaction.commit();
        }
        try {
            var type = Class.forName(StorageServerStub.class.getName() + "$StorageView");
            var constructor = type.getDeclaredConstructor(List.class, List.class);
            constructor.setAccessible(true);
            var view = constructor.newInstance(List.of(first, second), List.of());
            var extract = type.getDeclaredMethod("extract", int.class, ItemResource.class, int.class, Transaction.class);
            extract.setAccessible(true);
            var entriesField = type.getDeclaredField("entries");
            entriesField.setAccessible(true);
            var entry = ((List<?>) entriesField.get(view)).getFirst();
            var amount = entry.getClass().getDeclaredField("amount");
            amount.setAccessible(true);
            try (Transaction transaction = Transaction.openRoot()) {
                helper.assertTrue((int) extract.invoke(view, 0, resource, 6, transaction) == 6 && amount.getLong(entry) == 1,
                    "Merged extraction stopped at the first storage");
            }
            helper.assertTrue(first.getItems().getAmountAsLong(0) == 2 && second.getItems().getAmountAsLong(0) == 5
                && amount.getLong(entry) == 7, "Rollback lost resources or merged count");
            try (Transaction transaction = Transaction.openRoot()) {
                helper.assertTrue((int) extract.invoke(view, 0, resource, 6, transaction) == 6, "Committed merged extraction failed");
                helper.assertTrue((int) extract.invoke(view, 0, resource, 2, transaction) == 1 && amount.getLong(entry) == 0,
                    "Repeated extraction did not use live resources");
                transaction.commit();
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
        helper.succeed();
    }

    private static void locked(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper)) {
            var storage = StorageCraftingExecutionTests.storage(helper, fixture);
            storage.setCrafting(CraftingStorage.EMPTY.withCraftingSlot(8, new ItemStack(Items.DIAMOND)));
            fixture.player().getInventory().setItem(9, new ItemStack(Items.OAK_LOG, 2));
            fixture.authorize();
            helper.assertTrue(!StorageServerStub.craftingTransfer(fixture.playerId(), fixture.core().asLong(), false, false,
                List.of(new ItemStack(Items.OAK_LOG)), ItemStack.EMPTY, new IntArrayList(new int[]{1})),
                "Locked storage accepted recipe transfer");
            helper.assertTrue(storage.getCrafting().craftingInput().get(8).is(Items.DIAMOND)
                && fixture.player().getInventory().getItem(9).getCount() == 2, "Rejected transfer changed inputs or inventory");
            StorageCraftingTransferTests.unlock(fixture);
            fixture.authorize();
            helper.assertTrue(StorageServerStub.craftingTransfer(fixture.playerId(), fixture.core().asLong(), false, false,
                List.of(new ItemStack(Items.OAK_LOG)), ItemStack.EMPTY, new IntArrayList(new int[]{1})),
                "Unlocked storage rejected transfer");
        }
        helper.succeed();
    }
}
