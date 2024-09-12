package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.recipe.BlockCrushRecipe;
import dev.dubhe.anvilcraft.recipe.CompressRecipe;
import dev.dubhe.anvilcraft.recipe.ItemCrushRecipe;
import dev.dubhe.anvilcraft.recipe.MeshRecipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipeTypes {
    private static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, AnvilCraft.MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, AnvilCraft.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<BlockCrushRecipe>> BLOCK_CRUSH_TYPE =
            registerType("block_crush");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BlockCrushRecipe>>
            BLOCK_CRUSH_SERIALIZER =
                    RECIPE_SERIALIZERS.register("block_crush", BlockCrushRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<ItemCrushRecipe>> ITEM_CRUSH_TYPE =
            registerType("item_crush");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ItemCrushRecipe>>
            ITEM_CRUSH_SERIALIZERS =
                    RECIPE_SERIALIZERS.register("item_crush", ItemCrushRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<CompressRecipe>> COMPRESS_TYPE =
            registerType("compress");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CompressRecipe>>
            COMPRESS_SERIALIZER = RECIPE_SERIALIZERS.register("compress", CompressRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<MeshRecipe>> MESH_TYPE =
            registerType("mesh");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MeshRecipe>>
            MESH_SERIALIZER = RECIPE_SERIALIZERS.register("mesh", MeshRecipe.Serializer::new);

    private static <T extends Recipe<?>> DeferredHolder<RecipeType<?>, RecipeType<T>> registerType(
            String name) {
        return RECIPE_TYPES.register(name, () -> new RecipeType<>() {
            @Override
            public String toString() {
                return AnvilCraft.of(name).toString();
            }
        });
    }

    public static void register(IEventBus bus) {
        RECIPE_TYPES.register(bus);
        RECIPE_SERIALIZERS.register(bus);
    }
}
