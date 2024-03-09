package dev.dubhe.anvilcraft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import dev.dubhe.anvilcraft.api.event.EventManager;
import dev.dubhe.anvilcraft.api.network.Networking;
import dev.dubhe.anvilcraft.init.*;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import dev.dubhe.anvilcraft.config.AnvilCraftConfig;
import dev.dubhe.anvilcraft.api.log.Logger;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

public class AnvilCraft implements ModInitializer {
    public static final String MOD_ID = "anvilcraft";
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Logger LOGGER = new Logger(LogUtils.getLogger());
    public static final EventManager EVENT_BUS = new EventManager();
    public static AnvilCraftConfig config = new AnvilCraftConfig();

    @Override
    public void onInitialize() {
        AnvilCraft.loadOrCreateConfig();
        ModEvents.register();
        ModBlocks.register();
        ModItems.register();
        ModItemGroups.register();
        ModRecipeTypes.register();
        ModCommands.register();
        ModBlockEntities.register();
        ModMenuTypes.register();
        ModNetworks.register();
        Networking.SERVER.register();
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
