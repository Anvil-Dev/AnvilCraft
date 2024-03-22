package dev.dubhe.anvilcraft.init;

import com.mojang.datafixers.util.Pair;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.recipe.anvil.block.BlockAnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.anvil.item.ItemAnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.crafting_table.ShapedTagRecipe;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class ModRecipeTypes {
    private static final Map<String, Pair<RecipeSerializer<?>, RecipeType<?>>> RECIPE_TYPES = new HashMap<>();
    public static final @Nullable RecipeType<ShapedTagRecipe> TAG_CRAFTING_SHAPED = ModRecipeTypes.registerRecipeType("tag_crafting_shaped", ShapedTagRecipe.Serializer.INSTANCE, null);
    public static final RecipeType<ItemAnvilRecipe> ANVIL_ITEM = ModRecipeTypes.registerRecipeType("anvil_item_processing", ItemAnvilRecipe.Serializer.INSTANCE, ItemAnvilRecipe.Type.INSTANCE);
    public static final RecipeType<BlockAnvilRecipe> ANVIL_BLOCK = ModRecipeTypes.registerRecipeType("anvil_block_processing", BlockAnvilRecipe.Serializer.INSTANCE, BlockAnvilRecipe.Type.INSTANCE);
    private static <T extends Recipe<?>> @Nullable RecipeType<T> registerRecipeType(String id, @Nullable RecipeSerializer<T> serializer, @Nullable RecipeType<T> type) {
        RECIPE_TYPES.put(id, new Pair<>(serializer, type));
        return type;
    }

    public static void register() {
        for (Map.Entry<String, Pair<RecipeSerializer<?>, RecipeType<?>>> entry : ModRecipeTypes.RECIPE_TYPES.entrySet()) {
            if (null != entry.getValue().getFirst())
                Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, AnvilCraft.of(entry.getKey()), entry.getValue().getFirst());
            if (null != entry.getValue().getSecond())
                Registry.register(BuiltInRegistries.RECIPE_TYPE, AnvilCraft.of(entry.getKey()), entry.getValue().getSecond());
        }
    }
}
