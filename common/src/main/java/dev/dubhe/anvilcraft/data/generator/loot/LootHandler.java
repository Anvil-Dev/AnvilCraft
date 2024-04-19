package dev.dubhe.anvilcraft.data.generator.loot;

import com.tterrag.registrate.providers.loot.RegistrateLootTableProvider;

public class LootHandler {
    public static void init(RegistrateLootTableProvider provider) {
        CrabTrapLootLoader.init(provider);
    }
}
