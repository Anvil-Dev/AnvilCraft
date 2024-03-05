package dev.dubhe.anvilcraft.data.recipe;

import com.mojang.datafixers.util.Pair;
import dev.dubhe.anvilcraft.data.recipe.anvil.block.BlockAnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.anvil.item.ItemAnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.crafting_table.ShapedTagRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.HashMap;
import java.util.Map;

public class ModRecipeTypes {
    public static final Map<String, Pair<RecipeSerializer<?>, RecipeType<?>>> RECIPE_TYPES = new HashMap<>();
    public static final RecipeType<ShapedTagRecipe> TAG_CRAFTING_SHAPED = ModRecipeTypes.registerRecipeType("tag_crafting_shaped", ShapedTagRecipe.Serializer.INSTANCE, null);
    public static final RecipeType<ItemAnvilRecipe> ANVIL_ITEM = ModRecipeTypes.registerRecipeType("anvil_item_processing", ItemAnvilRecipe.Serializer.INSTANCE, ItemAnvilRecipe.Type.INSTANCE);
    public static final RecipeType<BlockAnvilRecipe> ANVIL_BLOCK = ModRecipeTypes.registerRecipeType("anvil_block_processing", BlockAnvilRecipe.Serializer.INSTANCE, BlockAnvilRecipe.Type.INSTANCE);

    public static <T extends Recipe<?>> RecipeType<T> registerRecipeType(String id, RecipeSerializer<T> serializer, RecipeType<T> type) {
        RECIPE_TYPES.put(id, new Pair<>(serializer, type));
        return type;
    }
}
