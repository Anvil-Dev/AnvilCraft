package dev.dubhe.anvilcraft.data;

import dev.dubhe.anvilcraft.data.crafting.ShapedTagRecipeBuilder;
import dev.dubhe.anvilcraft.item.ModItemTags;
import dev.dubhe.anvilcraft.item.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class MyRecipesGenerator extends FabricRecipeProvider {
    public MyRecipesGenerator(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void buildRecipes(Consumer<FinishedRecipe> exporter) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.MAGNET)
                .pattern(" A ")
                .pattern("BCB")
                .pattern(" A ")
                .define('A', Items.REDSTONE)
                .define('B', ModItems.MAGNET_INGOT)
                .define('C', Items.ENDER_PEARL)
                .unlockedBy(hasItem(Items.REDSTONE), FabricRecipeProvider.has(Items.REDSTONE))
                .unlockedBy(hasItem(ModItems.MAGNET_INGOT), FabricRecipeProvider.has(ModItems.MAGNET_INGOT))
                .unlockedBy(hasItem(Items.ENDER_PEARL), FabricRecipeProvider.has(Items.ENDER_PEARL))
                .save(exporter);
        ShapedTagRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.AMETHYST_PICKAXE)
                .pattern("AAA")
                .pattern(" B ")
                .pattern(" B ")
                .define('A', Items.AMETHYST_SHARD)
                .define('B', Items.BAMBOO)
                .unlockedBy(hasItem(Items.AMETHYST_SHARD), FabricRecipeProvider.has(Items.REDSTONE))
                .unlockedBy(hasItem(Items.BAMBOO), FabricRecipeProvider.has(Items.BAMBOO))
                .save(exporter);
        SimpleCookingRecipeBuilder.campfireCooking(Ingredient.of(ModItemTags.DOUGH), RecipeCategory.FOOD, Items.BREAD, 0, 1);
    }

    public static @NotNull String hasItem(@NotNull Item item) {
        return "has_" + BuiltInRegistries.ITEM.getKey(item).getPath();
    }
}
