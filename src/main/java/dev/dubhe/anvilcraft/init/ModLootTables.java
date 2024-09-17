package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;

public class ModLootTables {
    public static final ResourceKey<LootTable> CRAB_TRAP_COMMON = key("gameplay/crab_trap/common");
    public static final ResourceKey<LootTable> CRAB_TRAP_RIVER = key("gameplay/crab_trap/river");
    public static final ResourceKey<LootTable> CRAB_TRAP_OCEAN = key("gameplay/crab_trap/ocean");
    public static final ResourceKey<LootTable> CRAB_TRAP_WARM_OCEAN = key("gameplay/crab_trap/warm_ocean");
    public static final ResourceKey<LootTable> CRAB_TRAP_SWAMP = key("gameplay/crab_trap/swamp");
    public static final ResourceKey<LootTable> CRAB_TRAP_JUNGLE = key("gameplay/crab_trap/jungle");

    public static final ResourceKey<LootTable> ADVANCEMENT_ROOT = key("advancement/root");

    private static ResourceKey<LootTable> key(String path) {
        return ResourceKey.create(Registries.LOOT_TABLE, AnvilCraft.of(path));
    }
}
