package dev.dubhe.anvilcraft.integration.jei.recipe;

import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.recipe.anvil.MeshRecipe;
import dev.dubhe.anvilcraft.util.RecipeUtil;

import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public record MeshRecipeGroup(Ingredient ingredient, List<Result> results) {

    public static int maxRows;

    public static ImmutableList<MeshRecipeGroup> getAllRecipesGrouped() {
        maxRows = 1;

        List<MeshRecipe> recipes = JeiRecipeUtil.getRecipesFromType(ModRecipeTypes.MESH_TYPE.get());
        Multimap<Ingredient, MeshRecipe> ingredientGrouper = ArrayListMultimap.create();

        for (int i = 0; i < recipes.size(); i++) {
            MeshRecipe recipe = recipes.get(i);
            ingredientGrouper.put(recipe.getInput(), recipe);

            for (int j = i + 1; j < recipes.size(); j++) {
                MeshRecipe other = recipes.get(j);

                if (RecipeUtil.isIngredientsEqual(recipe.getInput(), other.getInput())) {
                    ingredientGrouper.put(recipe.getInput(), other);
                    recipes.remove(other);
                    j--;
                }
            }
        }

        ImmutableList.Builder<MeshRecipeGroup> jeiRecipes = ImmutableList.builder();
        Comparator<Result> resultSorter =
            Comparator.comparingDouble(Result::getExpectedCount).reversed();

        for (Ingredient ingredient : ingredientGrouper.keySet()) {
            Collection<MeshRecipe> values = ingredientGrouper.get(ingredient);

            List<Result> results = new ArrayList<>(values.size());

            for (MeshRecipe recipe : values) {
                int resultCount = recipe.getResultAmount() instanceof ConstantValue constantValue
                    ? Math.round(constantValue.value())
                    : 1;
                results.add(new Result(recipe.getResult().copyWithCount(resultCount), recipe.getResultAmount()));
            }

            results.sort(resultSorter);

            jeiRecipes.add(new MeshRecipeGroup(ingredient, results));
            int rows = Mth.ceil(values.size() / 9f);
            if (rows > maxRows) {
                maxRows = rows;
            }
        }
        return jeiRecipes.build();
    }

    public static final class Result {
        public final ItemStack item;
        public final NumberProvider provider;

        @Getter
        private final double expectedCount;

        public Result(ItemStack item, NumberProvider provider) {
            this.item = item;
            this.provider = provider;
            this.expectedCount = RecipeUtil.getExpectedValue(provider);
        }
    }
}
