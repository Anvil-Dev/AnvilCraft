package dev.dubhe.anvilcraft.data.generator.recipe;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.generator.MyRecipesGenerator;
import dev.dubhe.anvilcraft.data.recipe.anvil.item.ItemAnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.anvil.item.ItemAnvilRecipeBuilder;
import dev.dubhe.anvilcraft.init.ModItems;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.function.Consumer;

public abstract class CompressRecipesGenerator {
    private CompressRecipesGenerator() {
    }

    public static void buildRecipes(Consumer<FinishedRecipe> exporter) {
        ItemAnvilRecipeBuilder.item(RecipeCategory.TOOLS, ModItems.INTERACT_MACHINE)
                .requires(Items.ANVIL)
                .requires(Items.DISPENSER)
                .component(Blocks.CAULDRON)
                .location(ItemAnvilRecipe.Location.IN)
                .resultLocation(ItemAnvilRecipe.Location.IN)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.ANVIL), FabricRecipeProvider.has(Items.ANVIL))
                .unlockedBy(MyRecipesGenerator.hasItem(Items.DISPENSER), FabricRecipeProvider.has(Items.DISPENSER))
                .save(exporter);
        ItemAnvilRecipeBuilder.item(RecipeCategory.TOOLS, ModItems.AUTO_CRAFTER)
                .requires(Items.ANVIL)
                .requires(Items.CRAFTING_TABLE, 8)
                .component(Blocks.CAULDRON)
                .location(ItemAnvilRecipe.Location.IN)
                .resultLocation(ItemAnvilRecipe.Location.IN)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.ANVIL), FabricRecipeProvider.has(Items.ANVIL))
                .unlockedBy(MyRecipesGenerator.hasItem(Items.DISPENSER), FabricRecipeProvider.has(Items.DISPENSER))
                .save(exporter);
        ItemAnvilRecipeBuilder.item(RecipeCategory.TOOLS, Items.BONE_BLOCK)
                .requires(Items.BONE, 3)
                .component(Blocks.CAULDRON)
                .location(ItemAnvilRecipe.Location.IN)
                .resultLocation(ItemAnvilRecipe.Location.IN)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.BONE), FabricRecipeProvider.has(Items.BONE))
                .save(exporter);
    }
}
