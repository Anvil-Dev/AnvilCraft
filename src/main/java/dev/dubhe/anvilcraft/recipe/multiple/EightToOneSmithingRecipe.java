package dev.dubhe.anvilcraft.recipe.multiple;

import dev.anvilcraft.lib.recipe.component.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.reicpe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.multiple.result.MultipleToOneResult;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.List;

public class EightToOneSmithingRecipe extends BaseMultipleToOneSmithingRecipe {
    public EightToOneSmithingRecipe(
        ItemIngredientPredicate template,
        ItemIngredientPredicate material,
        List<ItemIngredientPredicate> inputs,
        MultipleToOneResult result
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
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.EIGHT_TO_ONE_SMITHING_SERIALIZER.get();
    }

    public static class Serializer extends BaseSerializer<EightToOneSmithingRecipe> {
        @Override
        protected EightToOneSmithingRecipe fromData(Data data) {
            return new EightToOneSmithingRecipe(data);
        }
    }

    public static class Builder extends BaseBuilder<EightToOneSmithingRecipe> {
        protected Builder(ItemIngredientPredicate template) {
            super(template, 2);
        }

        protected Builder() {
            this(ItemIngredientPredicate.of(ModItems.EIGHT_TO_ONE_SMITHING_TEMPLATE.get()).build());
        }

        @Override
        protected EightToOneSmithingRecipe of(
            ItemIngredientPredicate template,
            ItemIngredientPredicate material,
            List<ItemIngredientPredicate> inputs,
            MultipleToOneResult result
        ) {
            return new EightToOneSmithingRecipe(template, material, inputs, result);
        }
    }
}
