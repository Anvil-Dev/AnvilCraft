package dev.dubhe.anvilcraft.integration.jei.recipe;

import dev.dubhe.anvilcraft.block.CementCauldronBlock;
import dev.dubhe.anvilcraft.block.state.Color;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.util.RecipeUtil;

import net.minecraft.world.item.crafting.Ingredient;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;

import java.util.List;

public class CementStainingRecipe {
    public final List<Ingredient> ingredients;
    public final CementCauldronBlock resultBlock;
    public final List<Object2IntMap.Entry<Ingredient>> mergedIngredients;

    public CementStainingRecipe(List<Ingredient> ingredients, CementCauldronBlock resultBlock) {
        this.ingredients = ImmutableList.copyOf(ingredients);
        this.resultBlock = resultBlock;
        this.mergedIngredients = RecipeUtil.mergeIngredient(ingredients);
    }

    public static ImmutableList<CementStainingRecipe> getAllRecipes() {
        ImmutableList.Builder<CementStainingRecipe> builder = ImmutableList.builder();
        for (Color color : Color.values()) {
            CementStainingRecipe recipe = new CementStainingRecipe(
                List.of(Ingredient.of(color.dyeItem())),
                ModBlocks.CEMENT_CAULDRONS.get(color).get());
            builder.add(recipe);
        }
        return builder.build();
    }
}
