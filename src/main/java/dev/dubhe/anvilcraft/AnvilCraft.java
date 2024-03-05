package dev.dubhe.anvilcraft;

import com.mojang.datafixers.util.Pair;
import dev.dubhe.anvilcraft.block.ModBlocks;
import dev.dubhe.anvilcraft.data.crafting.ModRecipeTypes;
import dev.dubhe.anvilcraft.item.ModItems;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;

import java.util.Map;

public class AnvilCraft implements ModInitializer {
    public static final String MOD_ID = "anvilcraft";

    @Override
    public void onInitialize() {
        for (Map.Entry<String, Block> entry : ModBlocks.BLOCK_MAP.entrySet()) {
            Registry.register(BuiltInRegistries.BLOCK, AnvilCraft.of(entry.getKey()), entry.getValue());
        }
        for (Map.Entry<String, Item> entry : ModItems.ITEM_MAP.entrySet()) {
            Registry.register(BuiltInRegistries.ITEM, AnvilCraft.of(entry.getKey()), entry.getValue());
        }
        for (Map.Entry<String, CreativeModeTab.Builder> entry : ModItems.ITEM_GROUP_MAP.entrySet()) {
            Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, AnvilCraft.of(entry.getKey()), entry.getValue().build());
        }
        for (Map.Entry<String, Pair<RecipeSerializer<?>, RecipeType<?>>> entry : ModRecipeTypes.RECIPE_TYPES.entrySet()) {
            if (null != entry.getValue().getFirst())
                Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, AnvilCraft.of(entry.getKey()), entry.getValue().getFirst());
            if (null != entry.getValue().getSecond())
                Registry.register(BuiltInRegistries.RECIPE_TYPE, AnvilCraft.of(entry.getKey()), entry.getValue().getSecond());
        }
    }

    public static ResourceLocation of(String id) {
        return new ResourceLocation(MOD_ID, id);
    }
}
