package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.loot.conditions.MatchDataComponent;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModLootItemConditions {
    public static final DeferredRegister<LootItemConditionType> LOOT_CONDITION_TYPES =
        DeferredRegister.create(Registries.LOOT_CONDITION_TYPE, AnvilCraft.MOD_ID);

    public static final Supplier<LootItemConditionType> MATCH_DATA_COMPONENT =
        LOOT_CONDITION_TYPES.register("match_data_component", () -> new LootItemConditionType(MatchDataComponent.CODEC));
}
