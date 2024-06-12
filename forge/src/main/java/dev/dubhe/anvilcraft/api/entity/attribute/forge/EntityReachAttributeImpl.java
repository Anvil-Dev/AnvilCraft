package dev.dubhe.anvilcraft.api.entity.attribute.forge;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.common.ForgeMod;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

import java.util.function.Supplier;

public class EntityReachAttributeImpl {
    public static Supplier<Multimap<Attribute, AttributeModifier>> getRangeModifierSupplier(
            AttributeModifier modifier) {
        return Suppliers.memoize(() -> ImmutableMultimap.of(ForgeMod.BLOCK_REACH.get(), modifier));
    }

    public static Attribute getReachAttribute() {
        return ForgeMod.BLOCK_REACH.get();
    }
}
