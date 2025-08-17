package dev.dubhe.anvilcraft.integration.jei.recipe;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.recipe.anvil.util.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.MeshRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.components.ChanceItemStack;
import dev.dubhe.anvilcraft.util.RecipeUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public record MeshRecipeGroup(ItemIngredientPredicate ingredient, List<Result> results) {

    public static int maxRows;

    public static ImmutableList<MeshRecipeGroup> getAllRecipesGrouped() {
        maxRows = 1;

        List<MeshRecipe> recipes = JeiRecipeUtil.getRecipesFromType(ModRecipeTypes.MESH_TYPE.get());
        Multimap<ItemIngredientPredicate, MeshRecipe> ingredientGrouper = ArrayListMultimap.create();

        for (MeshRecipe recipe : recipes) {
            for (ItemIngredientPredicate ingredient : recipe.getInputItems()) {
                ingredientGrouper.put(ingredient, recipe);
            }
        }

        ImmutableList.Builder<MeshRecipeGroup> jeiRecipes = ImmutableList.builder();
        Comparator<Result> resultSorter = Comparator.comparingDouble(Result::expectedCount).reversed();

        for (ItemIngredientPredicate ingredient : ingredientGrouper.keySet()) {
            Collection<MeshRecipe> values = ingredientGrouper.get(ingredient);

            List<Result> results = new ArrayList<>(values.size());

            for (MeshRecipe recipe : values) {
                for (ChanceItemStack stack : recipe.getResultItems()) {
                    int resultCount = stack.getCount() instanceof ConstantValue(float value)
                        ? Math.round(value)
                        : 1;
                    results.add(new Result(stack.getStack().copyWithCount(resultCount), stack.getCount()));
                }
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

    public record Result(ItemStack item, NumberProvider provider, double expectedCount) {
        public Result(ItemStack item, NumberProvider provider) {
            this(item, provider, RecipeUtil.getExpectedValue(provider));
        }
    }
}
