package dev.dubhe.anvilcraft.init.forge;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;

import net.minecraft.resources.ResourceLocation;
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
        for (Map.Entry<String, Map.Entry<RecipeSerializer<?>, RecipeType<?>>> entry :
                ModRecipeTypes.RECIPE_TYPES.entrySet()) {
            ResourceLocation location = AnvilCraft.of(entry.getKey());
            RecipeSerializer<?> serializer = entry.getValue().getKey();
            RecipeType<?> type = entry.getValue().getValue();
            if (serializer != null) {
                event.register(
                        ForgeRegistries.Keys.RECIPE_SERIALIZERS,
                        (helper) -> helper.register(location, serializer));
            }
            if (type != null) {
                event.register(
                        ForgeRegistries.Keys.RECIPE_TYPES, (helper) -> helper.register(location, type));
            }
        }
    }
}
