package dev.dubhe.anvilcraft.api.entity.attribute;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.function.Supplier;

public class EntityReachAttribute {

    public static Supplier<Multimap<Holder<Attribute>, AttributeModifier>> getRangeModifierSupplier(
        AttributeModifier modifier
    ) {
        @SuppressWarnings("NullableProblems")
        Supplier<Multimap<Holder<Attribute>, AttributeModifier>> supplier = Suppliers.memoize(
            () -> ImmutableMultimap.of(
                Attributes.BLOCK_INTERACTION_RANGE, modifier,
                Attributes.ENTITY_INTERACTION_RANGE, modifier
            )
        );
        return supplier;
    }

    public static Attribute getReachAttribute() {
        return Attributes.BLOCK_INTERACTION_RANGE.value();
    }
}
