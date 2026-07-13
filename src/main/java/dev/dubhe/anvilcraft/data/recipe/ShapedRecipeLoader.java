package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumRecipeProvider;
import dev.dubhe.anvilcraft.data.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.core.HolderGetter;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

public class ShapedRecipeLoader {
    public ShapedRecipeLoader(RegistrumRecipeProvider provider) {
        this.nineToOne(provider);
        this.controlValve(provider);
    }

    private void controlValve(RegistrumRecipeProvider provider) {
        HolderGetter<Item> lookup = provider.getItems();
        ShapedRecipeBuilder.shaped(lookup, RecipeCategory.MISC, ModBlocks.CONTROL_VALVE)
            .pattern(" C ")
            .pattern("PHP")
            .define('C', ModItems.CIRCUIT_BOARD)
            .define('P', ModItems.PIPE)
            .define('H', ModBlocks.CHUTE)
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModItems.CIRCUIT_BOARD),
                AnvilCraftDatagen.has(lookup, ModItems.CIRCUIT_BOARD)
            )
            .unlockedBy(AnvilCraftDatagen.hasItem(ModItems.PIPE), AnvilCraftDatagen.has(lookup, ModItems.PIPE))
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.CHUTE), AnvilCraftDatagen.has(lookup, ModBlocks.CHUTE))
            .save(provider);
    }

    public void nineToOne(RegistrumRecipeProvider provider) {
        HolderGetter<Item> holder = provider.getItems();
        this.nine21(provider, RecipeCategory.BUILDING_BLOCKS, ModItemTags.BRONZE_INGOTS, ModBlocks.BRONZE_BLOCK);
        this.nine21(provider, RecipeCategory.BUILDING_BLOCKS, ModItemTags.BRASS_INGOTS, ModBlocks.BRASS_BLOCK);
        ShapedRecipeBuilder.shaped(holder, RecipeCategory.BUILDING_BLOCKS, ModBlocks.PLYWOOD_STAIRS, 4)
            .pattern("A  ")
            .pattern("AA ")
            .pattern("AAA")
            .define('A', ModBlocks.PLYWOOD_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.PLYWOOD_BLOCK), AnvilCraftDatagen.has(holder, ModBlocks.PLYWOOD_BLOCK))
            .save(provider);
        ShapedRecipeBuilder.shaped(holder, RecipeCategory.BUILDING_BLOCKS, ModBlocks.PLYWOOD_SLAB, 6)
            .pattern("AAA")
            .define('A', ModBlocks.PLYWOOD_BLOCK)
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.PLYWOOD_BLOCK), AnvilCraftDatagen.has(holder, ModBlocks.PLYWOOD_BLOCK))
            .save(provider);
    }

    private void nine21(
        RegistrumRecipeProvider provider,
        RecipeCategory recipeCategory,
        ItemLike ingredient,
        ItemLike result
    ) {
        HolderGetter<Item> holder = provider.getItems();
        ShapedRecipeBuilder.shaped(holder, recipeCategory, result)
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ingredient)
            .unlockedBy(AnvilCraftDatagen.hasItem(ingredient), AnvilCraftDatagen.has(holder, result))
            .save(provider);
    }

    private void nine21(
        RegistrumRecipeProvider provider,
        RecipeCategory recipeCategory,
        TagKey<Item> ingredient,
        ItemLike result
    ) {
        HolderGetter<Item> holder = provider.getItems();
        ShapedRecipeBuilder.shaped(holder, recipeCategory, result)
            .pattern("AAA")
            .pattern("AAA")
            .pattern("AAA")
            .define('A', ingredient)
            .unlockedBy(AnvilCraftDatagen.hasItem(ingredient), AnvilCraftDatagen.has(holder, result))
            .save(provider);
    }
}
