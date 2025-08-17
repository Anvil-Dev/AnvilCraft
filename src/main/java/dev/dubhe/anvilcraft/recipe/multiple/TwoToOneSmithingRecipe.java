package dev.dubhe.anvilcraft.recipe.multiple;

import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.multiple.result.MultipleToOneResult;
import dev.dubhe.anvilcraft.recipe.anvil.util.ItemIngredientPredicate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.crafting.RecipeSerializer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class TwoToOneSmithingRecipe extends BaseMultipleToOneSmithingRecipe {
    public TwoToOneSmithingRecipe(
        ItemIngredientPredicate template,
        ItemIngredientPredicate material,
        List<ItemIngredientPredicate> inputs,
        MultipleToOneResult result
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
            MultipleToOneResult result
        ) {
            return new TwoToOneSmithingRecipe(template, material, inputs, result);
        }
    }
}
