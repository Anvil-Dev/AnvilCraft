package dev.dubhe.anvilcraft.integration.kubejs.recipe.components;

import com.mojang.serialization.Codec;
import dev.dubhe.anvilcraft.recipe.anvil.util.ItemIngredientPredicate;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.rhino.type.TypeInfo;
import org.jetbrains.annotations.NotNull;

public record ItemIngredientPredicateComponent() implements RecipeComponent<ItemIngredientPredicate> {
    public static final ItemIngredientPredicateComponent INSTANCE = new ItemIngredientPredicateComponent();

    @Override
    public Codec<ItemIngredientPredicate> codec() {
        return ItemIngredientPredicate.CODEC;
    }

    @Override
    public TypeInfo typeInfo() {
        return TypeInfo.of(ItemIngredientPredicate.class);
    }

    @Override
    public @NotNull String toString() {
        return "block_state_predicate";
    }
}
