package dev.dubhe.anvilcraft.data.generator.recipe;

import dev.dubhe.anvilcraft.data.generator.MyRecipesGenerator;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.init.ModItems;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.function.Consumer;

public abstract class CompressRecipesGenerator {
    private CompressRecipesGenerator() {
    }

    public static void buildRecipes(Consumer<FinishedRecipe> exporter) {
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .hasBlock(Blocks.CAULDRON)
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.FRUIT_OF_THE_SEA), FabricRecipeProvider.has(ModItems.FRUIT_OF_THE_SEA))
                .hasItemIngredient(ModItems.FRUIT_OF_THE_SEA)
                .spawnItem(ModItems.KERNEL_OF_THE_SEA)
                .save(exporter);
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .hasBlock(Blocks.CAULDRON)
                .hasItemIngredient(Items.PRISMARINE_SHARD)
                .hasItemIngredient(ModItems.TEAR_OF_THE_SEA)
                .spawnItem(ModItems.BLADE_OF_THE_SEA)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.PRISMARINE_SHARD), FabricRecipeProvider.has(Items.PRISMARINE_SHARD))
                .unlockedBy(MyRecipesGenerator.hasItem(ModItems.TEAR_OF_THE_SEA), FabricRecipeProvider.has(ModItems.TEAR_OF_THE_SEA))
                .save(exporter);
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .hasBlock(Blocks.CAULDRON)
                .hasItemIngredient(Items.ANVIL)
                .hasItemIngredient(Items.DISPENSER)
                .spawnItem(ModItems.INTERACT_MACHINE)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.ANVIL), FabricRecipeProvider.has(Items.ANVIL))
                .unlockedBy(MyRecipesGenerator.hasItem(Items.DISPENSER), FabricRecipeProvider.has(Items.DISPENSER))
                .save(exporter);
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .hasBlock(Blocks.CAULDRON)
                .hasItemIngredient(Items.ANVIL)
                .hasItemIngredient(8, Items.CRAFTING_TABLE)
                .spawnItem(ModItems.AUTO_CRAFTER)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.ANVIL), FabricRecipeProvider.has(Items.ANVIL))
                .unlockedBy(MyRecipesGenerator.hasItem(Items.DISPENSER), FabricRecipeProvider.has(Items.DISPENSER))
                .save(exporter);
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .hasBlock(Blocks.CAULDRON)
                .hasItemIngredient(3,Items.BONE)
                .spawnItem(Items.BONE_BLOCK)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.BONE), FabricRecipeProvider.has(Items.BONE))
                .save(exporter);
    }
}
