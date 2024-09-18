package dev.dubhe.anvilcraft.recipe;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModItemTags;
import dev.dubhe.anvilcraft.recipe.anvil.CookingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.ItemCompressRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.ItemCrushRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.SuperHeatingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.builder.AbstractItemProcessBuilder;
import dev.dubhe.anvilcraft.util.RecipeUtil;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.SmokingRecipe;

import java.util.Optional;

import static dev.dubhe.anvilcraft.util.Util.generateUniqueRecipeSuffix;

public class GenerateRecipe {
    public static Optional<RecipeHolder<?>> handleVanillaRecipe(
            RecipeType<?> recipeType, RecipeHolder<?> recipeHolder) {
        if (recipeType == RecipeType.SMOKING) {
            SmokingRecipe recipe = (SmokingRecipe) recipeHolder.value();
            ResourceLocation newId = AnvilCraft.of(recipeHolder.id().getPath() + generateUniqueRecipeSuffix());
            CookingRecipe newRecipe = CookingRecipe.builder()
                    .requires(recipe.ingredient)
                    .result(recipe.result)
                    .buildRecipe();
            return Optional.of(new RecipeHolder<>(newId, newRecipe));
        }
        if (recipeType == RecipeType.BLASTING) {
            BlastingRecipe recipe = (BlastingRecipe) recipeHolder.value();
            ResourceLocation newId = AnvilCraft.of(recipeHolder.id().getPath() + generateUniqueRecipeSuffix());
            AbstractItemProcessBuilder<SuperHeatingRecipe> builder =
                    SuperHeatingRecipe.builder().requires(recipe.ingredient);
            ItemStack result = recipe.result.copy();
            for (ItemStack item : recipe.ingredient.getItems()) {
                if (item.is(ModItemTags.RAW_ORES) || item.is(ModItemTags.ORES)) {
                    result.setCount(result.getCount() * 2);
                    break;
                }
            }
            SuperHeatingRecipe newRecipe = builder.result(result).buildRecipe();
            return Optional.of(new RecipeHolder<>(newId, newRecipe));
        }
        if (recipeType == RecipeType.CRAFTING) {
            CraftingRecipe recipe = (CraftingRecipe) recipeHolder.value();
            ResourceLocation newId = AnvilCraft.of(recipeHolder.id().getPath() + generateUniqueRecipeSuffix());
            if (recipe instanceof ShapedRecipe shapedRecipe) {
                NonNullList<Ingredient> ingredients = shapedRecipe.getIngredients();
                if (ingredients.stream().allMatch(i -> RecipeUtil.isIngredientsEqual(i, ingredients.getFirst()))) {
                    if (ingredients.size() == 4 || ingredients.size() == 9) {
                        ItemCompressRecipe newRecipe = ItemCompressRecipe.builder()
                                .result(shapedRecipe.result)
                                .requires(ingredients.getFirst(), ingredients.size())
                                .buildRecipe();
                        return Optional.of(new RecipeHolder<>(newId, newRecipe));
                    }
                }
            } else if (recipe instanceof ShapelessRecipe shapelessRecipe) {
                NonNullList<Ingredient> ingredients = shapelessRecipe.getIngredients();
                if (ingredients.size() == 1) {
                    ItemCrushRecipe newRecipe = ItemCrushRecipe.builder()
                            .result(shapelessRecipe.result)
                            .requires(ingredients.getFirst())
                            .buildRecipe();
                    return Optional.of(new RecipeHolder<>(newId, newRecipe));
                }
                if (ingredients.stream().allMatch(i -> RecipeUtil.isIngredientsEqual(i, ingredients.getFirst()))) {
                    ItemCompressRecipe newRecipe = ItemCompressRecipe.builder()
                            .result(shapelessRecipe.result)
                            .requires(ingredients.getFirst(), ingredients.size())
                            .buildRecipe();
                    return Optional.of(new RecipeHolder<>(newId, newRecipe));
                }
            }
        }
        return Optional.empty();
    }
}
