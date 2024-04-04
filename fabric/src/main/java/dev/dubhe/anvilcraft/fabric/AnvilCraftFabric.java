package dev.dubhe.anvilcraft.fabric;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModCommands;
import dev.dubhe.anvilcraft.init.fabric.ModRecipeTypesFabric;
import dev.dubhe.anvilcraft.utils.fabric.ServerHooks;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class AnvilCraftFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        AnvilCraft.init();
        ServerHooks.init();
        ModRecipeTypesFabric.register();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> ModCommands.register(dispatcher));
    }
}