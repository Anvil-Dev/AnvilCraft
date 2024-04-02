package dev.dubhe.anvilcraft.fabric;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModCommands;
import dev.dubhe.anvilcraft.utils.fabric.ServerHooks;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;

public class AnvilCraftFabric implements ModInitializer {
    private static final File COMMON_CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("anvilcraft-common.json").toFile();

    @Override
    public void onInitialize() {
        AnvilCraft.init(new AnvilCraft.InitSettings(COMMON_CONFIG_FILE));
        ServerHooks.init();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> ModCommands.register(dispatcher));
    }
}