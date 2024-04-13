package dev.dubhe.anvilcraft.init.forge;

import com.mojang.datafixers.util.Pair;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

import java.util.Map;

public class ModRecipeTypesForge {
    /**
     * 注册配方类型
     *
     * @param event 事件
     */
    public static void register(RegisterEvent event) {
        for (Map.Entry<String, Pair<RecipeSerializer<?>, RecipeType<?>>> entry
                : ModRecipeTypes.RECIPE_TYPES.entrySet()) {
            if (entry.getValue().getFirst() != null) {
                event.register(ForgeRegistries.Keys.RECIPE_SERIALIZERS, (helper) ->
                        helper.register(AnvilCraft.of(entry.getKey()), entry.getValue().getFirst()));
            }
            if (entry.getValue().getSecond() != null) {
                event.register(ForgeRegistries.Keys.RECIPE_TYPES, (helper) ->
                        helper.register(AnvilCraft.of(entry.getKey()), entry.getValue().getSecond()));
            }
        }
    }
}
