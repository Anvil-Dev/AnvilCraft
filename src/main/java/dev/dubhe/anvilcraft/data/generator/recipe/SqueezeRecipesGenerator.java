package dev.dubhe.anvilcraft.data.generator.recipe;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.generator.MyRecipesGenerator;
import dev.dubhe.anvilcraft.data.recipe.Component;
import dev.dubhe.anvilcraft.data.recipe.anvil.block.BlockAnvilRecipeBuilder;
import dev.dubhe.anvilcraft.init.ModBlocks;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;

import java.util.function.Consumer;

public abstract class SqueezeRecipesGenerator {
    private SqueezeRecipesGenerator() {
    }

    public static void buildRecipes(Consumer<FinishedRecipe> exporter) {
        BlockAnvilRecipeBuilder.block(RecipeCategory.MISC, Blocks.NETHERRACK.defaultBlockState(), ModBlocks.LAVA_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 1))
                .component(Blocks.MAGMA_BLOCK)
                .component(Blocks.CAULDRON)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.WET_SPONGE), FabricRecipeProvider.has(Items.WET_SPONGE))
                .save(exporter, AnvilCraft.of("squeeze/magma_block_lava_cauldron_1"));
        BlockAnvilRecipeBuilder.block(RecipeCategory.MISC, Blocks.NETHERRACK.defaultBlockState(), ModBlocks.LAVA_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 2))
                .component(Blocks.MAGMA_BLOCK)
                .component(Component.of(Component.Value.of(ModBlocks.LAVA_CAULDRON).with("level", 1)))
                .unlockedBy(MyRecipesGenerator.hasItem(Items.WET_SPONGE), FabricRecipeProvider.has(Items.WET_SPONGE))
                .save(exporter, AnvilCraft.of("squeeze/magma_block_lava_cauldron_2"));
        BlockAnvilRecipeBuilder.block(RecipeCategory.MISC, Blocks.NETHERRACK.defaultBlockState(), Blocks.LAVA_CAULDRON.defaultBlockState())
                .component(Blocks.MAGMA_BLOCK)
                .component(Component.of(Component.Value.of(ModBlocks.LAVA_CAULDRON).with("level", 2)))
                .unlockedBy(MyRecipesGenerator.hasItem(Items.WET_SPONGE), FabricRecipeProvider.has(Items.WET_SPONGE))
                .save(exporter, AnvilCraft.of("squeeze/magma_block_lava_cauldron_3"));

        BlockAnvilRecipeBuilder.block(RecipeCategory.MISC, Blocks.SPONGE.defaultBlockState(), Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 1))
                .component(Blocks.WET_SPONGE)
                .component(Blocks.CAULDRON)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.WET_SPONGE), FabricRecipeProvider.has(Items.WET_SPONGE))
                .save(exporter, AnvilCraft.of("squeeze/wet_sponge_2_water_cauldron_1"));
        BlockAnvilRecipeBuilder.block(RecipeCategory.MISC, Blocks.SPONGE.defaultBlockState(), Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 2))
                .component(Blocks.WET_SPONGE)
                .component(Component.of(Component.Value.of(Blocks.WATER_CAULDRON).with("level", 1)))
                .unlockedBy(MyRecipesGenerator.hasItem(Items.WET_SPONGE), FabricRecipeProvider.has(Items.WET_SPONGE))
                .save(exporter, AnvilCraft.of("squeeze/wet_sponge_2_water_cauldron_2"));
        BlockAnvilRecipeBuilder.block(RecipeCategory.MISC, Blocks.SPONGE.defaultBlockState(), Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3))
                .component(Blocks.WET_SPONGE)
                .component(Component.of(Component.Value.of(Blocks.WATER_CAULDRON).with("level", 2)))
                .unlockedBy(MyRecipesGenerator.hasItem(Items.WET_SPONGE), FabricRecipeProvider.has(Items.WET_SPONGE))
                .save(exporter, AnvilCraft.of("squeeze/wet_sponge_2_water_cauldron_3"));

        BlockAnvilRecipeBuilder.block(RecipeCategory.MISC, Blocks.MOSS_CARPET.defaultBlockState(), Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 1))
                .component(Blocks.MOSS_BLOCK)
                .component(Blocks.CAULDRON)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.MOSS_BLOCK), FabricRecipeProvider.has(Items.MOSS_BLOCK))
                .save(exporter, AnvilCraft.of("squeeze/moss_2_water_cauldron_1"));
        BlockAnvilRecipeBuilder.block(RecipeCategory.MISC, Blocks.MOSS_CARPET.defaultBlockState(), Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 2))
                .component(Blocks.MOSS_BLOCK)
                .component(Component.of(Component.Value.of(Blocks.WATER_CAULDRON).with("level", 1)))
                .unlockedBy(MyRecipesGenerator.hasItem(Items.MOSS_BLOCK), FabricRecipeProvider.has(Items.MOSS_BLOCK))
                .save(exporter, AnvilCraft.of("squeeze/moss_2_water_cauldron_2"));
        BlockAnvilRecipeBuilder.block(RecipeCategory.MISC, Blocks.MOSS_CARPET.defaultBlockState(), Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3))
                .component(Blocks.MOSS_BLOCK)
                .component(Component.of(Component.Value.of(Blocks.WATER_CAULDRON).with("level", 2)))
                .unlockedBy(MyRecipesGenerator.hasItem(Items.MOSS_BLOCK), FabricRecipeProvider.has(Items.MOSS_BLOCK))
                .save(exporter, AnvilCraft.of("squeeze/moss_2_water_cauldron_3"));

        BlockAnvilRecipeBuilder.block(RecipeCategory.MISC, Blocks.ICE.defaultBlockState(), Blocks.POWDER_SNOW_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 1))
                .component(Blocks.SNOW_BLOCK)
                .component(Blocks.CAULDRON)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.SNOW_BLOCK), FabricRecipeProvider.has(Items.SNOW_BLOCK))
                .save(exporter, AnvilCraft.of("squeeze/snow_2_powder_snow_cauldron_1"));
        BlockAnvilRecipeBuilder.block(RecipeCategory.MISC, Blocks.ICE.defaultBlockState(), Blocks.POWDER_SNOW_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 2))
                .component(Blocks.SNOW_BLOCK)
                .component(Component.of(Component.Value.of(Blocks.POWDER_SNOW_CAULDRON).with("level", 1)))
                .unlockedBy(MyRecipesGenerator.hasItem(Items.SNOW_BLOCK), FabricRecipeProvider.has(Items.SNOW_BLOCK))
                .save(exporter, AnvilCraft.of("squeeze/snow_2_powder_snow_cauldron_2"));
        BlockAnvilRecipeBuilder.block(RecipeCategory.MISC, Blocks.ICE.defaultBlockState(), Blocks.POWDER_SNOW_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3))
                .component(Blocks.SNOW_BLOCK)
                .component(Component.of(Component.Value.of(Blocks.POWDER_SNOW_CAULDRON).with("level", 2)))
                .unlockedBy(MyRecipesGenerator.hasItem(Items.SNOW_BLOCK), FabricRecipeProvider.has(Items.SNOW_BLOCK))
                .save(exporter, AnvilCraft.of("squeeze/snow_2_powder_snow_cauldron_3"));

        BlockAnvilRecipeBuilder.block(RecipeCategory.MISC, Blocks.MOSS_BLOCK.defaultBlockState(), Blocks.MOSSY_COBBLESTONE.defaultBlockState())
                .component(Blocks.MOSS_BLOCK)
                .component(Blocks.COBBLESTONE)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.MOSS_BLOCK), FabricRecipeProvider.has(Items.MOSS_BLOCK))
                .save(exporter, AnvilCraft.of("squeeze/mossy_cobblestone"));

        BlockAnvilRecipeBuilder.block(RecipeCategory.MISC, Blocks.MOSS_BLOCK.defaultBlockState(), Blocks.MOSSY_STONE_BRICKS.defaultBlockState())
                .component(Blocks.MOSS_BLOCK)
                .component(Blocks.STONE_BRICKS)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.MOSS_BLOCK), FabricRecipeProvider.has(Items.MOSS_BLOCK))
                .save(exporter, AnvilCraft.of("squeeze/mossy_stone_bricks"));

        BlockAnvilRecipeBuilder.block(RecipeCategory.MISC, Blocks.HONEYCOMB_BLOCK.defaultBlockState(), Blocks.WAXED_COPPER_BLOCK.defaultBlockState())
                .component(Blocks.HONEYCOMB_BLOCK)
                .component(Blocks.COPPER_BLOCK)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.HONEYCOMB_BLOCK), FabricRecipeProvider.has(Items.HONEYCOMB_BLOCK))
                .save(exporter, AnvilCraft.of("squeeze/waxed_copper_block"));

        BlockAnvilRecipeBuilder.block(RecipeCategory.MISC, Blocks.HONEYCOMB_BLOCK.defaultBlockState(), Blocks.WAXED_WEATHERED_COPPER.defaultBlockState())
                .component(Blocks.HONEYCOMB_BLOCK)
                .component(Blocks.WEATHERED_COPPER)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.HONEYCOMB_BLOCK), FabricRecipeProvider.has(Items.HONEYCOMB_BLOCK))
                .save(exporter, AnvilCraft.of("squeeze/waxed_weathered_copper"));

        BlockAnvilRecipeBuilder.block(RecipeCategory.MISC, Blocks.HONEYCOMB_BLOCK.defaultBlockState(), Blocks.WAXED_EXPOSED_COPPER.defaultBlockState())
                .component(Blocks.HONEYCOMB_BLOCK)
                .component(Blocks.EXPOSED_COPPER)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.HONEYCOMB_BLOCK), FabricRecipeProvider.has(Items.HONEYCOMB_BLOCK))
                .save(exporter, AnvilCraft.of("squeeze/waxed_exposed_copper"));

        BlockAnvilRecipeBuilder.block(RecipeCategory.MISC, Blocks.HONEYCOMB_BLOCK.defaultBlockState(), Blocks.WAXED_OXIDIZED_COPPER.defaultBlockState())
                .component(Blocks.HONEYCOMB_BLOCK)
                .component(Blocks.OXIDIZED_COPPER)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.HONEYCOMB_BLOCK), FabricRecipeProvider.has(Items.HONEYCOMB_BLOCK))
                .save(exporter, AnvilCraft.of("squeeze/waxed_oxidized_copper"));

        BlockAnvilRecipeBuilder.block(RecipeCategory.MISC, Blocks.HONEYCOMB_BLOCK.defaultBlockState(), Blocks.WAXED_OXIDIZED_CUT_COPPER.defaultBlockState())
                .component(Blocks.HONEYCOMB_BLOCK)
                .component(Blocks.OXIDIZED_CUT_COPPER)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.HONEYCOMB_BLOCK), FabricRecipeProvider.has(Items.HONEYCOMB_BLOCK))
                .save(exporter, AnvilCraft.of("squeeze/waxed_oxidized_cut_copper"));

        BlockAnvilRecipeBuilder.block(RecipeCategory.MISC, Blocks.HONEYCOMB_BLOCK.defaultBlockState(), Blocks.WAXED_WEATHERED_CUT_COPPER.defaultBlockState())
                .component(Blocks.HONEYCOMB_BLOCK)
                .component(Blocks.WEATHERED_CUT_COPPER)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.HONEYCOMB_BLOCK), FabricRecipeProvider.has(Items.HONEYCOMB_BLOCK))
                .save(exporter, AnvilCraft.of("squeeze/waxed_weathered_cut_copper"));

        BlockAnvilRecipeBuilder.block(RecipeCategory.MISC, Blocks.HONEYCOMB_BLOCK.defaultBlockState(), Blocks.WAXED_EXPOSED_CUT_COPPER.defaultBlockState())
                .component(Blocks.HONEYCOMB_BLOCK)
                .component(Blocks.EXPOSED_CUT_COPPER)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.HONEYCOMB_BLOCK), FabricRecipeProvider.has(Items.HONEYCOMB_BLOCK))
                .save(exporter, AnvilCraft.of("squeeze/waxed_exposed_cut_copper"));

        BlockAnvilRecipeBuilder.block(RecipeCategory.MISC, Blocks.HONEYCOMB_BLOCK.defaultBlockState(), Blocks.WAXED_CUT_COPPER.defaultBlockState())
                .component(Blocks.HONEYCOMB_BLOCK)
                .component(Blocks.CUT_COPPER)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.HONEYCOMB_BLOCK), FabricRecipeProvider.has(Items.HONEYCOMB_BLOCK))
                .save(exporter, AnvilCraft.of("squeeze/waxed_cut_copper"));

        BlockAnvilRecipeBuilder.block(RecipeCategory.MISC, Blocks.HONEYCOMB_BLOCK.defaultBlockState(), Blocks.WAXED_OXIDIZED_CUT_COPPER_STAIRS.defaultBlockState())
                .component(Blocks.HONEYCOMB_BLOCK)
                .component(Blocks.OXIDIZED_CUT_COPPER_STAIRS)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.HONEYCOMB_BLOCK), FabricRecipeProvider.has(Items.HONEYCOMB_BLOCK))
                .save(exporter, AnvilCraft.of("squeeze/waxed_oxidized_cut_copper_stairs"));

        BlockAnvilRecipeBuilder.block(RecipeCategory.MISC, Blocks.HONEYCOMB_BLOCK.defaultBlockState(), Blocks.WAXED_WEATHERED_CUT_COPPER_STAIRS.defaultBlockState())
                .component(Blocks.HONEYCOMB_BLOCK)
                .component(Blocks.WEATHERED_CUT_COPPER_STAIRS)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.HONEYCOMB_BLOCK), FabricRecipeProvider.has(Items.HONEYCOMB_BLOCK))
                .save(exporter, AnvilCraft.of("squeeze/waxed_weathered_cut_copper_stairs"));

        BlockAnvilRecipeBuilder.block(RecipeCategory.MISC, Blocks.HONEYCOMB_BLOCK.defaultBlockState(), Blocks.WAXED_EXPOSED_CUT_COPPER_STAIRS.defaultBlockState())
                .component(Blocks.HONEYCOMB_BLOCK)
                .component(Blocks.EXPOSED_CUT_COPPER_STAIRS)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.HONEYCOMB_BLOCK), FabricRecipeProvider.has(Items.HONEYCOMB_BLOCK))
                .save(exporter, AnvilCraft.of("squeeze/waxed_exposed_cut_copper_stairs"));

        BlockAnvilRecipeBuilder.block(RecipeCategory.MISC, Blocks.HONEYCOMB_BLOCK.defaultBlockState(), Blocks.WAXED_CUT_COPPER_STAIRS.defaultBlockState())
                .component(Blocks.HONEYCOMB_BLOCK)
                .component(Blocks.CUT_COPPER_STAIRS)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.HONEYCOMB_BLOCK), FabricRecipeProvider.has(Items.HONEYCOMB_BLOCK))
                .save(exporter, AnvilCraft.of("squeeze/waxed_cut_copper_stairs"));

        BlockAnvilRecipeBuilder.block(RecipeCategory.MISC, Blocks.HONEYCOMB_BLOCK.defaultBlockState(), Blocks.WAXED_OXIDIZED_CUT_COPPER_SLAB.defaultBlockState())
                .component(Blocks.HONEYCOMB_BLOCK)
                .component(Blocks.OXIDIZED_CUT_COPPER_SLAB)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.HONEYCOMB_BLOCK), FabricRecipeProvider.has(Items.HONEYCOMB_BLOCK))
                .save(exporter, AnvilCraft.of("squeeze/waxed_oxidized_cut_copper_slab"));

        BlockAnvilRecipeBuilder.block(RecipeCategory.MISC, Blocks.HONEYCOMB_BLOCK.defaultBlockState(), Blocks.WAXED_WEATHERED_CUT_COPPER_SLAB.defaultBlockState())
                .component(Blocks.HONEYCOMB_BLOCK)
                .component(Blocks.WEATHERED_CUT_COPPER_SLAB)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.HONEYCOMB_BLOCK), FabricRecipeProvider.has(Items.HONEYCOMB_BLOCK))
                .save(exporter, AnvilCraft.of("squeeze/waxed_weathered_cut_copper_slab"));

        BlockAnvilRecipeBuilder.block(RecipeCategory.MISC, Blocks.HONEYCOMB_BLOCK.defaultBlockState(), Blocks.WAXED_EXPOSED_CUT_COPPER_SLAB.defaultBlockState())
                .component(Blocks.HONEYCOMB_BLOCK)
                .component(Blocks.EXPOSED_CUT_COPPER_SLAB)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.HONEYCOMB_BLOCK), FabricRecipeProvider.has(Items.HONEYCOMB_BLOCK))
                .save(exporter, AnvilCraft.of("squeeze/waxed_exposed_cut_copper_slab"));

        BlockAnvilRecipeBuilder.block(RecipeCategory.MISC, Blocks.HONEYCOMB_BLOCK.defaultBlockState(), Blocks.WAXED_CUT_COPPER_SLAB.defaultBlockState())
                .component(Blocks.HONEYCOMB_BLOCK)
                .component(Blocks.CUT_COPPER_SLAB)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.HONEYCOMB_BLOCK), FabricRecipeProvider.has(Items.HONEYCOMB_BLOCK))
                .save(exporter, AnvilCraft.of("squeeze/waxed_cut_copper_slab"));
    }
}
