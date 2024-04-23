package dev.dubhe.anvilcraft.fabric;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.event.fabric.ModFabricEvents;
import dev.dubhe.anvilcraft.init.fabric.ModRecipeTypesFabric;
import dev.dubhe.anvilcraft.init.fabric.ModVillagers;
import dev.dubhe.anvilcraft.utils.fabric.ModCustomTrades;
import net.fabricmc.api.ModInitializer;

public class AnvilCraftFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        AnvilCraft.init();
        ModVillagers.register();
        ModCustomTrades.registerCustomTrades();
        ModRecipeTypesFabric.register();
        ModFabricEvents.init();
    }
}