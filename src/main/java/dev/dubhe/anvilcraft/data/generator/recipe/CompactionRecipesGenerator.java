package dev.dubhe.anvilcraft.data.generator.recipe;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.generator.MyRecipesGenerator;
import dev.dubhe.anvilcraft.data.recipe.anvil.block.BlockAnvilRecipeBuilder;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public abstract class CompactionRecipesGenerator {
    private CompactionRecipesGenerator() {
    }

    public static void buildRecipes(Consumer<FinishedRecipe> exporter) {
        compaction(Blocks.STONE, Blocks.STONE, Blocks.DEEPSLATE, exporter);
        compaction(Blocks.ICE, Blocks.ICE, Blocks.BLUE_ICE, exporter);
        compaction(Blocks.MOSS_BLOCK, Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE, exporter);
        compaction(Blocks.MOSS_BLOCK, Blocks.STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS, exporter);
        compaction(Blocks.MOSS_BLOCK, Blocks.DIRT, Blocks.GRASS_BLOCK, exporter);
        compaction(Blocks.NETHER_WART_BLOCK, Blocks.NETHERRACK, Blocks.CRIMSON_NYLIUM, exporter);
        compaction(Blocks.WARPED_WART_BLOCK, Blocks.NETHERRACK, Blocks.WARPED_NYLIUM, exporter);
    }

    public static void compaction(Block block, @NotNull Block block1, @NotNull Block block2, Consumer<FinishedRecipe> exporter) {
        BlockAnvilRecipeBuilder.block(RecipeCategory.MISC, Blocks.AIR.defaultBlockState(), block2.defaultBlockState())
                .component(block)
                .component(block1)
                .unlockedBy(MyRecipesGenerator.hasItem(block.asItem()), FabricRecipeProvider.has(block.asItem()))
                .save(exporter, AnvilCraft.of("smash_block/" + BuiltInRegistries.BLOCK.getKey(block2).getPath()));
    }
}
