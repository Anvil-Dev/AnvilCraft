package dev.dubhe.anvilcraft.listener.fabric;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModItems;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

public class LootTableListener {
    /**
     * 修改战利品表
     */
    public static void register() {
        LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) -> {
            if (Blocks.BUDDING_AMETHYST.getLootTable().equals(id)) {
                tableBuilder.withPool(new LootPool.Builder()
                        .add(LootItem.lootTableItem(ModItems.GEODE))
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1)))
                );
            }
            if (BuiltInLootTables.SPAWN_BONUS_CHEST.equals(id)) {
                tableBuilder.withPool(new LootPool.Builder()
                        .add(LootItem.lootTableItem(ModItems.GEODE))
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(3, 6)))
                );
            }
        });
    }
}
