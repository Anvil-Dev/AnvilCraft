package dev.dubhe.anvilcraft.data.generator.loot;

import com.tterrag.registrate.providers.loot.RegistrateLootTableProvider;

public class LootHandler {
    /**
     * @param provider 提供器
     */
    public static void init(RegistrateLootTableProvider provider) {
        CrabTrapLootLoader.init(provider);
        BeheadingLootLoader.init(provider);
    }
}
