package dev.dubhe.anvilcraft.integration.kubejs.recipe.components;

import com.mojang.serialization.Codec;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentType;
import dev.latvian.mods.rhino.type.TypeInfo;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

public record IngredientListComponent() implements RecipeComponent<List<Ingredient>> {
    public static final IngredientListComponent INSTANCE = new IngredientListComponent();
    public static final Codec<List<Ingredient>> CONTENT_CODEC = Ingredient.LIST_CODEC;
    public static final RecipeComponentType<List<Ingredient>> TYPE = RecipeComponentType.unit(
        AnvilCraft.of("ingredient_list"),
        INSTANCE
    );

    @Override
    public RecipeComponentType<?> type() {
        return TYPE;
    }

    @Override
    public Codec<List<Ingredient>> codec() {
        return IngredientListComponent.CONTENT_CODEC;
    }

    @Override
    public TypeInfo typeInfo() {
        return TypeInfo.of(List.class);
    }

    @Override
    public String toString() {
        return "block_state_predicate";
    }
}
