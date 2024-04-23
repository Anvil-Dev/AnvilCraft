package dev.dubhe.anvilcraft.api.entity.attribute.fabric;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.function.Supplier;

public class EntityReachAttributeImpl {
    public static Supplier<Multimap<Attribute, AttributeModifier>> getRangeModifierSupplier(
            AttributeModifier modifier
    ) {
        return Suppliers.memoize(() ->
                ImmutableMultimap.of(
                        ReachEntityAttributes.REACH, modifier,
                        ReachEntityAttributes.ATTACK_RANGE, modifier
                )
        );
    }
}
