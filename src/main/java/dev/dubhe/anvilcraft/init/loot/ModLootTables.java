package dev.dubhe.anvilcraft.init.loot;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.HashMap;
import java.util.Map;

public class ModLootTables {
    public static final ResourceKey<LootTable> CRAB_TRAP_COMMON = ModLootTables.key("gameplay/crab_trap/common");
    public static final ResourceKey<LootTable> CRAB_TRAP_RIVER = ModLootTables.key("gameplay/crab_trap/river");
    public static final ResourceKey<LootTable> CRAB_TRAP_OCEAN = ModLootTables.key("gameplay/crab_trap/ocean");
    public static final ResourceKey<LootTable> CRAB_TRAP_WARM_OCEAN = ModLootTables.key("gameplay/crab_trap/warm_ocean");
    public static final ResourceKey<LootTable> CRAB_TRAP_SWAMP = ModLootTables.key("gameplay/crab_trap/swamp");
    public static final ResourceKey<LootTable> CRAB_TRAP_JUNGLE = ModLootTables.key("gameplay/crab_trap/jungle");

    public static final ResourceKey<LootTable> ADVANCEMENT_ROOT = ModLootTables.key("advancement/root");

    public static final Map<EntityType<?>, LootTable> BEHEADING_LOOT = new HashMap<>();

    public static final ResourceKey<LootTable> BEHEADING_WITHER_SKELETON = ModLootTables.beheadingKey(EntityType.WITHER_SKELETON);
    public static final ResourceKey<LootTable> BEHEADING_ZOMBIE = ModLootTables.beheadingKey(EntityType.ZOMBIE);
    public static final ResourceKey<LootTable> BEHEADING_SKELETON = ModLootTables.beheadingKey(EntityType.SKELETON);
    public static final ResourceKey<LootTable> BEHEADING_CREEPER = ModLootTables.beheadingKey(EntityType.CREEPER);
    public static final ResourceKey<LootTable> BEHEADING_PIGLIN = ModLootTables.beheadingKey(EntityType.PIGLIN);
    public static final ResourceKey<LootTable> BEHEADING_ENDER_DRAGON = ModLootTables.beheadingKey(EntityType.ENDER_DRAGON);
    public static final ResourceKey<LootTable> BEHEADING_PLAYER = ModLootTables.beheadingKey(EntityType.PLAYER);

    private static ResourceKey<LootTable> key(String path) {
        return ResourceKey.create(Registries.LOOT_TABLE, AnvilCraft.of(path));
    }

    private static ResourceKey<LootTable> beheadingKey(EntityType<?> entityType) {
        Identifier entityId = EntityType.getKey(entityType);
        return ModLootTables.key("entities/beheading/" + entityId.getNamespace() + '/' + entityId.getPath());
    }

    public static LootTable getBeheadingLoot(Entity entity) {
        MinecraftServer server = entity.level().getServer();
        if (server == null) return LootTable.EMPTY;
        EntityType<?> entityType = entity.getType();
        return ModLootTables.BEHEADING_LOOT.computeIfAbsent(entityType,
            e -> server.reloadableRegistries().getLootTable(ModLootTables.beheadingKey(entityType)));
    }
}
