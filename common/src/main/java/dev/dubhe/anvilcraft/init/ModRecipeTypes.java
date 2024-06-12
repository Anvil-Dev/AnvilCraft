package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.transform.MobTransformRecipe;

import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class ModRecipeTypes {
    public static final Map<String, Map.Entry<RecipeSerializer<?>, RecipeType<?>>> RECIPE_TYPES =
            new HashMap<>();
    public static final RecipeType<AnvilRecipe> ANVIL_RECIPE = ModRecipeTypes.registerRecipeType(
            "anvil_processing", AnvilRecipe.Serializer.INSTANCE, AnvilRecipe.Type.INSTANCE);
    public static final RecipeType<MobTransformRecipe> MOB_TRANSFORM_RECIPE = registerRecipeType(
            "mob_transform", MobTransformRecipe.Serializer.INSTANCE, MobTransformRecipe.Type.INSTANCE);

    @SuppressWarnings("SameParameterValue")
    private static <T extends Recipe<?>> @NotNull RecipeType<T> registerRecipeType(
            String id, @NotNull RecipeSerializer<?> serializer, @NotNull RecipeType<T> type) {
        RECIPE_TYPES.put(id, Map.entry(serializer, type));
        return type;
    }
}
