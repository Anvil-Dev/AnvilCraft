package dev.dubhe.anvilcraft.data.generator.recipe;

import dev.dubhe.anvilcraft.data.generator.MyRecipesGenerator;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.init.ModItems;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
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
        cook(ModItems.UTUSAN_RAW, 1, ModItems.UTUSAN, 1, exporter);
    }

    public static void cook(Item item, int count, Item item1, int count1, Consumer<FinishedRecipe> exporter) {
        AnvilRecipe.Builder.create(RecipeCategory.FOOD)
                .hasBlock(Blocks.CAULDRON)
                .hasBlock(new Vec3(0.0, -2.0, 0.0), BlockTags.CAMPFIRES)
                .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), count, item)
                .spawnItem(item1, count1)
                .unlockedBy(MyRecipesGenerator.hasItem(item), FabricRecipeProvider.has(item))
                .save(exporter, "cook/" + BuiltInRegistries.ITEM.getKey(item1).getPath());
    }

    public static void boil(Item item, int count, Item item1, int count1, Consumer<FinishedRecipe> exporter) {
        AnvilRecipe.Builder.create(RecipeCategory.FOOD)
                .hasBlock(Blocks.WATER_CAULDRON, new Vec3(0.0, -1.0, 0.0), Map.entry(LayeredCauldronBlock.LEVEL, 1))
                .hasBlock(new Vec3(0.0, -2.0, 0.0), BlockTags.CAMPFIRES)
                .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), count, item)
                .setBlock(Blocks.CAULDRON)
                .spawnItem(item1, count1)
                .unlockedBy(MyRecipesGenerator.hasItem(item), FabricRecipeProvider.has(item))
                .save(exporter, "boil/" + BuiltInRegistries.ITEM.getKey(item1).getPath() + "_1");
        AnvilRecipe.Builder.create(RecipeCategory.FOOD)
                .hasBlock(Blocks.WATER_CAULDRON, new Vec3(0.0, -1.0, 0.0), Map.entry(LayeredCauldronBlock.LEVEL, 2))
                .hasBlock(new Vec3(0.0, -2.0, 0.0), BlockTags.CAMPFIRES)
                .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), count, item)
                .setBlock(Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 1))
                .spawnItem(item1, count1)
                .unlockedBy(MyRecipesGenerator.hasItem(item), FabricRecipeProvider.has(item))
                .save(exporter, "boil/" + BuiltInRegistries.ITEM.getKey(item1).getPath() + "_2");
        AnvilRecipe.Builder.create(RecipeCategory.FOOD)
                .hasBlock(Blocks.WATER_CAULDRON, new Vec3(0.0, -1.0, 0.0), Map.entry(LayeredCauldronBlock.LEVEL, 3))
                .hasBlock(new Vec3(0.0, -2.0, 0.0), BlockTags.CAMPFIRES)
                .hasItemIngredient(new Vec3(0.0, -1.0, 0.0), count, item)
                .setBlock(Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 2))
                .spawnItem(item1, count1)
                .unlockedBy(MyRecipesGenerator.hasItem(item), FabricRecipeProvider.has(item))
                .save(exporter, "boil/" + BuiltInRegistries.ITEM.getKey(item1).getPath() + "_3");
    }
}
