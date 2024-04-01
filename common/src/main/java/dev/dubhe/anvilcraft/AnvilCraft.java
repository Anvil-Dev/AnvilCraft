package dev.dubhe.anvilcraft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import dev.dubhe.anvilcraft.api.event.EventManager;
import dev.dubhe.anvilcraft.api.log.Logger;
import dev.dubhe.anvilcraft.api.registry.AnvilCraftRegistrate;
import dev.dubhe.anvilcraft.config.AnvilCraftConfig;
import dev.dubhe.anvilcraft.data.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.init.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class AnvilCraft {
	public static final String MOD_ID = "anvilcraft";
	public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	public static final Logger LOGGER = new Logger(LogUtils.getLogger());
	public static final EventManager EVENT_BUS = new EventManager();
	public static final File COMMON_CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("anvilcraft-common.json").toFile();
	public static AnvilCraftConfig config = new AnvilCraftConfig();

	public static final AnvilCraftRegistrate REGISTRATE = AnvilCraftRegistrate.create(MOD_ID);

	public static void init() {
		AnvilCraft.loadOrCreateConfig();
		ModEvents.register();
		ModBlocks.register();
		ModItems.register();
		ModItemGroups.register();
		ModRecipeTypes.register();
		ModBlockEntities.register();
		ModMenuTypes.register();
		ModNetworks.register();
		ModDispenserBehavior.register();
		AnvilCraftDatagen.init();
		REGISTRATE.registerRegistrate();
	}

	@SuppressWarnings("all")
	public static void loadOrCreateConfig() {
		try {
			if (!COMMON_CONFIG_FILE.isFile()) {
				AnvilCraft.saveConfigFile();
			} else {
				try (FileReader reader = new FileReader(COMMON_CONFIG_FILE)) {
					AnvilCraft.config = AnvilCraft.GSON.fromJson(reader, AnvilCraftConfig.class);
					AnvilCraft.saveConfigFile();
				}
			}
		} catch (IOException e) {
			AnvilCraft.LOGGER.printStackTrace(e);
		}
	}

	@SuppressWarnings("all")
	public static void saveConfigFile() {
		try {
			if (!COMMON_CONFIG_FILE.isFile()) {
				COMMON_CONFIG_FILE.createNewFile();
			}
			try (FileWriter writer = new FileWriter(COMMON_CONFIG_FILE)) {
				AnvilCraft.GSON.toJson(AnvilCraft.config, writer);
			}
		} catch (IOException e) {
			AnvilCraft.LOGGER.printStackTrace(e);
		}
	}

	public static @NotNull ResourceLocation of(String id) {
		return new ResourceLocation(MOD_ID, id);
	}
}
