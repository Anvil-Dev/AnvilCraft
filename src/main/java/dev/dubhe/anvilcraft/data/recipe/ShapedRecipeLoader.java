package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.dubhe.anvilcraft.data.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

public class ShapedRecipeLoader {
    public ShapedRecipeLoader(RegistrumRecipeProvider provider) {
        this.nineToOne(provider);
    }

    public void nineToOne(RegistrumRecipeProvider provider) {
        this.nine21(provider, RecipeCategory.BUILDING_BLOCKS, ModItemTags.BRONZE_INGOTS, ModBlocks.BRONZE_BLOCK);
        this.nine21(provider, RecipeCategory.BUILDING_BLOCKS, ModItemTags.BRASS_INGOTS, ModBlocks.BRASS_BLOCK);
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.PLYWOOD_STAIRS, 4)
            .pattern("A  ")
            .pattern("AA ")
            .pattern("AAA")
            .define('A', ModBlocks.PLYWOOD_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.PLYWOOD_BLOCK), AnvilCraftDatagen.has(ModBlocks.PLYWOOD_BLOCK))
            .save(provider);
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.PLYWOOD_SLAB, 6)
            .pattern("AAA")
            .define('A', ModBlocks.PLYWOOD_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.PLYWOOD_BLOCK), AnvilCraftDatagen.has(ModBlocks.PLYWOOD_BLOCK))
            .save(provider);
    }

    private void nine21(
        RegistrumRecipeProvider provider,
        RecipeCategory recipeCategory,
        ItemLike ingredient,
        ItemLike result
    ) {
        ShapedRecipeBuilder.shaped(recipeCategory, result)
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ingredient)
            .unlockedBy(AnvilCraftDatagen.hasItem(ingredient), AnvilCraftDatagen.has(result))
            .save(provider);
    }

    private void nine21(
        RegistrumRecipeProvider provider,
        RecipeCategory recipeCategory,
        TagKey<Item> ingredient,
        ItemLike result
    ) {
        ShapedRecipeBuilder.shaped(recipeCategory, result)
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ingredient)
            .unlockedBy(AnvilCraftDatagen.hasItem(ingredient), AnvilCraftDatagen.has(result))
            .save(provider);
    }
}
