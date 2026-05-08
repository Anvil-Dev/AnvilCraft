package dev.dubhe.anvilcraft.recipe.multiple;

import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.api.recipe.result.RecipeResult;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeSerializers;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.List;

public class FourToOneSmithingRecipe extends BaseMultipleToOneSmithingRecipe {
    public static final RecipeSerializer<FourToOneSmithingRecipe> SERIALIZER = BaseMultipleToOneSmithingRecipe.makeSerializer(
        FourToOneSmithingRecipe::new
    );

    public FourToOneSmithingRecipe(
        ItemIngredientPredicate template,
        ItemIngredientPredicate material,
        List<ItemIngredientPredicate> inputs,
        RecipeResult result
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
    public String group() {
        return "four_to_one_smithing";
    }

    @Override
    public RecipeSerializer<FourToOneSmithingRecipe> getSerializer() {
        return ModRecipeSerializers._421.get();
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
            RecipeResult result
        ) {
            return new FourToOneSmithingRecipe(template, material, inputs, result);
        }
    }
}
