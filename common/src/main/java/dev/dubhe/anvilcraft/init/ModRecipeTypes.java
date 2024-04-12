package dev.dubhe.anvilcraft.init;

import com.mojang.datafixers.util.Pair;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class ModRecipeTypes {
    public static final Map<String, Pair<RecipeSerializer<?>, RecipeType<?>>> RECIPE_TYPES = new HashMap<>();
    public static final RecipeType<AnvilRecipe> ANVIL_RECIPE = ModRecipeTypes
        .registerRecipeType("anvil_processing", AnvilRecipe.Serializer.INSTANCE, AnvilRecipe.Type.INSTANCE);

    @SuppressWarnings("SameParameterValue")
    private static <T extends Recipe<?>> @NotNull RecipeType<T> registerRecipeType(
        String id, @Nullable RecipeSerializer<?> serializer, @NotNull RecipeType<T> type
    ) {
        RECIPE_TYPES.put(id, new Pair<>(serializer, type));
        return type;
    }
}
