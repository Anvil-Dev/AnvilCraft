package dev.dubhe.anvilcraft.data.generator.loot;

import dev.anvilcraft.lib.data.provider.RegistratorLootTableProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import org.jetbrains.annotations.NotNull;

public class AdvancementLootLoader {
    private static final LootTable.Builder ROOT = LootTable.lootTable()
        .withPool(
            LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0f))
                .add(LootItem.lootTableItem(ModItems.GUIDE_BOOK))
        );

    /**
     * @param provider 提供器
     */
    public static void init(@NotNull RegistratorLootTableProvider provider) {
        provider.addLootAction(LootContextParamSets.ENTITY, (bi) -> {
            bi.accept(AnvilCraft.of("advancement/root"), ROOT);
        });
    }
}
