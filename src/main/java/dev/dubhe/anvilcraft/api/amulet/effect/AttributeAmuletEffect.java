package dev.dubhe.anvilcraft.api.amulet.effect;

import dev.dubhe.anvilcraft.api.amulet.ctx.AmuletEffectContext;
import dev.dubhe.anvilcraft.init.item.ModAmuletEffectContextKeys;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/// 在护符启用时给予佩戴者属性修饰符的护符效果
public record AttributeAmuletEffect(
    Holder<Attribute> attribute,
    ResourceLocation id,
    double amount,
    AttributeModifier.Operation operation
) implements IAmuletEffect {
    @Override
    public void trigger(LivingEntity entity, ItemStack amulet, AmuletEffectContext ctx) {
        Optional<Boolean> enabled = ctx.get(ModAmuletEffectContextKeys.ENABLED);
        if (enabled.isEmpty()) {
            return;
        }
        AttributeInstance instance = entity.getAttribute(this.attribute);
        if (instance == null) {
            return;
        }
        if (enabled.get()) {
            if (!instance.hasModifier(this.id)) {
                instance.addTransientModifier(new AttributeModifier(this.id, this.amount, this.operation));
            }
        } else {
            instance.removeModifier(this.id);
        }
    }
}
