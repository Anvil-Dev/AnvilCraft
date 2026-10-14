package dev.dubhe.anvilcraft.item.property.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.SavedEntity;
import dev.dubhe.anvilcraft.recipe.transform.NumericTagValuePredicate;
import net.minecraft.advancements.critereon.SingleComponentItemPredicate;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public record ItemSavedEntityPredicate(
    Optional<HolderSet<EntityType<?>>> entitys,
    List<NumericTagValuePredicate> predicates,
    boolean isMonster
) implements SingleComponentItemPredicate<SavedEntity> {
    public static final Codec<ItemSavedEntityPredicate> CODEC = RecordCodecBuilder.create(ins -> ins.group(
        RegistryCodecs.homogeneousList(Registries.ENTITY_TYPE)
            .optionalFieldOf("entitys")
            .forGetter(ItemSavedEntityPredicate::entitys),
        NumericTagValuePredicate.CODEC
            .listOf()
            .optionalFieldOf("predicates", new ArrayList<>())
            .forGetter(ItemSavedEntityPredicate::predicates),
        Codec.BOOL
            .optionalFieldOf("is_monster", false)
            .forGetter(ItemSavedEntityPredicate::isMonster)
    ).apply(ins, ItemSavedEntityPredicate::new));

    @SuppressWarnings("deprecation")
    public static ItemSavedEntityPredicate of(EntityType<?> entityType) {
        return new ItemSavedEntityPredicate(Optional.of(HolderSet.direct(entityType.builtInRegistryHolder())), new ArrayList<>(), false);
    }

    public static ItemSavedEntityPredicate any() {
        return new ItemSavedEntityPredicate(Optional.empty(), new ArrayList<>(), false);
    }

    public static ItemSavedEntityPredicate monster() {
        return new ItemSavedEntityPredicate(Optional.empty(), new ArrayList<>(), true);
    }

    public ItemSavedEntityPredicate predicate(Consumer<NumericTagValuePredicate.Builder> predicateBuilder) {
        NumericTagValuePredicate.Builder builder = NumericTagValuePredicate.builder();
        predicateBuilder.accept(builder);
        this.predicates.add(builder.build());
        return this;
    }

    @Override
    public boolean matches(ItemStack itemStack, SavedEntity component) {
        CompoundTag tag = component.tag();
        Optional<EntityType<?>> optional = EntityType.by(tag);
        if (optional.isEmpty()) return false;
        EntityType<?> type = optional.get();
        if (this.entitys.isPresent() && !type.is(this.entitys.get())) return false;
        if (this.isMonster && !component.isMonster()) return false;
        return this.predicates.stream().allMatch(it -> it.test(tag));
    }

    @Override
    public DataComponentType<SavedEntity> componentType() {
        return ModComponents.SAVED_ENTITY;
    }
}
