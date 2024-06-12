package dev.dubhe.anvilcraft.api.entity.attribute;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import com.google.common.collect.Multimap;
import dev.architectury.injectables.annotations.ExpectPlatform;

import java.util.function.Supplier;

public class EntityReachAttribute {

    @ExpectPlatform
    public static Supplier<Multimap<Attribute, AttributeModifier>> getRangeModifierSupplier(
            AttributeModifier modifier) {
        return null;
    }

    @ExpectPlatform
    public static Attribute getReachAttribute() {
        return null;
    }
}
