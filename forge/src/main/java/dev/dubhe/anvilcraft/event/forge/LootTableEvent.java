package dev.dubhe.anvilcraft.event.forge;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class LootTableEvent {
    /**
     * 战利品表加载事件侦听器
     *
     * @param event 战利品表加载事件
     */
    @SubscribeEvent
    public static void lootTable(@NotNull LootTableLoadEvent event) {
        ResourceLocation id = event.getName();
        LootTable table = event.getTable();
        if (Blocks.BUDDING_AMETHYST.getLootTable().equals(id)) {
            table.addPool(new LootPool.Builder()
                .add(LootItem.lootTableItem(ModItems.GEODE))
                .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1)))
                .build()
            );
        }
        if (BuiltInLootTables.SPAWN_BONUS_CHEST.equals(id)) {
            table.addPool(new LootPool.Builder()
                .add(LootItem.lootTableItem(ModItems.GEODE))
                .apply(SetItemCountFunction.setCount(UniformGenerator.between(3, 6)))
                .build()
            );
        }
        if (BuiltInLootTables.VILLAGE_WEAPONSMITH.equals(id)) {
            table.addPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE)
                    .setWeight(2)
                    .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))))
                .add(LootItem.lootTableItem(ModItems.ROYAL_STEEL_INGOT)
                    .setWeight(3)
                    .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1))))
                .add(LootItem.lootTableItem(ModItems.ROYAL_STEEL_NUGGET)
                    .setWeight(10)
                    .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 5))))
                .build());
        }
        event.setTable(table);
    }
}
