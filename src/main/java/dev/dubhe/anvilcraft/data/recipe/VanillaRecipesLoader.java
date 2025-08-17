package dev.dubhe.anvilcraft.data.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItemTags;
import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

public class VanillaRecipesLoader {
    public static void init(RegistrateRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Items.ANVIL, 9)
            .pattern("AAA")
            .pattern(" B ")
            .pattern("BBB")
            .define('A', ModBlocks.HEAVY_IRON_BLOCK)
            .define('B', Items.IRON_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.HEAVY_IRON_BLOCK), AnvilCraftDatagen.has(ModBlocks.HEAVY_IRON_BLOCK))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_BLOCK), AnvilCraftDatagen.has(Items.IRON_BLOCK))
            .save(provider, AnvilCraft.of("anvil_9"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Items.CHIPPED_ANVIL)
            .pattern("AAB")
            .pattern(" B ")
            .pattern("BBB")
            .define('A', Items.IRON_BLOCK)
            .define('B', Items.IRON_INGOT)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_BLOCK), AnvilCraftDatagen.has(Items.IRON_BLOCK))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_INGOT), AnvilCraftDatagen.has(Items.IRON_INGOT))
            .save(provider, AnvilCraft.of("chipped_anvil"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Items.CHIPPED_ANVIL, 9)
            .pattern("AAB")
            .pattern(" B ")
            .pattern("BBB")
            .define('A', ModBlocks.HEAVY_IRON_BLOCK)
            .define('B', Items.IRON_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.HEAVY_IRON_BLOCK), AnvilCraftDatagen.has(ModBlocks.HEAVY_IRON_BLOCK))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_BLOCK), AnvilCraftDatagen.has(Items.IRON_BLOCK))
            .save(provider, AnvilCraft.of("chipped_anvil_9"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Items.DAMAGED_ANVIL)
            .pattern("BAB")
            .pattern(" B ")
            .pattern("BBB")
            .define('A', Items.IRON_BLOCK)
            .define('B', Items.IRON_INGOT)
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_BLOCK), AnvilCraftDatagen.has(Items.IRON_BLOCK))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_INGOT), AnvilCraftDatagen.has(Items.IRON_INGOT))
            .save(provider, AnvilCraft.of("damaged_anvil"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Items.DAMAGED_ANVIL, 9)
            .pattern("BAB")
            .pattern(" B ")
            .pattern("BBB")
            .define('A', ModBlocks.HEAVY_IRON_BLOCK)
            .define('B', Items.IRON_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.HEAVY_IRON_BLOCK), AnvilCraftDatagen.has(ModBlocks.HEAVY_IRON_BLOCK))
            .unlockedBy(AnvilCraftDatagen.hasItem(Items.IRON_BLOCK), AnvilCraftDatagen.has(Items.IRON_BLOCK))
            .save(provider, AnvilCraft.of("damaged_anvil_9"));
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, Items.TRIDENT)
            .pattern(" AA")
            .pattern(" BA")
            .pattern("B  ")
            .define('A', ModItems.PRISMARINE_BLADE)
            .define('B', Items.PRISMARINE_BRICKS)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModItems.PRISMARINE_BLADE))
            .save(provider, AnvilCraft.of("trident"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Items.HEART_OF_THE_SEA)
            .pattern("A")
            .pattern("B")
            .pattern("A")
            .define('A', ModItems.SEA_HEART_SHELL)
            .define('B', ModItems.SAPPHIRE)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModItems.SEA_HEART_SHELL))
            .save(provider, AnvilCraft.of("heart_of_the_sea"));
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, Items.WET_SPONGE)
            .pattern("AA")
            .pattern("AA")
            .define('A', ModItems.SPONGE_GEMMULE)
            .unlockedBy("hasitem", AnvilCraftDatagen.has(ModItems.SPONGE_GEMMULE))
            .save(provider, AnvilCraft.of("wet_sponge"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, Items.IRON_BLOCK, 9)
            .requires(ModBlocks.HEAVY_IRON_BLOCK)
            .unlockedBy("hasitem", RegistrateRecipeProvider.has(ModBlocks.HEAVY_IRON_BLOCK))
            .save(provider, AnvilCraft.of("iron_block_from_heavy_iron_block"));

        SimpleCookingRecipeBuilder.campfireCooking(Ingredient.of(ModItemTags.DOUGH), RecipeCategory.FOOD,
                Items.BREAD, 0.35f, 600)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.DOUGH.get()), AnvilCraftDatagen.has(ModItems.DOUGH))
            .save(provider, AnvilCraft.of("campfire_cooking_bread"));
        SimpleCookingRecipeBuilder.smoking(Ingredient.of(ModItemTags.DOUGH), RecipeCategory.FOOD,
                Items.BREAD, 0.35f, 100)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.DOUGH.get()), AnvilCraftDatagen.has(ModItems.DOUGH))
            .save(provider, AnvilCraft.of("smoking_bread"));
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(ModItemTags.DOUGH), RecipeCategory.FOOD,
                Items.BREAD, 0.35f, 200)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.DOUGH.get()), AnvilCraftDatagen.has(ModItems.DOUGH))
            .save(provider, AnvilCraft.of("smelting_cooking_bread"));
    }
}
