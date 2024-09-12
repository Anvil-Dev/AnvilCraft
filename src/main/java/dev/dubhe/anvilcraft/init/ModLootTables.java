package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;

public class ModLootTables {
    public static final ResourceKey<LootTable> CRAB_TRAP_COMMON =
            ResourceKey.create(Registries.LOOT_TABLE, AnvilCraft.of("gameplay/crab_trap/common"));
    public static final ResourceKey<LootTable> CRAB_TRAP_RIVER =
            ResourceKey.create(Registries.LOOT_TABLE, AnvilCraft.of("gameplay/crab_trap/river"));
    ;
    public static final ResourceKey<LootTable> CRAB_TRAP_OCEAN =
            ResourceKey.create(Registries.LOOT_TABLE, AnvilCraft.of("gameplay/crab_trap/ocean"));
    ;
    public static final ResourceKey<LootTable> CRAB_TRAP_WARM_OCEAN =
            ResourceKey.create(Registries.LOOT_TABLE, AnvilCraft.of("gameplay/crab_trap/warm_ocean"));
    ;
    public static final ResourceKey<LootTable> CRAB_TRAP_SWAMP =
            ResourceKey.create(Registries.LOOT_TABLE, AnvilCraft.of("gameplay/crab_trap/swamp"));
    ;
    public static final ResourceKey<LootTable> CRAB_TRAP_JUNGLE =
            ResourceKey.create(Registries.LOOT_TABLE, AnvilCraft.of("gameplay/crab_trap/jungle"));
    ;
}
