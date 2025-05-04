package dev.dubhe.anvilcraft.data.provider.loot;

import com.mojang.datafixers.util.Unit;
import dev.dubhe.anvilcraft.init.ModComponents;
import dev.dubhe.anvilcraft.init.ModEnchantments;
import dev.dubhe.anvilcraft.init.ModLootTables;
import dev.dubhe.anvilcraft.loot.conditions.MatchDataComponent;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.FillPlayerHead;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.InvertedLootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemKilledByPlayerCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceWithEnchantedBonusCondition;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.BiConsumer;

@ParametersAreNonnullByDefault
public class BeheadingLootSubProvider implements LootTableSubProvider {
    private final HolderLookup.Provider provider;

    public BeheadingLootSubProvider(HolderLookup.Provider provider) {
        this.provider = provider;
    }

    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> consumer) {
        generateBeheading(consumer,
            ModLootTables.BEHEADING_WITHER_SKELETON,
            Items.WITHER_SKELETON_SKULL,
            0.07f,
            0.02f
        );
        generateBeheading(consumer,
            ModLootTables.BEHEADING_ZOMBIE,
            Items.ZOMBIE_HEAD,
            0.01f,
            0.01f
        );
        generateBeheading(consumer,
            ModLootTables.BEHEADING_SKELETON,
            Items.SKELETON_SKULL,
            0.01f,
            0.01f
        );
        generateBeheading(consumer,
            ModLootTables.BEHEADING_CREEPER,
            Items.CREEPER_HEAD,
            0.01f,
            0.01f
        );
        generateBeheading(consumer,
            ModLootTables.BEHEADING_PIGLIN,
            Items.PIGLIN_HEAD,
            0.01f,
            0.01f
        );
        generateBeheading(consumer,
            ModLootTables.BEHEADING_ENDER_DRAGON,
            Items.DRAGON_HEAD,
            1.0f,
            0.0f
        );
        generateBeheading(consumer,
            ModLootTables.BEHEADING_PLAYER,
            Items.PLAYER_HEAD,
            1.0f,
            0.0f,
            FillPlayerHead.fillPlayerHead(LootContext.EntityTarget.THIS)
        );
    }

    public void generateBeheading(
        BiConsumer<ResourceKey<LootTable>, LootTable.Builder> consumer,
        ResourceKey<LootTable> lootTableKey,
        ItemLike headItem,
        float baseChance,
        float bonusChancePerLevel
    ) {
        consumer.accept(lootTableKey, LootTable.lootTable()
            .withPool(LootPool.lootPool()
                .add(LootItem.lootTableItem(headItem))
                .when(() -> new LootItemRandomChanceWithEnchantedBonusCondition(0.0f,
                    LevelBasedValue.perLevel(baseChance, bonusChancePerLevel),
                    new DummyHolder(ModEnchantments.BEHEADING_KEY)))
                .when(LootItemKilledByPlayerCondition.killedByPlayer())
                .when(InvertedLootItemCondition.invert(MatchDataComponent.component(
                    DataComponentPredicate.builder().expect(ModComponents.MERCILESS, Unit.INSTANCE)
                )))
            )
        );
    }

    public void generateBeheading(
        BiConsumer<ResourceKey<LootTable>, LootTable.Builder> consumer,
        ResourceKey<LootTable> lootTableKey,
        ItemLike headItem,
        float baseChance,
        float bonusChancePerLevel,
        LootItemFunction.Builder extraFunction
    ) {
        consumer.accept(lootTableKey, LootTable.lootTable()
            .withPool(LootPool.lootPool()
                .add(LootItem.lootTableItem(headItem)
                    .apply(extraFunction))
                .when(() -> new LootItemRandomChanceWithEnchantedBonusCondition(0.0f,
                    LevelBasedValue.perLevel(baseChance, bonusChancePerLevel),
                    new DummyHolder(ModEnchantments.BEHEADING_KEY)))
                .when(LootItemKilledByPlayerCondition.killedByPlayer())
                .when(InvertedLootItemCondition.invert(MatchDataComponent.component(
                    DataComponentPredicate.builder().expect(ModComponents.MERCILESS, Unit.INSTANCE)
                )))
            )
        );
    }

    private static class DummyHolder extends Holder.Reference<Enchantment> {

        @SuppressWarnings("DataFlowIssue")
        protected DummyHolder(@Nullable ResourceKey<Enchantment> key) {
            super(Type.STAND_ALONE, null, key, null);
        }

        @Override
        public boolean canSerializeIn(HolderOwner<Enchantment> owner) {
            return true;
        }
    }
}
