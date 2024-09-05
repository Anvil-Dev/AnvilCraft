package dev.dubhe.anvilcraft.data.generator.loot;


import dev.anvilcraft.lib.data.provider.RegistratorLootTableProvider;

public class LootHandler {
    /**
     * @param provider 提供器
     */
    public static void init(RegistratorLootTableProvider provider) {
        CrabTrapLootLoader.init(provider);
        BeheadingLootLoader.init(provider);
        AdvancementLootLoader.init(provider);
    }
}
