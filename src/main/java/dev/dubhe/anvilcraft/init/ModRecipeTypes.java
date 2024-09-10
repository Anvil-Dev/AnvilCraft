package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.recipe.CrushRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;


public class ModRecipeTypes {
    private static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, AnvilCraft.MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, AnvilCraft.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<CrushRecipe>> CRUSH_TYPE = registerType("crush");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CrushRecipe>> CRUSH_SERIALIZER = RECIPE_SERIALIZERS.register("crush", CrushRecipe.Serializer::new);

    private static <T extends Recipe<?>> DeferredHolder<RecipeType<?>, RecipeType<T>> registerType(String name) {
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
