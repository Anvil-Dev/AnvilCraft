package dev.dubhe.anvilcraft.recipe.multiple;

import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.api.recipe.result.RecipeResult;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeSerializers;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.List;

public class EightToOneSmithingRecipe extends BaseMultipleToOneSmithingRecipe {
    public static final RecipeSerializer<EightToOneSmithingRecipe> SERIALIZER = BaseMultipleToOneSmithingRecipe.makeSerializer(
        EightToOneSmithingRecipe::new
    );

    public EightToOneSmithingRecipe(
        ItemIngredientPredicate template,
        ItemIngredientPredicate material,
        List<ItemIngredientPredicate> inputs,
        RecipeResult result
    ) {
        super(template, material, inputs, result);
    }

    public EightToOneSmithingRecipe(Data data) {
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
        return "eight_to_one_smithing";
    }

    @Override
    public RecipeSerializer<EightToOneSmithingRecipe> getSerializer() {
        return ModRecipeSerializers._821.get();
    }

    public static class Builder extends BaseBuilder<EightToOneSmithingRecipe> {
        protected Builder(ItemIngredientPredicate template) {
            super(template, 8);
        }

        protected Builder() {
            this(ItemIngredientPredicate.of(ModItems.EIGHT_TO_ONE_SMITHING_TEMPLATE.get()).build());
        }

        @Override
        protected EightToOneSmithingRecipe of(
            ItemIngredientPredicate template,
            ItemIngredientPredicate material,
            List<ItemIngredientPredicate> inputs,
            RecipeResult result
        ) {
            return new EightToOneSmithingRecipe(template, material, inputs, result);
        }
    }
}
