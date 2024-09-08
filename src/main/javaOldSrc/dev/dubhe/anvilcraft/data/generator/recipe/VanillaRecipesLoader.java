package dev.dubhe.anvilcraft.data.generator.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.generator.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItemTags;
import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.recipes.packs.VanillaRecipeProvider;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmokingRecipe;

public class VanillaRecipesLoader {
    /**
     * 原版配方
     *
     * @param provider 提供器
     */
    public static void init(RegistrateRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, Items.CHIPPED_ANVIL)
            .pattern("AAB")
            .pattern(" B ")
            .pattern("BBB")
            .define('A', Items.IRON_BLOCK)
            .define('B', Items.IRON_INGOT)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_BLOCK), AnvilCraftDatagen.has(Items.IRON_BLOCK))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_INGOT), AnvilCraftDatagen.has(Items.IRON_INGOT))
            .save(provider);
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, Items.DAMAGED_ANVIL)
            .pattern("BAB")
            .pattern(" B ")
            .pattern("BBB")
            .define('A', Items.IRON_BLOCK)
            .define('B', Items.IRON_INGOT)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_BLOCK), AnvilCraftDatagen.has(Items.IRON_BLOCK))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_INGOT), AnvilCraftDatagen.has(Items.IRON_INGOT))
            .save(provider);
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, Items.TRIDENT)
            .pattern(" AA")
            .pattern(" BA")
            .pattern("B  ")
            .define('A', ModItems.PRISMARINE_BLADE)
            .define('B', Items.PRISMARINE_BRICKS)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModItems.PRISMARINE_BLADE))
            .save(provider);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Items.HEART_OF_THE_SEA)
            .pattern("A")
            .pattern("B")
            .pattern("A")
            .define('A', ModItems.SEA_HEART_SHELL)
            .define('B', ModItems.SAPPHIRE)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModItems.SEA_HEART_SHELL))
            .save(provider);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CIRCUIT_BOARD)
            .pattern("BCB")
            .pattern("AAA")
            .define('A', ModItems.HARDEND_RESIN)
            .define('B', Items.COPPER_INGOT)
            .define('C', Items.QUARTZ)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModItems.HARDEND_RESIN))
            .save(provider);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Items.WET_SPONGE)
            .pattern("AA")
            .pattern("AA")
            .define('A', ModItems.SPONGE_GEMMULE)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModItems.SPONGE_GEMMULE))
            .save(provider);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.HEAVY_IRON_BLOCK)
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', Items.IRON_BLOCK)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(Items.IRON_BLOCK))
            .save(provider);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Items.IRON_BLOCK, 9)
            .requires(ModBlocks.HEAVY_IRON_BLOCK)
            .unlockedBy("hasitem", RegistrateRecipeProvider.has(ModBlocks.HEAVY_IRON_BLOCK))
            .save(provider);

        SimpleCookingRecipeBuilder.campfireCooking(Ingredient.of(ModItemTags.DOUGH), RecipeCategory.FOOD,
                Items.BREAD, 0.35f, 600)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.DOUGH.get()), AnvilCraftDatagen.has(ModItems.DOUGH))
            .save(provider, AnvilCraft.of("campfire_cooking_bread"));
        SimpleCookingRecipeBuilder.smoking(Ingredient.of(ModItemTags.DOUGH), RecipeCategory.FOOD,
                Items.BREAD, 0.35f, 100)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.DOUGH.get()), AnvilCraftDatagen.has(ModItems.DOUGH))
            .save(provider, AnvilCraft.of("smoking_bread"));
        SimpleCookingRecipeBuilder.generic(
                Ingredient.of(ModItemTags.DOUGH),
                RecipeCategory.FOOD,
                Items.BREAD,
                0.35f,
                600,
                RecipeSerializer.SMOKING_RECIPE,
                SmokingRecipe::new
            )
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.DOUGH.get()), AnvilCraftDatagen.has(ModItems.DOUGH))
            .save(provider, AnvilCraft.of("generic_cooking_bread"));

        SimpleCookingRecipeBuilder.campfireCooking(Ingredient.of(ModItemTags.DOUGH_FORGE), RecipeCategory.FOOD,
                Items.BREAD, 0.35f, 600)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.DOUGH.get()), AnvilCraftDatagen.has(ModItems.DOUGH))
            .save(provider, AnvilCraft.of("campfire_cooking_bread_forge"));
        SimpleCookingRecipeBuilder.smoking(Ingredient.of(ModItemTags.DOUGH_FORGE), RecipeCategory.FOOD,
                Items.BREAD, 0.35f, 100)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.DOUGH.get()), AnvilCraftDatagen.has(ModItems.DOUGH))
            .save(provider, AnvilCraft.of("smoking_bread_forge"));
        SimpleCookingRecipeBuilder.generic(Ingredient.of(ModItemTags.DOUGH_FORGE), RecipeCategory.FOOD,
                Items.BREAD, 0.35f, 600, RecipeSerializer.SMOKING_RECIPE)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.DOUGH.get()), AnvilCraftDatagen.has(ModItems.DOUGH))
            .save(provider, AnvilCraft.of("generic_cooking_bread_forge"));

        VanillaRecipeProvider.stonecutterResultFromBase(provider, RecipeCategory.BUILDING_BLOCKS,
            ModBlocks.CUT_ROYAL_STEEL_STAIRS, ModBlocks.CUT_ROYAL_STEEL_BLOCK);
        VanillaRecipeProvider.stonecutterResultFromBase(provider, RecipeCategory.BUILDING_BLOCKS,
            ModBlocks.CUT_ROYAL_STEEL_PILLAR, ModBlocks.CUT_ROYAL_STEEL_BLOCK);
        VanillaRecipeProvider.stonecutterResultFromBase(provider, RecipeCategory.BUILDING_BLOCKS,
            ModBlocks.CUT_ROYAL_STEEL_PILLAR, ModBlocks.ROYAL_STEEL_BLOCK, 4);
        VanillaRecipeProvider.stonecutterResultFromBase(provider, RecipeCategory.BUILDING_BLOCKS,
            ModBlocks.CUT_ROYAL_STEEL_SLAB, ModBlocks.CUT_ROYAL_STEEL_BLOCK, 2);
        VanillaRecipeProvider.stonecutterResultFromBase(provider, RecipeCategory.BUILDING_BLOCKS,
            ModBlocks.CUT_ROYAL_STEEL_BLOCK, ModBlocks.ROYAL_STEEL_BLOCK, 4);
        VanillaRecipeProvider.stonecutterResultFromBase(provider, RecipeCategory.BUILDING_BLOCKS,
            ModBlocks.CUT_ROYAL_STEEL_STAIRS, ModBlocks.ROYAL_STEEL_BLOCK, 4);
        VanillaRecipeProvider.stonecutterResultFromBase(provider, RecipeCategory.BUILDING_BLOCKS,
            ModBlocks.CUT_ROYAL_STEEL_SLAB, ModBlocks.ROYAL_STEEL_BLOCK, 8);
        VanillaRecipeProvider.stonecutterResultFromBase(provider, RecipeCategory.BUILDING_BLOCKS,
            ModBlocks.SMOOTH_ROYAL_STEEL_BLOCK, ModBlocks.ROYAL_STEEL_BLOCK, 4);

        VanillaRecipeProvider.stonecutterResultFromBase(provider, RecipeCategory.BUILDING_BLOCKS,
            ModBlocks.POLISHED_HEAVY_IRON_BLOCK, ModBlocks.HEAVY_IRON_BLOCK);
        VanillaRecipeProvider.stonecutterResultFromBase(provider, RecipeCategory.BUILDING_BLOCKS,
            ModBlocks.POLISHED_HEAVY_IRON_SLAB, ModBlocks.HEAVY_IRON_BLOCK, 2);
        VanillaRecipeProvider.stonecutterResultFromBase(provider, RecipeCategory.BUILDING_BLOCKS,
            ModBlocks.POLISHED_HEAVY_IRON_STAIRS, ModBlocks.HEAVY_IRON_BLOCK);
        VanillaRecipeProvider.stonecutterResultFromBase(provider, RecipeCategory.BUILDING_BLOCKS,
            ModBlocks.POLISHED_HEAVY_IRON_SLAB, ModBlocks.POLISHED_HEAVY_IRON_BLOCK, 2);
        VanillaRecipeProvider.stonecutterResultFromBase(provider, RecipeCategory.BUILDING_BLOCKS,
            ModBlocks.POLISHED_HEAVY_IRON_STAIRS, ModBlocks.POLISHED_HEAVY_IRON_BLOCK);

        VanillaRecipeProvider.stonecutterResultFromBase(provider, RecipeCategory.BUILDING_BLOCKS,
            ModBlocks.CUT_HEAVY_IRON_BLOCK, ModBlocks.HEAVY_IRON_BLOCK, 4);
        VanillaRecipeProvider.stonecutterResultFromBase(provider, RecipeCategory.BUILDING_BLOCKS,
            ModBlocks.CUT_HEAVY_IRON_SLAB, ModBlocks.HEAVY_IRON_BLOCK, 8);
        VanillaRecipeProvider.stonecutterResultFromBase(provider, RecipeCategory.BUILDING_BLOCKS,
            ModBlocks.CUT_HEAVY_IRON_STAIRS, ModBlocks.HEAVY_IRON_BLOCK, 4);
        VanillaRecipeProvider.stonecutterResultFromBase(provider, RecipeCategory.BUILDING_BLOCKS,
            ModBlocks.CUT_HEAVY_IRON_SLAB, ModBlocks.CUT_HEAVY_IRON_BLOCK, 2);
        VanillaRecipeProvider.stonecutterResultFromBase(provider, RecipeCategory.BUILDING_BLOCKS,
            ModBlocks.CUT_HEAVY_IRON_STAIRS, ModBlocks.CUT_HEAVY_IRON_BLOCK);

        VanillaRecipeProvider.stonecutterResultFromBase(provider, RecipeCategory.BUILDING_BLOCKS,
            ModBlocks.HEAVY_IRON_PLATE, ModBlocks.HEAVY_IRON_BLOCK, 8);
        VanillaRecipeProvider.stonecutterResultFromBase(provider, RecipeCategory.BUILDING_BLOCKS,
            ModBlocks.HEAVY_IRON_COLUMN, ModBlocks.HEAVY_IRON_BLOCK, 4);
        VanillaRecipeProvider.stonecutterResultFromBase(provider, RecipeCategory.BUILDING_BLOCKS,
            ModBlocks.HEAVY_IRON_BEAM, ModBlocks.HEAVY_IRON_BLOCK, 4);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.DISCHARGER)
            .requires(ModBlocks.CHARGER, 1)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.CHARGER),
                AnvilCraftDatagen.has(ModBlocks.CHARGER)
            )
            .save(provider, AnvilCraft.of("discharger_from_charger"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.CHARGER)
            .requires(ModBlocks.DISCHARGER, 1)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.DISCHARGER),
                AnvilCraftDatagen.has(ModBlocks.DISCHARGER)
            )
            .save(provider, AnvilCraft.of("charger_from_discharger"));

        SimpleCookingRecipeBuilder
            .smelting(
                Ingredient.of(ModItems.RAW_ZINC),
                RecipeCategory.MISC,
                ModItems.ZINC_INGOT,
                1,
                200)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.RAW_ZINC.get()),
                AnvilCraftDatagen.has(ModItems.RAW_ZINC))
            .save(provider, AnvilCraft.of("zinc_ingot_from_raw_zinc"));

        SimpleCookingRecipeBuilder
            .blasting(
                Ingredient.of(ModItems.RAW_ZINC),
                RecipeCategory.MISC,
                ModItems.ZINC_INGOT,
                1,
                100)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.RAW_ZINC.get()),
                AnvilCraftDatagen.has(ModItems.RAW_ZINC))
            .save(provider, AnvilCraft.of("zinc_ingot_from_raw_zinc_blasting"));
        SimpleCookingRecipeBuilder
            .smelting(
                Ingredient.of(ModBlocks.DEEPSLATE_ZINC_ORE),
                RecipeCategory.MISC,
                ModItems.ZINC_INGOT,
                1,
                200)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.DEEPSLATE_ZINC_ORE),
                AnvilCraftDatagen.has(ModBlocks.DEEPSLATE_ZINC_ORE))
            .save(provider, AnvilCraft.of("zinc_ingot_from_deepslate_zinc_ore"));
        SimpleCookingRecipeBuilder
            .blasting(
                Ingredient.of(ModBlocks.DEEPSLATE_ZINC_ORE),
                RecipeCategory.MISC,
                ModItems.ZINC_INGOT,
                1,
                100)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.DEEPSLATE_ZINC_ORE),
                AnvilCraftDatagen.has(ModBlocks.DEEPSLATE_ZINC_ORE))
            .save(provider, AnvilCraft.of("zinc_ingot_from_deepslate_zinc_ore_blasting"));
        SimpleCookingRecipeBuilder
            .smelting(
                Ingredient.of(ModItems.RAW_TIN),
                RecipeCategory.MISC,
                ModItems.TIN_INGOT,
                1,
                200)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.RAW_TIN.get()),
                AnvilCraftDatagen.has(ModItems.RAW_TIN))
            .save(provider, AnvilCraft.of("tin_ingot_from_raw_tin"));

        SimpleCookingRecipeBuilder
            .blasting(
                Ingredient.of(ModItems.RAW_TIN),
                RecipeCategory.MISC,
                ModItems.TIN_INGOT,
                1,
                100)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.RAW_TIN.get()),
                AnvilCraftDatagen.has(ModItems.RAW_TIN))
            .save(provider, AnvilCraft.of("tin_ingot_from_raw_tin_blasting"));
        SimpleCookingRecipeBuilder
            .smelting(
                Ingredient.of(ModBlocks.DEEPSLATE_TIN_ORE),
                RecipeCategory.MISC,
                ModItems.TIN_INGOT,
                1,
                200)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.DEEPSLATE_TIN_ORE),
                AnvilCraftDatagen.has(ModBlocks.DEEPSLATE_TIN_ORE))
            .save(provider, AnvilCraft.of("tin_ingot_from_deepslate_tin_ore"));
        SimpleCookingRecipeBuilder
            .blasting(
                Ingredient.of(ModBlocks.DEEPSLATE_TIN_ORE),
                RecipeCategory.MISC,
                ModItems.TIN_INGOT,
                1,
                100)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.DEEPSLATE_TIN_ORE),
                AnvilCraftDatagen.has(ModBlocks.DEEPSLATE_TIN_ORE))
            .save(provider, AnvilCraft.of("tin_ingot_from_deepslate_tin_ore_blasting"));
        SimpleCookingRecipeBuilder
            .smelting(
                Ingredient.of(ModItems.RAW_TITANIUM),
                RecipeCategory.MISC,
                ModItems.TITANIUM_INGOT,
                1,
                200)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.RAW_TITANIUM.get()),
                AnvilCraftDatagen.has(ModItems.RAW_TITANIUM))
            .save(provider, AnvilCraft.of("titanium_ingot_from_raw_titanium"));

        SimpleCookingRecipeBuilder
            .blasting(
                Ingredient.of(ModItems.RAW_TITANIUM),
                RecipeCategory.MISC,
                ModItems.TITANIUM_INGOT,
                1,
                100)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.RAW_TITANIUM.get()),
                AnvilCraftDatagen.has(ModItems.RAW_TITANIUM))
            .save(provider, AnvilCraft.of("titanium_ingot_from_raw_titanium_blasting"));
        SimpleCookingRecipeBuilder
            .smelting(
                Ingredient.of(ModBlocks.DEEPSLATE_TITANIUM_ORE),
                RecipeCategory.MISC,
                ModItems.TITANIUM_INGOT,
                1,
                200)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.DEEPSLATE_TITANIUM_ORE),
                AnvilCraftDatagen.has(ModBlocks.DEEPSLATE_TITANIUM_ORE))
            .save(provider, AnvilCraft.of("titanium_ingot_from_deepslate_titanium_ore"));
        SimpleCookingRecipeBuilder
            .blasting(
                Ingredient.of(ModBlocks.DEEPSLATE_TITANIUM_ORE),
                RecipeCategory.MISC,
                ModItems.TITANIUM_INGOT,
                1,
                100)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.DEEPSLATE_TITANIUM_ORE),
                AnvilCraftDatagen.has(ModBlocks.DEEPSLATE_TITANIUM_ORE))
            .save(provider, AnvilCraft.of("titanium_ingot_from_deepslate_titanium_ore_blasting"));
        SimpleCookingRecipeBuilder
            .smelting(
                Ingredient.of(ModItems.RAW_TUNGSTEN),
                RecipeCategory.MISC,
                ModItems.TUNGSTEN_INGOT,
                1,
                200)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.RAW_TUNGSTEN.get()),
                AnvilCraftDatagen.has(ModItems.RAW_TUNGSTEN))
            .save(provider, AnvilCraft.of("tungsten_ingot_from_raw_tungsten"));

        SimpleCookingRecipeBuilder
            .blasting(
                Ingredient.of(ModItems.RAW_TUNGSTEN),
                RecipeCategory.MISC,
                ModItems.TUNGSTEN_INGOT,
                1,
                100)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.RAW_TUNGSTEN.get()),
                AnvilCraftDatagen.has(ModItems.RAW_TUNGSTEN))
            .save(provider, AnvilCraft.of("tungsten_ingot_from_raw_tungsten_blasting"));
        SimpleCookingRecipeBuilder
            .smelting(
                Ingredient.of(ModBlocks.DEEPSLATE_TUNGSTEN_ORE),
                RecipeCategory.MISC,
                ModItems.TUNGSTEN_INGOT,
                1,
                200)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.DEEPSLATE_TUNGSTEN_ORE),
                AnvilCraftDatagen.has(ModBlocks.DEEPSLATE_TUNGSTEN_ORE))
            .save(provider, AnvilCraft.of("tungsten_ingot_from_deepslate_tungsten_ore"));
        SimpleCookingRecipeBuilder
            .blasting(
                Ingredient.of(ModBlocks.DEEPSLATE_TUNGSTEN_ORE),
                RecipeCategory.MISC,
                ModItems.TUNGSTEN_INGOT,
                1,
                100)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.DEEPSLATE_TUNGSTEN_ORE),
                AnvilCraftDatagen.has(ModBlocks.DEEPSLATE_TUNGSTEN_ORE))
            .save(provider, AnvilCraft.of("tungsten_ingot_from_deepslate_tungsten_ore_blasting"));
        SimpleCookingRecipeBuilder
            .smelting(
                Ingredient.of(ModItems.RAW_LEAD),
                RecipeCategory.MISC,
                ModItems.LEAD_INGOT,
                1,
                200)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.RAW_LEAD.get()),
                AnvilCraftDatagen.has(ModItems.RAW_LEAD))
            .save(provider, AnvilCraft.of("lead_ingot_from_raw_lead"));

        SimpleCookingRecipeBuilder
            .blasting(
                Ingredient.of(ModItems.RAW_LEAD),
                RecipeCategory.MISC,
                ModItems.LEAD_INGOT,
                1,
                100)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.RAW_LEAD.get()),
                AnvilCraftDatagen.has(ModItems.RAW_LEAD))
            .save(provider, AnvilCraft.of("lead_ingot_from_raw_lead_blasting"));
        SimpleCookingRecipeBuilder
            .smelting(
                Ingredient.of(ModBlocks.DEEPSLATE_LEAD_ORE),
                RecipeCategory.MISC,
                ModItems.LEAD_INGOT,
                1,
                200)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.DEEPSLATE_LEAD_ORE),
                AnvilCraftDatagen.has(ModBlocks.DEEPSLATE_LEAD_ORE))
            .save(provider, AnvilCraft.of("lead_ingot_from_deepslate_lead_ore"));
        SimpleCookingRecipeBuilder
            .blasting(
                Ingredient.of(ModBlocks.DEEPSLATE_LEAD_ORE),
                RecipeCategory.MISC,
                ModItems.LEAD_INGOT,
                1,
                100)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.DEEPSLATE_LEAD_ORE),
                AnvilCraftDatagen.has(ModBlocks.DEEPSLATE_LEAD_ORE))
            .save(provider, AnvilCraft.of("lead_ingot_from_deepslate_lead_ore_blasting"));
        SimpleCookingRecipeBuilder
            .smelting(
                Ingredient.of(ModItems.RAW_SILVER),
                RecipeCategory.MISC,
                ModItems.SILVER_INGOT,
                1,
                200)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.RAW_SILVER.get()),
                AnvilCraftDatagen.has(ModItems.RAW_SILVER))
            .save(provider, AnvilCraft.of("silver_ingot_from_raw_silver"));

        SimpleCookingRecipeBuilder
            .blasting(
                Ingredient.of(ModItems.RAW_SILVER),
                RecipeCategory.MISC,
                ModItems.SILVER_INGOT,
                1,
                100)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.RAW_SILVER.get()),
                AnvilCraftDatagen.has(ModItems.RAW_SILVER))
            .save(provider, AnvilCraft.of("silver_ingot_from_raw_silver_blasting"));
        SimpleCookingRecipeBuilder
            .smelting(
                Ingredient.of(ModBlocks.DEEPSLATE_SILVER_ORE),
                RecipeCategory.MISC,
                ModItems.SILVER_INGOT,
                1,
                200)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.DEEPSLATE_SILVER_ORE),
                AnvilCraftDatagen.has(ModBlocks.DEEPSLATE_SILVER_ORE))
            .save(provider, AnvilCraft.of("silver_ingot_from_deepslate_silver_ore"));
        SimpleCookingRecipeBuilder
            .blasting(
                Ingredient.of(ModBlocks.DEEPSLATE_SILVER_ORE),
                RecipeCategory.MISC,
                ModItems.SILVER_INGOT,
                1,
                100)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.DEEPSLATE_SILVER_ORE),
                AnvilCraftDatagen.has(ModBlocks.DEEPSLATE_SILVER_ORE))
            .save(provider, AnvilCraft.of("silver_ingot_from_deepslate_silver_ore_blasting"));
        SimpleCookingRecipeBuilder
            .smelting(
                Ingredient.of(ModItems.RAW_URANIUM),
                RecipeCategory.MISC,
                ModItems.URANIUM_INGOT,
                1,
                200)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.RAW_URANIUM.get()),
                AnvilCraftDatagen.has(ModItems.RAW_URANIUM))
            .save(provider, AnvilCraft.of("uranium_ingot_from_raw_uranium"));

        SimpleCookingRecipeBuilder
            .blasting(
                Ingredient.of(ModItems.RAW_URANIUM),
                RecipeCategory.MISC,
                ModItems.URANIUM_INGOT,
                1,
                100)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.RAW_URANIUM.get()),
                AnvilCraftDatagen.has(ModItems.RAW_URANIUM))
            .save(provider, AnvilCraft.of("uranium_ingot_from_raw_uranium_blasting"));
        SimpleCookingRecipeBuilder
            .smelting(
                Ingredient.of(ModBlocks.DEEPSLATE_URANIUM_ORE),
                RecipeCategory.MISC,
                ModItems.URANIUM_INGOT,
                1,
                200)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.DEEPSLATE_URANIUM_ORE),
                AnvilCraftDatagen.has(ModBlocks.DEEPSLATE_URANIUM_ORE))
            .save(provider, AnvilCraft.of("uranium_ingot_from_deepslate_uranium_ore"));
        SimpleCookingRecipeBuilder
            .blasting(
                Ingredient.of(ModBlocks.DEEPSLATE_URANIUM_ORE),
                RecipeCategory.MISC,
                ModItems.URANIUM_INGOT,
                1,
                100)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.DEEPSLATE_URANIUM_ORE),
                AnvilCraftDatagen.has(ModBlocks.DEEPSLATE_URANIUM_ORE))
            .save(provider, AnvilCraft.of("uranium_ingot_from_deepslate_uranium_ore_blasting"));
    }
}
