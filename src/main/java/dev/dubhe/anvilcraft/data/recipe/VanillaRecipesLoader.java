package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.core.HolderGetter;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;

public class VanillaRecipesLoader {
    public static void init(RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapedRecipeBuilder.shaped(lookup, RecipeCategory.MISC, Items.ANVIL, 9)
            .pattern("AAA")
            .pattern(" B ")
            .pattern("BBB")
            .define('A', ModBlocks.HEAVY_IRON_BLOCK)
            .define('B', Items.IRON_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.HEAVY_IRON_BLOCK), AnvilCraftDatagen.has(lookup, ModBlocks.HEAVY_IRON_BLOCK))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_BLOCK), AnvilCraftDatagen.has(lookup, Items.IRON_BLOCK))
            .save(provider, AnvilCraft.recipe("anvil_9"));
        ShapedRecipeBuilder.shaped(lookup, RecipeCategory.MISC, Items.CHIPPED_ANVIL)
            .pattern("AAB")
            .pattern(" B ")
            .pattern("BBB")
            .define('A', Items.IRON_BLOCK)
            .define('B', Items.IRON_INGOT)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_BLOCK), AnvilCraftDatagen.has(lookup, Items.IRON_BLOCK))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_INGOT), AnvilCraftDatagen.has(lookup, Items.IRON_INGOT))
            .save(provider, AnvilCraft.recipe("chipped_anvil"));
        ShapedRecipeBuilder.shaped(lookup, RecipeCategory.MISC, Items.CHIPPED_ANVIL, 9)
            .pattern("AAB")
            .pattern(" B ")
            .pattern("BBB")
            .define('A', ModBlocks.HEAVY_IRON_BLOCK)
            .define('B', Items.IRON_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.HEAVY_IRON_BLOCK), AnvilCraftDatagen.has(lookup, ModBlocks.HEAVY_IRON_BLOCK))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_BLOCK), AnvilCraftDatagen.has(lookup, Items.IRON_BLOCK))
            .save(provider, AnvilCraft.recipe("chipped_anvil_9"));
        ShapedRecipeBuilder.shaped(lookup, RecipeCategory.MISC, Items.DAMAGED_ANVIL)
            .pattern("BAB")
            .pattern(" B ")
            .pattern("BBB")
            .define('A', Items.IRON_BLOCK)
            .define('B', Items.IRON_INGOT)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_BLOCK), AnvilCraftDatagen.has(lookup, Items.IRON_BLOCK))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_INGOT), AnvilCraftDatagen.has(lookup, Items.IRON_INGOT))
            .save(provider, AnvilCraft.recipe("damaged_anvil"));
        ShapedRecipeBuilder.shaped(lookup, RecipeCategory.MISC, Items.DAMAGED_ANVIL, 9)
            .pattern("BAB")
            .pattern(" B ")
            .pattern("BBB")
            .define('A', ModBlocks.HEAVY_IRON_BLOCK)
            .define('B', Items.IRON_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.HEAVY_IRON_BLOCK), AnvilCraftDatagen.has(lookup, ModBlocks.HEAVY_IRON_BLOCK))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_BLOCK), AnvilCraftDatagen.has(lookup, Items.IRON_BLOCK))
            .save(provider, AnvilCraft.recipe("damaged_anvil_9"));
        ShapedRecipeBuilder.shaped(lookup, RecipeCategory.COMBAT, Items.TRIDENT)
            .pattern(" AA")
            .pattern(" BA")
            .pattern("A  ")
            .define('A', Items.PRISMARINE_SHARD)
            .define('B', ModItems.SAPPHIRE)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(lookup, ModItems.SAPPHIRE))
            .save(provider, AnvilCraft.recipe("trident"));
        ShapedRecipeBuilder.shaped(lookup, RecipeCategory.MISC, Items.HEART_OF_THE_SEA)
            .pattern("A")
            .pattern("B")
            .pattern("A")
            .define('A', ModItems.SEA_HEART_SHELL)
            .define('B', ModItems.SAPPHIRE)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.SEA_HEART_SHELL), AnvilCraftDatagen.has(lookup, ModItems.SEA_HEART_SHELL))
            .save(provider, AnvilCraft.recipe("heart_of_the_sea"));
        ShapedRecipeBuilder.shaped(lookup, RecipeCategory.BUILDING_BLOCKS, Items.WET_SPONGE)
            .pattern("AA")
            .pattern("AA")
            .define('A', ModItems.SPONGE_GEMMULE)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.SPONGE_GEMMULE), AnvilCraftDatagen.has(lookup, ModItems.SPONGE_GEMMULE))
            .save(provider, AnvilCraft.recipe("wet_sponge"));
        ShapedRecipeBuilder.shaped(lookup, RecipeCategory.BUILDING_BLOCKS, Items.REPEATER)
            .pattern("   ")
            .pattern("TRT")
            .pattern("BBB")
            .define('R', Items.REDSTONE)
            .define('T', Items.REDSTONE_TORCH)
            .define('B', ModItems.HARDEND_RESIN)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.HARDEND_RESIN), AnvilCraftDatagen.has(lookup, ModItems.HARDEND_RESIN))
            .save(provider, AnvilCraft.recipe("repeater"));
        ShapedRecipeBuilder.shaped(lookup, RecipeCategory.BUILDING_BLOCKS, Items.COMPARATOR)
            .pattern(" T ")
            .pattern("TQT")
            .pattern("BBB")
            .define('Q', Items.QUARTZ)
            .define('T', Items.REDSTONE_TORCH)
            .define('B', ModItems.HARDEND_RESIN)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.HARDEND_RESIN), AnvilCraftDatagen.has(lookup, ModItems.HARDEND_RESIN))
            .save(provider, AnvilCraft.recipe("comparator"));

        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.BUILDING_BLOCKS, Items.IRON_BLOCK, 9)
            .requires(ModBlocks.HEAVY_IRON_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.HEAVY_IRON_BLOCK), AnvilCraftDatagen.has(lookup, ModBlocks.HEAVY_IRON_BLOCK))
            .save(provider, AnvilCraft.recipe("iron_block_from_heavy_iron_block"));

        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ModBlocks.DRAIN)
            .requires(Items.COPPER_GRATE)
            .requires(ModBlocks.BRASS_PRESSURE_PLATE)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.COPPER_GRATE), AnvilCraftDatagen.has(lookup, Items.COPPER_GRATE))
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.BRASS_PRESSURE_PLATE),
                AnvilCraftDatagen.has(lookup, ModBlocks.BRASS_PRESSURE_PLATE)
            )
            .save(provider);

        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ModBlocks.FLUID_TANK)
            .requires(ModBlocks.FLUID_TANK)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.FLUID_TANK),
                AnvilCraftDatagen.has(lookup, ModBlocks.FLUID_TANK)
            )
            .save(provider, AnvilCraft.recipe("empty_fluid_tank"));
        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, ModBlocks.LARGE_FLUID_TANK)
            .requires(ModBlocks.LARGE_FLUID_TANK)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.LARGE_FLUID_TANK),
                AnvilCraftDatagen.has(lookup, ModBlocks.LARGE_FLUID_TANK)
            )
            .save(provider, AnvilCraft.recipe("empty_large_fluid_tank"));

        SimpleCookingRecipeBuilder.campfireCooking(
            Ingredient.of(lookup.getOrThrow(ModItemTags.DOUGH)),
            RecipeCategory.FOOD,
            Items.BREAD,
            0.35F,
            600
        ).unlockedBy(
            AnvilCraftDatagen.hasItem(ModItems.DOUGH.get()),
            AnvilCraftDatagen.has(lookup, ModItems.DOUGH)
        ).save(provider, AnvilCraft.recipe("campfire_cooking_bread"));
        SimpleCookingRecipeBuilder.smoking(
            Ingredient.of(lookup.getOrThrow(ModItemTags.DOUGH)),
            RecipeCategory.FOOD,
            Items.BREAD,
            0.35F,
            100
        ).unlockedBy(
            AnvilCraftDatagen.hasItem(ModItems.DOUGH.get()),
            AnvilCraftDatagen.has(lookup, ModItems.DOUGH)
        ).save(provider, AnvilCraft.recipe("smoking_bread"));
        SimpleCookingRecipeBuilder.smelting(
            Ingredient.of(lookup.getOrThrow(ModItemTags.DOUGH)),
            RecipeCategory.FOOD,
            CookingBookCategory.FOOD,
            Items.BREAD,
            0.35F,
            200
        ).unlockedBy(
            AnvilCraftDatagen.hasItem(ModItems.DOUGH.get()),
            AnvilCraftDatagen.has(lookup, ModItems.DOUGH)
        ).save(provider, AnvilCraft.recipe("smelting_cooking_bread"));
        ShapedRecipeBuilder.shaped(lookup, RecipeCategory.MISC, Items.ENCHANTED_GOLDEN_APPLE)
            .pattern("AAA")
            .pattern("ABA")
            .pattern("AAA")
            .define('A', ModBlocks.ENCHANTED_GOLD_BLOCK)
            .define('B', Items.APPLE)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.ENCHANTED_GOLD_BLOCK),
                AnvilCraftDatagen.has(lookup, ModBlocks.ENCHANTED_GOLD_BLOCK)
            )
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.APPLE), AnvilCraftDatagen.has(lookup, Items.APPLE))
            .save(provider, AnvilCraft.recipe("enchanted_golden_apple"));

        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, Items.GOLD_NUGGET, 2)
            .requires(ModItems.ENCHANTED_GOLD_NUGGET)
            .requires(ModItems.CURSED_GOLD_NUGGET)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.ENCHANTED_GOLD_NUGGET),
                AnvilCraftDatagen.has(lookup, ModItems.ENCHANTED_GOLD_NUGGET)
            )
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.CURSED_GOLD_NUGGET), AnvilCraftDatagen.has(lookup, ModItems.CURSED_GOLD_NUGGET))
            .save(provider, AnvilCraft.recipe("gold_nugget_from_enchanted_and_cursed"));

        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.MISC, Items.GOLD_INGOT, 2)
            .requires(ModItems.ENCHANTED_GOLD_INGOT)
            .requires(ModItems.CURSED_GOLD_INGOT)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.ENCHANTED_GOLD_INGOT),
                AnvilCraftDatagen.has(lookup, ModItems.ENCHANTED_GOLD_INGOT)
            )
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.CURSED_GOLD_INGOT), AnvilCraftDatagen.has(lookup, ModItems.CURSED_GOLD_INGOT))
            .save(provider, AnvilCraft.recipe("gold_ingot_from_enchanted_and_cursed"));

        ShapelessRecipeBuilder.shapeless(lookup, RecipeCategory.BUILDING_BLOCKS, Items.GOLD_BLOCK, 2)
            .requires(ModBlocks.ENCHANTED_GOLD_BLOCK)
            .requires(ModBlocks.CURSED_GOLD_BLOCK)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.ENCHANTED_GOLD_BLOCK),
                AnvilCraftDatagen.has(lookup, ModBlocks.ENCHANTED_GOLD_BLOCK)
            )
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.CURSED_GOLD_BLOCK), AnvilCraftDatagen.has(lookup, ModBlocks.CURSED_GOLD_BLOCK))
            .save(provider, AnvilCraft.recipe("gold_block_from_enchanted_and_cursed"));
    }
}
