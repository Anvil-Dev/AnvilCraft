package dev.dubhe.anvilcraft.item.property.predicate;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.SavedEntity;
import dev.dubhe.anvilcraft.recipe.transform.NumericTagValuePredicate;
import net.minecraft.advancements.criterion.SingleComponentItemPredicate;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public record ItemSavedEntityPredicate(
    List<EntityType<?>> entities,
    List<NumericTagValuePredicate> predicates,
    boolean isMonster
) implements SingleComponentItemPredicate<SavedEntity> {
    public static final Codec<ItemSavedEntityPredicate> CODEC = RecordCodecBuilder.create(ins -> ins.group(
        EntityType.CODEC
            .listOf()
            .optionalFieldOf("entities", new ArrayList<>())
            .forGetter(ItemSavedEntityPredicate::entities),
        NumericTagValuePredicate.CODEC
            .listOf()
            .optionalFieldOf("predicates", new ArrayList<>())
            .forGetter(ItemSavedEntityPredicate::predicates),
        Codec.BOOL
            .optionalFieldOf("is_monster", false)
            .forGetter(ItemSavedEntityPredicate::isMonster)
    ).apply(ins, ItemSavedEntityPredicate::new));

    public static ItemSavedEntityPredicate of(EntityType<?> entityType) {
        return new ItemSavedEntityPredicate(Lists.newArrayList(entityType), new ArrayList<>(), false);
    }

    public static ItemSavedEntityPredicate any() {
        return new ItemSavedEntityPredicate(new ArrayList<>(), new ArrayList<>(), false);
    }

    public static ItemSavedEntityPredicate monster() {
        return new ItemSavedEntityPredicate(new ArrayList<>(), new ArrayList<>(), true);
    }

    public ItemSavedEntityPredicate predicate(Consumer<NumericTagValuePredicate.Builder> predicateBuilder) {
        NumericTagValuePredicate.Builder builder = NumericTagValuePredicate.builder();
        predicateBuilder.accept(builder);
        this.predicates.add(builder.build());
        return this;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean matches(SavedEntity value) {
        EntityType<?> type = value.type();
        if (this.isMonster && !value.isMonster()) return false;
        if (!this.entities.isEmpty()) {
            boolean matched = false;
            for (EntityType<?> entity : this.entities) {
                if (!type.builtInRegistryHolder().is(entity.builtInRegistryHolder())) continue;
                matched = true;
                break;
            }
            if (!matched) return false;
        }
        return this.predicates.stream().allMatch(it -> it.test(value.tag()));
    }

    @Override
    public DataComponentType<SavedEntity> componentType() {
        return ModComponents.SAVED_ENTITY;
    }
}
