package dev.dubhe.anvilcraft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import dev.dubhe.anvilcraft.api.events.EventManager;
import dev.dubhe.anvilcraft.block.ModBlocks;
import dev.dubhe.anvilcraft.command.ModCommands;
import dev.dubhe.anvilcraft.config.AnvilCraftConfig;
import dev.dubhe.anvilcraft.data.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.event.listener.AnvilEventListener;
import dev.dubhe.anvilcraft.event.listener.LightningEventListener;
import dev.dubhe.anvilcraft.item.ModItems;
import dev.dubhe.anvilcraft.log.Logger;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

@SuppressWarnings("unused")
public class AnvilCraft implements ModInitializer {
    public static final String MOD_ID = "anvilcraft";
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Logger LOGGER = new Logger(LogUtils.getLogger());
    public static final EventManager EVENT_BUS = new EventManager();
    public static AnvilCraftConfig config = new AnvilCraftConfig();

    @Override
    public void onInitialize() {
        AnvilCraft.EVENT_BUS.register(new AnvilEventListener());
        AnvilCraft.EVENT_BUS.register(new LightningEventListener());
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
        CommandRegistrationCallback.EVENT.register(ModCommands::register);
        AnvilCraft.loadOrCreateConfig();
    }

    @SuppressWarnings("all")
    public static void loadOrCreateConfig() {
        Path commonConfigPath = FabricLoader.getInstance().getConfigDir().resolve("anvilcraft-common.json");
        File commonConfigFile = commonConfigPath.toFile();
        try {
            if (!commonConfigFile.isFile()) {
                commonConfigFile.createNewFile();
                try (FileWriter writer = new FileWriter(commonConfigFile)) {
                    AnvilCraft.GSON.toJson(AnvilCraft.config, writer);
                }
            } else {
                try (FileReader reader = new FileReader(commonConfigFile)) {
                    AnvilCraft.config = AnvilCraft.GSON.fromJson(reader, AnvilCraftConfig.class);
                }
            }
        } catch (IOException e) {
            AnvilCraft.LOGGER.printStackTrace(e);
        }
    }

    public static @NotNull ResourceLocation of(String id) {
        return new ResourceLocation(MOD_ID, id);
    }
}
