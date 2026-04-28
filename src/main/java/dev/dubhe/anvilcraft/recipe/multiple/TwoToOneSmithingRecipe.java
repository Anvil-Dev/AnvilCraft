package dev.dubhe.anvilcraft.recipe.multiple;

import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.api.recipe.result.RecipeResult;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.List;

public class TwoToOneSmithingRecipe extends BaseMultipleToOneSmithingRecipe {
    public TwoToOneSmithingRecipe(
        ItemIngredientPredicate template,
        ItemIngredientPredicate material,
        List<ItemIngredientPredicate> inputs,
        RecipeResult result
    ) {
        super(template, material, inputs, result);
    }

    public TwoToOneSmithingRecipe(Data data) {
        super(data);
    }

    public static Builder builder(ItemIngredientPredicate template) {
        return new Builder(template);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.TWO_TO_ONE_SMITHING_SERIALIZER.get();
    }

    public static class Serializer extends BaseSerializer<TwoToOneSmithingRecipe> {
        @Override
        protected TwoToOneSmithingRecipe fromData(Data data) {
            return new TwoToOneSmithingRecipe(data);
        }
    }

    public static class Builder extends BaseBuilder<TwoToOneSmithingRecipe> {
        protected Builder(ItemIngredientPredicate template) {
            super(template, 2);
        }

        protected Builder() {
            this(ItemIngredientPredicate.of(ModItems.TWO_TO_ONE_SMITHING_TEMPLATE.get()).build());
        }

        @Override
        protected TwoToOneSmithingRecipe of(
            ItemIngredientPredicate template,
            ItemIngredientPredicate material,
            List<ItemIngredientPredicate> inputs,
            RecipeResult result
        ) {
            return new TwoToOneSmithingRecipe(template, material, inputs, result);
        }
    }
}
