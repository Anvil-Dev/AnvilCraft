package dev.dubhe.anvilcraft.data.crafting;

import com.mojang.datafixers.util.Pair;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.HashMap;
import java.util.Map;

public class ModRecipeTypes {
    public static final Map<String, Pair<RecipeSerializer<?>, RecipeType<?>>> RECIPE_TYPES = new HashMap<>();

    static {
        ModRecipeTypes.registerRecipeType("tag_crafting_shaped", ShapedTagRecipe.Serializer.INSTANCE, null);
    }

    public static void registerRecipeType(String id, RecipeSerializer<?> serializer, RecipeType<?> type) {
        RECIPE_TYPES.put(id, new Pair<>(serializer, type));
    }
}
