package dev.dubhe.anvilcraft.loot.conditions;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.predicates.DataComponentPredicate;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.HashMap;
import java.util.Map;

public record MatchDataComponent(Map<DataComponentPredicate.Type<?>, DataComponentPredicate> predicates) implements LootItemCondition {
    public static final MapCodec<MatchDataComponent> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
        DataComponentPredicate.CODEC
            .optionalFieldOf("predicates", new HashMap<>())
            .forGetter(MatchDataComponent::predicates)
    ).apply(inst, MatchDataComponent::new));

    @Override
    public MapCodec<? extends LootItemCondition> codec() {
        return MatchDataComponent.CODEC;
    }

    @Override
    public boolean test(LootContext context) {
        ItemInstance tool;
        if (context.hasParameter(LootContextParams.TOOL)) {
            tool = context.getParameter(LootContextParams.TOOL);
        } else if (context.hasParameter(LootContextParams.DIRECT_ATTACKING_ENTITY)) {
            tool = context.getParameter(LootContextParams.DIRECT_ATTACKING_ENTITY).getWeaponItem();
        } else if (context.hasParameter(LootContextParams.ATTACKING_ENTITY)) {
            tool = context.getParameter(LootContextParams.ATTACKING_ENTITY).getWeaponItem();
        } else {
            return false;
        }
        if (tool == null) return false;
        if (this.predicates.isEmpty()) return true;
        for (DataComponentPredicate predicate : this.predicates.values()) {
            if (!predicate.matches(tool)) return false;
        }
        return true;
    }

    public static LootItemCondition.Builder component(DataComponentPredicate.Type<?> type, DataComponentPredicate predicate) {
        return () -> new MatchDataComponent(new HashMap<>(Map.of(type, predicate)));
    }

    public static LootItemCondition.Builder component(Map<DataComponentPredicate.Type<?>, DataComponentPredicate> predicates) {
        return () -> new MatchDataComponent(new HashMap<>(predicates));
    }
}
