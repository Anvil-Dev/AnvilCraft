package dev.dubhe.anvilcraft.loot.conditions;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.ModLootItemConditions;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

import java.util.Optional;

@MethodsReturnNonnullByDefault
public record MatchDataComponent(Optional<DataComponentPredicate> predicate) implements LootItemCondition {
    public static final MapCodec<MatchDataComponent> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
        DataComponentPredicate.CODEC.optionalFieldOf("predicate").forGetter(MatchDataComponent::predicate)
    ).apply(inst, MatchDataComponent::new));

    @Override
    public LootItemConditionType getType() {
        return ModLootItemConditions.MATCH_DATA_COMPONENT.get();
    }

    @Override
    public boolean test(LootContext context) {
        ItemStack tool;
        if (context.hasParam(LootContextParams.TOOL)) {
            tool = context.getParam(LootContextParams.TOOL);
        } else if (context.hasParam(LootContextParams.DIRECT_ATTACKING_ENTITY)) {
            tool = context.getParam(LootContextParams.DIRECT_ATTACKING_ENTITY).getWeaponItem();
        } else if (context.hasParam(LootContextParams.ATTACKING_ENTITY)) {
            tool = context.getParam(LootContextParams.ATTACKING_ENTITY).getWeaponItem();
        } else {
            return false;
        }

        return tool != null && (this.predicate.isEmpty() || this.predicate.get().test(tool));
    }

    public static LootItemCondition.Builder component(DataComponentPredicate.Builder componentBuilder) {
        return () -> new MatchDataComponent(Optional.of(componentBuilder.build()));
    }
}
