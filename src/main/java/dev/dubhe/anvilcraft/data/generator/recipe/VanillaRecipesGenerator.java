package dev.dubhe.anvilcraft.data.generator.recipe;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.generator.MyRecipesGenerator;
import dev.dubhe.anvilcraft.data.recipe.crafting_table.ShapedTagRecipeBuilder;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItemTags;
import dev.dubhe.anvilcraft.init.ModItems;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.data.recipes.*;
import net.minecraft.data.recipes.packs.VanillaRecipeProvider;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.function.Consumer;

public abstract class VanillaRecipesGenerator {
    private VanillaRecipesGenerator() {
    }

    public static void buildRecipes(Consumer<FinishedRecipe> exporter) {
        // 工作台配方
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.FERRITE_CORE_MAGNET_BLOCK)
                .pattern("AAA")
                .pattern("ABA")
                .pattern("AAA")
                .define('A', ModItems.MAGNET_INGOT)
                .define('B', Items.IRON_INGOT)
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.MAGNET_INGOT), FabricRecipeProvider.has(ModItems.MAGNET_INGOT))
                .unlockedBy(MyRecipesGenerator.hasItem(Items.IRON_INGOT), FabricRecipeProvider.has(Items.IRON_INGOT))
                .save(exporter);
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.MAGNET_BLOCK)
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItems.MAGNET_INGOT)
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.MAGNET_INGOT), FabricRecipeProvider.has(ModItems.MAGNET_INGOT))
                .save(exporter);
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.HOLLOW_MAGNET_BLOCK)
                .pattern("AAA")
                .pattern("A A")
                .pattern("AAA")
                .define('A', ModItems.MAGNET_INGOT)
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.MAGNET_INGOT), FabricRecipeProvider.has(ModItems.MAGNET_INGOT))
                .save(exporter);
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.MAGNET)
                .pattern(" A ")
                .pattern("BCB")
                .pattern(" A ")
                .define('A', Items.REDSTONE)
                .define('B', ModItems.MAGNET_INGOT)
                .define('C', Items.ENDER_PEARL)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.REDSTONE), FabricRecipeProvider.has(Items.REDSTONE))
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.MAGNET_INGOT), FabricRecipeProvider.has(ModItems.MAGNET_INGOT))
                .unlockedBy(MyRecipesGenerator.hasItem(Items.ENDER_PEARL), FabricRecipeProvider.has(Items.ENDER_PEARL))
                .save(exporter);
        ShapedTagRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.AMETHYST_PICKAXE.getDefaultInstance())
                .pattern("AAA")
                .pattern(" B ")
                .pattern(" B ")
                .define('A', Items.AMETHYST_SHARD)
                .define('B', Items.STICK)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.AMETHYST_SHARD), FabricRecipeProvider.has(Items.AMETHYST_SHARD))
                .unlockedBy(MyRecipesGenerator.hasItem(Items.BAMBOO), FabricRecipeProvider.has(Items.BAMBOO))
                .save(exporter);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.NETHERITE_CORE)
                .pattern("AAA")
                .pattern("ABA")
                .pattern("AAA")
                .define('A', ModItems.DEBRIS_SCRAP)
                .define('B', ModItems.NETHER_STAR_SHARD)
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.DEBRIS_SCRAP), FabricRecipeProvider.has(ModItems.DEBRIS_SCRAP))
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.NETHER_STAR_SHARD), FabricRecipeProvider.has(ModItems.NETHER_STAR_SHARD))
                .save(exporter);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.NETHERITE_COIL)
                .pattern("AAA")
                .pattern("ABA")
                .pattern("AAA")
                .define('A', Items.LIGHT_WEIGHTED_PRESSURE_PLATE)
                .define('B', ModItems.NETHERITE_CORE)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.LIGHT_WEIGHTED_PRESSURE_PLATE), FabricRecipeProvider.has(Items.LIGHT_WEIGHTED_PRESSURE_PLATE))
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.NETHERITE_CORE), FabricRecipeProvider.has(ModItems.NETHERITE_CORE))
                .save(exporter);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ELYTRA_MEMBRANE)
                .pattern("AB")
                .pattern("BB")
                .define('A', ModItems.ELYTRA_FRAME)
                .define('B', Items.PHANTOM_MEMBRANE)
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.ELYTRA_FRAME), FabricRecipeProvider.has(ModItems.ELYTRA_FRAME))
                .unlockedBy(MyRecipesGenerator.hasItem(Items.PHANTOM_MEMBRANE), FabricRecipeProvider.has(Items.PHANTOM_MEMBRANE))
                .save(exporter);
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, Items.ELYTRA)
                .pattern("ABA")
                .pattern("C C")
                .pattern("C C")
                .define('A', ModItems.ELYTRA_MEMBRANE)
                .define('B', Items.STRING)
                .define('C', Items.FEATHER)
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.ELYTRA_MEMBRANE), FabricRecipeProvider.has(ModItems.ELYTRA_MEMBRANE))
                .unlockedBy(MyRecipesGenerator.hasItem(Items.STRING), FabricRecipeProvider.has(Items.STRING))
                .unlockedBy(MyRecipesGenerator.hasItem(Items.FEATHER), FabricRecipeProvider.has(Items.FEATHER))
                .save(exporter);
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, Items.TRIDENT)
                .pattern(" AA")
                .pattern(" BA")
                .pattern("B  ")
                .define('A', ModItems.BLADE_OF_THE_SEA)
                .define('B', Items.PRISMARINE_SHARD)
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.BLADE_OF_THE_SEA), FabricRecipeProvider.has(ModItems.BLADE_OF_THE_SEA))
                .unlockedBy(MyRecipesGenerator.hasItem(Items.PRISMARINE_SHARD), FabricRecipeProvider.has(Items.PRISMARINE_SHARD))
                .save(exporter);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ModItems.COCOA_LIQUOR, 2)
                .requires(ModItems.COCOA_POWDER)
                .requires(ModItems.COCOA_POWDER)
                .requires(ModItems.COCOA_BUTTER)
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.COCOA_POWDER), FabricRecipeProvider.has(ModItems.COCOA_POWDER))
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.COCOA_BUTTER), FabricRecipeProvider.has(ModItems.COCOA_BUTTER))
                .save(exporter);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CHOCOLATE)
                .pattern("ABA")
                .pattern("CDC")
                .pattern("ABA")
                .define('A', ModItems.COCOA_LIQUOR)
                .define('B', ModItems.COCOA_BUTTER)
                .define('C', ModItems.CREAM)
                .define('D', Items.SUGAR)
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.COCOA_LIQUOR), FabricRecipeProvider.has(ModItems.COCOA_LIQUOR))
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.COCOA_BUTTER), FabricRecipeProvider.has(ModItems.COCOA_BUTTER))
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.CREAM), FabricRecipeProvider.has(ModItems.CREAM))
                .unlockedBy(MyRecipesGenerator.hasItem(Items.SUGAR), FabricRecipeProvider.has(Items.SUGAR))
                .save(exporter);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CHOCOLATE_WHITE)
                .pattern("AAA")
                .pattern("BCB")
                .pattern("AAA")
                .define('A', ModItems.COCOA_BUTTER)
                .define('B', ModItems.CREAM)
                .define('C', Items.SUGAR)
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.COCOA_BUTTER), FabricRecipeProvider.has(ModItems.COCOA_BUTTER))
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.CREAM), FabricRecipeProvider.has(ModItems.CREAM))
                .unlockedBy(MyRecipesGenerator.hasItem(Items.SUGAR), FabricRecipeProvider.has(Items.SUGAR))
                .save(exporter);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CHOCOLATE_BLACK)
                .pattern("AAA")
                .pattern("BCB")
                .pattern("AAA")
                .define('A', ModItems.COCOA_LIQUOR)
                .define('B', ModItems.COCOA_BUTTER)
                .define('C', Items.SUGAR)
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.COCOA_LIQUOR), FabricRecipeProvider.has(ModItems.COCOA_LIQUOR))
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.COCOA_BUTTER), FabricRecipeProvider.has(ModItems.COCOA_BUTTER))
                .unlockedBy(MyRecipesGenerator.hasItem(Items.SUGAR), FabricRecipeProvider.has(Items.SUGAR))
                .save(exporter);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ROYAL_STEEL_BLOCK)
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItems.ROYAL_STEEL_INGOT)
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.ROYAL_STEEL_INGOT), FabricRecipeProvider.has(ModItems.ROYAL_STEEL_INGOT))
                .save(exporter);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.ROYAL_STEEL_INGOT, 9)
                .requires(ModItems.ROYAL_STEEL_BLOCK)
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.ROYAL_STEEL_BLOCK), FabricRecipeProvider.has(ModItems.ROYAL_STEEL_BLOCK))
                .save(exporter, AnvilCraft.of("craft/cut_royal_steel_slab_1"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ROYAL_ANVIL)
                .pattern("AAA")
                .pattern(" B ")
                .pattern("BBB")
                .define('A', ModItems.ROYAL_STEEL_BLOCK)
                .define('B', ModItems.ROYAL_STEEL_INGOT)
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.ROYAL_STEEL_BLOCK), FabricRecipeProvider.has(ModItems.ROYAL_STEEL_BLOCK))
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.ROYAL_STEEL_INGOT), FabricRecipeProvider.has(ModItems.ROYAL_STEEL_INGOT))
                .save(exporter);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ModItems.CREAMY_BREAD_ROLL)
                .requires(Items.BREAD)
                .requires(ModItems.CREAM)
                .requires(Items.SUGAR)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.BREAD), FabricRecipeProvider.has(Items.BREAD))
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.CREAM), FabricRecipeProvider.has(ModItems.CREAM))
                .unlockedBy(MyRecipesGenerator.hasItem(Items.SUGAR), FabricRecipeProvider.has(Items.SUGAR))
                .save(exporter);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ModItems.BEEF_MUSHROOM_STEW_RAW)
                .requires(Items.BEEF)
                .requires(Items.BROWN_MUSHROOM)
                .requires(Items.RED_MUSHROOM)
                .requires(Items.BOWL)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.BEEF), FabricRecipeProvider.has(Items.BEEF))
                .unlockedBy(MyRecipesGenerator.hasItem(Items.BROWN_MUSHROOM), FabricRecipeProvider.has(Items.BROWN_MUSHROOM))
                .unlockedBy(MyRecipesGenerator.hasItem(Items.RED_MUSHROOM), FabricRecipeProvider.has(Items.RED_MUSHROOM))
                .unlockedBy(MyRecipesGenerator.hasItem(Items.BOWL), FabricRecipeProvider.has(Items.BOWL))
                .save(exporter);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ModItems.UTUSAN_RAW)
                .requires(Items.SPIDER_EYE)
                .requires(Items.PUFFERFISH)
                .requires(Items.POISONOUS_POTATO)
                .requires(Items.LILY_OF_THE_VALLEY)
                .requires(Items.WITHER_ROSE)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.SPIDER_EYE), FabricRecipeProvider.has(Items.SPIDER_EYE))
                .unlockedBy(MyRecipesGenerator.hasItem(Items.PUFFERFISH), FabricRecipeProvider.has(Items.PUFFERFISH))
                .unlockedBy(MyRecipesGenerator.hasItem(Items.POISONOUS_POTATO), FabricRecipeProvider.has(Items.POISONOUS_POTATO))
                .unlockedBy(MyRecipesGenerator.hasItem(Items.LILY_OF_THE_VALLEY), FabricRecipeProvider.has(Items.LILY_OF_THE_VALLEY))
                .unlockedBy(MyRecipesGenerator.hasItem(Items.WITHER_ROSE), FabricRecipeProvider.has(Items.WITHER_ROSE))
                .save(exporter);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ModItems.MAGNET_INGOT, 9)
                .requires(ModItems.MAGNET_BLOCK)
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.MAGNET_BLOCK), FabricRecipeProvider.has(ModItems.MAGNET_BLOCK))
                .save(exporter, AnvilCraft.of("craft/magnet_ingot_9"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ModItems.MAGNET_INGOT, 8)
                .requires(ModItems.HOLLOW_MAGNET_BLOCK)
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.MAGNET_BLOCK), FabricRecipeProvider.has(ModItems.MAGNET_BLOCK))
                .save(exporter, AnvilCraft.of("craft/magnet_ingot_8"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CUT_ROYAL_STEEL_BLOCK, 4)
                .pattern("AA")
                .pattern("AA")
                .define('A', ModItems.ROYAL_STEEL_BLOCK)
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.ROYAL_STEEL_BLOCK), FabricRecipeProvider.has(ModItems.ROYAL_STEEL_BLOCK))
                .save(exporter, AnvilCraft.of("craft/cut_royal_steel_block"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CUT_ROYAL_STEEL_STAIRS, 4)
                .pattern("A  ")
                .pattern("AA ")
                .pattern("AAA")
                .define('A', ModItems.CUT_ROYAL_STEEL_BLOCK)
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.CUT_ROYAL_STEEL_BLOCK), FabricRecipeProvider.has(ModItems.CUT_ROYAL_STEEL_BLOCK))
                .save(exporter, AnvilCraft.of("craft/cut_royal_steel_stairs"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CUT_ROYAL_STEEL_SLAB, 6)
                .pattern("AAA")
                .define('A', ModItems.CUT_ROYAL_STEEL_BLOCK)
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.CUT_ROYAL_STEEL_BLOCK), FabricRecipeProvider.has(ModItems.CUT_ROYAL_STEEL_BLOCK))
                .save(exporter, AnvilCraft.of("craft/cut_royal_steel_slab"));
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.AUTO_CRAFTER)
                .pattern("AAA")
                .pattern("ABA")
                .pattern("ACA")
                .define('A', Items.IRON_INGOT)
                .define('B', Items.CRAFTING_TABLE)
                .define('C', Items.DROPPER)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.IRON_INGOT), FabricRecipeProvider.has(Items.IRON_INGOT))
                .unlockedBy(MyRecipesGenerator.hasItem(Items.CRAFTING_TABLE), FabricRecipeProvider.has(Items.CRAFTING_TABLE))
                .unlockedBy(MyRecipesGenerator.hasItem(Items.DROPPER), FabricRecipeProvider.has(Items.DROPPER))
                .save(exporter);
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.CHUTE)
                .pattern("A A")
                .pattern("ABA")
                .pattern(" A ")
                .define('A', Items.IRON_INGOT)
                .define('B', Items.DROPPER)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.IRON_INGOT), FabricRecipeProvider.has(Items.IRON_INGOT))
                .unlockedBy(MyRecipesGenerator.hasItem(Items.DROPPER), FabricRecipeProvider.has(Items.DROPPER))
                .save(exporter);
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, Items.CHIPPED_ANVIL)
                .pattern("AAB")
                .pattern(" B ")
                .pattern("BBB")
                .define('A', Items.IRON_BLOCK)
                .define('B', Items.IRON_INGOT)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.IRON_BLOCK), FabricRecipeProvider.has(Items.IRON_BLOCK))
                .unlockedBy(MyRecipesGenerator.hasItem(Items.IRON_INGOT), FabricRecipeProvider.has(Items.IRON_INGOT))
                .save(exporter);
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, Items.DAMAGED_ANVIL)
                .pattern("BAB")
                .pattern(" B ")
                .pattern("BBB")
                .define('A', Items.IRON_BLOCK)
                .define('B', Items.IRON_INGOT)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.IRON_BLOCK), FabricRecipeProvider.has(Items.IRON_BLOCK))
                .unlockedBy(MyRecipesGenerator.hasItem(Items.IRON_INGOT), FabricRecipeProvider.has(Items.IRON_INGOT))
                .save(exporter);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.ROYAL_STEEL_NUGGET, 9)
                .requires(ModItems.ROYAL_STEEL_INGOT)
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.ROYAL_STEEL_NUGGET), FabricRecipeProvider.has(ModItems.ROYAL_STEEL_NUGGET))
                .save(exporter);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ROYAL_STEEL_INGOT)
                .pattern("AAA")
                .pattern("AAA")
                .pattern("AAA")
                .define('A', ModItems.ROYAL_STEEL_NUGGET)
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.ROYAL_STEEL_NUGGET), FabricRecipeProvider.has(ModItems.ROYAL_STEEL_NUGGET))
                .save(exporter, AnvilCraft.of("craft/cut_royal_steel_slab_2"));
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.STAMPING_PLATFORM)
                .pattern("BAB")
                .pattern("B B")
                .pattern("B B")
                .define('A', ModItemTags.IRON_PLATES)
                .define('B', Items.IRON_INGOT)
                .unlockedBy("has_" + ModItemTags.IRON_PLATES.location().getPath(), FabricRecipeProvider.has(ModItemTags.IRON_PLATES))
                .unlockedBy(MyRecipesGenerator.hasItem(Items.IRON_INGOT), FabricRecipeProvider.has(Items.IRON_INGOT))
                .save(exporter);
        // 烹饪配方
        SimpleCookingRecipeBuilder.campfireCooking(Ingredient.of(ModItemTags.DOUGH), RecipeCategory.FOOD, Items.BREAD, 0.35f, 600)
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.DOUGH), FabricRecipeProvider.has(ModItems.DOUGH))
                .save(exporter, AnvilCraft.of("campfire_cooking_bread"));
        SimpleCookingRecipeBuilder.smoking(Ingredient.of(ModItemTags.DOUGH), RecipeCategory.FOOD, Items.BREAD, 0.35f, 100)
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.DOUGH), FabricRecipeProvider.has(ModItems.DOUGH))
                .save(exporter, AnvilCraft.of("smoking_bread"));
        SimpleCookingRecipeBuilder.generic(Ingredient.of(ModItemTags.DOUGH), RecipeCategory.FOOD, Items.BREAD, 0.35f, 600, RecipeSerializer.SMOKING_RECIPE)
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.DOUGH), FabricRecipeProvider.has(ModItems.DOUGH))
                .save(exporter, AnvilCraft.of("generic_cooking_bread"));
        // 切石机配方
        VanillaRecipeProvider.stonecutterResultFromBase(exporter, RecipeCategory.BUILDING_BLOCKS, ModBlocks.CUT_ROYAL_STEEL_STAIRS, ModBlocks.CUT_ROYAL_STEEL_BLOCK);
        VanillaRecipeProvider.stonecutterResultFromBase(exporter, RecipeCategory.BUILDING_BLOCKS, ModBlocks.CUT_ROYAL_STEEL_SLAB, ModBlocks.CUT_ROYAL_STEEL_BLOCK, 2);
        VanillaRecipeProvider.stonecutterResultFromBase(exporter, RecipeCategory.BUILDING_BLOCKS, ModBlocks.CUT_ROYAL_STEEL_BLOCK, ModBlocks.ROYAL_STEEL_BLOCK, 4);
        VanillaRecipeProvider.stonecutterResultFromBase(exporter, RecipeCategory.BUILDING_BLOCKS, ModBlocks.CUT_ROYAL_STEEL_STAIRS, ModBlocks.ROYAL_STEEL_BLOCK, 4);
        VanillaRecipeProvider.stonecutterResultFromBase(exporter, RecipeCategory.BUILDING_BLOCKS, ModBlocks.CUT_ROYAL_STEEL_SLAB, ModBlocks.ROYAL_STEEL_BLOCK, 8);
        VanillaRecipeProvider.stonecutterResultFromBase(exporter, RecipeCategory.BUILDING_BLOCKS, ModBlocks.SMOOTH_ROYAL_STEEL_BLOCK, ModBlocks.ROYAL_STEEL_BLOCK, 4);
        // 锻造台配方
        SmithingTransformRecipeBuilder.smithing(Ingredient.of(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE), Ingredient.of(ModItemTags.ROYAL_STEEL_PICKAXE_BASE), Ingredient.of(ModItems.ROYAL_STEEL_INGOT), RecipeCategory.TOOLS, ModItems.ROYAL_STEEL_PICKAXE)
                .unlocks("royal_steel_pickaxe", FabricRecipeProvider.has(ModItems.AMETHYST_PICKAXE))
                .save(exporter, AnvilCraft.of("smithing/royal_steel_pickaxe"));
    }
}
