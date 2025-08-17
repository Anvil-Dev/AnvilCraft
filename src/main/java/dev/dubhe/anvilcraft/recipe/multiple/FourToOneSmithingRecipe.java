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
public class FourToOneSmithingRecipe extends BaseMultipleToOneSmithingRecipe {
    public FourToOneSmithingRecipe(
        ItemIngredientPredicate template,
        ItemIngredientPredicate material,
        List<ItemIngredientPredicate> inputs,
        MultipleToOneResult result
    ) {
        super(template, material, inputs, result);
    }

    public FourToOneSmithingRecipe(Data data) {
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
        return ModRecipeTypes.FOUR_TO_ONE_SMITHING_SERIALIZER.get();
    }

    public static class Serializer extends BaseSerializer<FourToOneSmithingRecipe> {
        @Override
        protected FourToOneSmithingRecipe fromData(Data data) {
            return new FourToOneSmithingRecipe(data);
        }
    }

    public static class Builder extends BaseBuilder<FourToOneSmithingRecipe> {
        protected Builder(ItemIngredientPredicate template) {
            super(template, 4);
        }

        protected Builder() {
            this(ItemIngredientPredicate.of(ModItems.FOUR_TO_ONE_SMITHING_TEMPLATE.get()).build());
        }

        @Override
        protected FourToOneSmithingRecipe of(
            ItemIngredientPredicate template,
            ItemIngredientPredicate material,
            List<ItemIngredientPredicate> inputs,
            MultipleToOneResult result
        ) {
            return new FourToOneSmithingRecipe(template, material, inputs, result);
        }
    }
}
