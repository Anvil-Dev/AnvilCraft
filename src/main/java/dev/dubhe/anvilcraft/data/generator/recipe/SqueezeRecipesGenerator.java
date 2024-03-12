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

        BlockAnvilRecipeBuilder.block(RecipeCategory.MISC, Blocks.ICE.defaultBlockState(), Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 1))
                .component(Blocks.SNOW_BLOCK)
                .component(Blocks.CAULDRON)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.SNOW_BLOCK), FabricRecipeProvider.has(Items.SNOW_BLOCK))
                .save(exporter, AnvilCraft.of("squeeze/snow_2_water_cauldron_1"));
        BlockAnvilRecipeBuilder.block(RecipeCategory.MISC, Blocks.ICE.defaultBlockState(), Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 2))
                .component(Blocks.SNOW_BLOCK)
                .component(Component.of(Component.Value.of(Blocks.WATER_CAULDRON).with("level", 1)))
                .unlockedBy(MyRecipesGenerator.hasItem(Items.SNOW_BLOCK), FabricRecipeProvider.has(Items.SNOW_BLOCK))
                .save(exporter, AnvilCraft.of("squeeze/snow_2_water_cauldron_2"));
        BlockAnvilRecipeBuilder.block(RecipeCategory.MISC, Blocks.ICE.defaultBlockState(), Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3))
                .component(Blocks.SNOW_BLOCK)
                .component(Component.of(Component.Value.of(Blocks.WATER_CAULDRON).with("level", 2)))
                .unlockedBy(MyRecipesGenerator.hasItem(Items.SNOW_BLOCK), FabricRecipeProvider.has(Items.SNOW_BLOCK))
                .save(exporter, AnvilCraft.of("squeeze/snow_2_water_cauldron_3"));
    }
}
