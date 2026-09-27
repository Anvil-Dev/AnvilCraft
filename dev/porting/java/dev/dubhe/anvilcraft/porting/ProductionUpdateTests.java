package dev.dubhe.anvilcraft.porting;

import dev.anvilcraft.lib.v2.recipe.cache.ItemCache;
import dev.anvilcraft.lib.v2.recipe.event.ItemCacheEvent;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity;
import dev.dubhe.anvilcraft.block.entity.MineralFountainBlockEntity;
import dev.dubhe.anvilcraft.block.entity.fluid.DrainBlockEntity;
import dev.dubhe.anvilcraft.block.workstation.FishTankBlock;
import dev.dubhe.anvilcraft.event.InWorldRecipeEventListener;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemCompressRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.VanillaRecipesWrap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.crafting.CompoundIngredient;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class ProductionUpdateTests {
    private static final BlockPos BASE = new BlockPos(3, 2, 3);
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_production_fountain", ProductionUpdateTests::fountain,
        "port_production_fish_output", ProductionUpdateTests::fishOutput,
        "port_production_fish_catalyst", ProductionUpdateTests::fishCatalyst,
        "port_production_compression", ProductionUpdateTests::compression
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_production_update"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true))));
    }

    private static void fountain(GameTestHelper helper) {
        var level = helper.getLevel();
        var anchor = helper.absolutePos(BASE);
        var pos = new BlockPos(anchor.getX(), level.getMinY() + 4, anchor.getZ());
        level.setBlock(pos, ModBlocks.MINERAL_FOUNTAIN.getDefaultState(), Block.UPDATE_CLIENTS);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            level.setBlock(pos.relative(direction), Blocks.LAVA.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        level.setBlock(pos.above(), ModBlocks.DRAIN.getDefaultState(), Block.UPDATE_CLIENTS);
        var fountain = (MineralFountainBlockEntity) level.getBlockEntity(pos);
        var handler = ((DrainBlockEntity) level.getBlockEntity(pos.above())).getFluidHandler();
        fountain.resetTickCount();
        for (int i = 0; i < AnvilCraft.CONFIG.mineralFountainInterval - 1; i++) fountain.tick();
        helper.assertTrue(handler.getAmountAsInt(0) == 0, "Fountain observes the configured interval");
        fountain.tick();
        helper.assertTrue(handler.getAmountAsInt(0) == 1000 && handler.getResource(0).getFluid() == Fluids.LAVA,
            "Fountain supplies exactly one lava bucket");
        for (int i = 0; i < 3 * AnvilCraft.CONFIG.mineralFountainInterval; i++) fountain.tick();
        helper.assertTrue(handler.getAmountAsInt(0) == 4000, "Fountain resets and repeats its interval");
        try (Transaction tx = Transaction.openRoot()) {
            handler.extract(FluidResource.of(Fluids.LAVA), 500, tx);
            tx.commit();
        }
        for (int i = 0; i < AnvilCraft.CONFIG.mineralFountainInterval; i++) fountain.tick();
        helper.assertTrue(handler.getAmountAsInt(0) == 3500, "Partial bucket insertion rolls back completely");
        try (Transaction tx = Transaction.openRoot()) {
            handler.extract(FluidResource.of(Fluids.LAVA), 3500, tx);
            handler.insert(FluidResource.of(Fluids.WATER), 1000, tx);
            tx.commit();
        }
        for (int i = 0; i < AnvilCraft.CONFIG.mineralFountainInterval; i++) fountain.tick();
        helper.assertTrue(handler.getAmountAsInt(0) == 1000 && handler.getResource(0).getFluid() == Fluids.WATER,
            "Incompatible fluid remains unchanged");
        var highPos = pos.above(5);
        level.setBlock(highPos, ModBlocks.MINERAL_FOUNTAIN.getDefaultState(), Block.UPDATE_CLIENTS);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            level.setBlock(highPos.relative(direction), Blocks.LAVA.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        level.setBlock(highPos.above(), ModBlocks.DRAIN.getDefaultState(), Block.UPDATE_CLIENTS);
        var high = (MineralFountainBlockEntity) level.getBlockEntity(highPos);
        high.resetTickCount();
        for (int i = 0; i < AnvilCraft.CONFIG.mineralFountainInterval; i++) high.tick();
        helper.assertTrue(((DrainBlockEntity) level.getBlockEntity(highPos.above())).getFluidHandler().getAmountAsInt(0) == 0,
            "Drain integration preserves the bedrock-height restriction");
        helper.succeed();
    }

    private static FishTankBlockEntity tank(GameTestHelper helper) {
        helper.setBlock(BASE, ModBlocks.FISH_TANK.getDefaultState().setValue(FishTankBlock.OUTLET, true)
            .setValue(FishTankBlock.FACING, Direction.EAST));
        helper.setBlock(BASE.east(), Blocks.AIR);
        return helper.getBlockEntity(BASE, FishTankBlockEntity.class);
    }

    private static int drops(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(BASE)).inflate(2))
            .stream().mapToInt(entity -> entity.getItem().getCount()).sum();
    }

    private static void fishOutput(GameTestHelper helper) {
        var tank = tank(helper);
        tank.beginRecipeProcessing();
        tank.getOutputHandler().set(0, ItemResource.of(Items.DIAMOND), 1);
        tank.tryAutoOutputResults();
        helper.assertTrue(drops(helper) == 0 && tank.getOutputHandler().getAmountAsInt(0) == 1,
            "No automatic output during partial recipe commit");
        tank.getOutputHandler().set(0, ItemResource.of(Items.DIAMOND), 2);
        tank.finishRecipeProcessing();
        helper.assertTrue(drops(helper) == 2 && tank.getOutputHandler().getAmountAsInt(0) == 0,
            "Repeated cache writes settle the final amount exactly once");
        tank.finishRecipeProcessing();
        helper.assertTrue(drops(helper) == 2, "Repeated finish cannot duplicate output");
        helper.setBlock(BASE, tank.getBlockState().setValue(FishTankBlock.OUTLET, false));
        tank.getOutputHandler().set(0, ItemResource.of(Items.DIAMOND), 3);
        tank.beginRecipeProcessing();
        helper.assertTrue(tank.getInput() == tank.getOutputHandler() && tank.getOutput() != tank.getOutputHandler(),
            "Existing output can be processed once without aliased input/output caches");
        tank.finishRecipeProcessing();
        tank.beginRecipeProcessing();
        helper.assertTrue(tank.getInput() == tank.getInputHandler(), "Output cannot be processed twice in one tick");
        tank.finishRecipeProcessing();
        helper.assertTrue(drops(helper) == 2 && tank.getOutputHandler().getAmountAsInt(0) == 3,
            "Closed outlet preserves completed products");
        helper.setBlock(BASE.east(), Blocks.STONE);
        helper.setBlock(BASE, tank.getBlockState().setValue(FishTankBlock.OUTLET, true));
        tank.tryAutoOutputResults();
        helper.assertTrue(drops(helper) == 2 && tank.getOutputHandler().getAmountAsInt(0) == 3,
            "Blocked outlet preserves completed products");
        helper.setBlock(BASE.east(), Blocks.AIR);
        tank.tryAutoOutputResults();
        helper.assertTrue(drops(helper) == 5 && tank.getOutputHandler().getAmountAsInt(0) == 0,
            "Removing the obstruction releases only the retained output");
        helper.succeed();
    }

    private static void fishCatalyst(GameTestHelper helper) {
        var tank = tank(helper);
        tank.getInputHandler().set(0, ItemResource.of(Items.IRON_INGOT), 4);
        tank.beginRecipeProcessing();
        tank.getInputHandler().set(0, ItemResource.of(Items.IRON_INGOT), 3);
        var pos = helper.absolutePos(BASE).getCenter();
        var product = new ItemEntity(helper.getLevel(), pos.x, pos.y, pos.z, new ItemStack(Items.IRON_INGOT, 2));
        InWorldRecipeEventListener.spawnItemEntity(new ItemCacheEvent.SpawnItemEntity(new ItemCache(helper.getLevel()), product));
        helper.assertTrue(product.isRemoved() && tank.getInputHandler().getAmountAsInt(0) == 4 && drops(helper) == 0,
            "Spawned recipe product returns exactly the consumed catalyst before output");
        tank.insertRecipeOutputReturningCatalyst(new ItemStack(Items.IRON_INGOT));
        tank.finishRecipeProcessing();
        helper.assertTrue(tank.getInputHandler().getAmountAsInt(0) == 4 && drops(helper) == 2,
            "Repeated products cannot over-refund catalyst inputs");
        var named = new ItemStack(Items.IRON_INGOT);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("catalyst"));
        tank.getInputHandler().set(0, ItemResource.of(named), 1);
        tank.beginRecipeProcessing();
        tank.getInputHandler().set(0, ItemResource.EMPTY, 0);
        tank.insertRecipeOutputReturningCatalyst(new ItemStack(Items.IRON_INGOT));
        tank.finishRecipeProcessing();
        helper.assertTrue(tank.getInputHandler().getAmountAsInt(0) == 0 && drops(helper) == 3,
            "Different components are output, never refunded as a catalyst");
        helper.succeed();
    }

    private static void compression(GameTestHelper helper) {
        var previous = VanillaRecipesWrap.recipes;
        try {
            var common = new Recipe.CommonInfo(true);
            var book = new CraftingRecipe.CraftingBookInfo(CraftingBookCategory.MISC, "");
            var broad = Ingredient.of(Items.IRON_INGOT, Items.GOLD_INGOT);
            var narrow = Ingredient.of(Items.IRON_INGOT);
            var simpleCustom = CompoundIngredient.of(narrow, Ingredient.of(Items.COPPER_INGOT));
            var ingredients = List.of(broad, narrow, broad, simpleCustom);
            VanillaRecipesWrap.recipes = new ArrayList<>();
            VanillaRecipesWrap.wrap(new ShapelessRecipe(common, book, new ItemStackTemplate(Items.IRON_BLOCK, 1), ingredients));
            checkCompression(helper, 4);
            VanillaRecipesWrap.recipes.clear();
            VanillaRecipesWrap.wrap(new ShapedRecipe(common, book,
                ShapedRecipePattern.of(Map.of('A', broad, 'B', narrow, 'C', simpleCustom), "AB", "CA"),
                new ItemStackTemplate(Items.IRON_BLOCK, 1)));
            checkCompression(helper, 4);
            for (var rejected : List.of(Ingredient.of(Items.DIAMOND),
                DataComponentIngredient.of(DataComponents.CUSTOM_NAME, Component.literal("restricted"), Items.IRON_INGOT))) {
                VanillaRecipesWrap.recipes.clear();
                VanillaRecipesWrap.wrap(new ShapelessRecipe(common, book, new ItemStackTemplate(Items.IRON_BLOCK, 1),
                    List.of(broad, broad, broad, rejected)));
                helper.assertTrue(VanillaRecipesWrap.recipes.isEmpty(), "Disjoint or component-sensitive ingredients are not compressed");
            }
            VanillaRecipesWrap.recipes.clear();
            VanillaRecipesWrap.wrap(new ShapedRecipe(common, book, ShapedRecipePattern.of(Map.of('A', narrow), "AA", "A "),
                new ItemStackTemplate(Items.IRON_BLOCK, 1)));
            helper.assertTrue(VanillaRecipesWrap.recipes.isEmpty(), "Empty shaped cells cannot become counted ingredients");
        } finally {
            VanillaRecipesWrap.recipes = previous;
        }
        helper.succeed();
    }

    private static void checkCompression(GameTestHelper helper, int count) {
        helper.assertTrue(VanillaRecipesWrap.recipes.size() == 1, "One compression recipe is generated for the intersection");
        var holder = VanillaRecipesWrap.recipes.getFirst();
        var ingredient = ((ItemCompressRecipe) holder.value()).getInputItems().getFirst();
        helper.assertTrue(ingredient.count() == count && ingredient.test(new ItemStack(Items.IRON_INGOT, count))
            && !ingredient.test(new ItemStack(Items.GOLD_INGOT, count)), "Only common ingredients and the original count survive");
        helper.assertTrue(holder.id().identifier().getPath().equals("compress_warp_iron_ingot_2_iron_block"),
            "Generated compression ID matches source naming");
    }
}
