package dev.dubhe.anvilcraft.data.generator.loot;

import com.tterrag.registrate.providers.loot.RegistrateLootTableProvider;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.init.ModLootContextParamSet;
import dev.dubhe.anvilcraft.init.ModLootTables;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LocationCheck;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

public class CrabTrapLootLoader {
    private static final LootItemCondition.Builder IN_RIVER =
        LocationCheck.checkLocation(LocationPredicate.Builder.location().setBiome(Biomes.RIVER))
            .or(LocationCheck.checkLocation(LocationPredicate.Builder.location().setBiome(Biomes.FROZEN_RIVER)));

    private static final LootItemCondition.Builder IN_OCEAN =
        LocationCheck.checkLocation(LocationPredicate.Builder.location().setBiome(Biomes.OCEAN))
            .or(LocationCheck.checkLocation(LocationPredicate.Builder.location().setBiome(Biomes.COLD_OCEAN)))
            .or(LocationCheck.checkLocation(LocationPredicate.Builder.location().setBiome(Biomes.DEEP_OCEAN)))
            .or(LocationCheck.checkLocation(LocationPredicate.Builder.location().setBiome(Biomes.DEEP_COLD_OCEAN)))
            .or(LocationCheck.checkLocation(LocationPredicate.Builder.location().setBiome(Biomes.FROZEN_OCEAN)))
            .or(LocationCheck.checkLocation(LocationPredicate.Builder.location().setBiome(Biomes.DEEP_FROZEN_OCEAN)));

    private static final LootItemCondition.Builder IN_WARM_OCEAN =
        LocationCheck.checkLocation(LocationPredicate.Builder.location().setBiome(Biomes.WARM_OCEAN))
            .or(LocationCheck.checkLocation(LocationPredicate.Builder.location().setBiome(Biomes.LUKEWARM_OCEAN)))
            .or(LocationCheck.checkLocation(LocationPredicate.Builder.location().setBiome(Biomes.DEEP_LUKEWARM_OCEAN)));

    private static final LootItemCondition.Builder IN_SWAMP =
        LocationCheck.checkLocation(LocationPredicate.Builder.location().setBiome(Biomes.SWAMP))
            .or(LocationCheck.checkLocation(LocationPredicate.Builder.location().setBiome(Biomes.MANGROVE_SWAMP)));

    private static final LootItemCondition.Builder IN_JUNGLE =
        LocationCheck.checkLocation(LocationPredicate.Builder.location().setBiome(Biomes.JUNGLE))
            .or(LocationCheck.checkLocation(LocationPredicate.Builder.location().setBiome(Biomes.BAMBOO_JUNGLE)))
            .or(LocationCheck.checkLocation(LocationPredicate.Builder.location().setBiome(Biomes.SPARSE_JUNGLE)));

    private static final LootTable.Builder COMMON = LootTable.lootTable().withPool(
        LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
            .add(LootItem.lootTableItem(ModItems.CRAB_CLAW).setWeight(1))
            .add(LootItem.lootTableItem(Items.SEAGRASS).setWeight(1))
            .when(LootItemRandomChanceCondition.randomChance(0.1f))
    );

    private static final LootTable.Builder RIVER = LootTable.lootTable().withPool(
        LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
            .add(LootItem.lootTableItem(Items.SALMON))
            .when(IN_RIVER)
            .when(LootItemRandomChanceCondition.randomChance(0.1f))
    );

    private static final LootTable.Builder OCEAN = LootTable.lootTable().withPool(
        LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
            .add(LootItem.lootTableItem(Items.COD))
            .add(LootItem.lootTableItem(Items.KELP))
            .add(LootItem.lootTableItem(Items.NAUTILUS_SHELL))
            .add(LootItem.lootTableItem(Items.INK_SAC))
            .when(IN_OCEAN)
            .when(LootItemRandomChanceCondition.randomChance(0.1f))
    );

    private static final LootTable.Builder WARM_OCEAN = LootTable.lootTable().withPool(
        LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
            .add(LootItem.lootTableItem(Items.TROPICAL_FISH))
            .add(LootItem.lootTableItem(Items.PUFFERFISH))
            .add(LootItem.lootTableItem(Items.INK_SAC))
            .when(IN_WARM_OCEAN)
            .when(LootItemRandomChanceCondition.randomChance(0.1f))
    );

    private static final LootTable.Builder SWAMP = LootTable.lootTable().withPool(
        LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
            .add(LootItem.lootTableItem(Items.LILY_PAD))
            .when(IN_SWAMP)
            .when(LootItemRandomChanceCondition.randomChance(0.1f))
    );

    private static final LootTable.Builder JUNGLE = LootTable.lootTable().withPool(
        LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
            .add(LootItem.lootTableItem(Items.BAMBOO))
            .add(LootItem.lootTableItem(Items.COCOA_BEANS))
            .when(IN_JUNGLE)
            .when(LootItemRandomChanceCondition.randomChance(0.1f))
    );

    /**
     * 初始化蟹笼相关的战利品表
     *
     * @param provider 提供器
     */
    public static void init(RegistrateLootTableProvider provider) {
        provider.addLootAction(ModLootContextParamSet.CRAB_TRAP, (bi) -> {
            bi.accept(ModLootTables.CRAB_TRAP_COMMON, COMMON);
            bi.accept(ModLootTables.CRAB_TRAP_RIVER, RIVER);
            bi.accept(ModLootTables.CRAB_TRAP_OCEAN, OCEAN);
            bi.accept(ModLootTables.CRAB_TRAP_WARM_OCEAN, WARM_OCEAN);
            bi.accept(ModLootTables.CRAB_TRAP_SWAMP, SWAMP);
            bi.accept(ModLootTables.CRAB_TRAP_JUNGLE, JUNGLE);
        });
    }
}
