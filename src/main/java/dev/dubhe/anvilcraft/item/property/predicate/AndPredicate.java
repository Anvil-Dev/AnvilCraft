package dev.dubhe.anvilcraft.item.property.predicate;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.util.CodecUtil;
import net.minecraft.advancements.critereon.ItemSubPredicate;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;

public record AndPredicate(List<Map.Entry<Type<?>, ItemSubPredicate>> subPredicates) implements ItemSubPredicate {
    public static final Codec<AndPredicate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        CodecUtil.<Type<?>, ItemSubPredicate, Map.Entry<Type<?>, ItemSubPredicate>>byMap(
                ItemSubPredicate.CODEC,
                Map.Entry::getKey,
                Map.Entry::getValue,
                Map::entry
            ).listOf()
            .optionalFieldOf("predicates", List.of())
            .forGetter(AndPredicate::subPredicates)
    ).apply(instance, AndPredicate::new));

    @Override
    public boolean matches(ItemStack itemStack) {
        return this.subPredicates().stream().allMatch(it -> it.getValue().matches(itemStack));
    }

    public static AndPredicate of(Type<?> type, ItemSubPredicate subPredicate) {
        return new AndPredicate(List.of(Map.entry(type, subPredicate)));
    }

    public AndPredicate with(Type<?> type, ItemSubPredicate subPredicate) {
        ImmutableList.Builder<Map.Entry<Type<?>, ItemSubPredicate>> builder = ImmutableList.builder();
        builder.addAll(this.subPredicates());
        builder.add(Map.entry(type, subPredicate));
        return new AndPredicate(builder.build());
    }
}
