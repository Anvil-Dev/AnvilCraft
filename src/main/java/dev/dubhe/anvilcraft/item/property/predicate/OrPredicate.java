package dev.dubhe.anvilcraft.item.property.predicate;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.util.CodecUtil;
import net.minecraft.advancements.critereon.ItemSubPredicate;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;

public record OrPredicate(List<Map.Entry<Type<?>, ItemSubPredicate>> subPredicates) implements ItemSubPredicate {
    public static final Codec<OrPredicate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        CodecUtil.<Type<?>, ItemSubPredicate, Map.Entry<Type<?>, ItemSubPredicate>>byMap(
                ItemSubPredicate.CODEC,
                Map.Entry::getKey,
                Map.Entry::getValue,
                Map::entry
            ).listOf()
            .optionalFieldOf("predicates", List.of())
            .forGetter(OrPredicate::subPredicates)
    ).apply(instance, OrPredicate::new));

    @Override
    public boolean matches(ItemStack itemStack) {
        return this.subPredicates().stream().anyMatch(it -> it.getValue().matches(itemStack));
    }

    public static OrPredicate of(Type<?> type, ItemSubPredicate subPredicate) {
        return new OrPredicate(List.of(Map.entry(type, subPredicate)));
    }

    public OrPredicate with(Type<?> type, ItemSubPredicate subPredicate) {
        ImmutableList.Builder<Map.Entry<Type<?>, ItemSubPredicate>> builder = ImmutableList.builder();
        builder.addAll(this.subPredicates());
        builder.add(Map.entry(type, subPredicate));
        return new OrPredicate(builder.build());
    }
}
