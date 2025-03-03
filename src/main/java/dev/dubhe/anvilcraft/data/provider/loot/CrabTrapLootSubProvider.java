package dev.dubhe.anvilcraft.data.provider.loot;

import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.init.ModLootTables;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LocationCheck;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.BiConsumer;

@ParametersAreNonnullByDefault
public class CrabTrapLootSubProvider implements LootTableSubProvider {
    private final HolderLookup.Provider provider;

    public CrabTrapLootSubProvider(HolderLookup.Provider provider) {
        this.provider = provider;
    }

    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> consumer) {
        HolderLookup.RegistryLookup<Biome> biomeRegistryLookup = this.provider.lookupOrThrow(Registries.BIOME);
        consumer.accept(
            ModLootTables.CRAB_TRAP_COMMON,
            LootTable.lootTable()
                .withPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(ModItems.CRAB_CLAW).setWeight(1))
                    .add(LootItem.lootTableItem(Items.SEAGRASS).setWeight(15))
                    .when(LootItemRandomChanceCondition.randomChance(0.1f))));

        consumer.accept(
            ModLootTables.CRAB_TRAP_RIVER,
            LootTable.lootTable()
                .withPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.SALMON))
                    .when(LocationCheck.checkLocation(LocationPredicate.Builder.location()
                        .setBiomes(HolderSet.direct(
                            biomeRegistryLookup.getOrThrow(Biomes.RIVER),
                            biomeRegistryLookup.getOrThrow(Biomes.FROZEN_RIVER)))))
                    .when(LootItemRandomChanceCondition.randomChance(0.1f))));

        consumer.accept(
            ModLootTables.CRAB_TRAP_OCEAN,
            LootTable.lootTable()
                .withPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.COD))
                    .add(LootItem.lootTableItem(Items.KELP))
                    .add(LootItem.lootTableItem(Items.NAUTILUS_SHELL))
                    .add(LootItem.lootTableItem(Items.INK_SAC))
                    .when(LocationCheck.checkLocation(LocationPredicate.Builder.location()
                        .setBiomes(HolderSet.direct(
                            biomeRegistryLookup.getOrThrow(Biomes.OCEAN),
                            biomeRegistryLookup.getOrThrow(Biomes.COLD_OCEAN),
                            biomeRegistryLookup.getOrThrow(Biomes.DEEP_OCEAN),
                            biomeRegistryLookup.getOrThrow(Biomes.DEEP_COLD_OCEAN),
                            biomeRegistryLookup.getOrThrow(Biomes.FROZEN_OCEAN),
                            biomeRegistryLookup.getOrThrow(Biomes.DEEP_FROZEN_OCEAN)))))
                    .when(LootItemRandomChanceCondition.randomChance(0.1f))));

        consumer.accept(
            ModLootTables.CRAB_TRAP_WARM_OCEAN,
            LootTable.lootTable()
                .withPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.TROPICAL_FISH))
                    .add(LootItem.lootTableItem(Items.PUFFERFISH))
                    .add(LootItem.lootTableItem(Items.INK_SAC))
                    .when(LocationCheck.checkLocation(LocationPredicate.Builder.location()
                        .setBiomes(HolderSet.direct(
                            biomeRegistryLookup.getOrThrow(Biomes.WARM_OCEAN),
                            biomeRegistryLookup.getOrThrow(Biomes.LUKEWARM_OCEAN),
                            biomeRegistryLookup.getOrThrow(Biomes.DEEP_LUKEWARM_OCEAN)))))
                    .when(LootItemRandomChanceCondition.randomChance(0.1f))));

        consumer.accept(
            ModLootTables.CRAB_TRAP_SWAMP,
            LootTable.lootTable()
                .withPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.LILY_PAD))
                    .when(LocationCheck.checkLocation(LocationPredicate.Builder.location()
                        .setBiomes(HolderSet.direct(
                            biomeRegistryLookup.getOrThrow(Biomes.SWAMP),
                            biomeRegistryLookup.getOrThrow(Biomes.MANGROVE_SWAMP)))))
                    .when(LootItemRandomChanceCondition.randomChance(0.1f))));

        consumer.accept(
            ModLootTables.CRAB_TRAP_JUNGLE,
            LootTable.lootTable()
                .withPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.BAMBOO))
                    .add(LootItem.lootTableItem(Items.COCOA_BEANS))
                    .when(LocationCheck.checkLocation(LocationPredicate.Builder.location()
                        .setBiomes(HolderSet.direct(
                            biomeRegistryLookup.getOrThrow(Biomes.JUNGLE),
                            biomeRegistryLookup.getOrThrow(Biomes.BAMBOO_JUNGLE),
                            biomeRegistryLookup.getOrThrow(Biomes.SPARSE_JUNGLE)))))
                    .when(LootItemRandomChanceCondition.randomChance(0.1f))));
    }
}
