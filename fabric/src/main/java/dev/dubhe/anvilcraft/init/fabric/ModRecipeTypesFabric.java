package dev.dubhe.anvilcraft.init.fabric;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.Map;

public class ModRecipeTypesFabric {
    /**
     * 注册配方类型
     */
    public static void register() {
        for (Map.Entry<String, Map.Entry<RecipeSerializer<?>, RecipeType<?>>> entry
            : ModRecipeTypes.RECIPE_TYPES.entrySet()) {
            ResourceLocation location = AnvilCraft.of(entry.getKey());
            RecipeSerializer<?> serializer = entry.getValue().getKey();
            RecipeType<?> type = entry.getValue().getValue();
            if (null != serializer) Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, location, serializer);
            if (null != type) Registry.register(BuiltInRegistries.RECIPE_TYPE, location, type);
        }
    }
}
