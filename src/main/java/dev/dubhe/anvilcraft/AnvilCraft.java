package dev.dubhe.anvilcraft;

import dev.dubhe.anvilcraft.items.ModItems;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

import java.util.Map;

public class AnvilCraft implements ModInitializer {
    public static final String MOD_ID = "anvilcraft";

    @Override
    public void onInitialize() {
        for (Map.Entry<String, Item> entry : ModItems.ITEM_MAP.entrySet()) {
            Registry.register(BuiltInRegistries.ITEM, AnvilCraft.of(entry.getKey()), entry.getValue());
        }
        for (Map.Entry<String, CreativeModeTab.Builder> entry : ModItems.ITEM_GROUP_MAP.entrySet()) {
            Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, AnvilCraft.of(entry.getKey()), entry.getValue().build());
        }

    }

    public static ResourceLocation of(String id) {
        return new ResourceLocation(MOD_ID, id);
    }
}
