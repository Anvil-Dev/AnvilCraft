package dev.dubhe.anvilcraft.data.generator.recipe;

import dev.dubhe.anvilcraft.data.generator.MyRecipesGenerator;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.init.ModItems;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

public abstract class CookingRecipesGenerator {
    private CookingRecipesGenerator() {
    }

    public static void buildRecipes(Consumer<FinishedRecipe> exporter) {
        AnvilRecipe.Builder.create(RecipeCategory.FOOD)
                .hasBlock(Blocks.IRON_TRAPDOOR)
                .hasItemIngredient(Items.WHEAT)
                .spawnItem(new Vec3(0.0, -1.0, 0.0), ModItems.FLOUR)
                .unlockedBy(MyRecipesGenerator.hasItem(Items.WHEAT), FabricRecipeProvider.has(Items.WHEAT))
                .save(exporter);
        cook(ModItems.BEEF_MUSHROOM_STEW_RAW, 1, ModItems.BEEF_MUSHROOM_STEW, 1, exporter);
        cook(ModItems.UTUSAN_RAW, 1, ModItems.UTUSAN, 4, exporter);
    }

    public static void cook(Item item, int count, Item item1, int count1, Consumer<FinishedRecipe> exporter) {
        AnvilRecipe.Builder.create(RecipeCategory.FOOD)
                .hasBlock(Blocks.CAULDRON)
                .hasBlock(new Vec3(0.0, -2.0, 0.0), BlockTags.CAMPFIRES)
                .hasItemIngredient(count, item)
                .spawnItem(item1, count1)
                .unlockedBy(MyRecipesGenerator.hasItem(item), FabricRecipeProvider.has(item))
                .save(exporter);
    }
}
