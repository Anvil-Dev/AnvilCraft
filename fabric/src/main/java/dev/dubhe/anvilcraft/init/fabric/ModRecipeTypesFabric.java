package dev.dubhe.anvilcraft.init.fabric;

import com.mojang.datafixers.util.Pair;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.Map;

public class ModRecipeTypesFabric {
    /**
     * 注册配方类型
     */
    public static void register() {
        for (Map.Entry<String, Pair<RecipeSerializer<?>, RecipeType<?>>> entry
            : ModRecipeTypes.RECIPE_TYPES.entrySet()) {
            if (null != entry.getValue().getFirst())
                Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
                    AnvilCraft.of(entry.getKey()), entry.getValue().getFirst());
            if (null != entry.getValue().getSecond())
                Registry.register(BuiltInRegistries.RECIPE_TYPE,
                    AnvilCraft.of(entry.getKey()), entry.getValue().getSecond());
        }
    }
}
