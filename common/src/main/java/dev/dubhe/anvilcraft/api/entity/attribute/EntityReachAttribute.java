package dev.dubhe.anvilcraft.api.entity.attribute;

import com.google.common.collect.Multimap;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.function.Supplier;

public class EntityReachAttribute {

    @ExpectPlatform
    public static Supplier<Multimap<Attribute, AttributeModifier>> getRangeModifierSupplier(
            AttributeModifier modifier
    ) {
        return null;
    }
}
