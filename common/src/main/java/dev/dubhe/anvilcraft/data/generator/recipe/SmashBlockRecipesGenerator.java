package dev.dubhe.anvilcraft.data.generator.recipe;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.generator.MyRecipesGenerator;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public abstract class SmashBlockRecipesGenerator {
    private SmashBlockRecipesGenerator() {
    }

    public static void buildRecipes(Consumer<FinishedRecipe> exporter) {
        smash(Blocks.COBBLESTONE, Blocks.GRAVEL, exporter);
        smash(Blocks.GRAVEL, Blocks.SAND, exporter);
        smash(Blocks.POLISHED_GRANITE, Blocks.GRANITE, exporter);
        smash(Blocks.GRANITE, Blocks.RED_SAND, exporter);
        smash(Blocks.POLISHED_ANDESITE, Blocks.ANDESITE, exporter);
        smash(Blocks.ANDESITE, Blocks.TUFF, exporter);
        smash(Blocks.POLISHED_DIORITE, Blocks.DIORITE, exporter);
        smash(Blocks.DIORITE, Blocks.CALCITE, exporter);
        smash(Blocks.STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS, exporter);
        smash(Blocks.DEEPSLATE_BRICKS, Blocks.CRACKED_DEEPSLATE_BRICKS, exporter);
        smash(Blocks.NETHER_BRICKS, Blocks.CRACKED_NETHER_BRICKS, exporter);
        smash(Blocks.DEEPSLATE_TILES, Blocks.CRACKED_DEEPSLATE_TILES, exporter);
        smash(Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS, exporter);
    }

    public static void smash(Block block, @NotNull Block block1, Consumer<FinishedRecipe> exporter) {
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .hasBlock(block)
                .setBlock(block1)
                .unlockedBy(MyRecipesGenerator.hasItem(block.asItem()), FabricRecipeProvider.has(block.asItem()))
                .save(exporter, AnvilCraft.of("smash_block/" + BuiltInRegistries.BLOCK.getKey(block1).getPath()));
    }
}
