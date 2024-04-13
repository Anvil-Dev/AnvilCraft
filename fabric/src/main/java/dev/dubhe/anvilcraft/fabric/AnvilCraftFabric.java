package dev.dubhe.anvilcraft.fabric;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.event.fabric.ModFabricEvents;
import dev.dubhe.anvilcraft.init.fabric.ModRecipeTypesFabric;
import net.fabricmc.api.ModInitializer;

public class AnvilCraftFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        AnvilCraft.init();
        ModRecipeTypesFabric.register();
        ModFabricEvents.init();
    }
}