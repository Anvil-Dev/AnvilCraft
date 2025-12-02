package dev.dubhe.anvilcraft.integration.kubejs.recipe.components;

import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.recipe.component.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentType;
import dev.latvian.mods.rhino.type.TypeInfo;

public record ItemIngredientPredicateComponent() implements RecipeComponent<ItemIngredientPredicate> {
    public static final ItemIngredientPredicateComponent INSTANCE = new ItemIngredientPredicateComponent();
    public static final RecipeComponentType<ItemIngredientPredicate> TYPE = RecipeComponentType.unit(
        AnvilCraft.of("item_ingredient_predicate"),
        INSTANCE
    );

    @Override
    public RecipeComponentType<?> type() {
        return TYPE;
    }

    @Override
    public Codec<ItemIngredientPredicate> codec() {
        return ItemIngredientPredicate.CODEC;
    }

    @Override
    public TypeInfo typeInfo() {
        return TypeInfo.of(ItemIngredientPredicate.class);
    }

    @Override
    public String toString() {
        return "item_ingredient_predicate";
    }
}
