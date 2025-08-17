package dev.dubhe.anvilcraft.integration.jei.recipe;

import com.google.common.collect.ImmutableList;
import dev.dubhe.anvilcraft.block.CementCauldronBlock;
import dev.dubhe.anvilcraft.block.state.Color;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.recipe.anvil.util.ItemIngredientPredicate;

import java.util.List;

public record CementStainingRecipe(List<ItemIngredientPredicate> ingredients, CementCauldronBlock resultBlock) {
    public CementStainingRecipe(List<ItemIngredientPredicate> ingredients, CementCauldronBlock resultBlock) {
        this.ingredients = ImmutableList.copyOf(ingredients);
        this.resultBlock = resultBlock;
    }

    public static ImmutableList<CementStainingRecipe> getAllRecipes() {
        ImmutableList.Builder<CementStainingRecipe> builder = ImmutableList.builder();
        for (Color color : Color.values()) {
            CementStainingRecipe recipe = new CementStainingRecipe(
                List.of(ItemIngredientPredicate.Builder.item().of(color.dyeItem()).build()),
                ModBlocks.CEMENT_CAULDRONS.get(color).get()
            );
            builder.add(recipe);
        }
        return builder.build();
    }
}
