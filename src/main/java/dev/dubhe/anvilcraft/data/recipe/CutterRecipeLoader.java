package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.dubhe.anvilcraft.data.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.SingleItemRecipeBuilder;
import net.minecraft.world.item.crafting.Ingredient;

public class CutterRecipeLoader {
    public CutterRecipeLoader(RegistrumRecipeProvider provider) {
        this.bronzeSeriesBlockRecipe(provider);
        this.brassSeriesBlockRecipe(provider);
        this.plywoodSeriesBlockRecipe(provider);
    }

    public void bronzeSeriesBlockRecipe(RegistrumRecipeProvider provider) {
        // 青铜块 -> 切制青铜块x4
        SingleItemRecipeBuilder
            .stonecutting(
                Ingredient.of(ModBlocks.BRONZE_BLOCK),
                RecipeCategory.BUILDING_BLOCKS,
                ModBlocks.CUT_BRONZE_BLOCK,
                4
            )
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.BRONZE_BLOCK), AnvilCraftDatagen.has(ModBlocks.BRONZE_BLOCK))
            .save(provider);

        // 青铜块 -> 切制青铜楼梯x4
        SingleItemRecipeBuilder
            .stonecutting(
                Ingredient.of(ModBlocks.BRONZE_BLOCK),
                RecipeCategory.BUILDING_BLOCKS,
                ModBlocks.CUT_BRONZE_STAIRS,
                4
            )
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.BRONZE_BLOCK), AnvilCraftDatagen.has(ModBlocks.BRONZE_BLOCK))
            .save(provider, "cutting_bronze_stairs_from_bronze_block");

        // 青铜块 -> 切制青铜台阶x8
        SingleItemRecipeBuilder
            .stonecutting(
                Ingredient.of(ModBlocks.BRONZE_BLOCK),
                RecipeCategory.BUILDING_BLOCKS,
                ModBlocks.CUT_BRONZE_SLAB,
                8
            )
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.BRONZE_BLOCK), AnvilCraftDatagen.has(ModBlocks.BRONZE_BLOCK))
            .save(provider, "cutting_bronze_slab_from_bronze_block");

        // 青铜块 -> 切制青铜柱x4
        SingleItemRecipeBuilder
            .stonecutting(
                Ingredient.of(ModBlocks.BRONZE_BLOCK),
                RecipeCategory.BUILDING_BLOCKS,
                ModBlocks.CUT_BRONZE_PILLAR,
                4
            )
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.BRONZE_BLOCK), AnvilCraftDatagen.has(ModBlocks.BRONZE_BLOCK))
            .save(provider, "cutting_brass_pillar_from_bronze_block");

        // 切制青铜块 -> 切制青铜楼梯x1
        SingleItemRecipeBuilder
            .stonecutting(
                Ingredient.of(ModBlocks.CUT_BRONZE_BLOCK),
                RecipeCategory.BUILDING_BLOCKS,
                ModBlocks.CUT_BRONZE_STAIRS,
                1
            )
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.CUT_BRONZE_BLOCK), AnvilCraftDatagen.has(ModBlocks.CUT_BRONZE_BLOCK))
            .save(provider, "cutting_bronze_stairs_from_cut_bronze_block");

        // 切制青铜块 -> 切制青铜台阶x2
        SingleItemRecipeBuilder
            .stonecutting(
                Ingredient.of(ModBlocks.CUT_BRONZE_BLOCK),
                RecipeCategory.BUILDING_BLOCKS,
                ModBlocks.CUT_BRONZE_SLAB,
                2
            )
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.CUT_BRONZE_BLOCK), AnvilCraftDatagen.has(ModBlocks.CUT_BRONZE_BLOCK))
            .save(provider, "cutting_bronze_slab_from_cut_bronze_block");

        // 切制青铜块 -> 切制青铜柱
        SingleItemRecipeBuilder
            .stonecutting(
                Ingredient.of(ModBlocks.CUT_BRONZE_BLOCK),
                RecipeCategory.BUILDING_BLOCKS,
                ModBlocks.CUT_BRONZE_PILLAR
            )
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.CUT_BRONZE_BLOCK), AnvilCraftDatagen.has(ModBlocks.CUT_BRONZE_BLOCK))
            .save(provider, "cutting_bronze_pillar_from_cut_bronze_block");
    }

    public void brassSeriesBlockRecipe(RegistrumRecipeProvider provider) {
        // 黄铜块 -> 切制黄铜块x4
        SingleItemRecipeBuilder
            .stonecutting(
                Ingredient.of(ModBlocks.BRASS_BLOCK),
                RecipeCategory.BUILDING_BLOCKS,
                ModBlocks.CUT_BRASS_BLOCK,
                4
            )
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.BRASS_BLOCK), AnvilCraftDatagen.has(ModBlocks.BRASS_BLOCK))
            .save(provider);

        // 黄铜块 -> 切制黄铜楼梯x4
        SingleItemRecipeBuilder
            .stonecutting(
                Ingredient.of(ModBlocks.BRASS_BLOCK),
                RecipeCategory.BUILDING_BLOCKS,
                ModBlocks.CUT_BRASS_STAIRS,
                4
            )
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.BRASS_BLOCK), AnvilCraftDatagen.has(ModBlocks.BRASS_BLOCK))
            .save(provider, "cutting_brass_stairs_from_brass_block");

        // 黄铜块 -> 切制黄铜台阶x8
        SingleItemRecipeBuilder
            .stonecutting(
                Ingredient.of(ModBlocks.BRASS_BLOCK),
                RecipeCategory.BUILDING_BLOCKS,
                ModBlocks.CUT_BRASS_SLAB,
                8
            )
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.BRASS_BLOCK), AnvilCraftDatagen.has(ModBlocks.BRASS_BLOCK))
            .save(provider, "cutting_brass_slab_from_brass_block");

        // 黄铜块 -> 切制黄铜柱x4
        SingleItemRecipeBuilder
            .stonecutting(
                Ingredient.of(ModBlocks.BRASS_BLOCK),
                RecipeCategory.BUILDING_BLOCKS,
                ModBlocks.CUT_BRASS_PILLAR,
                4
            )
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.BRASS_BLOCK), AnvilCraftDatagen.has(ModBlocks.BRASS_BLOCK))
            .save(provider, "cutting_brass_pillar_from_brass_block");

        // 黄制青铜块 -> 切制黄铜楼梯x1
        SingleItemRecipeBuilder
            .stonecutting(
                Ingredient.of(ModBlocks.CUT_BRASS_BLOCK),
                RecipeCategory.BUILDING_BLOCKS,
                ModBlocks.CUT_BRASS_STAIRS,
                1
            )
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.CUT_BRASS_BLOCK), AnvilCraftDatagen.has(ModBlocks.BRASS_BLOCK))
            .save(provider, "cutting_brass_stairs_from_cut_brass_block");

        // 切制黄铜块 -> 切制黄铜台阶x2
        SingleItemRecipeBuilder
            .stonecutting(
                Ingredient.of(ModBlocks.CUT_BRASS_BLOCK),
                RecipeCategory.BUILDING_BLOCKS,
                ModBlocks.CUT_BRASS_SLAB,
                2
            )
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.CUT_BRASS_BLOCK), AnvilCraftDatagen.has(ModBlocks.BRASS_BLOCK))
            .save(provider, "cutting_brass_slab_from_cut_brass_block");

        // 切制黄铜块 -> 切制黄铜柱
        SingleItemRecipeBuilder
            .stonecutting(
                Ingredient.of(ModBlocks.CUT_BRASS_BLOCK),
                RecipeCategory.BUILDING_BLOCKS,
                ModBlocks.CUT_BRASS_PILLAR
            )
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.CUT_BRASS_BLOCK), AnvilCraftDatagen.has(ModBlocks.BRASS_BLOCK))
            .save(provider, "cutting_brass_pillar_from_cut_brass_block");
    }

    public void plywoodSeriesBlockRecipe(RegistrumRecipeProvider provider) {
        SingleItemRecipeBuilder
            .stonecutting(
                Ingredient.of(ModBlocks.PLYWOOD_BLOCK),
                RecipeCategory.BUILDING_BLOCKS,
                ModBlocks.PLYWOOD_STAIRS
            )
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.PLYWOOD_BLOCK), AnvilCraftDatagen.has(ModBlocks.PLYWOOD_BLOCK))
            .save(provider, "plywood_stairs_from_plywood_block_ccuttting");

        SingleItemRecipeBuilder
            .stonecutting(
                Ingredient.of(ModBlocks.PLYWOOD_BLOCK),
                RecipeCategory.BUILDING_BLOCKS,
                ModBlocks.PLYWOOD_SLAB,
                2
            )
            .unlockedBy(AnvilCraftDatagen.hasItem(ModBlocks.PLYWOOD_BLOCK), AnvilCraftDatagen.has(ModBlocks.PLYWOOD_BLOCK))
            .save(provider, "plywood_slab_from_plywood_block_ccuttting");
    }
}
