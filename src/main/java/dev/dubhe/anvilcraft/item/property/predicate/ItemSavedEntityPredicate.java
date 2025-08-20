package dev.dubhe.anvilcraft.item.property.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.SavedEntity;
import dev.dubhe.anvilcraft.recipe.transform.NumericTagValuePredicate;
import dev.dubhe.anvilcraft.util.CodecUtil;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.advancements.critereon.SingleComponentItemPredicate;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public record ItemSavedEntityPredicate(
    EntityType<?> entityType,
    List<NumericTagValuePredicate> predicates
) implements SingleComponentItemPredicate<SavedEntity> {
    public static final Codec<ItemSavedEntityPredicate> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            CodecUtil.ENTITY_CODEC.fieldOf("entityType").forGetter(o -> o.entityType),
            NumericTagValuePredicate.CODEC
                .listOf()
                .optionalFieldOf("tagPredicates")
                .forGetter(o -> Util.intoOptional(o.predicates))
        )
        .apply(ins, ItemSavedEntityPredicate::new));

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public ItemSavedEntityPredicate(EntityType<?> entityType, Optional<List<NumericTagValuePredicate>> predicates) {
        this(entityType, predicates.orElseGet(ArrayList::new));
    }

    @Contract("_ -> new")
    public static ItemSavedEntityPredicate of(EntityType<?> entityType) {
        return new ItemSavedEntityPredicate(entityType, new ArrayList<>());
    }

    public ItemSavedEntityPredicate predicate(Consumer<NumericTagValuePredicate.Builder> predicateBuilder) {
        NumericTagValuePredicate.Builder builder = NumericTagValuePredicate.builder();
        predicateBuilder.accept(builder);
        predicates.add(builder.build());
        return this;
    }

    @Override
    public boolean matches(ItemStack itemStack, SavedEntity component) {
        CompoundTag tag = component.tag();
        Optional<EntityType<?>> optional = EntityType.by(tag);
        if (optional.isEmpty()) return false;
        EntityType<?> type = optional.get();
        if (!type.equals(this.entityType)) return false;
        return predicates.stream().allMatch(it -> it.test(tag));
    }

    @Override
    public DataComponentType<SavedEntity> componentType() {
        return ModComponents.SAVED_ENTITY;
    }
}
