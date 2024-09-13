package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.recipe.BlockCompressRecipe;
import dev.dubhe.anvilcraft.recipe.BlockCrushRecipe;
import dev.dubhe.anvilcraft.recipe.ItemCompressRecipe;
import dev.dubhe.anvilcraft.recipe.ItemCrushRecipe;
import dev.dubhe.anvilcraft.recipe.MeshRecipe;
import dev.dubhe.anvilcraft.recipe.StampingRecipe;

import dev.dubhe.anvilcraft.recipe.SuperHeatingRecipe;
import dev.dubhe.anvilcraft.recipe.TimeWarpRecipe;
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

    public static final DeferredHolder<RecipeType<?>, RecipeType<BlockCompressRecipe>>
            BLOCK_COMPRESS_TYPE = registerType("block_compress");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BlockCompressRecipe>>
            BLOCK_COMPRESS_SERIALIZER =
                    RECIPE_SERIALIZERS.register("block_compress", BlockCompressRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<ItemCompressRecipe>>
            ITEM_COMPRESS_TYPE = registerType("item_compress");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ItemCompressRecipe>>
            ITEM_COMPRESS_SERIALIZER =
                    RECIPE_SERIALIZERS.register("item_compress", ItemCompressRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<StampingRecipe>> STAMPING_TYPE =
            registerType("stamping");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<StampingRecipe>>
            STAMPING_SERIALIZER = RECIPE_SERIALIZERS.register("stamping", StampingRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<SuperHeatingRecipe>> SUPER_HEATING_TYPE =
            registerType("super_heating");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SuperHeatingRecipe>>
            SUPER_HEATING_SERIALIZER = RECIPE_SERIALIZERS.register("super_heating", SuperHeatingRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<TimeWarpRecipe>> TIME_WARP_TYPE = registerType("time_warp");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<TimeWarpRecipe>> TIME_WARP_SERIALIZER =
            RECIPE_SERIALIZERS.register("time_warp", TimeWarpRecipe.Serializer::new);

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
